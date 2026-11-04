package ec.edu.unl.sec.evidence.infrastructure;

import ec.edu.unl.sec.evidence.application.EvidenceGovernanceRepository;
import ec.edu.unl.sec.evidence.domain.ApplicabilityDecision;
import ec.edu.unl.sec.evidence.domain.SourceCoverage;
import ec.edu.unl.sec.evidence.domain.StructuredEvidence;
import ec.edu.unl.sec.shared.infrastructure.JdbcValueSupport;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
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
public class JdbcEvidenceGovernanceRepository implements EvidenceGovernanceRepository {
    private final JdbcTemplate jdbc;
    private final boolean sqlite;

    public JdbcEvidenceGovernanceRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        this.sqlite = Boolean.TRUE.equals(jdbc.execute((ConnectionCallback<Boolean>) connection ->
                !connection.getMetaData().getDatabaseProductName().contains("PostgreSQL")));
    }

    @Override
    @Transactional
    public SourceCoverage saveCoverage(SourceCoverage coverage) {
        jdbc.update(
                "insert into source_coverage(id,version_id,required_source_id,coverage_note,declared_by,declared_at)"
                        + " values(?,?,?,?,?,?)",
                coverage.id(), versionKey(coverage.versionId()), coverage.requiredSourceId(), coverage.coverageNote(),
                coverage.declaredBy(), Timestamp.from(coverage.declaredAt()));
        return coverage;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SourceCoverage> coverages(UUID versionId) {
        return jdbc.query(
                "select id,version_id,required_source_id,coverage_note,declared_by,declared_at"
                        + " from source_coverage where version_id=? order by declared_at,id",
                (result, index) -> new SourceCoverage(
                        JdbcValueSupport.uuid(result, 1),
                        JdbcValueSupport.uuid(result, 2),
                        JdbcValueSupport.uuid(result, 3),
                        result.getString(4),
                        JdbcValueSupport.uuid(result, 5),
                        JdbcValueSupport.instant(result, 6)),
                versionKey(versionId));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SourceCoverage> coverage(UUID coverageId) {
        var rows = jdbc.query(
                "select id,version_id,required_source_id,coverage_note,declared_by,declared_at"
                        + " from source_coverage where id=?",
                (result, index) -> new SourceCoverage(
                        JdbcValueSupport.uuid(result, 1),
                        JdbcValueSupport.uuid(result, 2),
                        JdbcValueSupport.uuid(result, 3),
                        result.getString(4),
                        JdbcValueSupport.uuid(result, 5),
                        JdbcValueSupport.instant(result, 6)),
                coverageId);
        return rows.stream().findFirst();
    }

    @Override
    @Transactional
    public void deleteCoverage(UUID coverageId) {
        jdbc.update("delete from source_coverage where id=?", coverageId);
    }

    private static final String STRUCTURED_COLUMNS =
            "id,version_id,required_source_id,kind,value_type,value_text,unit,period_start,period_end,"
                    + "reference,created_by,created_at";

    @Override
    @Transactional
    public StructuredEvidence saveStructured(StructuredEvidence snapshot) {
        jdbc.update(
                "insert into evidence_structured_value(id,version_id,required_source_id,kind,value_type,value_text,"
                        + "unit,period_start,period_end,reference,created_by,created_at) values(?,?,?,?,?,?,?,?,?,?,?,?)",
                snapshot.id(), versionKey(snapshot.versionId()), snapshot.requiredSourceId(), snapshot.kind().name(),
                snapshot.value().type().name(), valueText(snapshot.value()), snapshot.unit(),
                snapshot.periodStart(), snapshot.periodEnd(), snapshot.reference(),
                snapshot.createdBy(), Timestamp.from(snapshot.createdAt()));
        return snapshot;
    }

    @Override
    @Transactional(readOnly = true)
    public List<StructuredEvidence> structuredSnapshots(UUID versionId) {
        return jdbc.query(
                "select " + STRUCTURED_COLUMNS + " from evidence_structured_value"
                        + " where version_id=? order by created_at,id",
                (result, index) -> structured(result),
                versionKey(versionId));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StructuredEvidence> structuredSnapshot(UUID versionId, UUID requiredSourceId) {
        var rows = jdbc.query(
                "select " + STRUCTURED_COLUMNS + " from evidence_structured_value"
                        + " where version_id=? and required_source_id=?",
                (result, index) -> structured(result),
                versionKey(versionId), requiredSourceId);
        return rows.stream().findFirst();
    }

    private static String valueText(StructuredEvidence.TypedValue value) {
        var raw = value.number() != null ? value.number().toString()
                : value.text() != null ? value.text() : Boolean.toString(value.flag().booleanValue());
        return raw;
    }

    private static StructuredEvidence.TypedValue typedValue(StructuredEvidence.ValueType type, String text) {
        return switch (type) {
            case NUMBER -> StructuredEvidence.TypedValue.number(new BigDecimal(text));
            case STRING -> StructuredEvidence.TypedValue.text(text);
            case BOOLEAN -> StructuredEvidence.TypedValue.flag(Boolean.valueOf(text));
        };
    }

    private static StructuredEvidence structured(java.sql.ResultSet result) throws java.sql.SQLException {
        return new StructuredEvidence(
                JdbcValueSupport.uuid(result, 1),
                JdbcValueSupport.uuid(result, 2),
                JdbcValueSupport.uuid(result, 3),
                StructuredEvidence.Kind.valueOf(result.getString(4)),
                typedValue(StructuredEvidence.ValueType.valueOf(result.getString(5)), result.getString(6)),
                result.getString(7),
                JdbcValueSupport.localDate(result, 8),
                JdbcValueSupport.localDate(result, 9),
                result.getString(10),
                JdbcValueSupport.uuid(result, 11),
                JdbcValueSupport.instant(result, 12));
    }

    @Override
    @Transactional
    public boolean markSubmitted(UUID versionId, Instant submittedAt, long expectedLockRevision) {
        int changed = jdbc.update(
                "update evidence_version set status='SUBMITTED',submitted_at=?,lock_revision=?"
                        + " where id=? and status='DRAFT' and lock_revision=?",
                Timestamp.from(submittedAt), expectedLockRevision + 1, versionKey(versionId), expectedLockRevision);
        if (changed != 1) return false;
        jdbc.update(
                "update evidence set latest_revision=(select max(revision_number) from evidence_version"
                        + " where evidence_id=evidence.id) where id=(select evidence_id from evidence_version where id=?)",
                versionKey(versionId));
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ApplicabilityDecision> latestApplicability(UUID processId,
            ApplicabilityDecision.SubjectType subjectType, UUID subjectId) {
        var rows = jdbc.query(
                "select id,process_id,subject_type,subject_id,decision,rule_reference,justification,legal_basis,"
                        + "document_reference,authorized_by,decided_at,predecessor_id,revision from applicability_decision"
                        + " where process_id=? and subject_type=? and subject_id=? order by revision desc limit 1",
                (result, index) -> applicability(result),
                processId, subjectType.name(), subjectId);
        return rows.stream().findFirst();
    }

    @Override
    @Transactional
    public ApplicabilityDecision saveApplicability(ApplicabilityDecision decision) {
        jdbc.update(
                "insert into applicability_decision(id,process_id,subject_type,subject_id,decision,rule_reference,"
                        + "justification,legal_basis,document_reference,authorized_by,decided_at,predecessor_id,revision)"
                        + " values(?,?,?,?,?,?,?,?,?,?,?,?,?)",
                decision.id(), decision.processId(), decision.subjectType().name(), decision.subjectId(),
                decision.decision().name(), decision.ruleReference(), decision.justification(),
                decision.legalBasis(), decision.documentReference(), decision.authorizedBy(),
                Timestamp.from(decision.decidedAt()), decision.predecessorId(), decision.revision());
        return decision;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicabilityDecision> applicabilityHistory(UUID processId) {
        return jdbc.query(
                "select id,process_id,subject_type,subject_id,decision,rule_reference,justification,legal_basis,"
                        + "document_reference,authorized_by,decided_at,predecessor_id,revision from applicability_decision"
                        + " where process_id=? order by decided_at,id",
                (result, index) -> applicability(result),
                processId);
    }

    private static ApplicabilityDecision applicability(java.sql.ResultSet result) throws java.sql.SQLException {
        return new ApplicabilityDecision(
                JdbcValueSupport.uuid(result, 1),
                JdbcValueSupport.uuid(result, 2),
                ApplicabilityDecision.SubjectType.valueOf(result.getString(3)),
                JdbcValueSupport.uuid(result, 4),
                ApplicabilityDecision.Decision.valueOf(result.getString(5)),
                result.getString(6),
                result.getString(7),
                result.getString(8),
                result.getString(9),
                JdbcValueSupport.uuid(result, 10),
                JdbcValueSupport.instant(result, 11),
                JdbcValueSupport.uuid(result, 12),
                result.getLong(13));
    }

    private Object versionKey(UUID id) {
        // SQLite evidence IDs are Hibernate UUID BLOBs; governance identities use TEXT.
        return sqlite ? ByteBuffer.allocate(16).putLong(id.getMostSignificantBits()).putLong(id.getLeastSignificantBits()).array() : id;
    }
}
