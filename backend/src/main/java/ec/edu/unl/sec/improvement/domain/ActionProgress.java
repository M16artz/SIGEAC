package ec.edu.unl.sec.improvement.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** An append-only progress entry with its verification means; recorded values never change. */
public record ActionProgress(UUID id, UUID actionId, Data data, UUID recordedBy, Instant createdAt) {
    public ActionProgress {
        if (id == null || actionId == null || data == null || recordedBy == null || createdAt == null)
            throw new IllegalArgumentException("progress identity is required");
        createdAt = createdAt.truncatedTo(ChronoUnit.MILLIS);
    }

    public record Data(String description, String achievedValue, List<UUID> evidenceVersionIds, Instant recordedAt) {
        public Data {
            Objects.requireNonNull(recordedAt, "recordedAt");
            recordedAt = recordedAt.truncatedTo(ChronoUnit.MILLIS);
            if (evidenceVersionIds == null || evidenceVersionIds.isEmpty()
                    || evidenceVersionIds.stream().anyMatch(Objects::isNull))
                throw new IllegalArgumentException("at least one evidence version is required");
            if (evidenceVersionIds.stream().distinct().count() != evidenceVersionIds.size())
                throw new IllegalArgumentException("distinct evidence versions are required");
            evidenceVersionIds = List.copyOf(evidenceVersionIds);
            description = text(description, 10, 4000, "progress description out of bounds");
            achievedValue = text(achievedValue, 1, 2000, "achieved value is required");
        }
    }

    private static String text(String value, int min, int max, String message) {
        if (value == null) throw new IllegalArgumentException(message);
        String stripped = value.strip();
        if (stripped.length() < min || stripped.length() > max) throw new IllegalArgumentException(message);
        return stripped;
    }
}
