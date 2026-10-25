package ec.edu.unl.sec.shared.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record DecisionProvenance(
        UUID decidedBy,
        UUID authorityAssignmentId,
        String legalBasis,
        String documentReference,
        String reason,
        Instant decidedAt) {
    public DecisionProvenance {
        Objects.requireNonNull(decidedBy, "decidedBy");
        Objects.requireNonNull(authorityAssignmentId, "authorityAssignmentId");
        Objects.requireNonNull(legalBasis, "legalBasis");
        Objects.requireNonNull(documentReference, "documentReference");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(decidedAt, "decidedAt");
        if (reason.isBlank() || reason.strip().length() < 10) {
            throw new IllegalArgumentException("reason must be at least ten characters");
        }
    }
}
