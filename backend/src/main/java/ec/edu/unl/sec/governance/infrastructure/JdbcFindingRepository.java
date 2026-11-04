package ec.edu.unl.sec.governance.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.unl.sec.governance.application.FindingRepository;
import ec.edu.unl.sec.governance.domain.Consolidation;
import ec.edu.unl.sec.governance.domain.Finding;
import ec.edu.unl.sec.shared.infrastructure.JdbcValueSupport;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcFindingRepository implements FindingRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public JdbcFindingRepository(JdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Override public void appendFinding(Finding finding) {
        try {
            jdbc.update("insert into governance_finding(id,process_id,registered_by,registered_at,payload_json) values(?,?,?,?,?)",
                    finding.id(), finding.processId(), finding.createdBy(), Timestamp.from(finding.createdAt()),
                    json.writeValueAsString(finding));
        } catch (JsonProcessingException ex) { throw new IllegalStateException("Cannot serialize finding", ex); }
    }

    @Override public Optional<Finding> findFinding(UUID processId, UUID findingId) {
        return jdbc.query("select * from governance_finding where process_id=? and id=?",
                (row, i) -> readFinding(row), processId, findingId).stream().findFirst();
    }

    @Override public List<Finding> findings(UUID processId, long offset, long limit) {
        return jdbc.query("select * from governance_finding where process_id=? order by registered_at desc, id desc limit ? offset ?",
                (row, i) -> readFinding(row), processId, limit, offset);
    }

    @Override public List<Finding> byIds(UUID processId, List<UUID> ids) {
        var distinct = ids.stream().distinct().toList();
        if (distinct.isEmpty()) return List.of();
        if (distinct.size() > 500) throw new IllegalArgumentException("too many findings for one consolidation");
        var placeholders = String.join(",", java.util.Collections.nCopies(distinct.size(), "?"));
        var parameters = new ArrayList<Object>();
        parameters.add(processId);
        parameters.addAll(distinct);
        return jdbc.query("select * from governance_finding where process_id=? and id in (" + placeholders + ") order by registered_at desc, id desc",
                (row, i) -> readFinding(row), parameters.toArray());
    }

    @Override public long totalFindings(UUID processId) {
        var total = jdbc.queryForObject("select count(*) from governance_finding where process_id=?", Long.class, processId);
        return total == null ? 0L : total;
    }

    @Override public void appendConsolidation(Consolidation consolidation) {
        try {
            jdbc.update("insert into governance_finding_consolidation(id,process_id,revision,predecessor_id,consolidated_by,consolidated_at,payload_json)"
                            + " values(?,?,?,?,?,?,?)",
                    consolidation.id(), consolidation.processId(), consolidation.revision(), consolidation.predecessorId(),
                    consolidation.createdBy(), Timestamp.from(consolidation.createdAt()), json.writeValueAsString(consolidation));
        } catch (JsonProcessingException ex) { throw new IllegalStateException("Cannot serialize consolidation", ex); }
        jdbc.batchUpdate("insert into governance_consolidation_finding(consolidation_id,finding_id) values(?,?)",
                consolidation.data().findingIds().stream().map(findingId -> new Object[]{consolidation.id(), findingId}).toList());
    }

    @Override public Optional<Consolidation> latestConsolidation(UUID processId) {
        return jdbc.query("select * from governance_finding_consolidation where process_id=? order by revision desc limit 1",
                (row, i) -> readConsolidation(row), processId).stream().findFirst();
    }

    @Override public long totalConsolidations(UUID processId) {
        var total = jdbc.queryForObject("select count(*) from governance_finding_consolidation where process_id=?",
                Long.class, processId);
        return total == null ? 0L : total;
    }

    @Override public Set<UUID> findingsPresentInConsolidations(UUID processId) {
        return new HashSet<>(jdbc.query("select distinct cf.finding_id from governance_consolidation_finding cf"
                        + " join governance_finding_consolidation c on c.id=cf.consolidation_id where c.process_id=?",
                (row, i) -> JdbcValueSupport.uuid(row, 1), processId));
    }

    private Finding readFinding(ResultSet row) throws SQLException {
        try {
            var value = json.readValue(row.getString("payload_json"), Finding.class);
            if (!value.id().equals(JdbcValueSupport.uuid(row, row.findColumn("id")))
                    || !value.processId().equals(JdbcValueSupport.uuid(row, row.findColumn("process_id")))
                    || !value.createdBy().equals(JdbcValueSupport.uuid(row, row.findColumn("registered_by")))
                    || !value.createdAt().equals(JdbcValueSupport.instant(row, row.findColumn("registered_at"))))
                throw new IllegalStateException("Finding metadata mismatch");
            return value;
        } catch (JsonProcessingException ex) { throw new IllegalStateException("Cannot read finding", ex); }
    }

    private Consolidation readConsolidation(ResultSet row) throws SQLException {
        try {
            var value = json.readValue(row.getString("payload_json"), Consolidation.class);
            if (!value.id().equals(JdbcValueSupport.uuid(row, row.findColumn("id")))
                    || !value.processId().equals(JdbcValueSupport.uuid(row, row.findColumn("process_id")))
                    || value.revision() != row.getLong("revision")
                    || !java.util.Objects.equals(value.predecessorId(), JdbcValueSupport.uuid(row, row.findColumn("predecessor_id")))
                    || !value.createdBy().equals(JdbcValueSupport.uuid(row, row.findColumn("consolidated_by")))
                    || !value.createdAt().equals(JdbcValueSupport.instant(row, row.findColumn("consolidated_at"))))
                throw new IllegalStateException("Consolidation metadata mismatch");
            return value;
        } catch (JsonProcessingException ex) { throw new IllegalStateException("Cannot read consolidation", ex); }
    }
}
