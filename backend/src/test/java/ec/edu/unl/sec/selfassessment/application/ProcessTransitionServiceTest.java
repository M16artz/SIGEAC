package ec.edu.unl.sec.selfassessment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ec.edu.unl.sec.identityaccess.application.AuthorizationPort;
import ec.edu.unl.sec.identityaccess.application.CurrentActorPort;
import ec.edu.unl.sec.identityaccess.application.InstitutionalAuthorityPort;
import ec.edu.unl.sec.selfassessment.domain.ProcessState;
import ec.edu.unl.sec.selfassessment.domain.SelfAssessmentProcess;
import ec.edu.unl.sec.selfassessment.support.InMemorySelfAssessmentRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProcessTransitionServiceTest {
    @Test
    void preventsActivationAndClosureByGenericTransitionButAllowsAuthorizedSuspension() {
        var repo = new InMemorySelfAssessmentRepository();
        UUID actor = UUID.randomUUID(), assignment = UUID.randomUUID();
        var process = new SelfAssessmentProcess(null, UUID.randomUUID(), "P-1", "Proceso", LocalDate.now(),
                LocalDate.now().plusDays(1), UUID.randomUUID(), 1, ProcessState.PREPARATION, 0, actor,
                Instant.now(), List.of());
        repo.save(process, -1);
        var auth = mock(AuthorizationPort.class);
        when(auth.isAllowed(any(), any(), any())).thenReturn(true);
        var authority = mock(InstitutionalAuthorityPort.class);
        when(authority.isAuthorizedAssignment(any(), any(), any(), any())).thenReturn(true);
        var service = new ProcessTransitionService(repo, () -> Optional.of(new CurrentActorPort.Actor(actor, false)),
                auth, authority, mock(SelfAssessmentAuditService.class));
        assertThatThrownBy(() -> service.transition(process.id(), 0, ProcessState.ACTIVE,
                "Inicio sin autorizacion", UUID.randomUUID()))
                .isInstanceOf(ProcessProblemException.class).extracting("code").isEqualTo("ACTIVATION_REQUIRED");
        var active = new SelfAssessmentProcess(process.id(), process.careerId(), process.code(), process.name(),
                process.periodStart(), process.periodEnd(), process.modelVersionId(), 1, ProcessState.ACTIVE, 1,
                actor, Instant.now(), List.of());
        repo.processes.put(active.id(), active);
        for (var target : List.of(ProcessState.DOCUMENTARY_CLOSED, ProcessState.CLOSING, ProcessState.CLOSED))
            assertThatThrownBy(() -> service.transition(active.id(), 1, target,
                    "Cierre sin validaciones", UUID.randomUUID()))
                    .isInstanceOf(ProcessProblemException.class).extracting("code").isEqualTo("CLOSURE_POLICY_REQUIRED");
        var decision = new ProcessTransitionService.Decision(assignment, "Designacion sintetica", "fixture://decision");
        assertThatThrownBy(() -> service.transition(active.id(), 1, ProcessState.SUSPENDED,
                "Suspension sin autoridad", UUID.randomUUID()))
                .isInstanceOf(ProcessProblemException.class).extracting("code").isEqualTo("AUTHORITY_DECISION_REQUIRED");
        var suspended = service.transition(active.id(), 1, ProcessState.SUSPENDED, decision,
                "Suspension justificada", UUID.randomUUID());
        assertThat(suspended.state()).isEqualTo(ProcessState.SUSPENDED);
        assertThat(service.transition(active.id(), 2, ProcessState.ACTIVE, decision,
                "Reanudacion justificada", UUID.randomUUID()).revision()).isEqualTo(3);
    }
}
