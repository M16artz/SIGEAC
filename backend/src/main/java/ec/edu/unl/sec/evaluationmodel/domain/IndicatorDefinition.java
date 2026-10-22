package ec.edu.unl.sec.evaluationmodel.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record IndicatorDefinition(UUID nodeId, Nature nature, String standardText, List<String> fundamentalElements,
        String evaluationPeriodDefinition, UUID ratingScaleId, UUID valuationRuleId, List<RequiredSource> sources) {
    public IndicatorDefinition {
        if (nodeId == null || nature == null) throw new IllegalArgumentException("indicator and nature required");
        standardText = RequiredSource.text(standardText, 20000, "standard text");
        evaluationPeriodDefinition = RequiredSource.text(evaluationPeriodDefinition, 4000, "evaluation period");
        fundamentalElements = fundamentalElements == null ? List.of() : fundamentalElements.stream()
                .map(element -> RequiredSource.text(element, 4000, "fundamental element")).toList();
        sources = sources == null ? List.of() : List.copyOf(sources);
        Set<String> codes = new HashSet<>();
        Set<UUID> ids = new HashSet<>();
        Set<Integer> ordinals = new HashSet<>();
        for (RequiredSource source : sources) {
            if (!nodeId.equals(source.indicatorNodeId())) throw new IllegalArgumentException("source belongs to another indicator");
            if (!codes.add(source.code())) throw new IllegalArgumentException("duplicate source code");
            if (!ids.add(source.id())) throw new IllegalArgumentException("duplicate source id");
            if (!ordinals.add(source.ordinal())) throw new IllegalArgumentException("duplicate source ordinal");
        }
    }

    public enum Nature { QUALITATIVE, QUANTITATIVE }
}
