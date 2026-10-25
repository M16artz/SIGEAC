package ec.edu.unl.sec.governance.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

/** Append-only reviewer observation against an emitted version; never edits emitted bytes. */
public record ReportObservation(UUID id, UUID versionId, String sectionReference, String text,
        UUID createdBy, Instant createdAt) {
    public ReportObservation {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(versionId, "versionId");
        if (sectionReference == null || sectionReference.strip().isEmpty() || sectionReference.strip().length() > 200)
            throw new IllegalArgumentException("section reference is required");
        if (text == null || text.strip().length() < 10 || text.strip().length() > 4000)
            throw new IllegalArgumentException("observation text out of bounds");
        Objects.requireNonNull(createdBy, "createdBy");
        Objects.requireNonNull(createdAt, "createdAt");
        sectionReference = sectionReference.strip();
        text = text.strip();
        createdAt = createdAt.truncatedTo(ChronoUnit.MILLIS);
    }
}
