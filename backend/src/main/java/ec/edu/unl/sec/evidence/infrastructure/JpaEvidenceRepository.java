package ec.edu.unl.sec.evidence.infrastructure;

import ec.edu.unl.sec.evidence.application.EvidenceDraftRepository;
import ec.edu.unl.sec.evidence.application.EvidenceDraftService;
import ec.edu.unl.sec.evidence.application.EvidenceProblemException;
import ec.edu.unl.sec.evidence.domain.Evidence;
import ec.edu.unl.sec.evidence.domain.EvidenceVersion;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaEvidenceRepository implements EvidenceDraftRepository {
    private final EntityManager entityManager;
    private final JdbcTemplate jdbc;
    private final boolean sqlite;

    public JpaEvidenceRepository(EntityManager entityManager, JdbcTemplate jdbc) {
        this.entityManager = entityManager;
        this.jdbc = jdbc;
        this.sqlite = Boolean.TRUE.equals(jdbc.execute((ConnectionCallback<Boolean>) connection ->
                !connection.getMetaData().getDatabaseProductName().contains("PostgreSQL")));
    }

    @Override
    @Transactional
    public void create(Evidence evidence, EvidenceVersion version) {
        var root = EvidenceJpaEntity.from(evidence);
        entityManager.persist(root);
        entityManager.persist(EvidenceVersionJpaEntity.from(version, root));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EvidenceDraftService.EvidenceDetails> findEvidence(UUID evidenceId) {
        var versions = entityManager.createQuery(
                        "select v from EvidenceVersionJpaEntity v join fetch v.evidence e "
                                + "where e.id=:id and v.revisionNumber=e.latestRevision",
                        EvidenceVersionJpaEntity.class)
                .setParameter("id", evidenceId)
                .setMaxResults(1)
                .getResultList();
        return versions.stream().findFirst().map(JpaEvidenceRepository::details);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EvidenceVersion> findVersion(UUID versionId) {
        return entityManager.createQuery(
                        "select v from EvidenceVersionJpaEntity v join fetch v.evidence where v.id=:id",
                        EvidenceVersionJpaEntity.class)
                .setParameter("id", versionId)
                .getResultStream()
                .findFirst()
                .map(EvidenceVersionJpaEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EvidenceDraftService.EvidenceDetails> listByProcess(UUID processId) {
        return entityManager.createQuery(
                        "select v from EvidenceVersionJpaEntity v join fetch v.evidence e "
                                + "where e.processId=:processId and v.revisionNumber=e.latestRevision "
                                + "order by e.createdAt desc,e.id",
                        EvidenceVersionJpaEntity.class)
                .setParameter("processId", processId)
                .getResultStream()
                .map(JpaEvidenceRepository::details)
                .toList();
    }

    @Override
    @Transactional
    public void update(EvidenceVersion version, long expectedRevision) {
        var entity = entityManager.find(EvidenceVersionJpaEntity.class, version.id());
        if (entity == null) throw notFound();
        if (entity.lockRevision() != expectedRevision) throw stale();
        entity.apply(version);
        entityManager.flush();
    }

    @Override
    @Transactional
    public void createSuccessor(EvidenceVersion predecessor, EvidenceVersion successor) {
        var root = entityManager.find(EvidenceJpaEntity.class, predecessor.evidenceId(), LockModeType.PESSIMISTIC_WRITE);
        var previous = entityManager.find(EvidenceVersionJpaEntity.class, predecessor.id());
        if (root == null || previous == null || root.latestRevision() != predecessor.revisionNumber()
                || previous.toDomain().status() != ec.edu.unl.sec.evidence.domain.EvidenceVersionStatus.SUBMITTED) {
            throw new EvidenceProblemException(EvidenceProblemException.Kind.CONFLICT,
                    "SUCCESSOR_ALREADY_EXISTS", "La evidencia ya tiene una sucesora o la versión no está remitida.");
        }
        if (!successor.evidenceId().equals(predecessor.evidenceId())
                || successor.revisionNumber() != predecessor.revisionNumber() + 1
                || !predecessor.id().equals(successor.predecessorId())) {
            throw new IllegalArgumentException("invalid successor lineage");
        }
        var successorEntity = EvidenceVersionJpaEntity.from(successor, root);
        entityManager.persist(successorEntity);
        entityManager.flush();
        copySources(predecessor.id(), successor.id());
        root.advanceLatestRevision(predecessor.revisionNumber(), successor.revisionNumber());
        entityManager.flush();
    }

    private void copySources(UUID predecessorId, UUID successorId) {
        jdbc.update("insert into evidence_version_file(version_id,file_id,original_name,declared_media_type,"
                        + "detected_media_type,size_bytes,sha256,position) select ?,file_id,original_name,"
                        + "declared_media_type,detected_media_type,size_bytes,sha256,position "
                        + "from evidence_version_file where version_id=?",
                key(successorId), key(predecessorId));
        var references = jdbc.query("select uri,description,position from evidence_external_reference where version_id=?",
                (result, index) -> new Object[] {result.getString(1), result.getString(2), result.getInt(3)}, key(predecessorId));
        references.forEach(reference -> jdbc.update(
                "insert into evidence_external_reference(id,version_id,uri,description,position) values(?,?,?,?,?)",
                sqlite ? UUID.randomUUID().toString() : UUID.randomUUID(), key(successorId), reference[0], reference[1], reference[2]));
        var coverages = jdbc.query("select required_source_id,coverage_note,declared_by,declared_at "
                        + "from source_coverage where version_id=?",
                (result, index) -> new Object[] {result.getObject(1), result.getString(2), result.getObject(3), result.getTimestamp(4)},
                key(predecessorId));
        coverages.forEach(coverage -> jdbc.update(
                "insert into source_coverage(id,version_id,required_source_id,coverage_note,declared_by,declared_at) "
                        + "values(?,?,?,?,?,?)",
                key(UUID.randomUUID()), key(successorId), coverage[0], coverage[1], coverage[2], coverage[3]));
        var structured = jdbc.query("select required_source_id,kind,value_type,value_text,unit,period_start,period_end,"
                        + "reference,created_by,created_at from evidence_structured_value where version_id=?",
                (result, index) -> new Object[] {result.getObject(1), result.getString(2), result.getString(3),
                        result.getString(4), result.getString(5), result.getObject(6), result.getObject(7),
                        result.getString(8), result.getObject(9), result.getTimestamp(10)}, key(predecessorId));
        structured.forEach(snapshot -> jdbc.update(
                "insert into evidence_structured_value(id,version_id,required_source_id,kind,value_type,value_text,"
                        + "unit,period_start,period_end,reference,created_by,created_at) values(?,?,?,?,?,?,?,?,?,?,?,?)",
                key(UUID.randomUUID()), key(successorId), snapshot[0], snapshot[1], snapshot[2], snapshot[3], snapshot[4],
                snapshot[5], snapshot[6], snapshot[7], snapshot[8], snapshot[9]));
    }

    private Object key(UUID id) {
        return sqlite ? ByteBuffer.allocate(16).putLong(id.getMostSignificantBits())
                .putLong(id.getLeastSignificantBits()).array() : id;
    }

    @Transactional
    public Optional<Evidence> lockEvidence(UUID evidenceId) {
        return Optional.ofNullable(entityManager.find(EvidenceJpaEntity.class, evidenceId, LockModeType.PESSIMISTIC_WRITE))
                .map(EvidenceJpaEntity::toDomain);
    }

    private static EvidenceDraftService.EvidenceDetails details(EvidenceVersionJpaEntity version) {
        return new EvidenceDraftService.EvidenceDetails(version.evidence().toDomain(), version.toDomain());
    }

    private static EvidenceProblemException stale() {
        return new EvidenceProblemException(
                EvidenceProblemException.Kind.PRECONDITION_FAILED,
                "STALE_DRAFT",
                "El borrador cambió; recargue antes de continuar.");
    }

    private static EvidenceProblemException notFound() {
        return new EvidenceProblemException(
                EvidenceProblemException.Kind.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                "No se encontró el recurso solicitado.");
    }
}
