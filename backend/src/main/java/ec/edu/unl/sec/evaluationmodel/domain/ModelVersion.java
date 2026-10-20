package ec.edu.unl.sec.evaluationmodel.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ModelVersion(UUID id, UUID modelId, int versionNumber, ModelVersionStatus status, String source,
        UUID authorId, UUID approverId, UUID predecessorId, long revision, Instant createdAt, Instant publishedAt,
        List<ModelNode> criteria, RatingScale scale, ProgressFormula formula) {
    public ModelVersion {
        id = id == null ? UUID.randomUUID() : id;
        if (modelId == null || authorId == null) throw new IllegalArgumentException("model and author required");
        if (versionNumber < 1 || revision < 0) throw new IllegalArgumentException("invalid version or revision");
        status = status == null ? ModelVersionStatus.DRAFT : status;
        source = source == null ? "" : source.strip();
        if (source.length() > 2000) throw new IllegalArgumentException("source too long");
        createdAt = createdAt == null ? Instant.now() : createdAt;
        criteria = criteria == null ? List.of() : List.copyOf(criteria);
    }

    public ModelVersion replaceDraft(String source, List<ModelNode> criteria, RatingScale scale, ProgressFormula formula) {
        requireDraft();
        return new ModelVersion(id, modelId, versionNumber, status, source, authorId, null, predecessorId,
                revision + 1, createdAt, null, criteria, scale, formula);
    }

    public ModelVersion publish(UUID approver, Instant at) {
        requireDraft();
        return new ModelVersion(id, modelId, versionNumber, ModelVersionStatus.PUBLISHED, source, authorId,
                approver, predecessorId, revision + 1, createdAt, at, criteria, scale, formula);
    }

    public ModelVersion supersede() {
        if (status != ModelVersionStatus.PUBLISHED) throw new IllegalStateException("only published version can be superseded");
        return new ModelVersion(id, modelId, versionNumber, ModelVersionStatus.SUPERSEDED, source, authorId,
                approverId, predecessorId, revision + 1, createdAt, publishedAt, criteria, scale, formula);
    }

    public ModelVersion successor(int number, UUID actor, Instant at) {
        if (status == ModelVersionStatus.DRAFT) throw new IllegalStateException("draft cannot be predecessor");
        return new ModelVersion(UUID.randomUUID(), modelId, number, ModelVersionStatus.DRAFT, source, actor,
                null, id, 0, at, null, criteria, scale, formula);
    }

    private void requireDraft() { if (status != ModelVersionStatus.DRAFT) throw new IllegalStateException("published version is immutable"); }
}
