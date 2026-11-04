package ec.edu.unl.sec.improvement.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import ec.edu.unl.sec.audit.application.AuditPort;
import ec.edu.unl.sec.identityaccess.application.AuthorizationPort;
import ec.edu.unl.sec.identityaccess.application.CurrentActorPort;
import ec.edu.unl.sec.identityaccess.application.InstitutionalAuthorityPort;
import ec.edu.unl.sec.identityaccess.domain.AuthorizationScope;
import ec.edu.unl.sec.identityaccess.domain.OperationCode;
import ec.edu.unl.sec.improvement.domain.ActionProgress;
import ec.edu.unl.sec.improvement.domain.ImprovementAction;
import ec.edu.unl.sec.selfassessment.application.DocumentaryProcessPort;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.ConcurrencyFailureException;

class PostClosureImprovementTest {
    private static final UUID PROCESS = UUID.randomUUID(), CAREER = UUID.randomUUID(),
            MODEL = UUID.randomUUID(), ACTOR = UUID.randomUUID(), ASSIGNMENT = UUID.randomUUID(),
            EVIDENCE = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-10-25T12:00:00Z");

    private final CurrentActorPort actors = mock(CurrentActorPort.class);
    private final AuthorizationPort auth = mock(AuthorizationPort.class);
    private final DocumentaryProcessPort processes = mock(DocumentaryProcessPort.class);
    private final ImprovementRepository repository = mock(ImprovementRepository.class);
    private final InstitutionalAuthorityPort authority = mock(InstitutionalAuthorityPort.class);
    private final AuditPort audit = mock(AuditPort.class);
    private final ActionProgressService service =
            new ActionProgressService(actors, auth, processes, repository, authority, audit);

    @BeforeEach
    void allowEverything() {
        when(actors.currentActor()).thenReturn(Optional.of(new CurrentActorPort.Actor(ACTOR, false)));
        when(auth.hasOperation(any(), any())).thenReturn(true);
        when(auth.isAllowed(any(), any(), any())).thenReturn(true);
        when(authority.isAuthorizedAssignment(any(), any(), any(), any())).thenReturn(true);
        when(repository.replaceHead(any(), anyLong())).thenReturn(true);
    }

    private DocumentaryProcessPort.Context context(String state) {
        return new DocumentaryProcessPort.Context(PROCESS, CAREER, MODEL, state, 4, List.of(), null, null);
    }

    private ImprovementAction head(ImprovementAction.State state, Instant dueAt) {
        return new ImprovementAction(UUID.randomUUID(), PROCESS,
                new ImprovementAction.Data(UUID.randomUUID(), List.of(UUID.randomUUID()),
                        "Acción de mejora sintética", "Descripción suficientemente amplia de la acción.",
                        ASSIGNMENT, dueAt, "Reducir la brecha en un 50%", "Revisión documental y muestreo"),
                state, ACTOR, NOW.minus(5, ChronoUnit.DAYS), 1L);
    }

    private ActionProgressService.DecisionCommand decision(ImprovementAction.Transition transition) {
        return new ActionProgressService.DecisionCommand(transition,
                new ActionProgressService.Provenance(ASSIGNMENT, "Norma sintética aplicable",
                        "Oficio sintético 001-2026", "Decisión sintética suficientemente fundamentada."));
    }

    private ActionProgressService.ProgressCommand progress() {
        return new ActionProgressService.ProgressCommand("Avance sintético verificado con evidencia",
                "Brecha reducida al 25%", List.of(EVIDENCE), NOW);
    }

    @Test
    void closedProcessStillAllowsApprovedActionsToAdvanceAndBeVerified() {
        when(processes.lock(PROCESS)).thenReturn(Optional.of(context("CLOSED")));
        when(repository.findAction(any())).thenReturn(Optional.of(head(ImprovementAction.State.APPROVED, NOW)));

        var decision = service.decide(UUID.randomUUID(), 1L,
                decision(ImprovementAction.Transition.START), UUID.randomUUID());

        assertThat(decision.action()).isEqualTo(ImprovementAction.Transition.START);
        verify(repository).replaceHead(argThat(next -> next.state() == ImprovementAction.State.IN_PROGRESS
                && next.revision() == 2), eq(1L));
        verify(audit).record(argThat(event -> "IMPROVEMENT_DECISION_RECORDED".equals(event.action())));
    }

    @Test
    void closedProcessBlocksApprovalReturnAndProgressOfUndecidedActions() {
        when(processes.lock(PROCESS)).thenReturn(Optional.of(context("CLOSED")));
        when(repository.findAction(any())).thenReturn(Optional.of(head(ImprovementAction.State.PROPOSED, NOW)));

        assertThatThrownBy(() -> service.decide(UUID.randomUUID(), 1L,
                decision(ImprovementAction.Transition.APPROVE), UUID.randomUUID()))
                .isInstanceOf(ImprovementProblemException.class)
                .extracting("code").isEqualTo("IMPROVEMENT_PROCESS_CLOSED");
        assertThatThrownBy(() -> service.recordProgress(UUID.randomUUID(), 1L, progress(), UUID.randomUUID()))
                .isInstanceOf(ImprovementProblemException.class)
                .extracting("code").isEqualTo("IMPROVEMENT_PROCESS_CLOSED");
        verify(repository, never()).appendProgress(any());
    }

