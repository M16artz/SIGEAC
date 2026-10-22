package ec.edu.unl.sec.evidence.infrastructure;

import ec.edu.unl.sec.evidence.domain.EvidenceVersion;
import ec.edu.unl.sec.evidence.domain.EvidenceVersionStatus;
import ec.edu.unl.sec.evidence.domain.VersionIndicator;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "evidence_version")
public class EvidenceVersionJpaEntity {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evidence_id", nullable = false)
    private EvidenceJpaEntity evidence;

    @Column(name = "revision_number", nullable = false)
    private int revisionNumber;

    @Column(name = "predecessor_id")
    private UUID predecessorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EvidenceVersionStatus status;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(nullable = false, length = 8000)
    private String description;

    @Column(nullable = false, length = 4000)
    private String provenance;

    @Column(name = "covered_from", nullable = false)
    private LocalDate coveredFrom;

    @Column(name = "covered_to", nullable = false)
    private LocalDate coveredTo;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Version
    @Column(name = "lock_revision", nullable = false)
    private long lockRevision;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "evidence_version_indicator", joinColumns = @JoinColumn(name = "version_id"))
    @OrderBy("position")
    private List<IndicatorRow> indicators = new ArrayList<>();

    protected EvidenceVersionJpaEntity() {}

    static EvidenceVersionJpaEntity from(EvidenceVersion value, EvidenceJpaEntity evidence) {
        var entity = new EvidenceVersionJpaEntity();
        entity.id = value.id();
        entity.evidence = evidence;
        entity.revisionNumber = value.revisionNumber();
        entity.predecessorId = value.predecessorId();
        entity.status = value.status();
        entity.apply(value);
        entity.createdAt = value.createdAt();
        entity.submittedAt = value.submittedAt();
        entity.lockRevision = value.lockRevision();
        return entity;
    }

    void apply(EvidenceVersion value) {
        title = value.title();
        description = value.description();
        provenance = value.provenance();
        coveredFrom = value.coveredFrom();
        coveredTo = value.coveredTo();
        authorId = value.authorId();
        indicators.clear();
        value.indicators().stream().map(IndicatorRow::from).forEach(indicators::add);
    }

    EvidenceVersion toDomain() {
        return new EvidenceVersion(
                id,
                evidence.id(),
                revisionNumber,
                predecessorId,
                status,
                title,
                description,
                provenance,
                coveredFrom,
                coveredTo,
                authorId,
                createdAt,
                submittedAt,
                lockRevision,
                indicators.stream().map(row -> row.toDomain(id)).toList());
    }

    EvidenceJpaEntity evidence() {
        return evidence;
    }

    long lockRevision() {
        return lockRevision;
    }

    @Embeddable
    public static class IndicatorRow {
        @Column(name = "indicator_id", nullable = false)
        private UUID indicatorId;

        @Column(name = "indicator_code", nullable = false, length = 100)
        private String code;

        @Column(name = "indicator_title", nullable = false, length = 300)
        private String title;

        @Column(nullable = false)
        private int position;

        protected IndicatorRow() {}

        static IndicatorRow from(VersionIndicator value) {
            var row = new IndicatorRow();
            row.indicatorId = value.indicatorId();
            row.code = value.code();
            row.title = value.title();
            row.position = value.position();
            return row;
        }

        VersionIndicator toDomain(UUID versionId) {
            return new VersionIndicator(versionId, indicatorId, code, title, position);
        }
    }
}
