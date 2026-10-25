package ec.edu.unl.sec.academicevaluation.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class IndicatorValuationRuleTest {
    @Test
    void supportsQuantitativeAndQualitativeWithMethod() {
        var q = new IndicatorValuationRule("R1", Type.QUANTITATIVE, "FORMULA", "Limites");
        var qual = new IndicatorValuationRule("R2", Type.QUALITATIVE, "CRITERIA", "Justificación obligatoria");
        assertThat(q.type()).isEqualTo(Type.QUANTITATIVE);
        assertThat(qual.method()).isEqualTo("CRITERIA");
    }

    public record IndicatorValuationRule(String code, Type type, String method, String limitations) {
        public IndicatorValuationRule {
            if (code == null || code.isBlank()) throw new IllegalArgumentException("code required");
            if (type == null) throw new IllegalArgumentException("type required");
            if (method == null || method.isBlank()) throw new IllegalArgumentException("method required");
        }
    }

    public enum Type { QUANTITATIVE, QUALITATIVE }
}
