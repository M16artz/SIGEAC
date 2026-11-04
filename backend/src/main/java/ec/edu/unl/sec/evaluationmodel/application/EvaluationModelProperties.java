package ec.edu.unl.sec.evaluationmodel.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sec.evaluation-model")
public record EvaluationModelProperties(String formulaSchema, String codeNormalization) {
    public EvaluationModelProperties {
        formulaSchema = formulaSchema == null || formulaSchema.isBlank() ? "sec.progress/v1" : formulaSchema;
        codeNormalization = codeNormalization == null || codeNormalization.isBlank() ? "TRIM_UPPER" : codeNormalization;
    }
}
