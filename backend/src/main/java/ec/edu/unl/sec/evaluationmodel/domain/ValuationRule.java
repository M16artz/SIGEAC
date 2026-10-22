package ec.edu.unl.sec.evaluationmodel.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record ValuationRule(UUID id, UUID indicatorNodeId, Type ruleType, String methodDefinition,
        String formulaDefinition, String strategyKey, String strategyVersion, UUID scaleId,
        List<ValuationVariable> variables, List<ValuationThreshold> thresholds, String missingDataPolicy, Status status) {
    public ValuationRule {
        id = id == null ? UUID.randomUUID() : id;
        if (indicatorNodeId == null || ruleType == null || status == null) throw new IllegalArgumentException("rule identity, type and status required");
        methodDefinition = RequiredSource.text(methodDefinition, 20000, "valuation method");
        missingDataPolicy = RequiredSource.text(missingDataPolicy, 4000, "missing data policy");
        formulaDefinition = optional(formulaDefinition, 20000);
        strategyKey = optional(strategyKey, 200);
        strategyVersion = optional(strategyVersion, 100);
        variables = variables == null ? List.of() : List.copyOf(variables);
        thresholds = thresholds == null ? List.of() : List.copyOf(thresholds);
        Set<String> codes = new HashSet<>();
        for (var variable : variables) {
            if (!codes.add(variable.code())) throw new IllegalArgumentException("duplicate variable code");
        }
    }

    public List<String> validationIssues(RatingScale scale) {
        List<String> issues = new ArrayList<>();
        if (status == Status.RETIRED) issues.add("RULE_RETIRED");
        if (ruleType == Type.MANUAL_QUALITATIVE) {
            if (scaleId == null) issues.add("RULE_SCALE_REQUIRED");
            if (formulaDefinition != null || strategyKey != null || strategyVersion != null || !variables.isEmpty() || !thresholds.isEmpty()) {
                issues.add("MANUAL_RULE_HAS_CALCULATION");
            }
        }
        if (scaleId != null && (scale == null || !scaleId.equals(scale.id()))) issues.add("RULE_SCALE_NOT_FOUND");
        Set<UUID> options = new HashSet<>();
        if (scale != null) scale.options().forEach(option -> options.add(option.id()));
        for (int i = 0; i < thresholds.size(); i++) {
            if (!options.contains(thresholds.get(i).ratingOptionId())) issues.add("THRESHOLD_OPTION_NOT_FOUND");
            for (int j = 0; j < i; j++) {
                if (thresholds.get(i).overlaps(thresholds.get(j))) issues.add("THRESHOLD_OVERLAP");
            }
        }
        return List.copyOf(issues);
    }

    public List<String> automaticCalculationIssues(RatingScale scale, Set<StrategyReference> validatedStrategies) {
        List<String> issues = new ArrayList<>(validationIssues(scale));
        if (ruleType != Type.QUANTITATIVE) issues.add("AUTOMATIC_CALCULATION_NOT_SUPPORTED");
        if (status != Status.VALIDATED) issues.add("RULE_NOT_VALIDATED");
        if (formulaDefinition == null) issues.add("RULE_FORMULA_REQUIRED");
        if (variables.isEmpty()) issues.add("RULE_VARIABLE_REQUIRED");
        if (scaleId != null && thresholds.isEmpty()) issues.add("RULE_THRESHOLDS_REQUIRED");
        if (strategyKey == null || strategyVersion == null) issues.add("RULE_STRATEGY_REQUIRED");
        else if (!validatedStrategies.contains(new StrategyReference(strategyKey, strategyVersion))) issues.add("STRATEGY_NOT_VALIDATED");
        return List.copyOf(issues);
    }

    public boolean automaticCalculationAllowed(RatingScale scale, Set<StrategyReference> validatedStrategies) {
        return automaticCalculationIssues(scale, validatedStrategies).isEmpty();
    }

    private static String optional(String value, int max) {
        return value == null || value.isBlank() ? null : RequiredSource.text(value, max, "rule field");
    }
    public enum Type { MANUAL_QUALITATIVE, QUANTITATIVE }
    public enum Status { DRAFT, VALIDATED, RETIRED }
    public record StrategyReference(String key, String version) {
        public StrategyReference {
            key = RequiredSource.text(key, 200, "strategy key");
            version = RequiredSource.text(version, 100, "strategy version");
        }
    }
}
