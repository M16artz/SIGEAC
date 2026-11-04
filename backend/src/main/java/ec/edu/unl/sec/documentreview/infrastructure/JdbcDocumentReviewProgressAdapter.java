package ec.edu.unl.sec.documentreview.infrastructure;

import ec.edu.unl.sec.documentreview.application.DocumentReviewProgressPort;
import ec.edu.unl.sec.shared.infrastructure.JdbcValueSupport;
import java.nio.ByteBuffer;
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
public class JdbcDocumentReviewProgressAdapter implements DocumentReviewProgressPort {
    private final NamedParameterJdbcTemplate jdbc;
    private final boolean sqlite;

    public JdbcDocumentReviewProgressAdapter(JdbcTemplate jdbc) {
        this.jdbc = new NamedParameterJdbcTemplate(jdbc);
        this.sqlite = Boolean.TRUE.equals(jdbc.execute((ConnectionCallback<Boolean>) connection ->
                !connection.getMetaData().getDatabaseProductName().contains("PostgreSQL")));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Outcome> latestOutcomes(Set<UUID> versionIds) {
        if (versionIds.isEmpty()) return List.of();
        return jdbc.query("select d.version_id,o.required_source_id,d.id,o.outcome"
                        + " from document_review_decision d join document_review_outcome o on o.decision_id=d.id"
                        + " where d.version_id in (:versions) and not exists"
                        + " (select 1 from document_review_decision newer"
                        + " where newer.version_id=d.version_id and newer.revision>d.revision)"
                        + " order by d.version_id,o.required_source_id",
                Map.of("versions", versionIds.stream().map(this::key).toList()),
                (result, index) -> new Outcome(JdbcValueSupport.uuid(result, 1),
                        JdbcValueSupport.uuid(result, 2), JdbcValueSupport.uuid(result, 3), result.getString(4)));
    }

    private Object key(UUID id) {
        return sqlite ? ByteBuffer.allocate(16).putLong(id.getMostSignificantBits())
                .putLong(id.getLeastSignificantBits()).array() : id;
    }
}
