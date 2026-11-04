package ec.edu.unl.sec.selfassessment.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import ec.edu.unl.sec.identityaccess.application.*;
import ec.edu.unl.sec.identityaccess.domain.OperationCode;
import ec.edu.unl.sec.selfassessment.application.*;
import ec.edu.unl.sec.selfassessment.domain.*;
import ec.edu.unl.sec.selfassessment.support.InMemorySelfAssessmentRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TeamAssignmentAccessTest {
    private final UUID user = UUID.randomUUID(), indicator = UUID.randomUUID(), role = UUID.randomUUID();
    private final Instant now = Instant.now(), from = now.minusSeconds(60), until = now.plusSeconds(3600);
    private InMemorySelfAssessmentRepository repository;
    private FunctionalAssignmentPolicyPort policy;
    private ProcessAssignmentCatalogAdapter catalog;
    private SelfAssessmentProcess process;
    private ProcessTeam team;
    private ProcessAssignment assignment;

    @BeforeEach void setup() {
        repository = new InMemorySelfAssessmentRepository();
        policy = mock(FunctionalAssignmentPolicyPort.class);
        when(policy.evaluateMembership(any(), any(), any(), any(), any())).thenReturn(FunctionalAssignmentPolicyPort.Decision.allow());
        catalog = new ProcessAssignmentCatalogAdapter(repository, policy);
        process = new SelfAssessmentProcess(null, UUID.randomUUID(), "P", "Proceso", LocalDate.now(),
                LocalDate.now().plusDays(1), UUID.randomUUID(), 1, ProcessState.ACTIVE, 1, user, now,
                List.of(new SelfAssessmentProcess.IndicatorSnapshot(indicator, "I", "Indicador")));
        repository.save(process, -1);
        team = new ProcessTeam(null, process.id(), "Equipo", "COLLECTION", from, until, user, now, 0);
        repository.saveTeam(team);
        repository.saveMembership(new TeamMembership(null, team.id(), user, role, from, until, user, now));
        assignment = new ProcessAssignment(null, process.id(), indicator, null, team.id(),
                ProcessAssignment.Function.RESPONSIBLE, from, until, user, "Designacion", null, 0);
        repository.saveAssignment(assignment);
    }

    @Test void recognizesOnlyCurrentMembersForTheAssignedIndicatorAndFunction() {
        assertThat(catalog.hasCurrentAssignment(process.id(), indicator, user, "RESPONSIBLE", now)).isTrue();
        assertThat(catalog.hasCurrentAssignment(process.id(), indicator, UUID.randomUUID(), "RESPONSIBLE", now)).isFalse();
        assertThat(catalog.hasCurrentAssignment(process.id(), UUID.randomUUID(), user, "RESPONSIBLE", now)).isFalse();
        assertThat(catalog.hasCurrentAssignment(process.id(), indicator, user, "REVIEWER", now)).isFalse();
        assertThat(catalog.isCurrentParticipant(assignment, user, from.minusNanos(1))).isFalse();
        assertThat(catalog.isCurrentParticipant(assignment, user, from)).isTrue();
        assertThat(catalog.isCurrentParticipant(assignment, user, until)).isFalse();
    }

    @Test void rechecksRoleEligibilityOnEveryAccess() {
        assertThat(catalog.isCurrentParticipant(assignment, user, now)).isTrue();
        when(policy.evaluateMembership(any(), any(), any(), any(), any()))
                .thenReturn(FunctionalAssignmentPolicyPort.Decision.deny("MEMBERSHIP_ROLE_INVALID"));
        assertThat(catalog.isCurrentParticipant(assignment, user, now)).isFalse();
    }

    @Test void rejectsExpiredMembershipsRevokedAssignmentsAndTeamsFromAnotherProcess() {
        repository.memberships.clear();
        repository.saveMembership(new TeamMembership(null, team.id(), user, role, from, now, user, from));
        assertThat(catalog.isCurrentParticipant(assignment, user, now)).isFalse();
        repository.saveMembership(new TeamMembership(null, team.id(), user, role, from, until, user, from));
        var revoked = new ProcessAssignment(assignment.id(), process.id(), indicator, null, team.id(),
                assignment.function(), from, until, user, "Revocacion", now, 1);
        assertThat(catalog.isCurrentParticipant(revoked, user, now)).isFalse();
        repository.saveTeam(new ProcessTeam(team.id(), UUID.randomUUID(), team.name(), team.processRole(),
                from, until, user, from, 0));
        assertThat(catalog.isCurrentParticipant(assignment, user, now)).isFalse();
    }

    @Test void listsOnlyTheMembersAssignmentsAndRequiresIndependentOperationPermission() {
        var authorization = mock(AuthorizationPort.class);
        when(authorization.isAllowed(any(), any(), any())).thenAnswer(call ->
                OperationCode.EVIDENCE_COLLECT.equals(call.getArgument(1)));
        var service = new ProcessAssignmentService(repository, () -> Optional.of(new CurrentActorPort.Actor(user, false)),
                authorization, policy, mock(SelfAssessmentAuditService.class), catalog);
        repository.saveAssignment(new ProcessAssignment(null, process.id(), indicator, UUID.randomUUID(),
                assignment.function(), from, until, user, "Otro responsable", null, 0));
        assertThat(service.list(process.id())).containsExactly(assignment);
        when(authorization.isAllowed(any(), any(), any())).thenReturn(false);
        assertThat(service.list(process.id())).isEmpty();
    }
}
