package ec.edu.unl.sec.selfassessment.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ProcessTeam(UUID id, UUID processId, String name, String processRole,
        Instant validFrom, Instant validUntil, UUID createdBy, Instant createdAt, long revision) {
    public ProcessTeam {
        id = id == null ? UUID.randomUUID() : id;
        Objects.requireNonNull(processId, "process required");
        Objects.requireNonNull(createdBy, "creator required");
        name = text(name, 300);
        processRole = text(processRole, 100);
        if (validFrom == null || validUntil == null || !validFrom.isBefore(validUntil))
            throw new IllegalArgumentException("invalid team vigency");
        if (revision < 0) throw new IllegalArgumentException("invalid revision");
        createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    public boolean activeAt(Instant at) {
        return !at.isBefore(validFrom) && at.isBefore(validUntil);
    }

    public boolean overlaps(Instant from, Instant until) {
        return from.isBefore(validUntil) && until.isAfter(validFrom);
    }

    private static String text(String value, int max) {
        if (value == null || value.isBlank() || value.strip().length() > max)
            throw new IllegalArgumentException("invalid text");
        return value.strip();
    }
}
