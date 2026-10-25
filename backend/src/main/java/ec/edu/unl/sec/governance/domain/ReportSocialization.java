package ec.edu.unl.sec.governance.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Append-only socialization record linked to the emitted version it discloses. */
public record ReportSocialization(UUID id, UUID versionId, Instant occurredAt,
        List<UUID> participantAssignmentIds, String recordReference, UUID recordedBy, Instant recordedAt) {
    public ReportSocialization {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(versionId, "versionId");
        if (occurredAt == null) throw new IllegalArgumentException("occurrence date is required");
        if (participantAssignmentIds == null || participantAssignmentIds.isEmpty()
                || participantAssignmentIds.stream().distinct().count() != participantAssignmentIds.size())
            throw new IllegalArgumentException("at least one participant assignment is required");
        if (recordReference == null || recordReference.strip().isEmpty() || recordReference.strip().length() > 2000)
            throw new IllegalArgumentException("record reference is required");
        Objects.requireNonNull(recordedBy, "recordedBy");
        Objects.requireNonNull(recordedAt, "recordedAt");
        participantAssignmentIds = List.copyOf(participantAssignmentIds);
        recordReference = recordReference.strip();
        occurredAt = occurredAt.truncatedTo(ChronoUnit.MILLIS);
        recordedAt = recordedAt.truncatedTo(ChronoUnit.MILLIS);
    }
}
