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

class TeamPlanningIntegrityTest {
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
        process = new SelfAssessmentProcess(null, UUID.randomUUID(), "P-INTEGRITY", "Integridad",
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
    void requiresExactlyOneIndividualOrTeamPerAssignment() {
        long revision = repo.find(process.id()).orElseThrow().revision();
        assertThatThrownBy(() -> service.assignParticipant(process.id(), revision, indicator, null, null,
                ProcessAssignment.Function.RESPONSIBLE, now(), now().plus(Duration.ofDays(1)), "Sin sujeto",
                UUID.randomUUID()))
                .isInstanceOf(ProcessProblemException.class).extracting("code")
                .isEqualTo("ASSIGNMENT_SUBJECT_REQUIRED");
        UUID team = service.createTeam(process.id(), revision, "Equipo", "COLLECTION", now(),
                now().plus(Duration.ofDays(5)), UUID.randomUUID()).value().id();
        assertThatThrownBy(() -> service.assignParticipant(process.id(), revision + 1, indicator, actor, team,
                ProcessAssignment.Function.RESPONSIBLE, now(), now().plus(Duration.ofDays(1)), "Ambos sujetos",
                UUID.randomUUID()))
                .isInstanceOf(ProcessProblemException.class).extracting("code")
                .isEqualTo("ASSIGNMENT_SUBJECT_REQUIRED");
    }

    @Test
    void rejectsDependencyCyclesWithoutChangingThePreviousPlan() {
        long revision = repo.find(process.id()).orElseThrow().revision();
        var first = service.createActivity(process.id(), revision, input("Primera", now().plus(Duration.ofDays(2)),
                List.of()), UUID.randomUUID());
        var second = service.createActivity(process.id(), first.processRevision(), input("Segunda",
                now().plus(Duration.ofDays(3)), List.of(first.value().id())), UUID.randomUUID());
        assertThatThrownBy(() -> service.updateActivity(first.value().id(), second.processRevision(),
                input("Primera ciclica", now().plus(Duration.ofDays(4)), List.of(second.value().id())),
                UUID.randomUUID()))
                .isInstanceOf(ProcessProblemException.class).extracting("code").isEqualTo("DEPENDENCY_CYCLE");
        assertThat(repo.findActivity(first.value().id()).orElseThrow().dependencyIds()).isEmpty();
    }

    @Test
    void rejectsDependenciesFromAnotherProcessAndOverlappingTeamMembership() {
        long revision = repo.find(process.id()).orElseThrow().revision();
        var team = service.createTeam(process.id(), revision, "Equipo", "REVIEW", now(),
                now().plus(Duration.ofDays(5)), UUID.randomUUID());
        service.addMember(team.value().id(), team.processRevision(), actor, UUID.randomUUID(), now(),
                now().plus(Duration.ofDays(2)), UUID.randomUUID());
        assertThatThrownBy(() -> service.addMember(team.value().id(), team.processRevision() + 1, actor,
                UUID.randomUUID(), now(), now().plus(Duration.ofDays(1)), UUID.randomUUID()))
                .isInstanceOf(ProcessProblemException.class).extracting("code").isEqualTo("MEMBERSHIP_OVERLAP");
        assertThatThrownBy(() -> service.createActivity(process.id(), team.processRevision() + 1,
                input("Dependencia externa", now().plus(Duration.ofDays(2)), List.of(UUID.randomUUID())),
                UUID.randomUUID()))
                .isInstanceOf(ProcessProblemException.class).extracting("code")
                .isEqualTo("DEPENDENCY_OUTSIDE_PROCESS");
    }

    private ProcessPlanningService.ActivityInput input(String title, Instant dueAt, List<UUID> dependencies) {
        return new ProcessPlanningService.ActivityInput(indicator, null, assignmentId, title, dueAt, dependencies,
                null, null, null);
    }

    private Instant now() { return Instant.now(); }
}
