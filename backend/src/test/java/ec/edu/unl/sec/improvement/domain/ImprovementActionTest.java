package ec.edu.unl.sec.improvement.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ImprovementActionTest {
    private final UUID process = UUID.randomUUID(), plan = UUID.randomUUID(), finding = UUID.randomUUID(),
            responsible = UUID.randomUUID(), actor = UUID.randomUUID();
    private final Instant now = Instant.parse("2026-10-25T12:00:00Z");

    private ImprovementAction action(ImprovementAction.State state, Instant dueAt) {
        return new ImprovementAction(UUID.randomUUID(), process,
                new ImprovementAction.Data(plan, List.of(finding), "Acción de mejora sintética",
                        "Descripción suficientemente amplia de la acción de mejora.", responsible, dueAt,
                        "Reducir la brecha en un 50% verificable", "Revisión documental y muestreo aleatorio"),
                state, actor, now, 1L);
    }

    @Test
    void advancesOnlyThroughTheDeclaredLifecycle() {
        var action = action(ImprovementAction.State.PROPOSED, now.plus(30, ChronoUnit.DAYS));
        assertThat(action.advance(ImprovementAction.Transition.APPROVE)).isEqualTo(ImprovementAction.State.APPROVED);
        var approved = action(ImprovementAction.State.APPROVED, action.data().dueAt());
        assertThat(approved.advance(ImprovementAction.Transition.START))
                .isEqualTo(ImprovementAction.State.IN_PROGRESS);
        var running = action(ImprovementAction.State.IN_PROGRESS, action.data().dueAt());
        assertThat(running.advance(ImprovementAction.Transition.SUBMIT_VERIFICATION))
                .isEqualTo(ImprovementAction.State.PENDING_VERIFICATION);
        var pending = action(ImprovementAction.State.PENDING_VERIFICATION, action.data().dueAt());
        assertThat(pending.advance(ImprovementAction.Transition.REQUEST_CORRECTION))
                .isEqualTo(ImprovementAction.State.IN_PROGRESS);
        assertThat(pending.advance(ImprovementAction.Transition.VERIFY)).isEqualTo(ImprovementAction.State.VERIFIED);
        assertThatThrownBy(() -> action.advance(ImprovementAction.Transition.START))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> pending.advance(ImprovementAction.Transition.APPROVE))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void returnResetsAnyNonVerifiedStateAndVerifiedIsTerminal() {
        assertThat(action(ImprovementAction.State.IN_PROGRESS, now).advance(ImprovementAction.Transition.RETURN))
                .isEqualTo(ImprovementAction.State.PROPOSED);
        assertThat(action(ImprovementAction.State.PENDING_VERIFICATION, now)
                .advance(ImprovementAction.Transition.RETURN)).isEqualTo(ImprovementAction.State.PROPOSED);
        assertThatThrownBy(() -> action(ImprovementAction.State.VERIFIED, now).advance(ImprovementAction.Transition.RETURN))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void derivesOverdueUntilVerification() {
        var due = now.minus(1, ChronoUnit.DAYS);
        assertThat(action(ImprovementAction.State.APPROVED, due).overdue(now)).isTrue();
        assertThat(action(ImprovementAction.State.PENDING_VERIFICATION, due).overdue(now)).isTrue();
        assertThat(action(ImprovementAction.State.VERIFIED, due).overdue(now)).isFalse();
        assertThat(action(ImprovementAction.State.IN_PROGRESS, now.plus(1, ChronoUnit.DAYS)).overdue(now)).isFalse();
    }

    @Test
    void requiresDistinctFindingsAndMeasurableCommitments() {
        assertThatThrownBy(() -> new ImprovementAction.Data(plan, List.of(), "Título", "Descripción suficiente",
                responsible, now, "Meta", "Método")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ImprovementAction.Data(plan, List.of(finding, finding), "Título",
                "Descripción suficiente", responsible, now, "Meta", "Método"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ImprovementAction.Data(plan, List.of(finding), "Título", "corta",
                responsible, now, "Meta", "Método")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ImprovementAction.Data(plan, List.of(finding), " ", "Descripción suficiente",
                responsible, now, "Meta", "Método")).isInstanceOf(IllegalArgumentException.class);
    }
}
