package ec.edu.unl.sec.documentreview.infrastructure;

import ec.edu.unl.sec.documentreview.application.DocumentReviewRepository;
import ec.edu.unl.sec.documentreview.domain.DocumentReviewDecision;
import ec.edu.unl.sec.documentreview.domain.ReviewCoverageOutcome;
import ec.edu.unl.sec.shared.infrastructure.JdbcValueSupport;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Append-only persistence of documentary review decisions and their
 * per-coverage outcomes. Evidence identities stay opaque behind this adapter
 * so the review module never reads evidence repositories directly.
 */
@Repository
public class JdbcDocumentReviewRepository implements DocumentReviewRepository {
    private static final String DECISION_COLUMNS =
            "id,version_id,decision,justification,reviewer_id,reviewed_at,self_review_exception,"
                    + "exception_reason,predecessor_id,successor_version_id,revision";
    private static final String ALIASED_DECISION_COLUMNS =
            "d.id AS id,d.version_id AS version_id,d.decision AS decision,d.justification AS justification,"
                    + "d.reviewer_id AS reviewer_id,d.reviewed_at AS reviewed_at,"
                    + "d.self_review_exception AS self_review_exception,d.exception_reason AS exception_reason,"
                    + "d.predecessor_id AS predecessor_id,d.successor_version_id AS successor_version_id,"
                    + "d.revision AS revision";

    private final JdbcTemplate jdbc;
    private final boolean sqlite;

    public JdbcDocumentReviewRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        this.sqlite = Boolean.TRUE.equals(jdbc.execute((ConnectionCallback<Boolean>) connection ->
                !connection.getMetaData().getDatabaseProductName().contains("PostgreSQL")));
    }

    @Override
    @Transactional
    public DocumentReviewDecision save(DocumentReviewDecision decision) {
        jdbc.update(
                "insert into document_review_decision(id,version_id,decision,justification,reviewer_id,reviewed_at,"
                        + "self_review_exception,exception_reason,predecessor_id,successor_version_id,revision)"
                        + " values(?,?,?,?,?,?,?,?,?,?,?)",
                key(decision.id()), key(decision.versionId()), decision.decision().name(), decision.justification(),
                decision.reviewerId(), Timestamp.from(decision.reviewedAt()), decision.selfReviewException(),
                decision.exceptionReason(), key(decision.predecessorId()),
                key(decision.successorVersionId()), decision.revision());
        for (var outcome : decision.outcomes()) {
            jdbc.update(
                    "insert into document_review_outcome(id,decision_id,required_source_id,outcome,note)"
                            + " values(?,?,?,?,?)",
                    UUID.randomUUID(), key(decision.id()), outcome.requiredSourceId(),
                    outcome.outcome().name(), outcome.note());
        }
        return decision;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DocumentReviewDecision> latest(UUID versionId) {
        var rows = jdbc.query(
                "select " + DECISION_COLUMNS + " from document_review_decision"
                        + " where version_id=? order by revision desc limit 1",
                (result, index) -> raw(result),
                key(versionId));
        return rows.stream().findFirst().map(this::decode);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DocumentReviewDecision> decision(UUID decisionId) {
        var rows = jdbc.query(
                "select " + DECISION_COLUMNS + " from document_review_decision where id=?",
                (result, index) -> raw(result),
                key(decisionId));
        return rows.stream().findFirst().map(this::decode);
    }

    @Override
    @Transactional
    public void linkSuccessor(UUID decisionId, UUID successorVersionId) {
        jdbc.update("update document_review_decision set successor_version_id=? where id=?",
                key(successorVersionId), key(decisionId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentReviewDecision> historyForEvidence(UUID evidenceId) {
        return historyOrOutcomes(jdbc.query(
                "select " + ALIASED_DECISION_COLUMNS + " from document_review_decision d"
                        + " join evidence_version v on d.version_id=v.id where v.evidence_id=?"
                        + " order by v.revision_number, d.revision",
                (result, index) -> raw(result),
                key(evidenceId)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentReviewDecision> historyForVersion(UUID versionId) {
        return historyOrOutcomes(jdbc.query(
                "select " + DECISION_COLUMNS + " from document_review_decision where version_id=?"
                        + " order by revision",
                (result, index) -> raw(result),
                key(versionId)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentReviewDecision> historyForProcess(UUID processId) {
        return historyOrOutcomes(jdbc.query(
                "select " + ALIASED_DECISION_COLUMNS + " from document_review_decision d"
                        + " join evidence_version v on d.version_id=v.id"
                        + " join evidence e on v.evidence_id=e.id where e.process_id=?"
                        + " order by v.revision_number, d.revision",
                (result, index) -> raw(result),
                key(processId)));
    }

    private List<DocumentReviewDecision> historyOrOutcomes(List<RawDecision> decisions) {
        if (decisions.isEmpty()) return List.of();
        var enriched = new ArrayList<DocumentReviewDecision>(decisions.size());
        for (var raw : decisions) enriched.add(decode(raw));
        return List.copyOf(enriched);
    }

    private DocumentReviewDecision decode(RawDecision raw) {
        var stored = outcomes(raw.id());
        return new DocumentReviewDecision(
                raw.id(), raw.versionId(), raw.decision(), raw.justification(),
                raw.reviewerId(), raw.reviewedAt(), raw.selfReviewException(),
                raw.exceptionReason(), raw.predecessorId(), raw.successorVersionId(),
                raw.revision(), stored);
    }

    private List<ReviewCoverageOutcome> outcomes(UUID decisionId) {
        return jdbc.query(
                "select required_source_id,outcome,note from document_review_outcome"
                        + " where decision_id=? order by required_source_id",
                (result, index) -> new ReviewCoverageOutcome(
                        uuid(result, 1),
                        ReviewCoverageOutcome.Outcome.valueOf(result.getString(2)),
                        result.getString(3)),
                key(decisionId));
    }

    private static RawDecision raw(java.sql.ResultSet result) throws java.sql.SQLException {
        return new RawDecision(
                uuid(result, 1),
                uuid(result, 2),
                DocumentReviewDecision.Decision.valueOf(result.getString(3)),
                result.getString(4),
                uuid(result, 5),
                JdbcValueSupport.instant(result, 6),
                result.getBoolean(7),
                result.getString(8),
                uuid(result, 9),
                uuid(result, 10),
                result.getLong(11));
    }

    private static UUID uuid(java.sql.ResultSet result, int index) throws java.sql.SQLException {
        return JdbcValueSupport.uuid(result, index);
    }

    /** Raw read state before per-coverage outcomes hydration (domain invariant kept). */
    private record RawDecision(UUID id, UUID versionId, DocumentReviewDecision.Decision decision, String justification,
            UUID reviewerId, Instant reviewedAt, boolean selfReviewException, String exceptionReason,
            UUID predecessorId, UUID successorVersionId, long revision) {}

    private Object key(UUID id) {
        return id == null ? null
                : sqlite ? ByteBuffer.allocate(16).putLong(id.getMostSignificantBits())
                        .putLong(id.getLeastSignificantBits()).array()
                : id;
    }
}
