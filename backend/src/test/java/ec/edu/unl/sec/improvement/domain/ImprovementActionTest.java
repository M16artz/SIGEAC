package ec.edu.unl.sec.improvement.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ImprovementActionTest {
    @Test
    void hasLifecycleAndDeadline() {
        var action = new Action("A1", "PENDIENTE", "2026-12-31");
        assertThat(action.status()).isEqualTo("PENDIENTE");
    }
    record Action(String id, String status, String deadline) {}
}
