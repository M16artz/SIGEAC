package ec.edu.unl.sec.governance.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.unl.sec.governance.application.ReportRepository;
import ec.edu.unl.sec.governance.domain.ApprovalDecision;
import ec.edu.unl.sec.governance.domain.ObservationReply;
import ec.edu.unl.sec.governance.domain.ReportObservation;
import ec.edu.unl.sec.governance.domain.ReportSocialization;
import ec.edu.unl.sec.governance.domain.ReportVersion;
import ec.edu.unl.sec.shared.domain.DomainClock;
import ec.edu.unl.sec.shared.infrastructure.JdbcValueSupport;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JdbcReportRepository implements ReportRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final DomainClock clock = DomainClock.systemUtc();

    public JdbcReportRepository(JdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Override @Transactional
    public void appendVersion(ReportVersion version) {
        try {
            jdbc.update("insert into governance_report_version(id,process_id,version_number,predecessor_version_id,"
                            + "emitted_by,emitted_at,content_sha256,payload_json) values(?,?,?,?,?,?,?,?)",
                    version.id(), version.processId(), version.versionNumber(), version.predecessorVersionId(),
                    version.createdBy(), Timestamp.from(version.createdAt()), version.contentSha256(),
                    json.writeValueAsString(version));
        } catch (JsonProcessingException ex) { throw new IllegalStateException("Cannot serialize report version", ex); }
        jdbc.update("insert into governance_report_processing(version_id,process_id,state,revision,updated_at) values(?,?,?,?,?)",
                version.id(), version.processId(), version.data().kind().name(), 0L, Timestamp.from(clock.now()));
    }

    @Override public Optional<ReportVersion> latest(UUID processId) {
        return jdbc.query("select * from governance_report_version where process_id=? order by version_number desc limit 1",
                (row, i) -> readVersion(row), processId).stream().findFirst();
    }

    @Override public List<ReportVersion> versions(UUID processId, long offset, long limit) {
        return jdbc.query("select * from governance_report_version where process_id=? order by version_number asc limit ? offset ?",
                (row, i) -> readVersion(row), processId, limit, offset);
    }

    @Override public long totalVersions(UUID processId) {
        var total = jdbc.queryForObject("select count(*) from governance_report_version where process_id=?",
                Long.class, processId);
        return total == null ? 0L : total;
    }

    @Override public Optional<ReportVersion> findVersion(UUID versionId) {
        return jdbc.query("select * from governance_report_version where id=?", (row, i) -> readVersion(row), versionId)
                .stream().findFirst();
    }

    @Override public boolean hasApprovedFinal(UUID processId) {
        var approved = jdbc.queryForObject("select count(*) from governance_report_decision d"
                        + " join governance_report_version v on v.id=d.version_id"
                        + " where v.process_id=? and d.payload_json like '%\"APPROVED\"%'",
                Long.class, processId);
        return approved != null && approved > 0;
    }

    @Override public Optional<Processing> processing(UUID versionId) {
        return jdbc.query("select state,revision from governance_report_processing where version_id=?",
                (row, i) -> new Processing(row.getString("state"), row.getLong("revision")), versionId).stream().findFirst();
    }

    @Override public boolean advanceProcessing(UUID versionId, long expectedRevision, String nextState) {
        var updated = jdbc.update("update governance_report_processing set revision=revision+1,"
                        + "state=coalesce(?,state),updated_at=? where version_id=? and revision=?",
                nextState, Timestamp.from(clock.now()), versionId, expectedRevision);
        return updated == 1;
    }

    @Override public void appendObservation(ReportObservation observation) {
        try {
            jdbc.update("insert into governance_report_observation(id,version_id,observed_by,observed_at,payload_json) values(?,?,?,?,?)",
                    observation.id(), observation.versionId(), observation.createdBy(),
                    Timestamp.from(observation.createdAt()), json.writeValueAsString(observation));
        } catch (JsonProcessingException ex) { throw new IllegalStateException("Cannot serialize observation", ex); }
    }

    @Override public void appendSocialization(ReportSocialization socialization) {
        try {
            jdbc.update("insert into governance_report_socialization(id,version_id,socialized_by,recorded_at,payload_json) values(?,?,?,?,?)",
                    socialization.id(), socialization.versionId(), socialization.recordedBy(),
                    Timestamp.from(socialization.recordedAt()), json.writeValueAsString(socialization));
        } catch (JsonProcessingException ex) { throw new IllegalStateException("Cannot serialize socialization", ex); }
    }

    @Override public void appendReply(ObservationReply reply) {
        try {
            jdbc.update("insert into governance_report_reply(id,observation_id,replied_by,replied_at,payload_json) values(?,?,?,?,?)",
                    reply.id(), reply.observationId(), reply.recordedBy(), Timestamp.from(reply.recordedAt()),
                    json.writeValueAsString(reply));
        } catch (JsonProcessingException ex) { throw new IllegalStateException("Cannot serialize reply", ex); }
    }

    @Override public void appendDecision(ApprovalDecision decision) {
        try {
            jdbc.update("insert into governance_report_decision(id,version_id,decided_by,authority_assignment_id,decided_at,payload_json)"
                            + " values(?,?,?,?,?,?)",
                    decision.id(), decision.versionId(), decision.provenance().decidedBy(),
                    decision.provenance().authorityAssignmentId(), Timestamp.from(decision.provenance().decidedAt()),
                    json.writeValueAsString(decision));
        } catch (JsonProcessingException ex) { throw new IllegalStateException("Cannot serialize decision", ex); }
    }

    @Override public Optional<ReportObservation> findObservation(UUID observationId) {
        return jdbc.query("select * from governance_report_observation where id=?",
                (row, i) -> readObservation(row), observationId).stream().findFirst();
    }

    @Override public List<ReportObservation> observations(UUID versionId) {
        return jdbc.query("select * from governance_report_observation where version_id=? order by observed_at asc, id asc",
                (row, i) -> readObservation(row), versionId);
    }

    @Override public List<ReportSocialization> socializations(UUID versionId) {
        return jdbc.query("select * from governance_report_socialization where version_id=? order by recorded_at asc, id asc",
                (row, i) -> readSocialization(row), versionId);
    }

    @Override public List<ObservationReply> repliesForVersion(UUID versionId) {
        return jdbc.query("select r.* from governance_report_reply r join governance_report_observation o"
                        + " on o.id=r.observation_id where o.version_id=? order by r.replied_at asc, r.id asc",
                (row, i) -> readReply(row), versionId);
    }

    @Override public List<ApprovalDecision> decisions(UUID versionId) {
        return jdbc.query("select * from governance_report_decision where version_id=? order by decided_at asc, id asc",
                (row, i) -> readDecision(row), versionId);
    }

    private ReportVersion readVersion(ResultSet row) throws SQLException {
        try {
            var value = json.readValue(row.getString("payload_json"), ReportVersion.class);
            if (!value.id().equals(JdbcValueSupport.uuid(row, row.findColumn("id")))
                    || !value.processId().equals(JdbcValueSupport.uuid(row, row.findColumn("process_id")))
                    || value.versionNumber() != row.getLong("version_number")
                    || !java.util.Objects.equals(value.predecessorVersionId(),
                            JdbcValueSupport.uuid(row, row.findColumn("predecessor_version_id")))
                    || !value.contentSha256().equals(row.getString("content_sha256")))
                throw new IllegalStateException("Report version metadata mismatch");
            return value;
        } catch (JsonProcessingException ex) { throw new IllegalStateException("Cannot read report version", ex); }
    }

    private ReportObservation readObservation(ResultSet row) throws SQLException {
        try { return json.readValue(row.getString("payload_json"), ReportObservation.class); }
        catch (JsonProcessingException ex) { throw new IllegalStateException("Cannot read observation", ex); }
    }

    private ReportSocialization readSocialization(ResultSet row) throws SQLException {
        try { return json.readValue(row.getString("payload_json"), ReportSocialization.class); }
        catch (JsonProcessingException ex) { throw new IllegalStateException("Cannot read socialization", ex); }
    }

    private ObservationReply readReply(ResultSet row) throws SQLException {
        try { return json.readValue(row.getString("payload_json"), ObservationReply.class); }
        catch (JsonProcessingException ex) { throw new IllegalStateException("Cannot read reply", ex); }
    }

    private ApprovalDecision readDecision(ResultSet row) throws SQLException {
        try { return json.readValue(row.getString("payload_json"), ApprovalDecision.class); }
        catch (JsonProcessingException ex) { throw new IllegalStateException("Cannot read decision", ex); }
    }
}
