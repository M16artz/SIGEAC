package ec.edu.unl.sec.selfassessment.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.unl.sec.selfassessment.application.SelfAssessmentRepository;
import ec.edu.unl.sec.selfassessment.application.ProcessProblemException;
import ec.edu.unl.sec.selfassessment.domain.ProcessActivity;
import ec.edu.unl.sec.selfassessment.domain.ProcessAssignment;
import ec.edu.unl.sec.selfassessment.domain.ProcessAuthorization;
import ec.edu.unl.sec.selfassessment.domain.ProcessState;
import ec.edu.unl.sec.selfassessment.domain.ProcessTeam;
import ec.edu.unl.sec.selfassessment.domain.SelfAssessmentProcess;
import ec.edu.unl.sec.selfassessment.domain.TeamMembership;
import ec.edu.unl.sec.shared.infrastructure.JdbcValueSupport;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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
                    "insert into self_assessment_process(id,career_id,code,name,period_start,period_end,model_version_id,model_version_number,state,revision,created_by,created_at,snapshot_json) values(?,?,?,?,?,?,?,?,?,?,?,?," + jsonParameter() + ")",
                    process.id(), process.careerId(), process.code(), process.name(), process.periodStart(),
                    process.periodEnd(), process.modelVersionId(), process.modelVersionNumber(), process.state().name(),
                    process.revision(), process.createdBy(), Timestamp.from(process.createdAt()), encode(process));
            return;
        }
        int changed = jdbc.update(
                "update self_assessment_process set state=?,revision=?,snapshot_json=" + jsonParameter()
                        + " where id=? and revision=?",
                process.state().name(), process.revision(), encode(process), process.id(), expectedRevision);
        if (changed != 1) throw new ProcessProblemException(ProcessProblemException.Kind.PRECONDITION,
                "STALE_PROCESS", "El proceso cambio; recargue antes de continuar.");
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
                "insert into process_assignment(id,process_id,indicator_id,identity_id,team_id,function_code,valid_from,valid_to,created_by,reason,revoked_at,revision) values(?,?,?,?,?,?,?,?,?,?,?,?)",
                assignment.id(), assignment.processId(), assignment.indicatorId(), assignment.identityId(),
                assignment.teamId(), assignment.function().name(), Timestamp.from(assignment.validFrom()),
                Timestamp.from(assignment.validTo()), assignment.createdBy(), assignment.reason(),
                assignment.revokedAt() == null ? null : Timestamp.from(assignment.revokedAt()), assignment.revision());
    }

    @Override
    public List<ProcessAssignment> assignments(UUID id) {
        return jdbc.query(
                "select id,process_id,indicator_id,identity_id,team_id,function_code,valid_from,valid_to,created_by,reason,revoked_at,revision from process_assignment where process_id=? order by valid_from",
                (result, row) -> new ProcessAssignment(
                        JdbcValueSupport.uuid(result, 1), JdbcValueSupport.uuid(result, 2),
                        JdbcValueSupport.uuid(result, 3), JdbcValueSupport.uuid(result, 4),
                        JdbcValueSupport.uuid(result, 5), ProcessAssignment.Function.valueOf(result.getString(6)),
                        JdbcValueSupport.instant(result, 7), JdbcValueSupport.instant(result, 8),
                        JdbcValueSupport.uuid(result, 9), result.getString(10),
                        JdbcValueSupport.instant(result, 11), result.getLong(12)),
                id);
    }

    @Override
    public void saveTeam(ProcessTeam team) {
        jdbc.update(
                "insert into process_team(id,process_id,name,process_role,valid_from,valid_until,created_by,created_at,revision) values(?,?,?,?,?,?,?,?,?)",
                team.id(), team.processId(), team.name(), team.processRole(), Timestamp.from(team.validFrom()),
                Timestamp.from(team.validUntil()), team.createdBy(), Timestamp.from(team.createdAt()), team.revision());
    }

    @Override
    public Optional<ProcessTeam> findTeam(UUID id) {
        return jdbc.query(
                "select id,process_id,name,process_role,valid_from,valid_until,created_by,created_at,revision from process_team where id=?",
                (result, row) -> new ProcessTeam(JdbcValueSupport.uuid(result, 1), JdbcValueSupport.uuid(result, 2),
                        result.getString(3), result.getString(4), JdbcValueSupport.instant(result, 5),
                        JdbcValueSupport.instant(result, 6), JdbcValueSupport.uuid(result, 7),
                        JdbcValueSupport.instant(result, 8), result.getLong(9)),
                id).stream().findFirst();
    }

    @Override
    public List<ProcessTeam> teams(UUID processId) {
        return jdbc.query(
                "select id,process_id,name,process_role,valid_from,valid_until,created_by,created_at,revision from process_team where process_id=? order by created_at",
                (result, row) -> new ProcessTeam(JdbcValueSupport.uuid(result, 1), JdbcValueSupport.uuid(result, 2),
                        result.getString(3), result.getString(4), JdbcValueSupport.instant(result, 5),
                        JdbcValueSupport.instant(result, 6), JdbcValueSupport.uuid(result, 7),
                        JdbcValueSupport.instant(result, 8), result.getLong(9)),
                processId);
    }

    @Override
    public void saveMembership(TeamMembership membership) {
        jdbc.update(
                "insert into team_membership(id,team_id,user_id,role_assignment_id,valid_from,valid_until,created_by,created_at) values(?,?,?,?,?,?,?,?)",
                membership.id(), membership.teamId(), membership.userId(), membership.roleAssignmentId(),
                Timestamp.from(membership.validFrom()), Timestamp.from(membership.validUntil()),
                membership.createdBy(), Timestamp.from(membership.createdAt()));
    }

    @Override
    public List<TeamMembership> memberships(UUID teamId) {
        return jdbc.query(
                "select id,team_id,user_id,role_assignment_id,valid_from,valid_until,created_by,created_at from team_membership where team_id=? order by created_at",
                (result, row) -> new TeamMembership(JdbcValueSupport.uuid(result, 1), JdbcValueSupport.uuid(result, 2),
                        JdbcValueSupport.uuid(result, 3), JdbcValueSupport.uuid(result, 4),
                        JdbcValueSupport.instant(result, 5), JdbcValueSupport.instant(result, 6),
                        JdbcValueSupport.uuid(result, 7), JdbcValueSupport.instant(result, 8)),
                teamId);
    }

    @Override
    @Transactional
    public void saveActivity(ProcessActivity activity, long expectedRevision) {
        if (expectedRevision < 0) {
            jdbc.update(
                    "insert into process_activity(id,process_id,indicator_id,required_source_id,assignment_id,title,due_at,status,created_by,created_at,revision,snapshot_json) values(?,?,?,?,?,?,?,?,?,?,?," + jsonParameter() + ")",
                    activity.id(), activity.processId(), activity.indicatorId(), activity.requiredSourceId(),
                    activity.assignmentId(), activity.title(), Timestamp.from(activity.dueAt()), activity.status().name(),
                    activity.createdBy(), Timestamp.from(activity.createdAt()), activity.revision(), encode(activity));
        } else {
            jdbc.update(
                    "insert into process_activity_revision(id,activity_id,revision,snapshot_json,recorded_at) select ?,id,revision,snapshot_json,? from process_activity where id=? and revision=?",
                    UUID.randomUUID(), Timestamp.from(Instant.now()), activity.id(), expectedRevision);
            int changed = jdbc.update(
                    "update process_activity set indicator_id=?,required_source_id=?,assignment_id=?,title=?,due_at=?,status=?,revision=?,snapshot_json=" + jsonParameter()
                            + " where id=? and revision=?",
                    activity.indicatorId(), activity.requiredSourceId(), activity.assignmentId(), activity.title(),
                    Timestamp.from(activity.dueAt()), activity.status().name(), activity.revision(), encode(activity),
                    activity.id(), expectedRevision);
            if (changed != 1) throw new ProcessProblemException(ProcessProblemException.Kind.PRECONDITION,
                    "STALE_ACTIVITY", "La actividad cambio; recargue antes de continuar.");
        }
        jdbc.update("delete from process_activity_dependency where activity_id=?", activity.id());
        for (UUID dependency : activity.dependencyIds()) {
            jdbc.update(
                    "insert into process_activity_dependency(activity_id,depends_on_activity_id) values(?,?)",
                    activity.id(), dependency);
        }
    }

    @Override
    public Optional<ProcessActivity> findActivity(UUID id) {
        return jdbc.query("select " + jsonColumn() + " from process_activity where id=?",
                        (result, row) -> decodeActivity(result.getString(1)), id)
                .stream().findFirst();
    }

    @Override
    public List<ProcessActivity> activities(UUID processId) {
        return jdbc.query("select " + jsonColumn() + " from process_activity where process_id=? order by due_at",
                (result, row) -> decodeActivity(result.getString(1)), processId);
    }

    @Override
    public void saveAuthorization(UUID processId, ProcessAuthorization authorization) {
        jdbc.update(
                "insert into process_authorization(id,process_id,decision_type,authority_assignment_id,legal_basis,document_reference,reason,decided_by,decided_at) values(?,?,?,?,?,?,?,?,?)",
                authorization.id(), processId, "INITIAL", authorization.authorityAssignmentId(),
                authorization.legalBasis(), authorization.documentReference(), authorization.reason(),
                authorization.authorizedBy(), Timestamp.from(authorization.authorizedAt()));
    }

    @Override
    @Transactional
    public void recordTransition(UUID id, ProcessState from, ProcessState to, UUID actor, String reason,
            UUID correlation, long revision, ProcessAuthorization decision) {
        jdbc.update(
                "insert into process_transition(id,process_id,from_state,to_state,actor_id,reason,occurred_at,correlation_id,resulting_revision,authority_assignment_id,legal_basis,document_reference) values(?,?,?,?,?,?,?,?,?,?,?,?)",
                UUID.randomUUID(), id, from.name(), to.name(), actor, reason == null ? "" : reason,
                Timestamp.from(Instant.now()), correlation, revision,
                decision == null ? null : decision.authorityAssignmentId(),
                decision == null ? null : decision.legalBasis(),
                decision == null ? null : decision.documentReference());
        if (decision != null) {
            jdbc.update(
                    "insert into process_authorization(id,process_id,decision_type,authority_assignment_id,legal_basis,document_reference,reason,decided_by,decided_at) values(?,?,?,?,?,?,?,?,?)",
                    decision.id(), id, decisionType(from, to), decision.authorityAssignmentId(),
                    decision.legalBasis(), decision.documentReference(), decision.reason(),
                    decision.authorizedBy(), Timestamp.from(decision.authorizedAt()));
        }
        if (to == ProcessState.CLOSED) {
            jdbc.update("insert into process_closure(id,process_id,closed_at,closed_by) values(?,?,?,?)",
                    UUID.randomUUID(), id, Timestamp.from(Instant.now()), actor);
        }
        if (to == ProcessState.REOPENED) {
            jdbc.update("update process_closure set reopened_at=?,reopened_by=?,reopen_reason=? where process_id=? and reopened_at is null",
                    Timestamp.from(Instant.now()), actor, reason, id);
        }
    }

    private static String decisionType(ProcessState from, ProcessState to) {
        if (to == ProcessState.SUSPENDED) return "SUSPENSION";
        if (from == ProcessState.SUSPENDED && to == ProcessState.ACTIVE) return "RESUMPTION";
        if (to == ProcessState.REOPENED) return "REOPENING";
        return "TRANSITION";
    }

    private String encode(Object value) {
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

    private ProcessActivity decodeActivity(String value) {
        try {
            return json.readValue(value, ProcessActivity.class);
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
