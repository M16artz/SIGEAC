package ec.edu.unl.sec.academicevaluation.domain;

import ec.edu.unl.sec.evaluationmodel.domain.*;
import java.math.*;
import java.util.*;

/** Exact supported contract, never an interpreter for user-supplied expressions. */
public final class RatioPercentStrategy {
    public static final ValuationRule.StrategyReference REFERENCE=new ValuationRule.StrategyReference("ratio-percent","1");
    private RatioPercentStrategy() {}
    public static void validate(ValuationRule rule, RatingScale scale) {
        if (!rule.automaticCalculationIssues(scale,Set.of(REFERENCE)).isEmpty()
                || !"A / B * 100".equals(rule.formulaDefinition()) || !"REJECT".equals(rule.missingDataPolicy())
                || rule.variables().size()!=2 || scale==null || !scale.id().equals(rule.scaleId()) || scale.options().isEmpty()) throw invalid();
        var variables=rule.variables().stream().collect(java.util.stream.Collectors.toMap(ValuationVariable::code,v->v));
        if (!variables.keySet().equals(Set.of("A","B"))) throw invalid();
        var a=variables.get("A");var b=variables.get("B");
        if (!a.required() || !b.required() || !a.unit().equals(b.unit())
                || !"NON_NEGATIVE".equals(a.validationDefinition()) || !"POSITIVE".equals(b.validationDefinition())) throw invalid();
    }
    public static BigDecimal value(String text, ValuationVariable definition) {
        if (text==null || !text.matches("(?:0|[1-9][0-9]{0,11})(?:\\.[0-9]{1,6})?")) throw invalid();
        var value=new BigDecimal(text);
        if (definition.dataType()==ValuationVariable.DataType.INTEGER && value.stripTrailingZeros().scale()>0) throw invalid();
        if (definition.code().equals("B") && value.signum()<=0) throw invalid();
        return value.stripTrailingZeros();
    }
    public static Result calculate(ValuationRule rule, RatingScale scale, Map<String,BigDecimal> values) {
        validate(rule,scale);
        if (!values.keySet().equals(Set.of("A","B")) || values.get("A").signum()<0 || values.get("B").signum()<=0) throw invalid();
        var result=values.get("A").multiply(BigDecimal.valueOf(100)).divide(values.get("B"),MathContext.DECIMAL128);
        var thresholds=rule.thresholds().stream().filter(t -> matches(t,result)).toList();
        if (thresholds.size()!=1) throw new IllegalArgumentException("NO_THRESHOLD_MATCH");
        var threshold=thresholds.getFirst();
        var option=scale.options().stream().filter(o -> o.id().equals(threshold.ratingOptionId())).findFirst().orElseThrow(RatioPercentStrategy::invalid);
        return new Result(result,threshold,option);
    }
    private static boolean matches(ValuationThreshold threshold,BigDecimal value) {
        return (threshold.lowerBound()==null || value.compareTo(threshold.lowerBound())>0 || threshold.lowerInclusive() && value.compareTo(threshold.lowerBound())==0)
                && (threshold.upperBound()==null || value.compareTo(threshold.upperBound())<0 || threshold.upperInclusive() && value.compareTo(threshold.upperBound())==0);
    }
    private static IllegalArgumentException invalid() { return new IllegalArgumentException("UNSUPPORTED_RATIO_INPUT_OR_RULE"); }
    public record Result(BigDecimal value,ValuationThreshold threshold,RatingScale.Option option) {}
}
