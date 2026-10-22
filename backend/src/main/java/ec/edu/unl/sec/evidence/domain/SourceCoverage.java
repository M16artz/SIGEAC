package ec.edu.unl.sec.evidence.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Explicit coverage declared on a DRAFT evidence version that satisfies one
 * required source of the fixed model. One coverage per version and source;
 * files, links, structured data and institutional references are source
 * modalities, not coverage substitutes.
 */
public record SourceCoverage(UUID id, UUID versionId, UUID requiredSourceId, String coverageNote,
        UUID declaredBy, Instant declaredAt) {
    public SourceCoverage {
        id = id == null ? UUID.randomUUID() : id;
        Objects.requireNonNull(versionId, "version required");
        Objects.requireNonNull(requiredSourceId, "required source required");
        Objects.requireNonNull(declaredBy, "declarant required");
        coverageNote = text(coverageNote);
        declaredAt = declaredAt == null ? Instant.now() : declaredAt;
    }

    private static String text(String value) {
        if (value == null || value.isBlank() || value.strip().length() > 4000)
            throw new IllegalArgumentException("coverage note required");
        return value.strip();
    }
}
