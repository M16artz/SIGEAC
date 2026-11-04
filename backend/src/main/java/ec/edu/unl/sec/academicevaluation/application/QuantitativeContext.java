package ec.edu.unl.sec.academicevaluation.application;

import ec.edu.unl.sec.evaluationmodel.application.*;
import ec.edu.unl.sec.evaluationmodel.domain.*;
import ec.edu.unl.sec.academicevaluation.domain.*;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class QuantitativeContext {
    private final AcademicEvaluationService academic;
    private final ModelCatalogPort models;
    private final ValidatedStrategyPort strategies;
    public QuantitativeContext(AcademicEvaluationService academic,ModelCatalogPort models,ValidatedStrategyPort strategies) {
        this.academic=academic;this.models=models;this.strategies=strategies;
    }
    Context require(UUID processId,UUID indicatorId,boolean write) {
        var scope=academic.scope(processId,indicatorId,write);
        var model=models.publishedVersion(scope.process().modelVersionId()).orElseThrow(()->invalid("RULE_UNAVAILABLE","No se encontró el modelo publicado."));
        var config=model.configuration();
        if(config==null) throw invalid("RULE_UNAVAILABLE","No hay configuración de valoración.");
        var definition=config.indicators().stream().filter(i->i.nodeId().equals(indicatorId)).findFirst().orElseThrow(()->invalid("RULE_UNAVAILABLE","No existe la definición del indicador."));
        var rule=config.rules().stream().filter(r->r.id().equals(definition.valuationRuleId()) && r.indicatorNodeId().equals(indicatorId)).findFirst().orElseThrow(()->invalid("RULE_UNAVAILABLE","No hay regla cuantitativa."));
        var scale=config.scales().stream().filter(s->s.id().equals(definition.ratingScaleId())).findFirst().orElse(null);
        if (definition.nature()!=IndicatorDefinition.Nature.QUANTITATIVE || rule.status()!=ValuationRule.Status.VALIDATED
                || !rule.automaticCalculationIssues(scale,strategies.validatedStrategies()).isEmpty())
            throw invalid("RULE_NOT_VALIDATED","La regla o estrategia cuantitativa no están validadas.");
        try { RatioPercentStrategy.validate(rule,scale); } catch (IllegalArgumentException ex) {
            throw invalid("UNSUPPORTED_QUANTITATIVE_RULE","La fórmula o las restricciones no corresponden a la estrategia soportada.");
        }
        return new Context(scope,rule,scale,academic.accepted(scope.process(),indicatorId));
    }
    static AcademicProblemException invalid(String code,String message) { return new AcademicProblemException(AcademicProblemException.Kind.INVALID,code,message); }
    static AcademicProblemException stale() { return new AcademicProblemException(AcademicProblemException.Kind.PRECONDITION_FAILED,"STALE_VARIABLE_SET","Las variables cambiaron; recargue antes de continuar."); }
    record Context(AcademicEvaluationService.Scope scope,ValuationRule rule,RatingScale scale,List<ManualAcademicEvaluation.Support> supports) {}
}
