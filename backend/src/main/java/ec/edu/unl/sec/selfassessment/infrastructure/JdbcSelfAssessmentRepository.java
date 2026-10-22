package ec.edu.unl.sec.selfassessment.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.unl.sec.selfassessment.application.SelfAssessmentRepository;
import ec.edu.unl.sec.selfassessment.domain.ProcessAssignment;
import ec.edu.unl.sec.selfassessment.domain.ProcessState;
import ec.edu.unl.sec.selfassessment.domain.SelfAssessmentProcess;
import ec.edu.unl.sec.shared.infrastructure.JdbcValueSupport;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcSelfAssessmentRepository implements SelfAssessmentRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final boolean postgres;

    public JdbcSelfAssessmentRepository(JdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
        this.postgres = Boolean.TRUE.equals(jdbc.execute((ConnectionCallback<Boolean>) connection ->
                connection.getMetaData().getDatabaseProductName().contains("PostgreSQL")));
    }

    @Override
    public void save(SelfAssessmentProcess process, long expectedRevision) {
        if (expectedRevision < 0) {
            jdbc.update(
                    "insert into self_assessment_process(id,career_id,code,name,period_start,period_end,model_version_id,model_version_number,state,revision,created_by,created_at,snapshot_json) values(?,?,?,?,?,?,?,?,?,?,?,?,"
                            + jsonParameter() + ")",
                    process.id(), process.careerId(), process.code(), process.name(), process.periodStart(),
                    process.periodEnd(), process.modelVersionId(), process.modelVersionNumber(), process.state().name(),
                    process.revision(), process.createdBy(), Timestamp.from(process.createdAt()), encode(process));
            return;
        }
        int changed = jdbc.update(
                "update self_assessment_process set state=?,revision=?,snapshot_json=" + jsonParameter()
                        + " where id=? and revision=?",
                process.state().name(), process.revision(), encode(process), process.id(), expectedRevision);
        if (changed != 1) throw new IllegalStateException("STALE_PROCESS");
        if (process.state() == ProcessState.ACTIVE) {
            for (var indicator : process.indicators()) {
                jdbc.update(
                        "insert into process_indicator(process_id,indicator_id,code,name) values(?,?,?,?) on conflict do nothing",
                        process.id(), indicator.id(), indicator.code(), indicator.name());
            }
        }
    }

    @Override
    public Optional<SelfAssessmentProcess> find(UUID id) {
        return jdbc.query("select " + jsonColumn() + " from self_assessment_process where id=?",
                        (result, row) -> decode(result.getString(1)), id)
                .stream().findFirst();
    }

    @Override
    public List<SelfAssessmentProcess> list() {
        return jdbc.query("select " + jsonColumn() + " from self_assessment_process order by created_at desc",
                (result, row) -> decode(result.getString(1)));
    }

    @Override
    public void saveAssignment(ProcessAssignment assignment) {
        jdbc.update(
                "insert into process_assignment(id,process_id,indicator_id,identity_id,function_code,valid_from,valid_to,created_by,reason,revoked_at,revision) values(?,?,?,?,?,?,?,?,?,?,?)",
                assignment.id(), assignment.processId(), assignment.indicatorId(), assignment.identityId(),
                assignment.function().name(), Timestamp.from(assignment.validFrom()), Timestamp.from(assignment.validTo()),
                assignment.createdBy(), assignment.reason(),
                assignment.revokedAt() == null ? null : Timestamp.from(assignment.revokedAt()), assignment.revision());
    }

    @Override
    public List<ProcessAssignment> assignments(UUID id) {
        return jdbc.query(
                "select id,process_id,indicator_id,identity_id,function_code,valid_from,valid_to,created_by,reason,revoked_at,revision from process_assignment where process_id=? order by valid_from",
                (result, row) -> new ProcessAssignment(
                        JdbcValueSupport.uuid(result, 1), JdbcValueSupport.uuid(result, 2),
                        JdbcValueSupport.uuid(result, 3), JdbcValueSupport.uuid(result, 4),
                        ProcessAssignment.Function.valueOf(result.getString(5)),
                        JdbcValueSupport.instant(result, 6), JdbcValueSupport.instant(result, 7),
                        JdbcValueSupport.uuid(result, 8), result.getString(9),
                        JdbcValueSupport.instant(result, 10), result.getLong(11)),
                id);
    }

    @Override
    public void recordTransition(
            UUID id, ProcessState from, ProcessState to, UUID actor, String reason, UUID correlation, long revision) {
        jdbc.update(
                "insert into process_transition(id,process_id,from_state,to_state,actor_id,reason,occurred_at,correlation_id,resulting_revision) values(?,?,?,?,?,?,?,?,?)",
                UUID.randomUUID(), id, from.name(), to.name(), actor, reason == null ? "" : reason,
                Timestamp.from(Instant.now()), correlation, revision);
        if (to == ProcessState.CLOSED) {
            jdbc.update("insert into process_closure(id,process_id,closed_at,closed_by) values(?,?,?,?)",
                    UUID.randomUUID(), id, Timestamp.from(Instant.now()), actor);
        }
        if (to == ProcessState.REOPENED) {
            jdbc.update("update process_closure set reopened_at=?,reopened_by=?,reopen_reason=? where process_id=? and reopened_at is null",
                    Timestamp.from(Instant.now()), actor, reason, id);
        }
    }

    private String encode(SelfAssessmentProcess value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private SelfAssessmentProcess decode(String value) {
        try {
            return json.readValue(value, SelfAssessmentProcess.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String jsonParameter() {
        return postgres ? "?::jsonb" : "?";
    }

    private String jsonColumn() {
        return postgres ? "snapshot_json::text" : "snapshot_json";
    }
}
