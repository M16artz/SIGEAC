package ec.edu.unl.sec.evaluationmodel.support;

import ec.edu.unl.sec.evaluationmodel.application.EvaluationModelRepository;
import ec.edu.unl.sec.evaluationmodel.domain.EvaluationModel;
import ec.edu.unl.sec.evaluationmodel.domain.ModelVersion;
import java.util.*;

public class InMemoryEvaluationModelRepository implements EvaluationModelRepository {
    public final Map<UUID,EvaluationModel> models=new LinkedHashMap<>();public final Map<UUID,ModelVersion> versions=new LinkedHashMap<>();
    public void saveModel(EvaluationModel m){models.put(m.id(),m);} public List<EvaluationModel> listModels(){return List.copyOf(models.values());} public Optional<EvaluationModel> findModel(UUID id){return Optional.ofNullable(models.get(id));}
    public void saveVersion(ModelVersion v,long expected){ModelVersion old=versions.get(v.id());if(expected>=0&&(old==null||old.revision()!=expected))throw new IllegalStateException("STALE_MODEL_VERSION");versions.put(v.id(),v);}
    public Optional<ModelVersion> findVersion(UUID id){return Optional.ofNullable(versions.get(id));}public List<ModelVersion> listVersions(UUID model){return versions.values().stream().filter(v->v.modelId().equals(model)).sorted(Comparator.comparingInt(ModelVersion::versionNumber)).toList();}public int nextVersionNumber(UUID model){return listVersions(model).stream().mapToInt(ModelVersion::versionNumber).max().orElse(0)+1;}
}
