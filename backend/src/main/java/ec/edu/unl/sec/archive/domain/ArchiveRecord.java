package ec.edu.unl.sec.archive.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Evidence that a sealed closure snapshot was archived with a verified digest and retention policy. */
public record ArchiveRecord(UUID id, UUID processId, UUID closureSnapshotId, String retentionPolicyVersion,
        UUID archivedBy, Instant archivedAt, String integrityHash, String integrityStatus) {
    public static final String VERIFIED = "VERIFIED";

    public ArchiveRecord {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(processId, "processId");
        Objects.requireNonNull(closureSnapshotId, "closureSnapshotId");
        Objects.requireNonNull(archivedBy, "archivedBy");
        Objects.requireNonNull(archivedAt, "archivedAt");
        if (retentionPolicyVersion == null || retentionPolicyVersion.isBlank()
                || retentionPolicyVersion.length() > 100)
            throw new IllegalArgumentException("retention policy version is required");
        if (integrityHash == null || !integrityHash.matches("[a-f0-9]{64}"))
            throw new IllegalArgumentException("invalid archive digest");
        integrityStatus = integrityStatus == null ? VERIFIED : integrityStatus;
        if (!VERIFIED.equals(integrityStatus)) throw new IllegalArgumentException("invalid archive status");
        archivedAt = archivedAt.truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
    }
}
