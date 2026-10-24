package ec.edu.unl.sec.reporting.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record DocumentaryClosure(Content content,String payloadJson,String sha256) {
    public DocumentaryClosure {
        Objects.requireNonNull(content);
        if (payloadJson==null || payloadJson.isBlank() || sha256==null || !sha256.matches("[a-f0-9]{64}"))
            throw new IllegalArgumentException("Invalid closure encoding");
    }
    public Header header() {
        return new Header(content.id(),content.processId(),content.careerId(),content.snapshotId(),content.processRevision(),
                content.policyVersion(),content.closedBy(),content.closedAt(),content.reason(),sha256);
    }
    public record Header(UUID id,UUID processId,UUID careerId,UUID snapshotId,long processRevision,String policyVersion,
            UUID closedBy,Instant closedAt,String reason,String sha256) {}
    public record Content(UUID id,UUID processId,UUID careerId,UUID snapshotId,String snapshotSha256,
            String state,long processRevision,String policyVersion,UUID closedBy,Instant closedAt,String reason,List<FileReference> files) {
        public Content {
            Objects.requireNonNull(id);Objects.requireNonNull(processId);Objects.requireNonNull(careerId);
            Objects.requireNonNull(snapshotId);Objects.requireNonNull(closedBy);Objects.requireNonNull(closedAt);
            if (!"DOCUMENTARY_CLOSED".equals(state) || processRevision<0 || !"DOCUMENTARY_STRICT_V1".equals(policyVersion)
                    || snapshotSha256==null || !snapshotSha256.matches("[a-f0-9]{64}")
                    || reason==null || reason.strip().length()<10 || reason.strip().length()>2000)
                throw new IllegalArgumentException("Invalid documentary closure");
            reason=reason.strip(); closedAt=closedAt.truncatedTo(java.time.temporal.ChronoUnit.MILLIS);files=List.copyOf(files);
        }
    }
    public record FileReference(UUID versionId,UUID fileId,long sizeBytes,String sha256) {}
}
