package ec.edu.unl.sec.evaluationmodel.application;

import ec.edu.unl.sec.evaluationmodel.domain.ProgressFormula;
import ec.edu.unl.sec.evaluationmodel.domain.RatingScale;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ModelCatalogPort {
    Optional<PublishedModel> publishedVersion(UUID versionId);
    record PublishedModel(UUID modelId, UUID versionId, int versionNumber, List<Indicator> indicators,
            RatingScale scale, ProgressFormula formula) {}
    record Indicator(UUID id, String code, String name) {}
}
