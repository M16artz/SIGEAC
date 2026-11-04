package ec.edu.unl.sec.governance.domain;

import ec.edu.unl.sec.shared.domain.DecisionProvenance;
import java.util.UUID;

/** Terminal committee decision over a FINAL version; exactly one decision per version. */
public record ApprovalDecision(UUID id, UUID versionId, Decision decision, DecisionProvenance provenance) {
    public enum Decision { APPROVED, RETURNED }

    public ApprovalDecision {
        if (id == null || versionId == null || decision == null || provenance == null)
            throw new IllegalArgumentException("decision identity is required");
    }
}
