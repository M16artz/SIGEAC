package ec.edu.unl.sec.selfassessment.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A responsibility for one indicator. Exactly one of {@code identityId} or
 * {@code teamId} is set: membership never replaces individual attribution.
 */
public record ProcessAssignment(UUID id, UUID processId, UUID indicatorId, UUID identityId, UUID teamId,
        Function function, Instant validFrom, Instant validTo, UUID createdBy, String reason,
        Instant revokedAt, long revision) {
    public enum Function { RESPONSIBLE, REVIEWER, EVALUATOR }

    public ProcessAssignment {
        id = id == null ? UUID.randomUUID() : id;
        Objects.requireNonNull(processId);
        Objects.requireNonNull(indicatorId);
        if ((identityId == null) == (teamId == null))
            throw new IllegalArgumentException("exactly one user or team is required");
        Objects.requireNonNull(function);
        Objects.requireNonNull(createdBy);
        if (validFrom == null || validTo == null || !validFrom.isBefore(validTo))
            throw new IllegalArgumentException("invalid vigency");
        reason = reason == null ? "" : reason.strip();
    }

    public ProcessAssignment(UUID id, UUID processId, UUID indicatorId, UUID identityId,
            Function function, Instant validFrom, Instant validTo, UUID createdBy, String reason,
            Instant revokedAt, long revision) {
        this(id, processId, indicatorId, identityId, null, function, validFrom, validTo,
                createdBy, reason, revokedAt, revision);
    }

    public boolean assignedTo(UUID subjectId) {
        return identityId != null && identityId.equals(subjectId);
    }
}
