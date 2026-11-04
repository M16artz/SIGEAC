package ec.edu.unl.sec.evaluationmodel.support;

import ec.edu.unl.sec.evaluationmodel.application.NormativeReferenceRepository;
import ec.edu.unl.sec.evaluationmodel.domain.ModelCareerApplicability;
import ec.edu.unl.sec.evaluationmodel.domain.NormativeReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class InMemoryNormativeReferenceRepository implements NormativeReferenceRepository {
    public final Map<UUID, NormativeReference> references = new LinkedHashMap<>();
    public final Map<UUID, Set<IndicatorLink>> links = new LinkedHashMap<>();
    public final List<ModelCareerApplicability> decisions = new ArrayList<>();
    public final Set<UUID> careers = new LinkedHashSet<>();

    @Override public void saveReference(NormativeReference reference) {
        references.putIfAbsent(reference.id(), reference);
    }

    @Override public Optional<NormativeReference> findReference(UUID id) {
        return Optional.ofNullable(references.get(id));
    }

    @Override public void replaceIndicatorLinks(UUID modelVersionId, Collection<IndicatorLink> links) {
        this.links.computeIfAbsent(modelVersionId, ignored -> new LinkedHashSet<>()).addAll(links);
    }

    @Override public List<IndicatorLink> indicatorLinks(UUID modelVersionId) {
        return List.copyOf(links.getOrDefault(modelVersionId, Set.of()));
    }

    @Override public void appendApplicability(ModelCareerApplicability applicability) {
        boolean duplicate = decisions.stream().anyMatch(existing -> existing.modelVersionId().equals(applicability.modelVersionId())
                && existing.careerId().equals(applicability.careerId())
                && existing.revision() == applicability.revision());
        if (duplicate) throw new IllegalStateException("duplicate applicability revision");
        decisions.add(applicability);
    }

    @Override public Optional<ModelCareerApplicability> latestApplicability(UUID modelVersionId, UUID careerId) {
        return decisions.stream()
                .filter(value -> value.modelVersionId().equals(modelVersionId) && value.careerId().equals(careerId))
                .max(Comparator.comparingInt(ModelCareerApplicability::revision));
    }

    @Override public List<ModelCareerApplicability> listApplicability(UUID modelVersionId) {
        return decisions.stream().filter(value -> value.modelVersionId().equals(modelVersionId)).toList();
    }

    @Override public boolean careerExists(UUID careerId) {
        return careers.contains(careerId);
    }
}
