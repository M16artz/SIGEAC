package ec.edu.unl.sec.evaluationmodel.application;

import ec.edu.unl.sec.evaluationmodel.domain.ModelCareerApplicability;
import ec.edu.unl.sec.evaluationmodel.domain.NormativeReference;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NormativeReferenceRepository {
    void saveReference(NormativeReference reference);
    Optional<NormativeReference> findReference(UUID id);
    void replaceIndicatorLinks(UUID modelVersionId, Collection<IndicatorLink> links);
    List<IndicatorLink> indicatorLinks(UUID modelVersionId);
    void appendApplicability(ModelCareerApplicability applicability);
    Optional<ModelCareerApplicability> latestApplicability(UUID modelVersionId, UUID careerId);
    List<ModelCareerApplicability> listApplicability(UUID modelVersionId);
    boolean careerExists(UUID careerId);

    record IndicatorLink(UUID indicatorNodeId, UUID referenceId) {}
}
