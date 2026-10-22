package ec.edu.unl.sec.evaluationmodel.domain;

import static org.assertj.core.api.Assertions.*;

import ec.edu.unl.sec.evaluationmodel.support.ConfiguredModelFixtures;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ValuationRuleTest {
    @Test void variableUnitsTypesAndNormalizedCodesAreRequired() {
        assertThatIllegalArgumentException().isThrownBy(() -> variable("A", " ", ValuationVariable.DataType.INTEGER));
        assertThatIllegalArgumentException().isThrownBy(() -> variable("A", "personas", null));
        var fixture = ConfiguredModelFixtures.first();
        var rule = fixture.configuration().rules().getLast();
        assertThatIllegalArgumentException().isThrownBy(() -> new ValuationRule(rule.id(), rule.indicatorNodeId(), rule.ruleType(),
                rule.methodDefinition(), rule.formulaDefinition(), rule.strategyKey(), rule.strategyVersion(), rule.scaleId(),
                List.of(variable("A", "personas", ValuationVariable.DataType.INTEGER), variable(" a ", "personas", ValuationVariable.DataType.INTEGER)),
                rule.thresholds(), rule.missingDataPolicy(), rule.status()));
    }

    @Test void thresholdsRespectOpenClosedAndUnboundedLimitsWithoutDoubleMapping() {
        UUID option = UUID.randomUUID();
        var low = new ValuationThreshold(option, null, new BigDecimal("50"), false, false);
        var high = new ValuationThreshold(option, new BigDecimal("50.0"), null, true, false);
        assertThat(low.overlaps(high)).isFalse();
        var inclusive = new ValuationThreshold(option, null, new BigDecimal("50"), false, true);
        assertThat(inclusive.overlaps(high)).isTrue();
        assertThat(high.overlaps(inclusive)).isTrue();
        assertThatIllegalArgumentException().isThrownBy(() -> new ValuationThreshold(option, BigDecimal.TEN, BigDecimal.ONE, true, true));
        assertThatIllegalArgumentException().isThrownBy(() -> new ValuationThreshold(option, BigDecimal.ONE, BigDecimal.ONE, true, false));
        assertThat(new ValuationThreshold(option, BigDecimal.ONE, BigDecimal.ONE, true, true).overlaps(high)).isFalse();
    }

    @Test void qualitativeRulesNeverCalculateAndValidatedQuantitativeRulesKeepASeparateScale() {
        var configuration = ConfiguredModelFixtures.first().configuration();
        var qualitative = ConfiguredModelFixtures.withStatus(configuration.rules().getFirst(), ValuationRule.Status.VALIDATED);
        assertThat(qualitative.automaticCalculationAllowed(configuration.scales().getFirst(), Set.of())).isFalse();
        var quantitative = ConfiguredModelFixtures.withStatus(configuration.rules().getLast(), ValuationRule.Status.VALIDATED);
        var registry = Set.of(new ValuationRule.StrategyReference(quantitative.strategyKey(), quantitative.strategyVersion()));
        assertThat(quantitative.automaticCalculationAllowed(configuration.scales().getLast(), registry)).isTrue();
        assertThat(quantitative.automaticCalculationAllowed(configuration.scales().getFirst(), registry)).isFalse();
        assertThat(quantitative.automaticCalculationAllowed(configuration.scales().getLast(), Set.of())).isFalse();
    }

    private static ValuationVariable variable(String code, String unit, ValuationVariable.DataType type) {
        return new ValuationVariable(code, "Cantidad", unit, type, true, "No negativo");
    }
}
