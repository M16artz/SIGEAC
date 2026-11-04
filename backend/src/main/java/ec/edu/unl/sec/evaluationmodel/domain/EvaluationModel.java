package ec.edu.unl.sec.evaluationmodel.domain;

import java.time.Instant;
import java.util.UUID;

public record EvaluationModel(UUID id, String name, String description, UUID createdBy, Instant createdAt) {
    public EvaluationModel {
        id = id == null ? UUID.randomUUID() : id;
        if (name == null || name.isBlank() || name.strip().length() > 200) throw new IllegalArgumentException("model name invalid");
        name = name.strip();
        description = description == null ? "" : description.strip();
        if (description.length() > 2000) throw new IllegalArgumentException("model description too long");
        if (createdBy == null) throw new IllegalArgumentException("createdBy required");
        createdAt = createdAt == null ? Instant.now() : createdAt;
    }
}
