package ec.edu.unl.sec.evaluationmodel.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class FormulaValidator {
    private FormulaValidator() {}

    public static List<String> validate(ProgressFormula formula, String expectedSchema, Set<String> indicatorCodes) {
        List<String> issues = new ArrayList<>();
        if (formula == null) return List.of("FORMULA_REQUIRED");
        if (!expectedSchema.equals(formula.schema())) issues.add("FORMULA_SCHEMA_UNSUPPORTED");
        if (formula.results().isEmpty()) issues.add("FORMULA_RESULTS_REQUIRED");
        if (formula.inputs().isEmpty()) issues.add("FORMULA_INPUTS_REQUIRED");
        if (formula.zeroPolicy().isBlank()) issues.add("FORMULA_ZERO_POLICY_REQUIRED");
        if (formula.notApplicablePolicy().isBlank()) issues.add("FORMULA_NOT_APPLICABLE_POLICY_REQUIRED");
        if (formula.examples().isEmpty()) issues.add("FORMULA_EXAMPLES_REQUIRED");
        Set<String> known = new HashSet<>(indicatorCodes);
        for (String input : formula.inputs()) {
            if (!known.contains(ModelNode.normalize(input))) issues.add("FORMULA_UNKNOWN_INPUT:" + input);
        }
        formula.examples().forEach(example -> {
            if (!example.expected().keySet().containsAll(formula.results())) issues.add("FORMULA_EXAMPLE_RESULT_MISSING");
        });
        return List.copyOf(issues);
    }
}
