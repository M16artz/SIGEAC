package ec.edu.unl.sec.selfassessment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ec.edu.unl.sec.evaluationmodel.application.ModelCatalogPort;
import ec.edu.unl.sec.identityaccess.application.AuthorizationPort;
import ec.edu.unl.sec.identityaccess.application.CurrentActorPort;
import ec.edu.unl.sec.identityaccess.application.FunctionalAssignmentPolicyPort;
import ec.edu.unl.sec.identityaccess.application.InstitutionalAuthorityPort;
import ec.edu.unl.sec.selfassessment.domain.ProcessAssignment;
import ec.edu.unl.sec.selfassessment.domain.ProcessState;
import ec.edu.unl.sec.selfassessment.domain.SelfAssessmentProcess;
import ec.edu.unl.sec.selfassessment.support.InMemorySelfAssessmentRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProcessPlanningServiceTest {
    private InMemorySelfAssessmentRepository repo;
    private ProcessPlanningService service;
    private UUID actor;
    private UUID indicator;
    private UUID assignmentId;
    private SelfAssessmentProcess process;

    @BeforeEach
    void setUp() {
        repo = new InMemorySelfAssessmentRepository();
        actor = UUID.randomUUID();
        indicator = UUID.randomUUID();
        process = new SelfAssessmentProcess(null, UUID.randomUUID(), "P-PLAN", "Planificacion",
                LocalDate.now(), LocalDate.now().plusDays(10), UUID.randomUUID(), 1, ProcessState.ACTIVE, 1,
                actor, Instant.now(), List.of(new SelfAssessmentProcess.IndicatorSnapshot(indicator, "IND", "Indicador")));
        repo.save(process, -1);
        var auth = mock(AuthorizationPort.class);
        when(auth.isAllowed(any(), any(), any())).thenReturn(true);
        var authority = mock(InstitutionalAuthorityPort.class);
        when(authority.isAuthorizedAssignment(any(), any(), any(), any())).thenReturn(true);
        var policy = mock(FunctionalAssignmentPolicyPort.class);
        when(policy.evaluate(any(), any(), any(), any(), any())).thenReturn(FunctionalAssignmentPolicyPort.Decision.allow());
        service = new ProcessPlanningService(repo, mock(ModelCatalogPort.class), () -> Optional.of(
                new CurrentActorPort.Actor(actor, false)), auth, authority, policy, mock(SelfAssessmentAuditService.class));
        assignmentId = service.assignParticipant(process.id(), 1, indicator, actor, null,
                ProcessAssignment.Function.RESPONSIBLE, now(), now().plus(Duration.ofDays(5)),
                "Asignacion sintetica", UUID.randomUUID()).value().id();
    }

    @Test
    void createsTeamsWithIndividualMembershipAndOverlapControl() {
        var created = service.createTeam(process.id(), repo.find(process.id()).orElseThrow().revision(),
                "Comision", "EVALUATION", now(), now().plus(Duration.ofDays(5)), UUID.randomUUID());
        assertThat(created.value().name()).isEqualTo("Comision");
        var member = service.addMember(created.value().id(), created.processRevision(), actor, UUID.randomUUID(),
                now(), now().plus(Duration.ofDays(2)), UUID.randomUUID());
        assertThat(service.memberships(created.value().id())).containsExactly(member.value());
        assertThatThrownBy(() -> service.addMember(created.value().id(), member.processRevision(), actor,
                UUID.randomUUID(), now(), now().plus(Duration.ofDays(3)), UUID.randomUUID()))
                .isInstanceOf(ProcessProblemException.class).extracting("code").isEqualTo("MEMBERSHIP_OVERLAP");
    }

    @Test
    void plansActivitiesWithDependenciesAndPreservesRevisionHistory() {
        long revision = repo.find(process.id()).orElseThrow().revision();
        var first = service.createActivity(process.id(), revision, input("Recolectar", now().plus(Duration.ofDays(2)),
                List.of(), null), UUID.randomUUID());
        var second = service.createActivity(process.id(), first.processRevision(), input("Revisar",
                now().plus(Duration.ofDays(3)), List.of(first.value().id()), null), UUID.randomUUID());
        assertThat(second.value().dependencyIds()).containsExactly(first.value().id());
        var updated = service.updateActivity(second.value().id(), second.processRevision(),
                input("Revisar corregida", now().plus(Duration.ofDays(4)), List.of(first.value().id()), null),
                UUID.randomUUID());
        assertThat(updated.value().revision()).isEqualTo(1);
        assertThat(updated.value().dueAt()).isAfter(second.value().dueAt());
    }

    @Test
    void requiresAnAuthorizedExceptionForDeadlinesOutsideThePeriod() {
        long revision = repo.find(process.id()).orElseThrow().revision();
        Instant outside = now().plus(Duration.ofDays(30));
        assertThatThrownBy(() -> service.createActivity(process.id(), revision,
                input("Fuera de plazo", outside, List.of(), null), UUID.randomUUID()))
                .isInstanceOf(ProcessProblemException.class).extracting("code")
                .isEqualTo("DEADLINE_EXCEPTION_REQUIRED");
        var accepted = service.createActivity(process.id(), revision,
                input("Fuera de plazo", outside, List.of(), UUID.randomUUID()), UUID.randomUUID());
        assertThat(accepted.value().hasDeadlineException()).isTrue();
    }

    private ProcessPlanningService.ActivityInput input(String title, Instant dueAt, List<UUID> dependencies,
            UUID exceptionAuthority) {
        return new ProcessPlanningService.ActivityInput(indicator, null, assignmentId, title, dueAt, dependencies,
                exceptionAuthority == null ? null : "Excepcion sintetica de plazo",
                exceptionAuthority, exceptionAuthority == null ? null : "Designacion sintetica");
    }

    private Instant now() { return Instant.now(); }
}
