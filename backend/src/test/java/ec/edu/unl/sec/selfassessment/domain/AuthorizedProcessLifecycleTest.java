package ec.edu.unl.sec.selfassessment.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuthorizedProcessLifecycleTest {
    private final UUID actor = UUID.randomUUID();
    private final UUID authorityAssignment = UUID.randomUUID();

    @Test
    void preparationRemainsPreparationUntilAVerifiedAuthorizationIsRecorded() {
        var process = process(ProcessState.PREPARATION);
        assertThatThrownBy(() -> process.activate(List.of(indicator())))
                .isInstanceOf(IllegalStateException.class).hasMessage("AUTHORIZATION_MISSING");
        var authorized = process.authorize(authorization());
        assertThat(authorized.state()).isEqualTo(ProcessState.PREPARATION);
        assertThat(authorized.authorization().authorityAssignmentId()).isEqualTo(authorityAssignment);
        assertThatThrownBy(() -> authorized.authorize(authorization()))
                .isInstanceOf(IllegalStateException.class).hasMessage("AUTHORIZATION_ALREADY_FIXED");
        assertThat(authorized.activate(List.of(indicator())).state()).isEqualTo(ProcessState.ACTIVE);
    }

    @Test
    void legacyActiveProcessDoesNotInventAnAuthorization() {
        var legacy = process(ProcessState.ACTIVE);
        assertThat(legacy.authorization()).isNull();
        assertThatThrownBy(() -> legacy.authorize(authorization()))
                .isInstanceOf(IllegalStateException.class).hasMessage("AUTHORIZATION_ALREADY_FIXED");
    }

    @Test
    void lifecycleAllowsPolicyBackedSuspensionAndResumptionOnly() {
        var active = process(ProcessState.PREPARATION).authorize(authorization()).activate(List.of(indicator()));
        var suspended = active.transition(ProcessState.SUSPENDED);
        assertThat(suspended.state()).isEqualTo(ProcessState.SUSPENDED);
        assertThat(suspended.transition(ProcessState.ACTIVE).state()).isEqualTo(ProcessState.ACTIVE);
        assertThatThrownBy(() -> active.transition(ProcessState.CLOSED))
                .isInstanceOf(IllegalStateException.class);
    }

    private SelfAssessmentProcess process(ProcessState state) {
        return new SelfAssessmentProcess(null, UUID.randomUUID(), "P-AUTH", "Proceso autorizado",
                LocalDate.now(), LocalDate.now().plusDays(30), UUID.randomUUID(), 1, state,
                state == ProcessState.ACTIVE ? 1 : 0, actor, Instant.now(), List.of());
    }

    private ProcessAuthorization authorization() {
        return new ProcessAuthorization(null, authorityAssignment, "Designacion sintetica",
                "fixture://authorization", "Inicio autorizado sintetico", actor, Instant.now());
    }

    private SelfAssessmentProcess.IndicatorSnapshot indicator() {
        return new SelfAssessmentProcess.IndicatorSnapshot(UUID.randomUUID(), "IND", "Indicador");
    }
}
