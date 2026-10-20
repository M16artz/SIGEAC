package ec.edu.unl.sec.evaluationmodel.domain;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

public record ModelNode(UUID id, String code, String name, String description, int position, List<ModelNode> children) {
    public ModelNode {
        id = id == null ? UUID.randomUUID() : id;
        code = normalize(code);
        name = require(name, 300, "name");
        description = description == null ? "" : description.strip();
        if (description.length() > 4000) throw new IllegalArgumentException("description too long");
        if (position < 1) throw new IllegalArgumentException("position must be positive");
        children = children == null ? List.of() : List.copyOf(children);
    }

    public static String normalize(String value) {
        String normalized = require(value, 100, "code").toUpperCase(Locale.ROOT);
        return normalized.replaceAll("\\s+", " ");
    }

    private static String require(String value, int max, String field) {
        if (value == null || value.strip().isEmpty() || value.strip().length() > max) {
            throw new IllegalArgumentException(field + " invalid");
        }
        return value.strip();
    }
}
