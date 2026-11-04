package ec.edu.unl.sec.evaluationmodel.application;

import ec.edu.unl.sec.evaluationmodel.domain.EvaluationModel;
import ec.edu.unl.sec.evaluationmodel.domain.ModelVersion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EvaluationModelRepository {
    void saveModel(EvaluationModel model);
    List<EvaluationModel> listModels();
    Optional<EvaluationModel> findModel(UUID id);
    void saveVersion(ModelVersion version, long expectedRevision);
    Optional<ModelVersion> findVersion(UUID id);
    List<ModelVersion> listVersions(UUID modelId);
    int nextVersionNumber(UUID modelId);
}
