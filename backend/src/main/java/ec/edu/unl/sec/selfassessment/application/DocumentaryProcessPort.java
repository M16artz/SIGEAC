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
    Optional<Context> completeIntegralClosure(UUID processId,long expectedRevision,UUID actor,String reason,
            UUID correlation,UUID authorityAssignmentId,String legalBasis,String documentReference);
    record Context(UUID id, UUID careerId, UUID modelVersionId, String state, long revision, List<UUID> indicatorIds,
            java.time.LocalDate periodStart, java.time.LocalDate periodEnd) {
        public Context(UUID id, UUID careerId, UUID modelVersionId, String state, long revision, List<UUID> indicatorIds) {
            this(id,careerId,modelVersionId,state,revision,indicatorIds,null,null);
        }
        public Context { indicatorIds = List.copyOf(indicatorIds); }
    }
}
