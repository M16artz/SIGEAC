package ec.edu.unl.sec.evaluationmodel.infrastructure;

import ec.edu.unl.sec.evaluationmodel.application.ModelProblemException;
import ec.edu.unl.sec.evaluationmodel.application.NormativeReferenceRepository;
import ec.edu.unl.sec.evaluationmodel.domain.ModelCareerApplicability;
import ec.edu.unl.sec.evaluationmodel.domain.NormativeReference;
import ec.edu.unl.sec.shared.infrastructure.JdbcValueSupport;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JpaNormativeReferenceRepository implements NormativeReferenceRepository {
    private final JdbcTemplate jdbc;

    public JpaNormativeReferenceRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override public void saveReference(NormativeReference reference) {
        jdbc.update("insert into normative_reference(id,title,issuer,edition,document_reference,document_sha256,pages,"
                        + "disposition,verification_status,verified_by,verified_at) values(?,?,?,?,?,?,?,?,?,?,?) "
                        + "on conflict (id) do nothing",
                reference.id(), reference.title(), reference.issuer(), reference.edition(), reference.documentReference(),
                reference.documentSha256(), reference.pages(), reference.disposition(),
                reference.verificationStatus().name(), reference.verifiedBy(),
                reference.verifiedAt() == null ? null : Timestamp.from(reference.verifiedAt()));
    }

    @Override public Optional<NormativeReference> findReference(UUID id) {
        return jdbc.query("select id,title,issuer,edition,document_reference,document_sha256,pages,disposition,"
                        + "verification_status,verified_by,verified_at from normative_reference where id=?",
                (r, n) -> mapReference(r), id).stream().findFirst();
    }

    @Override public void replaceIndicatorLinks(UUID modelVersionId, Collection<IndicatorLink> links) {
        for (IndicatorLink link : links)
            jdbc.update("insert into indicator_normative_reference(model_version_id,indicator_node_id,normative_reference_id) "
                    + "values(?,?,?) on conflict do nothing", modelVersionId, link.indicatorNodeId(), link.referenceId());
    }

    @Override public List<IndicatorLink> indicatorLinks(UUID modelVersionId) {
        return jdbc.query("select indicator_node_id,normative_reference_id from indicator_normative_reference "
                        + "where model_version_id=? order by indicator_node_id,normative_reference_id",
                (r, n) -> new IndicatorLink(JdbcValueSupport.uuid(r, 1), JdbcValueSupport.uuid(r, 2)), modelVersionId);
    }

    @Override public void appendApplicability(ModelCareerApplicability applicability) {
        try {
            jdbc.update("insert into model_career_applicability(id,model_version_id,career_id,status,valid_from,valid_until,"
                            + "authority_function,legal_basis,document_reference,reason,predecessor_id,revision,decided_by,decided_at) "
                            + "values(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    applicability.id(), applicability.modelVersionId(), applicability.careerId(),
                    applicability.status().name(), Date.valueOf(applicability.validFrom()),
                    applicability.validUntil() == null ? null : Date.valueOf(applicability.validUntil()),
                    applicability.provenance().authorityFunction(), applicability.provenance().legalBasis(),
                    applicability.provenance().documentReference(), applicability.provenance().reason(),
                    applicability.predecessorId(), applicability.revision(), applicability.decidedBy(),
                    Timestamp.from(applicability.decidedAt()));
        } catch (org.springframework.dao.DataAccessException conflict) {
            throw new ModelProblemException(ModelProblemException.Kind.PRECONDITION, "STALE_MODEL_APPLICABILITY",
                    "La aplicabilidad cambio; recargue antes de continuar.");
        }
    }

    @Override public Optional<ModelCareerApplicability> latestApplicability(UUID modelVersionId, UUID careerId) {
        return jdbc.query("select id,model_version_id,career_id,status,valid_from,valid_until,authority_function,legal_basis,"
                        + "document_reference,reason,predecessor_id,revision,decided_by,decided_at "
                        + "from model_career_applicability where model_version_id=? and career_id=? order by revision desc limit 1",
                (r, n) -> mapApplicability(r), modelVersionId, careerId).stream().findFirst();
    }

    @Override public List<ModelCareerApplicability> listApplicability(UUID modelVersionId) {
        return jdbc.query("select id,model_version_id,career_id,status,valid_from,valid_until,authority_function,legal_basis,"
                        + "document_reference,reason,predecessor_id,revision,decided_by,decided_at "
                        + "from model_career_applicability where model_version_id=? order by decided_at,revision",
                (r, n) -> mapApplicability(r), modelVersionId);
    }

    @Override public boolean careerExists(UUID careerId) {
        Integer count = jdbc.queryForObject("select count(*) from career where id=?", Integer.class, careerId);
        return count != null && count > 0;
    }

    private static NormativeReference mapReference(ResultSet r) throws SQLException {
        return new NormativeReference(JdbcValueSupport.uuid(r, 1), r.getString(2), r.getString(3), r.getString(4),
                r.getString(5), r.getString(6), r.getString(7), r.getString(8),
                NormativeReference.VerificationStatus.valueOf(r.getString(9)), JdbcValueSupport.uuid(r, 10),
                JdbcValueSupport.instant(r, 11));
    }

    private static ModelCareerApplicability mapApplicability(ResultSet r) throws SQLException {
        Instant decidedAt = JdbcValueSupport.instant(r, 14);
        return new ModelCareerApplicability(JdbcValueSupport.uuid(r, 1), JdbcValueSupport.uuid(r, 2),
                JdbcValueSupport.uuid(r, 3), ModelCareerApplicability.Status.valueOf(r.getString(4)), localDate(r, 5),
                localDate(r, 6), new ModelCareerApplicability.Decision(r.getString(7), r.getString(8), r.getString(9),
                r.getString(10)), JdbcValueSupport.uuid(r, 11), r.getInt(12), JdbcValueSupport.uuid(r, 13), decidedAt);
    }

    private static LocalDate localDate(ResultSet r, int index) throws SQLException {
        Object value = r.getObject(index);
        if (value == null) return null;
        if (value instanceof LocalDate date) return date;
        if (value instanceof Date date) return date.toLocalDate();
        if (value instanceof Number epochMillis) return Instant.ofEpochMilli(epochMillis.longValue())
                .atZone(java.time.ZoneOffset.UTC).toLocalDate();
        String text = value.toString();
        if (text.matches("-?[0-9]+")) return Instant.ofEpochMilli(Long.parseLong(text))
                .atZone(java.time.ZoneOffset.UTC).toLocalDate();
        return LocalDate.parse(text);
    }
}
