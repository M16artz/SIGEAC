package ec.edu.unl.sec.evidence.infrastructure;

import ec.edu.unl.sec.evidence.domain.Evidence;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "evidence")
public class EvidenceJpaEntity {
    @Id
    private UUID id;

    @Column(name = "process_id", nullable = false)
    private UUID processId;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "correlation_id", nullable = false)
    private UUID correlationId;

    @Column(name = "latest_revision", nullable = false)
    private int latestRevision;

    protected EvidenceJpaEntity() {}

    private EvidenceJpaEntity(Evidence value) {
        id = value.id();
        processId = value.processId();
        ownerId = value.ownerId();
        createdAt = value.createdAt();
        correlationId = value.correlationId();
        latestRevision = value.latestRevision();
    }

    static EvidenceJpaEntity from(Evidence value) {
        return new EvidenceJpaEntity(value);
    }

    Evidence toDomain() {
        return new Evidence(id, processId, ownerId, createdAt, correlationId, latestRevision);
    }

    UUID id() {
        return id;
    }

    void bumpRevision(int revisionNumber) {
        if (revisionNumber <= latestRevision) {
            throw new IllegalStateException("successor revision must advance the evidence lineage");
        }
        latestRevision = revisionNumber;
    }
}
