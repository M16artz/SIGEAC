package ec.edu.unl.sec.evaluationmodel.domain;

import java.util.List;
import java.util.Map;

public record ProgressFormula(String code, String schema, List<String> results, List<String> inputs,
        String zeroPolicy, String notApplicablePolicy, List<Example> examples, Map<String, Object> parameters) {
    public ProgressFormula {
        code = ModelNode.normalize(code);
        schema = schema == null ? "" : schema.strip();
        results = results == null ? List.of() : List.copyOf(results);
        inputs = inputs == null ? List.of() : List.copyOf(inputs);
        zeroPolicy = zeroPolicy == null ? "" : zeroPolicy.strip();
        notApplicablePolicy = notApplicablePolicy == null ? "" : notApplicablePolicy.strip();
        examples = examples == null ? List.of() : List.copyOf(examples);
        parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
    }
    public record Example(Map<String, Object> input, Map<String, Object> expected) {
        public Example { input = input == null ? Map.of() : Map.copyOf(input); expected = expected == null ? Map.of() : Map.copyOf(expected); }
    }
}
