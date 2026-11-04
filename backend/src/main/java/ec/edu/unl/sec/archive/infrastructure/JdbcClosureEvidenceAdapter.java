package ec.edu.unl.sec.archive.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.unl.sec.archive.application.ClosureEvidencePort;
import ec.edu.unl.sec.archive.domain.IntegralClosurePolicy;
import ec.edu.unl.sec.shared.infrastructure.JdbcValueSupport;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class JdbcClosureEvidenceAdapter implements ClosureEvidencePort {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public JdbcClosureEvidenceAdapter(JdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Override
    public List<DocumentaryClosureEvidence> documentaryClosures(UUID processId) {
        return jdbc.query("select id,snapshot_id,policy_version,sha256,closed_at,closed_by from documentary_closure"
                        + " where process_id=? order by closed_at desc, id desc",
                (row, index) -> new DocumentaryClosureEvidence(
                        JdbcValueSupport.uuid(row, row.findColumn("id")),
                        JdbcValueSupport.uuid(row, row.findColumn("snapshot_id")),
                        row.getString("policy_version"), row.getString("sha256"),
                        JdbcValueSupport.instant(row, row.findColumn("closed_at")),
                        JdbcValueSupport.uuid(row, row.findColumn("closed_by"))),
                processId);
    }

    @Override
    public Optional<ReportEvidence> finalReport(UUID processId) {
        return jdbc.query("select v.id,v.version_number,p.state from governance_report_version v"
                        + " join governance_report_processing p on p.version_id=v.id"
                        + " where v.process_id=? order by v.version_number desc limit 1",
                (row, index) -> new ReportEvidence(JdbcValueSupport.uuid(row, row.findColumn("id")),
                        row.getInt("version_number"), row.getString("state")),
                processId).stream().findFirst();
    }

    @Override
    public List<IntegralClosurePolicy.ActionSnapshot> actions(UUID processId) {
        return jdbc.query("select id,state,payload_json from improvement_action where process_id=?"
                        + " order by revised_at asc, id asc",
                (row, index) -> new IntegralClosurePolicy.ActionSnapshot(
                        JdbcValueSupport.uuid(row, row.findColumn("id")),
                        title(row.getString("payload_json")), row.getString("state"),
                        dueAt(row.getString("payload_json"))),
                processId);
    }

    @Override
    public boolean authorizedDecision(UUID processId, UUID decisionId) {
        Long total = jdbc.queryForObject("select count(*) from process_authorization where process_id=? and id=?",
                Long.class, processId, decisionId);
        return total != null && total > 0;
    }

    private String title(String payload) {
        try {
            return json.readTree(payload).path("data").path("title").asText("Acción de mejora");
        } catch (Exception invalid) {
            return "Acción de mejora";
        }
    }

    private Instant dueAt(String payload) {
        try {
            var value = json.readTree(payload).path("data").path("dueAt");
            if (value.isNumber()) {
                var seconds = value.decimalValue();
                long whole = seconds.longValue();
                int nanos = seconds.subtract(java.math.BigDecimal.valueOf(whole)).movePointRight(9).intValue();
                return Instant.ofEpochSecond(whole, nanos);
            }
            var text = value.asText(null);
            return text == null || text.isBlank() ? null : Instant.parse(text);
        } catch (Exception invalid) {
            return null;
        }
    }
}
