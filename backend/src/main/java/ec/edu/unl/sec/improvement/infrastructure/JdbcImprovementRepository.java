package ec.edu.unl.sec.improvement.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.unl.sec.improvement.application.ImprovementRepository;
import ec.edu.unl.sec.improvement.domain.ActionDecision;
import ec.edu.unl.sec.improvement.domain.ActionProgress;
import ec.edu.unl.sec.improvement.domain.ImprovementAction;
import ec.edu.unl.sec.improvement.domain.ImprovementPlan;
import ec.edu.unl.sec.shared.infrastructure.JdbcValueSupport;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JdbcImprovementRepository implements ImprovementRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public JdbcImprovementRepository(JdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Override @Transactional
    public void appendPlan(ImprovementPlan plan) {
        try {
            jdbc.update("insert into improvement_plan(id,process_id,report_version_id,created_by,created_at,payload_json)"
                            + " values(?,?,?,?,?,?)",
                    plan.id(), plan.processId(), plan.reportVersionId(), plan.createdBy(),
                    Timestamp.from(plan.createdAt()), json.writeValueAsString(plan));
        } catch (JsonProcessingException ex) { throw new IllegalStateException("Cannot serialize improvement plan", ex); }
    }

    @Override public Optional<ImprovementPlan> findPlan(UUID planId) {
        return jdbc.query("select payload_json from improvement_plan where id=?",
                (row, i) -> readPlan(row), planId).stream().findFirst();
    }

    @Override public long planTotal(UUID processId) {
        var total = jdbc.queryForObject("select count(*) from improvement_plan where process_id=?", Long.class, processId);
        return total == null ? 0L : total;
    }

    @Override public Optional<String> reportVersionState(UUID processId, UUID reportVersionId) {
        return jdbc.query("select p.state from governance_report_version v"
                        + " join governance_report_processing p on p.version_id=v.id"
                        + " where v.id=? and v.process_id=?",
                (row, i) -> row.getString("state"), reportVersionId, processId).stream().findFirst();
    }

    @Override public Set<UUID> findings(UUID processId, Collection<UUID> findingIds) {
        Set<UUID> found = new LinkedHashSet<>();
        for (UUID id : findingIds) {
            if (id == null) continue;
            jdbc.query("select id from governance_finding where process_id=? and id=?",
                    (row, i) -> JdbcValueSupport.uuid(row, row.findColumn("id")), processId, id)
                    .stream().findFirst().ifPresent(found::add);
        }
        return found;
    }

    @Override @Transactional
    public void appendAction(ImprovementAction action, long predecessorRevision) {
        try {
            jdbc.update("insert into improvement_action(id,process_id,plan_id,state,revision,revised_by,revised_at,payload_json)"
                            + " values(?,?,?,?,?,?,?,?)",
                    action.id(), action.processId(), action.data().planId(), action.state().name(),
                    action.revision(), action.createdBy(), Timestamp.from(action.createdAt()),
                    json.writeValueAsString(action));
            insertRevision(action, predecessorRevision);
        } catch (JsonProcessingException ex) { throw new IllegalStateException("Cannot serialize improvement action", ex); }
    }

    @Override public Optional<ImprovementAction> findAction(UUID actionId) {
        return jdbc.query("select payload_json, revision, state from improvement_action where id=?",
                (row, i) -> readAction(row), actionId).stream().findFirst();
    }

    @Override public List<ImprovementAction> actions(UUID processId, long offset, long limit) {
        return jdbc.query("select payload_json, revision, state from improvement_action where process_id=?"
                        + " order by revised_at desc, id desc limit ? offset ?",
                (row, i) -> readAction(row), processId, limit, offset);
    }

    @Override public long actionTotal(UUID processId) {
        var total = jdbc.queryForObject("select count(*) from improvement_action where process_id=?", Long.class,
                processId);
        return total == null ? 0L : total;
    }

    @Override @Transactional
    public boolean replaceHead(ImprovementAction head, long expectedRevision) {
        try {
            var updated = jdbc.update("update improvement_action set state=?,revision=?,revised_by=?,revised_at=?,"
                            + "payload_json=? where id=? and revision=?",
                    head.state().name(), head.revision(), head.createdBy(), Timestamp.from(head.createdAt()),
                    json.writeValueAsString(head), head.id(), expectedRevision);
            if (updated != 1) return false;
            insertRevision(head, expectedRevision);
            return true;
        } catch (JsonProcessingException ex) { throw new IllegalStateException("Cannot serialize improvement action", ex); }
    }

    private void insertRevision(ImprovementAction action, long predecessorRevision) throws JsonProcessingException {
        jdbc.update("insert into improvement_action_revision(action_id,revision,predecessor_revision,state,changed_by,"
                        + "changed_at,payload_json) values(?,?,?,?,?,?,?)",
                action.id(), action.revision(), predecessorRevision < 1 ? null : predecessorRevision,
                action.state().name(), action.createdBy(), Timestamp.from(action.createdAt()),
                json.writeValueAsString(action));
    }

    @Override public void appendProgress(ActionProgress progress) {
        try {
            jdbc.update("insert into improvement_progress(id,action_id,recorded_by,recorded_at,payload_json)"
                            + " values(?,?,?,?,?)",
                    progress.id(), progress.actionId(), progress.recordedBy(),
                    Timestamp.from(progress.createdAt()), json.writeValueAsString(progress));
        } catch (JsonProcessingException ex) { throw new IllegalStateException("Cannot serialize progress", ex); }
    }

    @Override public List<ActionProgress> progressFor(UUID actionId) {
        return jdbc.query("select payload_json from improvement_progress where action_id=?"
                        + " order by recorded_at asc, id asc",
                (row, i) -> readProgress(row), actionId);
    }

    @Override public void appendDecision(ActionDecision decision) {
        try {
            jdbc.update("insert into improvement_decision(id,action_id,action_code,decided_by,decided_at,payload_json)"
                            + " values(?,?,?,?,?,?)",
                    decision.id(), decision.actionId(), decision.action().name(),
                    decision.provenance().decidedBy(), Timestamp.from(decision.provenance().decidedAt()),
                    json.writeValueAsString(decision));
        } catch (JsonProcessingException ex) { throw new IllegalStateException("Cannot serialize decision", ex); }
    }

    @Override public List<ActionDecision> decisionsFor(UUID actionId) {
        return jdbc.query("select payload_json from improvement_decision where action_id=?"
                        + " order by decided_at asc, id asc",
                (row, i) -> readDecision(row), actionId);
    }

    @Override public Optional<Assignment> activeAssignment(UUID assignmentId) {
        return jdbc.query("select id,identity_id,function_id,career_id,process_id,valid_from,valid_to"
                        + " from functional_assignment where id=?",
                (row, i) -> new Assignment(JdbcValueSupport.uuid(row, row.findColumn("id")),
                        JdbcValueSupport.uuid(row, row.findColumn("identity_id")),
                        JdbcValueSupport.uuid(row, row.findColumn("function_id")),
                        JdbcValueSupport.uuid(row, row.findColumn("career_id")),
                        JdbcValueSupport.uuid(row, row.findColumn("process_id")),
                        JdbcValueSupport.instant(row, row.findColumn("valid_from")),
                        JdbcValueSupport.instant(row, row.findColumn("valid_to"))),
                assignmentId).stream().findFirst();
    }

    private ImprovementPlan readPlan(ResultSet row) throws SQLException {
        try { return json.readValue(row.getString("payload_json"), ImprovementPlan.class); }
        catch (JsonProcessingException ex) { throw new IllegalStateException("Cannot read improvement plan", ex); }
    }

    private ImprovementAction readAction(ResultSet row) throws SQLException {
        try {
            var action = json.readValue(row.getString("payload_json"), ImprovementAction.class);
            if (action.revision() != row.getLong("revision")
                    || !action.state().name().equals(row.getString("state")))
                throw new IllegalStateException("Improvement action metadata mismatch");
            return action;
        } catch (JsonProcessingException ex) { throw new IllegalStateException("Cannot read improvement action", ex); }
    }

    private ActionProgress readProgress(ResultSet row) throws SQLException {
        try { return json.readValue(row.getString("payload_json"), ActionProgress.class); }
        catch (JsonProcessingException ex) { throw new IllegalStateException("Cannot read progress", ex); }
    }

    private ActionDecision readDecision(ResultSet row) throws SQLException {
        try { return json.readValue(row.getString("payload_json"), ActionDecision.class); }
        catch (JsonProcessingException ex) { throw new IllegalStateException("Cannot read decision", ex); }
    }
}
