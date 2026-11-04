package ec.edu.unl.sec.improvement.domain;

import ec.edu.unl.sec.shared.domain.DecisionProvenance;
import java.util.UUID;

/** Approval, return, execution, verification or correction: append-only and never edited. */
public record ActionDecision(UUID id, UUID actionId, ImprovementAction.Transition action,
        DecisionProvenance provenance) {
    public ActionDecision {
        if (id == null || actionId == null || action == null || provenance == null)
            throw new IllegalArgumentException("decision identity is required");
    }
}
