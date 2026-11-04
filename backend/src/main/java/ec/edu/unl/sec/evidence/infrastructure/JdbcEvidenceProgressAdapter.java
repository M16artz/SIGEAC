package ec.edu.unl.sec.evidence.infrastructure;

import ec.edu.unl.sec.evidence.application.EvidenceProgressPort;
import ec.edu.unl.sec.shared.infrastructure.JdbcValueSupport;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JdbcEvidenceProgressAdapter implements EvidenceProgressPort {
    private final JdbcTemplate jdbc;
    private final NamedParameterJdbcTemplate named;
    private final boolean sqlite;

    public JdbcEvidenceProgressAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        this.named = new NamedParameterJdbcTemplate(jdbc);
        this.sqlite = Boolean.TRUE.equals(jdbc.execute((ConnectionCallback<Boolean>) connection ->
                !connection.getMetaData().getDatabaseProductName().contains("PostgreSQL")));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CurrentCoverage> currentCoverages(UUID processId, Set<UUID> sourceIds) {
        if (sourceIds.isEmpty()) return List.of();
        return named.query("select e.id,v.id,c.required_source_id,v.title from evidence e"
                        + " join evidence_version v on v.evidence_id=e.id"
                        + " join source_coverage c on c.version_id=v.id"
                        + " where e.process_id=:process and v.status='SUBMITTED'"
                        + " and c.required_source_id in (:sources)"
                        + " and not exists (select 1 from evidence_version newer where newer.evidence_id=e.id"
                        + " and newer.status='SUBMITTED' and newer.revision_number>v.revision_number)"
                        + " order by e.id,v.id,c.required_source_id",
                Map.of("process", key(processId), "sources", sourceIds),
                (result, index) -> new CurrentCoverage(JdbcValueSupport.uuid(result, 1),
                        JdbcValueSupport.uuid(result, 2), JdbcValueSupport.uuid(result, 3), result.getString(4)));
    }

    @Override
    @Transactional(readOnly = true)
    public Applicability applicability(UUID processId) {
        var indicators = new HashSet<UUID>();
        var sources = new HashSet<UUID>();
        for (var decision : currentApplicabilityDecisions(processId)) {
            if (!"NOT_APPLICABLE".equals(decision.decision())) continue;
            if ("INDICATOR".equals(decision.subjectType())) indicators.add(decision.subjectId());
            else sources.add(decision.subjectId());
        }
        return new Applicability(indicators, sources);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicabilityReference> currentApplicabilityDecisions(UUID processId) {
        return jdbc.query("select d.id,d.subject_type,d.subject_id,d.decision,d.revision from applicability_decision d"
                        + " where d.process_id=?"
                        + " and not exists (select 1 from applicability_decision newer"
                        + " where newer.process_id=d.process_id and newer.subject_type=d.subject_type"
                        + " and newer.subject_id=d.subject_id and newer.revision>d.revision)"
                        + " order by d.subject_type,d.subject_id",
                (result, index) -> new ApplicabilityReference(JdbcValueSupport.uuid(result, 1), result.getString(2),
                        JdbcValueSupport.uuid(result, 3), result.getString(4), result.getLong(5)), processId);
    }

    private Object key(UUID id) {
        // SQLite evidence/process references use Hibernate UUID blobs; applicability uses UUID text.
        return sqlite ? ByteBuffer.allocate(16).putLong(id.getMostSignificantBits())
                .putLong(id.getLeastSignificantBits()).array() : id;
    }
}
