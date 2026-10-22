package ec.edu.unl.sec.evaluationmodel.api;

import ec.edu.unl.sec.evaluationmodel.application.EvaluationModelService;
import ec.edu.unl.sec.evaluationmodel.domain.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LegacyModelVersion(UUID id, UUID modelId, int versionNumber, ModelVersionStatus status, String source,
        UUID authorId, UUID approverId, UUID predecessorId, long revision, Instant createdAt, Instant publishedAt,
        List<LegacyModelNode> criteria, RatingScale scale, ProgressFormula formula) {
    static LegacyModelVersion from(ModelVersion version) {
        EvaluationModelService.requireLegacy(version);
        return new LegacyModelVersion(version.id(), version.modelId(), version.versionNumber(), version.status(),
                version.source(), version.authorId(), version.approverId(), version.predecessorId(), version.revision(),
                version.createdAt(), version.publishedAt(), version.criteria().stream().map(LegacyModelNode::from).toList(),
                version.scale(), version.formula());
    }
}
