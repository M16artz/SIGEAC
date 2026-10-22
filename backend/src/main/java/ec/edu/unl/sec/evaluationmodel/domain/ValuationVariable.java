package ec.edu.unl.sec.evaluationmodel.domain;

public record ValuationVariable(String code, String name, String unit, DataType dataType, boolean required,
        String validationDefinition) {
    public ValuationVariable {
        code = ModelNode.normalize(code);
        name = RequiredSource.text(name, 300, "variable name");
        unit = RequiredSource.text(unit, 100, "variable unit");
        if (dataType == null) throw new IllegalArgumentException("variable data type required");
        validationDefinition = RequiredSource.text(validationDefinition, 4000, "variable validation");
    }
    public enum DataType { DECIMAL, INTEGER }
}
