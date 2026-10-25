package ec.edu.unl.sec.academicevaluation.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.unl.sec.academicevaluation.application.ManualEvaluationRepository;
import ec.edu.unl.sec.academicevaluation.domain.ManualAcademicEvaluation;
import ec.edu.unl.sec.shared.infrastructure.JdbcValueSupport;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcManualEvaluationRepository implements ManualEvaluationRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    public JdbcManualEvaluationRepository(JdbcTemplate jdbc, ObjectMapper json) { this.jdbc=jdbc; this.json=json; }
    @Override public void append(ManualAcademicEvaluation evaluation) {
        try {
            jdbc.update("insert into manual_academic_evaluation(id,process_id,indicator_id,evaluator_id,revision,predecessor_id,evaluated_at,payload_json) values(?,?,?,?,?,?,?,?)",
                    evaluation.id(), evaluation.processId(), evaluation.indicatorId(), evaluation.evaluatorId(), evaluation.revision(),
                    evaluation.predecessorId(), Timestamp.from(evaluation.evaluatedAt()), json.writeValueAsString(evaluation));
        } catch (JsonProcessingException ex) { throw new IllegalStateException("Cannot serialize evaluation", ex); }
    }
    @Override public Optional<ManualAcademicEvaluation> latest(UUID processId, UUID indicatorId, UUID evaluatorId) {
        return jdbc.query("select * from manual_academic_evaluation where process_id=? and indicator_id=? and evaluator_id=? order by revision desc limit 1",
                (row,index) -> read(row), processId, indicatorId, evaluatorId).stream().findFirst();
    }
    @Override public List<ManualAcademicEvaluation> history(UUID processId, UUID indicatorId, long offset, int limit) {
        return jdbc.query("select * from manual_academic_evaluation where process_id=? and indicator_id=? order by evaluated_at desc,id desc limit ? offset ?",
                (row,index) -> read(row), processId, indicatorId, limit, offset);
    }
    @Override public List<ManualAcademicEvaluation> current(UUID processId,UUID indicatorId) {
        return jdbc.query("select v.* from manual_academic_evaluation v where v.process_id=? and v.indicator_id=?"
                +" and not exists(select 1 from manual_academic_evaluation newer where newer.process_id=v.process_id"
                +" and newer.indicator_id=v.indicator_id and newer.evaluator_id=v.evaluator_id and newer.revision>v.revision) order by v.evaluator_id",
                (row,index)->read(row),processId,indicatorId);
    }
    private ManualAcademicEvaluation read(ResultSet row) throws SQLException {
        try {
            var value=json.readValue(row.getString("payload_json"), ManualAcademicEvaluation.class);
            if (!value.id().equals(JdbcValueSupport.uuid(row, row.findColumn("id")))
                    || !value.processId().equals(JdbcValueSupport.uuid(row,row.findColumn("process_id")))
                    || !value.indicatorId().equals(JdbcValueSupport.uuid(row,row.findColumn("indicator_id")))
                    || !value.evaluatorId().equals(JdbcValueSupport.uuid(row,row.findColumn("evaluator_id")))
                    || value.revision()!=row.getLong("revision")
                    || !java.util.Objects.equals(value.predecessorId(), JdbcValueSupport.uuid(row,row.findColumn("predecessor_id"))))
                throw new IllegalStateException("Evaluation metadata mismatch");
            return value;
        } catch (JsonProcessingException ex) { throw new IllegalStateException("Cannot read evaluation", ex); }
    }
}
