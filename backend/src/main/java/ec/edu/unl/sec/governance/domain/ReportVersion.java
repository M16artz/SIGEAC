package ec.edu.unl.sec.governance.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

/**
 * An emitted report version is an immutable snapshot. Later observations, replies, socializations
 * and decisions never change its bytes; workflow state lives in separate events.
 */
public record ReportVersion(UUID id, UUID processId, Data data, UUID createdBy, Instant createdAt,
        UUID predecessorVersionId, long versionNumber, long revision, String contentSha256) {
    public ReportVersion {
        if (id == null || processId == null || data == null || createdBy == null || createdAt == null)
            throw new IllegalArgumentException("report version identity is required");
        if (predecessorVersionId == null && data.kind() != Kind.PRELIMINARY)
            throw new IllegalArgumentException("only a preliminary version may start the chain");
        if (versionNumber < 1 || revision < 1) throw new IllegalArgumentException("report version numbering is required");
        if (contentSha256 == null || !contentSha256.matches("[a-f0-9]{64}"))
            throw new IllegalArgumentException("content sha256 is required");
        createdAt = createdAt.truncatedTo(ChronoUnit.MILLIS);
    }

    public enum Kind { PRELIMINARY, FINAL }

    public record Data(Kind kind, UUID consolidationId, String templateReference, boolean templateValidated,
            String content, UUID predecessorVersionId) {
        public Data {
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(consolidationId, "consolidationId");
            if (!templateValidated) throw new IllegalArgumentException("an unvalidated template cannot be emitted");
            if (templateReference == null || templateReference.strip().isEmpty()
                    || templateReference.strip().length() > 500)
                throw new IllegalArgumentException("template reference out of bounds");
            if (content == null || content.strip().length() < 10 || content.strip().length() > 200000)
                throw new IllegalArgumentException("report content out of bounds");
            templateReference = templateReference.strip();
            content = content.strip();
        }
    }
}
