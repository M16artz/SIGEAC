package ec.edu.unl.sec.documentreview.application;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Latest per-source outcomes for submitted versions selected by an authorized caller. */
public interface DocumentReviewProgressPort {
    List<Outcome> latestOutcomes(Set<UUID> versionIds);
    record Outcome(UUID versionId, UUID requiredSourceId, UUID decisionId, String outcome) {}
}
