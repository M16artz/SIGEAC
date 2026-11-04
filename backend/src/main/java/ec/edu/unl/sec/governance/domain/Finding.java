package ec.edu.unl.sec.governance.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * A registered finding stays immutable and linked to its supports. It carries no rating and no
 * aggregated result; aggregation only happens later under an authorized methodology.
 */
public record Finding(UUID id, UUID processId, Data data, UUID createdBy, Instant createdAt) {
    public Finding {
        if (id == null || processId == null || data == null || createdBy == null || createdAt == null)
            throw new IllegalArgumentException("finding identity and supports required");
        createdAt = createdAt.truncatedTo(ChronoUnit.MILLIS);
    }

    public enum Status { REGISTERED }

    public enum Type { STRENGTH, WEAKNESS, OBSERVATION }

    public enum Kind { INDICATOR, EVALUATION, EVIDENCE_VERSION }

    public record Data(Type type, String title, String description, List<UUID> indicatorIds,
            List<UUID> evaluationIds, List<UUID> evidenceVersionIds) {
        public Data {
            if (type == null) throw new IllegalArgumentException("finding type is required");
            if (indicatorIds == null || indicatorIds.isEmpty() || indicatorIds.stream().anyMatch(java.util.Objects::isNull))
                throw new IllegalArgumentException("at least one indicator support is required");
            requireDistinct(indicatorIds);
            requireDistinct(evaluationIds);
            requireDistinct(evidenceVersionIds);
            title = text(title, 1, 300, "justified title is required");
            description = text(description, 10, 4000, "finding text out of bounds");
        }

        private static void requireDistinct(List<UUID> values) {
            if (values.stream().distinct().count() != values.size() || values.stream().anyMatch(java.util.Objects::isNull))
                throw new IllegalArgumentException("distinct supports are required");
        }
    }

    public long version() { return 1L; }

    public Status status() { return Status.REGISTERED; }

    public List<Support> supports() {
        List<Support> all = new ArrayList<>();
        data.indicatorIds().forEach(id -> all.add(new Support(Kind.INDICATOR, id)));
        data.evaluationIds().forEach(id -> all.add(new Support(Kind.EVALUATION, id)));
        data.evidenceVersionIds().forEach(id -> all.add(new Support(Kind.EVIDENCE_VERSION, id)));
        Set<Support> distinct = new LinkedHashSet<>(all);
        if (distinct.size() != all.size() || distinct.size() > 500)
            throw new IllegalArgumentException("finding supports must be distinct and bounded");
        return List.copyOf(distinct);
    }

    private static String text(String value, int min, int max, String message) {
        if (value == null) throw new IllegalArgumentException(message);
        String stripped = value.strip();
        if (stripped.length() < min || stripped.length() > max) throw new IllegalArgumentException(message);
        return stripped;
    }

    public record Support(Kind kind, UUID subjectId) {
        public Support {
            if (kind == null || subjectId == null) throw new IllegalArgumentException("support identity required");
        }
    }
}
