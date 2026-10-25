package ec.edu.unl.sec.governance.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/** Consolidation prepares a traceable set of findings; nothing beyond an authorized rule aggregates. */
public record Consolidation(UUID id, UUID processId, Data data, UUID createdBy, Instant createdAt,
        UUID predecessorId, long revision) {
    public Consolidation {
        if (id == null || processId == null || data == null || createdBy == null || createdAt == null || revision < 1)
            throw new IllegalArgumentException("consolidation identity required");
        createdAt = createdAt.truncatedTo(ChronoUnit.MILLIS);
    }

    public record Data(List<UUID> findingIds, UUID aggregationRuleId, String reason) {
        public Data {
            if (reason == null || reason.strip().length() < 10 || reason.strip().length() > 4000)
                throw new IllegalArgumentException("justified consolidation reason required");
            reason = reason.strip();
            findingIds = List.copyOf(findingIds);
            predecessorIdInvariantCheck(findingIds);
        }
        private static void predecessorIdInvariantCheck(List<UUID> ids) {
            if (ids.stream().distinct().count() != ids.size())
                throw new IllegalArgumentException("distinct findings are required");
        }
    }

    public Data copy() { return new Data(List.copyOf(data.findingIds()), data.aggregationRuleId(), data.reason()); }
}
