package ec.edu.unl.sec.improvement.application;

import ec.edu.unl.sec.improvement.domain.ActionDecision;
import ec.edu.unl.sec.improvement.domain.ActionProgress;
import ec.edu.unl.sec.improvement.domain.ImprovementAction;
import ec.edu.unl.sec.improvement.domain.ImprovementPlan;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ImprovementRepository {
    void appendPlan(ImprovementPlan plan);

    Optional<ImprovementPlan> findPlan(UUID planId);

    long planTotal(UUID processId);

    Optional<String> reportVersionState(UUID processId, UUID reportVersionId);

    Set<UUID> findings(UUID processId, Collection<UUID> findingIds);

    void appendAction(ImprovementAction action, long predecessorRevision);

    Optional<ImprovementAction> findAction(UUID actionId);

    List<ImprovementAction> actions(UUID processId, long offset, long limit);

    long actionTotal(UUID processId);

    boolean replaceHead(ImprovementAction head, long expectedRevision);

    void appendProgress(ActionProgress progress);

    List<ActionProgress> progressFor(UUID actionId);

    void appendDecision(ActionDecision decision);

    List<ActionDecision> decisionsFor(UUID actionId);

    Optional<Assignment> activeAssignment(UUID assignmentId);

    record Assignment(UUID id, UUID identityId, UUID functionId, UUID careerId, UUID processId,
            Instant validFrom, Instant validTo) {
        public boolean activeAt(Instant at) {
            return (validFrom == null || !validFrom.isAfter(at)) && (validTo == null || validTo.isAfter(at));
        }

        public boolean covers(UUID career, UUID process, Instant at) {
            return activeAt(at) && (careerId == null || careerId.equals(career))
                    && (processId == null || processId.equals(process));
        }
    }
}
