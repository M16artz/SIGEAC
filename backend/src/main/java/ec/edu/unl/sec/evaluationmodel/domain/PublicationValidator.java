package ec.edu.unl.sec.evaluationmodel.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

public final class PublicationValidator {
    private PublicationValidator() {}

    public static List<String> validate(ModelVersion version, UUID approver, String formulaSchema) {
        return validate(version, approver, formulaSchema, Set.of());
    }

    public static List<String> validate(ModelVersion version, UUID approver, String formulaSchema,
            Set<ValuationRule.StrategyReference> validatedStrategies) {
        List<String> issues = new ArrayList<>();
        if (version.source().isBlank()) issues.add("SOURCE_REQUIRED");
        if (approver == null) issues.add("APPROVER_REQUIRED");
        else if (approver.equals(version.authorId())) issues.add("APPROVER_MUST_DIFFER_FROM_AUTHOR");
        if (version.criteria().isEmpty()) issues.add("CRITERION_REQUIRED");
        var hierarchy = ModelHierarchy.inspect(version.criteria());
        issues.addAll(hierarchy.issues());
        if (version.configuration() != null) {
            validateConfiguration(version, hierarchy, validatedStrategies, issues);
            return List.copyOf(issues);
        }
        if (hierarchy.entries().stream().anyMatch(entry -> entry.node().nodeType() != null)) issues.add("MODEL_CONFIGURATION_REQUIRED");
        Set<String> indicators = new HashSet<>();
        hierarchy.indicators().forEach(i -> indicators.add(i.code()));
        if (version.scale() == null) issues.add("SCALE_REQUIRED");
        else {
            if (version.scale().options().isEmpty()) issues.add("SCALE_OPTION_REQUIRED");
            Set<String> options = new HashSet<>();
            version.scale().options().forEach(o -> duplicate(options, o.code(), "SCALE_OPTION_CODE_DUPLICATE", issues));
        }
        issues.addAll(FormulaValidator.validate(version.formula(), formulaSchema, indicators));
        return List.copyOf(issues);
    }

    private static void validateConfiguration(ModelVersion version, ModelHierarchy.Result hierarchy,
            Set<ValuationRule.StrategyReference> validatedStrategies, List<String> issues) {
        if (version.scale() != null || version.formula() != null) issues.add("LEGACY_VALUATION_FIELDS_FORBIDDEN");
        hierarchy.entries().stream().filter(entry -> entry.node().nodeType() == null)
                .forEach(entry -> issues.add("NODE_TYPE_REQUIRED:" + entry.node().code()));
        var configuration = version.configuration();
        var nodes = index(hierarchy.indicators(), ModelNode::id, "INDICATOR_ID_DUPLICATE", issues);
        if (nodes.isEmpty()) issues.add("INDICATOR_REQUIRED");
        var definitions = index(configuration.indicators(), IndicatorDefinition::nodeId, "INDICATOR_DEFINITION_DUPLICATE", issues);
        var scales = index(configuration.scales(), RatingScale::id, "SCALE_ID_DUPLICATE", issues);
        var rules = index(configuration.rules(), ValuationRule::id, "RULE_ID_DUPLICATE", issues);
        Set<String> scaleCodes = new HashSet<>();
        Set<UUID> optionIds = new HashSet<>();
        for (var scale : configuration.scales()) {
            duplicate(scaleCodes, scale.code(), "SCALE_CODE_DUPLICATE", issues);
            if (scale.options().isEmpty()) issues.add("SCALE_OPTION_REQUIRED:" + scale.code());
            Set<String> codes = new HashSet<>();
            Set<Integer> positions = new HashSet<>();
            for (var option : scale.options()) {
                duplicate(codes, option.code(), "SCALE_OPTION_CODE_DUPLICATE", issues);
                if (!optionIds.add(option.id())) issues.add("SCALE_OPTION_ID_DUPLICATE:" + option.id());
                if (!positions.add(option.position())) issues.add("SCALE_OPTION_POSITION_DUPLICATE:" + scale.code());
            }
        }
        Set<UUID> sourceIds = new HashSet<>();
        for (var definition : configuration.indicators()) {
            if (!nodes.containsKey(definition.nodeId())) issues.add("INDICATOR_NODE_NOT_FOUND:" + definition.nodeId());
            if (definition.sources().isEmpty()) issues.add("INDICATOR_SOURCE_REQUIRED:" + definition.nodeId());
            for (var source : definition.sources()) {
                if (!sourceIds.add(source.id())) issues.add("SOURCE_ID_DUPLICATE:" + source.id());
            }
            if (definition.ratingScaleId() != null && !scales.containsKey(definition.ratingScaleId())) {
                issues.add("INDICATOR_SCALE_NOT_FOUND:" + definition.nodeId());
            }
            var rule = rules.get(definition.valuationRuleId());
            if (rule == null) {
                issues.add("INDICATOR_RULE_REQUIRED:" + definition.nodeId());
            } else {
                if (!rule.indicatorNodeId().equals(definition.nodeId())) issues.add("RULE_INDICATOR_MISMATCH:" + rule.id());
                if (!java.util.Objects.equals(rule.scaleId(), definition.ratingScaleId())) issues.add("RULE_SCALE_MISMATCH:" + rule.id());
                boolean qualitative = definition.nature() == IndicatorDefinition.Nature.QUALITATIVE;
                if (qualitative != (rule.ruleType() == ValuationRule.Type.MANUAL_QUALITATIVE)) issues.add("RULE_NATURE_MISMATCH:" + rule.id());
            }
        }
        for (UUID id : nodes.keySet()) {
            if (!definitions.containsKey(id)) issues.add("INDICATOR_DEFINITION_REQUIRED:" + id);
        }
        for (var rule : configuration.rules()) {
            if (!nodes.containsKey(rule.indicatorNodeId())) issues.add("RULE_INDICATOR_NOT_FOUND:" + rule.id());
            var scale = scales.get(rule.scaleId());
            var ruleIssues = rule.status() == ValuationRule.Status.VALIDATED && rule.ruleType() == ValuationRule.Type.QUANTITATIVE
                    ? rule.automaticCalculationIssues(scale, validatedStrategies) : rule.validationIssues(scale);
            ruleIssues.forEach(issue -> issues.add(issue + ":" + rule.id()));
        }
    }

    private static <T> Map<UUID, T> index(List<T> values, Function<T, UUID> id, String issue, List<String> issues) {
        Map<UUID, T> result = new LinkedHashMap<>();
        for (T value : values) {
            UUID key = id.apply(value);
            if (result.putIfAbsent(key, value) != null) issues.add(issue + ":" + key);
        }
        return result;
    }

    private static void duplicate(Set<String> seen, String code, String issue, List<String> issues) {
        if (!seen.add(code)) issues.add(issue + ":" + code);
    }
}
