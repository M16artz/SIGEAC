package ec.edu.unl.sec.archive.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.unl.sec.archive.application.IntegralClosureRepository;
import ec.edu.unl.sec.archive.domain.ArchiveRecord;
import ec.edu.unl.sec.archive.domain.IntegralClosure;
import ec.edu.unl.sec.shared.infrastructure.JdbcValueSupport;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcIntegralClosureRepository implements IntegralClosureRepository {
    private static final TypeReference<List<UUID>> IDS = new TypeReference<>() {};
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final boolean postgres;

    public JdbcIntegralClosureRepository(JdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
        this.postgres = Boolean.TRUE.equals(jdbc.execute((ConnectionCallback<Boolean>) connection ->
                connection.getMetaData().getDatabaseProductName().contains("PostgreSQL")));
    }

    @Override
    public void append(IntegralClosure closure) {
        try {
            jdbc.update("insert into integral_closure(id,process_id,documentary_closure_id,policy_version,"
                            + "payload_json,sha256,pending_ids,exception_decision_ids,closed_by,closed_at,revision)"
                            + " values(?,?,?,?," + jsonParameter() + ",?," + jsonParameter() + ","
                            + jsonParameter() + ",?,?,?)",
                    closure.id(), closure.processId(), closure.documentaryClosureId(), closure.policyVersion(),
                    closure.payloadJson(), closure.sha256(), json.writeValueAsString(closure.pendingIds()),
                    json.writeValueAsString(closure.exceptionDecisionIds()), closure.closedBy(),
                    Timestamp.from(closure.closedAt()), 1);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize integral closure", exception);
        }
    }

    @Override
    public Optional<IntegralClosure> find(UUID closureId) {
        return jdbc.query("select " + columns() + " from integral_closure where id=?",
                (row, index) -> read(row), closureId).stream().findFirst();
    }

    @Override
    public Optional<IntegralClosure> findForProcess(UUID processId, UUID closureId) {
        return jdbc.query("select " + columns() + " from integral_closure where process_id=? and id=?",
                (row, index) -> read(row), processId, closureId).stream().findFirst();
    }

    @Override
    public List<IntegralClosure> closures(UUID processId, long offset, int limit) {
        return jdbc.query("select " + columns() + " from integral_closure where process_id=?"
                        + " order by closed_at desc, id desc limit ? offset ?",
                (row, index) -> read(row), processId, limit, offset);
    }

    @Override
    public long closureTotal(UUID processId) {
        Long total = jdbc.queryForObject("select count(*) from integral_closure where process_id=?",
                Long.class, processId);
        return total == null ? 0L : total;
    }

    @Override
    public void appendArchive(ArchiveRecord record) {
        jdbc.update("insert into archive_record(id,process_id,closure_snapshot_id,retention_policy_version,"
                        + "archived_by,archived_at,integrity_hash,integrity_status) values(?,?,?,?,?,?,?,?)",
                record.id(), record.processId(), record.closureSnapshotId(), record.retentionPolicyVersion(),
                record.archivedBy(), Timestamp.from(record.archivedAt()), record.integrityHash(),
                record.integrityStatus());
    }

    @Override
    public Optional<ArchiveRecord> archiveForClosure(UUID closureId) {
        return jdbc.query("select id,process_id,closure_snapshot_id,retention_policy_version,archived_by,"
                        + "archived_at,integrity_hash,integrity_status from archive_record where closure_snapshot_id=?",
                (row, index) -> archive(row), closureId).stream().findFirst();
    }

    private IntegralClosure read(ResultSet row) throws SQLException {
        try {
            return new IntegralClosure(JdbcValueSupport.uuid(row, row.findColumn("id")),
                    JdbcValueSupport.uuid(row, row.findColumn("process_id")),
                    JdbcValueSupport.uuid(row, row.findColumn("documentary_closure_id")),
                    row.getString("policy_version"), row.getString("payload_json"), row.getString("sha256"),
                    json.readValue(row.getString("pending_ids"), IDS),
                    json.readValue(row.getString("exception_decision_ids"), IDS),
                    JdbcValueSupport.uuid(row, row.findColumn("closed_by")),
                    JdbcValueSupport.instant(row, row.findColumn("closed_at")));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot read integral closure", exception);
        }
    }

    private ArchiveRecord archive(ResultSet row) throws SQLException {
        return new ArchiveRecord(JdbcValueSupport.uuid(row, row.findColumn("id")),
                JdbcValueSupport.uuid(row, row.findColumn("process_id")),
                JdbcValueSupport.uuid(row, row.findColumn("closure_snapshot_id")),
                row.getString("retention_policy_version"),
                JdbcValueSupport.uuid(row, row.findColumn("archived_by")),
                JdbcValueSupport.instant(row, row.findColumn("archived_at")),
                row.getString("integrity_hash"), row.getString("integrity_status"));
    }

    private String columns() {
        return "id,process_id,documentary_closure_id,policy_version," + jsonColumn("payload_json")
                + ",sha256," + jsonColumn("pending_ids") + "," + jsonColumn("exception_decision_ids")
                + ",closed_by,closed_at";
    }

    private String jsonParameter() {
        return postgres ? "?::jsonb" : "?";
    }

    private String jsonColumn(String column) {
        return postgres ? column + "::text" : column;
    }
}
