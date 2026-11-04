package ec.edu.unl.sec.evidence.infrastructure;

import ec.edu.unl.sec.evidence.application.RequiredSourceCatalogPort;
import ec.edu.unl.sec.evaluationmodel.application.EvaluationModelRepository;
import ec.edu.unl.sec.evaluationmodel.domain.ModelNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class RequiredSourceCatalogAdapter implements RequiredSourceCatalogPort {
    private final EvaluationModelRepository models;

    public RequiredSourceCatalogAdapter(EvaluationModelRepository models) {
        this.models = models;
    }

    @Override
    public Optional<ModelSources> model(UUID versionId) {
        return models.findVersion(versionId).map(version -> {
            Map<UUID, String> codes = new HashMap<>();
            for (ModelNode node : ec.edu.unl.sec.evaluationmodel.domain.ModelHierarchy
                    .inspect(version.criteria()).indicators()) {
                codes.put(node.id(), node.code());
            }
            var entries = new ArrayList<Entry>();
            var indicators = version.configuration() == null
                    ? List.<ec.edu.unl.sec.evaluationmodel.domain.IndicatorDefinition>of()
                    : version.configuration().indicators();
            for (var indicator : indicators) {
                for (var source : indicator.sources()) {
                    entries.add(new Entry(source.id(), indicator.nodeId(),
                            codes.getOrDefault(indicator.nodeId(), ""), source.code(), source.name(),
                            source.sourceType().name(), source.required(), source.cardinalityMin(),
                            source.cardinalityMax()));
                }
            }
            return new ModelSources(version.id(), entries);
        });
    }
}
