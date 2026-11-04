package ec.edu.unl.sec.evidence.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Read-only catalog of required sources published for the model version fixed
 * by the process. The evidence module never mutates model content.
 */
public interface RequiredSourceCatalogPort {
    Optional<ModelSources> model(UUID versionId);

    record ModelSources(UUID versionId, List<Entry> sources) {
        public ModelSources {
            sources = List.copyOf(sources);
        }
    }

    record Entry(UUID id, UUID indicatorId, String indicatorCode, String code, String name, String sourceType,
            boolean required, int cardinalityMin, Integer cardinalityMax) {}
}
