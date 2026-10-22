package ec.edu.unl.sec.selfassessment.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record TeamMembership(UUID id, UUID teamId, UUID userId, UUID roleAssignmentId,
        Instant validFrom, Instant validUntil, UUID createdBy, Instant createdAt) {
    public TeamMembership {
        id = id == null ? UUID.randomUUID() : id;
        Objects.requireNonNull(teamId, "team required");
        Objects.requireNonNull(userId, "individual member required");
        Objects.requireNonNull(roleAssignmentId, "role assignment required");
        Objects.requireNonNull(createdBy, "creator required");
        if (validFrom == null || validUntil == null || !validFrom.isBefore(validUntil))
            throw new IllegalArgumentException("invalid membership vigency");
        createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    public boolean activeAt(Instant at) {
        return !at.isBefore(validFrom) && at.isBefore(validUntil);
    }
}