    @Test
    void progressIsAppendOnlyAndBlockedAfterVerification() {
        var running = head(ImprovementAction.State.IN_PROGRESS, NOW.plus(10, ChronoUnit.DAYS));
        when(processes.lock(PROCESS)).thenReturn(Optional.of(context("ACTIVE")));
        when(repository.findAction(any())).thenReturn(Optional.of(running));

        var recorded = service.recordProgress(running.id(), 1L, progress(), UUID.randomUUID());

        assertThat(recorded.actionId()).isEqualTo(running.id());
        verify(repository).appendProgress(recorded);
        verify(repository).replaceHead(argThat(next -> next.state() == ImprovementAction.State.IN_PROGRESS
                && next.revision() == 2), eq(1L));

        when(repository.findAction(any()))
                .thenReturn(Optional.of(head(ImprovementAction.State.VERIFIED, NOW.plus(10, ChronoUnit.DAYS))));
        assertThatThrownBy(() -> service.recordProgress(UUID.randomUUID(), 1L, progress(), UUID.randomUUID()))
                .isInstanceOf(ImprovementProblemException.class)
                .extracting("code").isEqualTo("IMPROVEMENT_ACTION_CLOSED");
    }

    @Test
    void verificationUsesItsOwnCapabilityAndRequiresCompetentAuthority() {
        var pending = head(ImprovementAction.State.PENDING_VERIFICATION, NOW.plus(10, ChronoUnit.DAYS));
        when(repository.findAction(any())).thenReturn(Optional.of(pending));
        when(processes.lock(PROCESS)).thenReturn(Optional.of(context("ACTIVE")));

        when(auth.hasOperation(ACTOR, OperationCode.IMPROVEMENT_ACTION_VERIFY)).thenReturn(false);
        assertThatThrownBy(() -> service.decide(pending.id(), 1L, decision(ImprovementAction.Transition.VERIFY),
                UUID.randomUUID())).isInstanceOf(ImprovementProblemException.class)
                .extracting("code").isEqualTo("IMPROVEMENT_CAPABILITY_REQUIRED");

        when(auth.hasOperation(ACTOR, OperationCode.IMPROVEMENT_ACTION_VERIFY)).thenReturn(true);
        when(authority.isAuthorizedAssignment(any(), any(), any(), any())).thenReturn(false);
        assertThatThrownBy(() -> service.decide(pending.id(), 1L, decision(ImprovementAction.Transition.VERIFY),
                UUID.randomUUID())).isInstanceOf(ImprovementProblemException.class)
                .extracting("code").isEqualTo("IMPROVEMENT_AUTHORITY_DENIED");
        verify(repository, never()).appendDecision(any());
    }

    @Test
    void verificationWithoutProgressPreservesStateAndDoesNotRecordDecision() {
        when(processes.lock(PROCESS)).thenReturn(Optional.of(context("ACTIVE")));
        for (var transition : List.of(ImprovementAction.Transition.SUBMIT_VERIFICATION, ImprovementAction.Transition.VERIFY)) {
            var state = transition == ImprovementAction.Transition.VERIFY
                    ? ImprovementAction.State.PENDING_VERIFICATION : ImprovementAction.State.IN_PROGRESS;
            var current = head(state, NOW);
            when(repository.findAction(current.id())).thenReturn(Optional.of(current));
            assertThatThrownBy(() -> service.decide(current.id(), 1L, decision(transition), UUID.randomUUID()))
                    .isInstanceOf(ImprovementProblemException.class)
                    .extracting("code").isEqualTo("IMPROVEMENT_PROGRESS_REQUIRED");
        }
        verify(repository, never()).appendDecision(any());
        verify(repository, never()).replaceHead(any(), anyLong());
    }

    @Test
    void staleRevisionUnknownActionAndOutOfScopeAreRejectedBeforeMutation() {
        var head = head(ImprovementAction.State.APPROVED, NOW.plus(5, ChronoUnit.DAYS));
        when(repository.findAction(any())).thenReturn(Optional.of(head));
        when(processes.lock(PROCESS)).thenReturn(Optional.of(context("ACTIVE")));

        assertThatThrownBy(() -> service.decide(head.id(), 2L, decision(ImprovementAction.Transition.START),
                UUID.randomUUID())).isInstanceOf(ConcurrencyFailureException.class);

        when(repository.findAction(any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.decide(UUID.randomUUID(), 1L,
                decision(ImprovementAction.Transition.START), UUID.randomUUID()))
                .isInstanceOf(ImprovementProblemException.class).extracting("code").isEqualTo("RESOURCE_NOT_FOUND");

        when(repository.findAction(any())).thenReturn(Optional.of(head));
        when(auth.isAllowed(any(), any(), any())).thenReturn(false);
        assertThatThrownBy(() -> service.decide(head.id(), 1L, decision(ImprovementAction.Transition.START),
                UUID.randomUUID())).isInstanceOf(ImprovementProblemException.class)
                .extracting("code").isEqualTo("RESOURCE_NOT_FOUND");
        verify(repository, never()).replaceHead(any(), anyLong());
    }

    @Test
    void technicalSessionsCannotOperateImprovement() {
        when(actors.currentActor()).thenReturn(Optional.of(new CurrentActorPort.Actor(ACTOR, true)));
        assertThatThrownBy(() -> service.decide(UUID.randomUUID(), 1L,
                decision(ImprovementAction.Transition.START), UUID.randomUUID()))
                .isInstanceOf(ImprovementProblemException.class)
                .extracting("code").isEqualTo("IMPROVEMENT_ACTOR_REQUIRED");
    }

    @Test
    void overdueIsDerivedAndDisappearsAfterVerification() {
        var detail = new ImprovementPlanService.ActionDetail(
                head(ImprovementAction.State.IN_PROGRESS, NOW.minus(1, ChronoUnit.DAYS)), true, List.of(), List.of());
        assertThat(detail.overdue()).isTrue();
        assertThat(head(ImprovementAction.State.VERIFIED, NOW.minus(1, ChronoUnit.DAYS)).overdue(NOW)).isFalse();
    }
}
