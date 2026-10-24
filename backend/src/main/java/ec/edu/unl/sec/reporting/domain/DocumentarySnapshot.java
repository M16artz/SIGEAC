package ec.edu.unl.sec.reporting.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** An immutable documentary cut; it never changes the process state. */
public record DocumentarySnapshot(Content content, String payloadJson, String sha256) {
    public DocumentarySnapshot {
        Objects.requireNonNull(content);
        if (payloadJson == null || payloadJson.isBlank() || sha256 == null || !sha256.matches("[a-f0-9]{64}"))
            throw new IllegalArgumentException("Invalid snapshot encoding");
    }

    public Header header() {
        return new Header(content.id(), content.processId(), content.careerId(), content.modelVersionId(),
                content.processRevision(), content.createdBy(), content.createdAt(), content.reason(), content.progress(), sha256);
    }

    public record Header(UUID id, UUID processId, UUID careerId, UUID modelVersionId, long processRevision,
            UUID createdBy, Instant createdAt, String reason, DocumentaryProgressCalculator.Progress progress, String sha256) {}

    public record Content(UUID id, UUID processId, UUID careerId, UUID modelVersionId, String processState, long processRevision,
            UUID createdBy, Instant createdAt, String reason, List<UUID> indicatorIds,
            DocumentaryProgressCalculator.Progress progress, List<Source> sources, List<Applicability> applicability) {
        public Content {
            Objects.requireNonNull(id); Objects.requireNonNull(processId); Objects.requireNonNull(careerId);
            Objects.requireNonNull(modelVersionId); Objects.requireNonNull(createdBy); Objects.requireNonNull(createdAt);
            Objects.requireNonNull(progress);
            if (!"ACTIVE".equals(processState)) throw new IllegalArgumentException("A documentary cut requires an active process");
            if (processRevision < 0 || reason == null || reason.strip().length() < 10 || reason.strip().length() > 4000)
                throw new IllegalArgumentException("Snapshot reason/revision invalid");
            reason = reason.strip();
            createdAt = createdAt.truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
            indicatorIds = List.copyOf(indicatorIds);
            sources = List.copyOf(sources);
            applicability = List.copyOf(applicability);
        }
    }

    public record Source(UUID id, UUID indicatorId, String code, String name, boolean required, boolean applicable,
            int cardinalityMin, List<Coverage> coverages) {
        public Source { coverages = List.copyOf(coverages); }
    }
    public record Coverage(UUID evidenceId, UUID versionId, UUID decisionId, String outcome) {}
    public record Applicability(UUID id, String subjectType, UUID subjectId, String decision, long revision) {}
}
