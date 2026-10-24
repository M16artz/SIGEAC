package ec.edu.unl.sec.selfassessment.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Snapshot reservation advances concurrency revision without changing lifecycle state. */
public interface DocumentaryProcessPort {
    Optional<Context> context(UUID processId);
    Optional<Context> lock(UUID processId);
    boolean reserveSnapshot(UUID processId, long expectedRevision);
    Optional<Context> completeClosure(UUID processId,long expectedRevision,UUID actor,String reason,UUID correlation);
    record Context(UUID id, UUID careerId, UUID modelVersionId, String state, long revision, List<UUID> indicatorIds) {
        public Context { indicatorIds = List.copyOf(indicatorIds); }
    }
}
