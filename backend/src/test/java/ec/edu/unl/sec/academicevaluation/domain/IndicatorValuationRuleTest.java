package ec.edu.unl.sec.academicevaluation.domain;

import static org.assertj.core.api.Assertions.*;
import ec.edu.unl.sec.evaluationmodel.domain.*;
import java.math.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class IndicatorValuationRuleTest {
    final UUID indicator=UUID.randomUUID(), low=UUID.randomUUID(), high=UUID.randomUUID();
    final RatingScale scale=new RatingScale(null,"S","Escala sintética",List.of(new RatingScale.Option(low,"LOW","Bajo",1,null),new RatingScale.Option(high,"HIGH","Alto",2,null)));
    ValuationRule rule(String formula,String version,ValuationRule.Status status) {
        return new ValuationRule(null,indicator,ValuationRule.Type.QUANTITATIVE,"Razón porcentual sintética",formula,"ratio-percent",version,scale.id(),
                List.of(new ValuationVariable("A","Numerador","personas",ValuationVariable.DataType.INTEGER,true,"NON_NEGATIVE"),
                        new ValuationVariable("B","Denominador","personas",ValuationVariable.DataType.INTEGER,true,"POSITIVE")),
                List.of(new ValuationThreshold(low,BigDecimal.ZERO,new BigDecimal("50"),true,false),new ValuationThreshold(high,new BigDecimal("50"),null,true,false)),"REJECT",status);
    }
    @ParameterizedTest @CsvSource({"0,4,0,LOW","1,4,25,LOW","2,4,50,HIGH","3,4,75,HIGH","5,4,125,HIGH"})
    void computesExactPercentAndUsesDeclaredBoundary(String a,String b,String expected,String code) {
        var result=RatioPercentStrategy.calculate(rule("A / B * 100","1",ValuationRule.Status.VALIDATED),scale,Map.of("A",new BigDecimal(a),"B",new BigDecimal(b)));
        assertThat(result.value()).isEqualByComparingTo(expected);assertThat(result.option().code()).isEqualTo(code);
    }
    @Test void preservesDeclaredArithmeticForRepeatingDecimals() {
        var value=RatioPercentStrategy.calculate(rule("A / B * 100","1",ValuationRule.Status.VALIDATED),scale,Map.of("A",BigDecimal.ONE,"B",new BigDecimal("3"))).value();
        assertThat(value).isEqualByComparingTo("33.33333333333333333333333333333333");
    }
    @Test void refusesUnsupportedFormulaVersionAndDraftRule() {
        assertThatThrownBy(()->RatioPercentStrategy.validate(rule("B / A","1",ValuationRule.Status.VALIDATED),scale)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->RatioPercentStrategy.validate(rule("A / B * 100","2",ValuationRule.Status.VALIDATED),scale)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->RatioPercentStrategy.validate(rule("A / B * 100","1",ValuationRule.Status.DRAFT),scale)).isInstanceOf(IllegalArgumentException.class);
    }
    @ParameterizedTest @ValueSource(strings={"0","-1","1.5","1e3","NaN","9999999999999","1.0000001"})
    void refusesInvalidIntegerDenominator(String text) {
        assertThatThrownBy(()->RatioPercentStrategy.value(text,rule("A / B * 100","1",ValuationRule.Status.VALIDATED).variables().get(1))).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void manualRuleDoesNotEnableCalculation() {
        var manual=new ValuationRule(null,indicator,ValuationRule.Type.MANUAL_QUALITATIVE,"Juicio cualitativo",null,null,null,scale.id(),List.of(),List.of(),"Declarar limitaciones",ValuationRule.Status.VALIDATED);
        assertThat(manual.validationIssues(scale)).isEmpty();
        assertThatThrownBy(()->RatioPercentStrategy.validate(manual,scale)).isInstanceOf(IllegalArgumentException.class);
    }
}