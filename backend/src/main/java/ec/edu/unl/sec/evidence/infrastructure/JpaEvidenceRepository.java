package ec.edu.unl.sec.evidence.infrastructure;

import ec.edu.unl.sec.evidence.application.EvidenceDraftRepository;
import ec.edu.unl.sec.evidence.application.EvidenceDraftService;
import ec.edu.unl.sec.evidence.application.EvidenceProblemException;
import ec.edu.unl.sec.evidence.domain.Evidence;
import ec.edu.unl.sec.evidence.domain.EvidenceVersion;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaEvidenceRepository implements EvidenceDraftRepository {
    private final EntityManager entityManager;

    public JpaEvidenceRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
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
