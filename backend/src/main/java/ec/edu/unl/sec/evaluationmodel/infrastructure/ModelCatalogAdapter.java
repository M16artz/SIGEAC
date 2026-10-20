package ec.edu.unl.sec.evaluationmodel.infrastructure;

import ec.edu.unl.sec.evaluationmodel.application.EvaluationModelRepository;
import ec.edu.unl.sec.evaluationmodel.application.ModelCatalogPort;
import ec.edu.unl.sec.evaluationmodel.domain.ModelVersionStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ModelCatalogAdapter implements ModelCatalogPort {
    private final EvaluationModelRepository repository;
    public ModelCatalogAdapter(EvaluationModelRepository repository){this.repository=repository;}
    @Override public Optional<PublishedModel> publishedVersion(UUID id){
        return repository.findVersion(id).filter(v->v.status()!=ModelVersionStatus.DRAFT)
                .map(v->new PublishedModel(v.modelId(),v.id(),v.versionNumber(),v.criteria().stream()
                        .flatMap(c -> c.children().stream()).flatMap(s -> s.children().stream())
                        .map(i -> new Indicator(i.id(), i.code(), i.name())).toList(),v.scale(),v.formula()));
    }
}
