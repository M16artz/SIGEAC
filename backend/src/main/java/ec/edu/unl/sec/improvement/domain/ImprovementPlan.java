package ec.edu.unl.sec.improvement.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/** A plan is created from an issued report version and stays immutable. */
public record ImprovementPlan(UUID id, UUID processId, UUID reportVersionId, String title, UUID createdBy,
        Instant createdAt) {
    public ImprovementPlan {
        if (id == null || processId == null || reportVersionId == null || createdBy == null || createdAt == null)
            throw new IllegalArgumentException("improvement plan identity is required");
        if (title == null || title.strip().isEmpty() || title.strip().length() > 300)
            throw new IllegalArgumentException("improvement plan title is required");
        title = title.strip();
        createdAt = createdAt.truncatedTo(ChronoUnit.MILLIS);
    }
}
