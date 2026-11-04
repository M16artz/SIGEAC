package ec.edu.unl.sec.documentreview.infrastructure;

import ec.edu.unl.sec.documentreview.application.ReviewSubmissionPort;
import ec.edu.unl.sec.shared.infrastructure.JdbcValueSupport;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read adapter over submitted evidence versions for the review inbox. Only
 * the latest version of each evidence lineage is a review candidate: older
 * versions keep their decisions but are superseded once corrected.
 */
@Repository
public class JdbcReviewSubmissionAdapter implements ReviewSubmissionPort {
    private final JdbcTemplate jdbc;
    private final boolean sqlite;

    public JdbcReviewSubmissionAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        this.sqlite = Boolean.TRUE.equals(jdbc.execute((ConnectionCallback<Boolean>) connection ->
                !connection.getMetaData().getDatabaseProductName().contains("PostgreSQL")));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SubmittedVersion> submission(UUID versionId) {
        var rows = jdbc.query(
                "select v.id, v.revision_number, e.id, e.process_id, v.author_id, v.status from evidence_version v"
                        + " join evidence e on v.evidence_id=e.id where v.id=? and v.status='SUBMITTED'",
                (result, index) -> new SubmittedVersion(
                        JdbcValueSupport.uuid(result, 1),
                        JdbcValueSupport.uuid(result, 3),
                        JdbcValueSupport.uuid(result, 4),
                        careerOf(JdbcValueSupport.uuid(result, 4)),
                        result.getInt(2),
                        JdbcValueSupport.uuid(result, 5),
                        indicators(versionId),
                        coveredSources(versionId)),
                key(versionId));
        return rows.stream().findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PendingSubmission> pending(UUID processId) {
        return jdbc.query(
                "select v.id, v.revision_number, e.id, e.process_id, v.title, v.author_id, v.submitted_at"
                        + " from evidence e join evidence_version v on v.evidence_id=e.id"
                        + " and v.revision_number=e.latest_revision"
                        + " where e.process_id=? and v.status='SUBMITTED' order by v.submitted_at, v.id",
                (result, index) -> {
                    var versionId = JdbcValueSupport.uuid(result, 1);
                    var innerProcessId = JdbcValueSupport.uuid(result, 4);
                    return new PendingSubmission(
                            versionId,
                            JdbcValueSupport.uuid(result, 3),
                            innerProcessId,
                            careerOf(innerProcessId),
                            result.getInt(2),
                            result.getString(5),
                            JdbcValueSupport.uuid(result, 6),
                            JdbcValueSupport.instant(result, 7),
                            indicators(versionId),
                            coveredSources(versionId));
                },
                key(processId));
    }

    @Override
    @Transactional(readOnly = true)
    public long latestRevisionNumber(UUID evidenceId) {
        var revisions = jdbc.query(
                "select latest_revision from evidence where id=?",
                (result, index) -> result.getLong(1),
                key(evidenceId));
        return revisions.isEmpty() ? 0 : revisions.getFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UUID> career(UUID processId) {
        var careers = jdbc.query(
                "select career_id from self_assessment_process where id=?",
                (result, index) -> JdbcValueSupport.uuid(result, 1),
                processId);
        return careers.stream().findFirst();
    }

    private UUID careerOf(UUID processId) {
        return career(processId).orElse(null);
    }

    private List<UUID> coveredSources(UUID versionId) {
        return jdbc.query(
                "select required_source_id from source_coverage where version_id=? order by required_source_id",
                (result, index) -> JdbcValueSupport.uuid(result, 1),
                key(versionId));
    }

    private List<UUID> indicators(UUID versionId) {
        return jdbc.query(
                "select indicator_id from evidence_version_indicator where version_id=? order by position",
                (result, index) -> JdbcValueSupport.uuid(result, 1),
                key(versionId));
    }

    private Object key(UUID id) {
        // SQLite evidence identities are Hibernate UUID blobs; process rows are UUID text.
        return id == null ? null
                : (sqlite) ? ByteBuffer.allocate(16).putLong(id.getMostSignificantBits())
                        .putLong(id.getLeastSignificantBits()).array()
                : id;
    }
}
