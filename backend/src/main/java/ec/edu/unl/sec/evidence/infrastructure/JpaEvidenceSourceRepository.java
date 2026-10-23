package ec.edu.unl.sec.evidence.infrastructure;

import ec.edu.unl.sec.evidence.application.EvidenceProblemException;
import ec.edu.unl.sec.evidence.application.EvidenceSourceRepository;
import ec.edu.unl.sec.evidence.domain.ExternalReference;
import ec.edu.unl.sec.evidence.domain.VersionFile;
import ec.edu.unl.sec.shared.infrastructure.JdbcValueSupport;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(propagation = Propagation.MANDATORY)
public class JpaEvidenceSourceRepository implements EvidenceSourceRepository {
    private final JdbcTemplate jdbc;
    private final boolean sqlite;

    public JpaEvidenceSourceRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
        sqlite = Boolean.TRUE.equals(jdbc.execute((ConnectionCallback<Boolean>) connection ->
                connection.getMetaData().getDatabaseProductName().equalsIgnoreCase("SQLite")));
    }

    @Override @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public List<VersionFile> files(UUID versionId) {
        return jdbc.query("select version_id,file_id,original_name,declared_media_type,detected_media_type,size_bytes,sha256,position"
                        + " from evidence_version_file where version_id=? order by position,file_id",
                (row, number) -> new VersionFile(JdbcValueSupport.uuid(row, 1), JdbcValueSupport.uuid(row, 2),
                        row.getString(3), row.getString(4), row.getString(5), row.getLong(6), row.getString(7), row.getInt(8)),
                versionKey(versionId));
    }

    @Override @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public List<ExternalReference> references(UUID versionId) {
        return jdbc.query("select id,version_id,uri,description,position from evidence_external_reference"
                        + " where version_id=? order by position,id",
                (row, number) -> new ExternalReference(JdbcValueSupport.uuid(row, 1), JdbcValueSupport.uuid(row, 2),
                        row.getString(3), row.getString(4), row.getInt(5)), versionKey(versionId));
    }

    @Override public boolean advanceDraftRevision(UUID versionId, long expectedRevision) {
        return jdbc.update("update evidence_version set lock_revision=lock_revision+1 where id=? and status='DRAFT' and lock_revision=?",
                versionKey(versionId), expectedRevision) == 1;
    }

    @Override public int nextFilePosition(UUID versionId) {
        return jdbc.queryForObject("select coalesce(max(position),0)+1 from evidence_version_file where version_id=?",
                Integer.class, versionKey(versionId));
    }

    @Override public int nextReferencePosition(UUID versionId) {
        return jdbc.queryForObject("select coalesce(max(position),0)+1 from evidence_external_reference where version_id=?",
                Integer.class, versionKey(versionId));
    }

    @Override public void addFile(VersionFile file) {
        int changed = jdbc.update("insert into evidence_version_file(version_id,file_id,original_name,declared_media_type,"
                        + "detected_media_type,size_bytes,sha256,position) select v.id,f.id,f.original_name,f.declared_media_type,"
                        + "f.detected_media_type,f.size_bytes,f.sha256,? from file_asset f cross join evidence_version v"
                        + " where v.id=? and v.status='DRAFT' and f.id=? and f.status='AVAILABLE' and f.original_name=?"
                        + " and f.declared_media_type=? and f.detected_media_type=? and f.size_bytes=? and f.sha256=?",
                file.position(), versionKey(file.versionId()), file.fileId(), file.originalName(), file.declaredMediaType(),
                file.detectedMediaType(), file.sizeBytes(), file.sha256());
        if (changed != 1) throw new EvidenceProblemException(EvidenceProblemException.Kind.CONFLICT,
                "FILE_NOT_AVAILABLE", "El archivo no está disponible o su ficha no coincide con la instantánea.");
    }

    @Override public void removeFile(UUID versionId, UUID fileId) {
        requireChanged(jdbc.update("delete from evidence_version_file where version_id=? and file_id=? and "
                + draftExists("evidence_version_file"), versionKey(versionId), fileId));
    }

    @Override public void addReference(ExternalReference reference) {
        requireChanged(jdbc.update("insert into evidence_external_reference(id,version_id,uri,description,position)"
                        + " select ?,id,?,?,? from evidence_version where id=? and status='DRAFT'", reference.id(),
                reference.uri(), reference.description(), reference.position(), versionKey(reference.versionId())));
    }

    @Override public void updateReference(ExternalReference reference) {
        requireChanged(jdbc.update("update evidence_external_reference set uri=?,description=? where id=? and version_id=? and "
                        + draftExists("evidence_external_reference"), reference.uri(), reference.description(), reference.id(),
                versionKey(reference.versionId())));
    }

    @Override public void removeReference(UUID versionId, UUID referenceId) {
        requireChanged(jdbc.update("delete from evidence_external_reference where id=? and version_id=? and "
                + draftExists("evidence_external_reference"), referenceId, versionKey(versionId)));
    }

    private Object versionKey(UUID id) {
        // SQLite evidence IDs are Hibernate UUID BLOBs; source/file identities use TEXT.
        return sqlite ? ByteBuffer.allocate(16).putLong(id.getMostSignificantBits()).putLong(id.getLeastSignificantBits()).array() : id;
    }

    private static String draftExists(String table) {
        return "exists(select 1 from evidence_version v where v.id=" + table + ".version_id and v.status='DRAFT')";
    }

    private static void requireChanged(int changed) {
        if (changed != 1) throw new EvidenceProblemException(EvidenceProblemException.Kind.NOT_FOUND,
                "RESOURCE_NOT_FOUND", "No se encontró el recurso solicitado.");
    }
}
