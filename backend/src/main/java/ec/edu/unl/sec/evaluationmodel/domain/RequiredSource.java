package ec.edu.unl.sec.evaluationmodel.domain;

import java.util.UUID;

public record RequiredSource(UUID id, UUID indicatorNodeId, String code, SourceType sourceType, String name,
        String description, boolean required, int cardinalityMin, Integer cardinalityMax, String periodRule, int ordinal) {
    public RequiredSource {
        id = id == null ? UUID.randomUUID() : id;
        if (indicatorNodeId == null || sourceType == null) throw new IllegalArgumentException("indicator and source type required");
        code = ModelNode.normalize(code);
        name = text(name, 300, "source name");
        description = text(description, 4000, "source description");
        periodRule = text(periodRule, 4000, "period rule");
        if (cardinalityMin < 1 || cardinalityMax != null && cardinalityMax < cardinalityMin) {
            throw new IllegalArgumentException("invalid source cardinality");
        }
        if (ordinal < 1) throw new IllegalArgumentException("ordinal must be positive");
    }

    static String text(String value, int max, String field) {
        if (value == null || value.isBlank() || value.strip().length() > max) throw new IllegalArgumentException(field + " invalid");
        return value.strip();
    }

    public enum SourceType { FILE, EXTERNAL_LINK, STRUCTURED_DATA, INSTITUTIONAL_REFERENCE }
}
