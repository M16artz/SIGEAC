package ec.edu.unl.sec.archive.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class IntegralClosurePolicyTest {
    @Test
    void definesStrictPolicy() {
        var p = new IntegralClosurePolicy("STRICT", true, true);
        assertThat(p.strict()).isTrue();
    }
}
