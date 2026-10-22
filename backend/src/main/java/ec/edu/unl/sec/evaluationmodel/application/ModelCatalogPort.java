package ec.edu.unl.sec.evaluationmodel.application;

import ec.edu.unl.sec.evaluationmodel.domain.ProgressFormula;
import ec.edu.unl.sec.evaluationmodel.domain.RatingScale;
import ec.edu.unl.sec.evaluationmodel.domain.ModelConfiguration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ModelCatalogPort {
    Optional<PublishedModel> publishedVersion(UUID versionId);
    record PublishedModel(UUID modelId, UUID versionId, int versionNumber, List<Indicator> indicators,
            RatingScale scale, ProgressFormula formula, ModelConfiguration configuration) {
        public PublishedModel(UUID modelId, UUID versionId, int versionNumber, List<Indicator> indicators,
                RatingScale scale, ProgressFormula formula) {
            this(modelId, versionId, versionNumber, indicators, scale, formula, null);
        }
    }
    record Indicator(UUID id, String code, String name) {}
}
