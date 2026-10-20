package ec.edu.unl.sec.evaluationmodel.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.unl.sec.evaluationmodel.application.EvaluationModelRepository;
import ec.edu.unl.sec.evaluationmodel.domain.EvaluationModel;
import ec.edu.unl.sec.evaluationmodel.domain.ModelVersion;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcEvaluationModelRepository implements EvaluationModelRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public JdbcEvaluationModelRepository(JdbcTemplate jdbc, ObjectMapper json) { this.jdbc = jdbc; this.json = json; }

    @Override public void saveModel(EvaluationModel m) {
        jdbc.update("insert into evaluation_model(id,name,description,created_by,created_at) values(?,?,?,?,?)",
                m.id(), m.name(), m.description(), m.createdBy(), Timestamp.from(m.createdAt()));
    }
    @Override public List<EvaluationModel> listModels() {
        return jdbc.query("select id,name,description,created_by,created_at from evaluation_model order by name",
                (r,n) -> new EvaluationModel(r.getObject(1,UUID.class),r.getString(2),r.getString(3),r.getObject(4,UUID.class),r.getTimestamp(5).toInstant()));
    }
    @Override public Optional<EvaluationModel> findModel(UUID id) {
        return jdbc.query("select id,name,description,created_by,created_at from evaluation_model where id=?",
                (r,n) -> new EvaluationModel(r.getObject(1,UUID.class),r.getString(2),r.getString(3),r.getObject(4,UUID.class),r.getTimestamp(5).toInstant()), id).stream().findFirst();
    }
    @Override public void saveVersion(ModelVersion v, long expectedRevision) {
        if (expectedRevision < 0) {
            jdbc.update("insert into model_version(id,model_id,version_number,status,source,author_id,approver_id,predecessor_id,revision,created_at,published_at,snapshot_json) values(?,?,?,?,?,?,?,?,?,?,?,?::jsonb)",
                    v.id(),v.modelId(),v.versionNumber(),v.status().name(),v.source(),v.authorId(),v.approverId(),v.predecessorId(),v.revision(),Timestamp.from(v.createdAt()),v.publishedAt()==null?null:Timestamp.from(v.publishedAt()),encode(v));
            return;
        }
        int changed=jdbc.update("update model_version set status=?,source=?,approver_id=?,revision=?,published_at=?,snapshot_json=?::jsonb where id=? and revision=?",
                v.status().name(),v.source(),v.approverId(),v.revision(),v.publishedAt()==null?null:Timestamp.from(v.publishedAt()),encode(v),v.id(),expectedRevision);
        if(changed!=1) throw new IllegalStateException("STALE_MODEL_VERSION");
    }
    @Override public Optional<ModelVersion> findVersion(UUID id) {
        return jdbc.query("select snapshot_json::text from model_version where id=?",(r,n)->decode(r.getString(1)),id).stream().findFirst();
    }
    @Override public List<ModelVersion> listVersions(UUID modelId) {
        return jdbc.query("select snapshot_json::text from model_version where model_id=? order by version_number",(r,n)->decode(r.getString(1)),modelId);
    }
    @Override public int nextVersionNumber(UUID modelId) {
        Integer value=jdbc.queryForObject("select coalesce(max(version_number),0)+1 from model_version where model_id=?",Integer.class,modelId);
        return value==null?1:value;
    }
    private String encode(ModelVersion value){try{return json.writeValueAsString(value);}catch(JsonProcessingException e){throw new IllegalStateException("MODEL_SERIALIZATION_FAILED",e);}}
    private ModelVersion decode(String value){try{return json.readValue(value,ModelVersion.class);}catch(JsonProcessingException e){throw new IllegalStateException("MODEL_DESERIALIZATION_FAILED",e);}}
}
