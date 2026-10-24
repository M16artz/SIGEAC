package ec.edu.unl.sec.evidence.application;

import ec.edu.unl.sec.audit.application.AuditEventCommand;
import ec.edu.unl.sec.audit.application.AuditPort;
import ec.edu.unl.sec.evidence.domain.EvidenceVersion;
import ec.edu.unl.sec.evidence.domain.ExternalReference;
import ec.edu.unl.sec.evidence.domain.SourceCoverage;
import ec.edu.unl.sec.evidence.domain.StructuredEvidence;
import ec.edu.unl.sec.evidence.domain.VersionFile;
import java.time.Clock;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Correction flow (P10): creates one successor draft from the latest
 * submitted version of an evidence, inheriting coverages, structured
 * snapshots, files and references so the responsible edits and resubmits
 * while the predecessor and its review decisions stay immutable. No
 * acceptance is inherited; the correction re-enters review (P09).
 */
@Service
public class CreateEvidenceSuccessorService {
    private final EvidenceDraftService drafts;
    private final EvidenceDraftRepository draftRepository;
    private final EvidenceSourceRepository sources;
    private final EvidenceGovernanceRepository governance;
    private final AuditPort audit;
    private final Clock clock;

    @Autowired
    public CreateEvidenceSuccessorService(EvidenceDraftService drafts, EvidenceDraftRepository draftRepository,
            EvidenceSourceRepository sources, EvidenceGovernanceRepository governance, AuditPort audit) {
        this(drafts, draftRepository, sources, governance, audit, Clock.systemUTC());
    }

    CreateEvidenceSuccessorService(EvidenceDraftService drafts, EvidenceDraftRepository draftRepository,
            EvidenceSourceRepository sources, EvidenceGovernanceRepository governance, AuditPort audit, Clock clock) {
        this.drafts = drafts;
        this.draftRepository = draftRepository;
        this.sources = sources;
        this.governance = governance;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public SuccessorVersion createSuccessor(UUID sourceVersionId) {
        var successor = drafts.createSuccessorDraft(sourceVersionId);
        draftRepository.createSuccessor(successor, sourceVersionId);
        for (var coverage : governance.coverages(sourceVersionId)) {
            governance.saveCoverage(new SourceCoverage(
                    null, successor.id(), coverage.requiredSourceId(), coverage.coverageNote(),
                    successor.authorId(), clock.instant()));
        }
        for (var snapshot : governance.structuredSnapshots(sourceVersionId)) {
            governance.saveStructured(new StructuredEvidence(
                    null, successor.id(), snapshot.requiredSourceId(), snapshot.kind(), snapshot.value(),
                    snapshot.unit(), snapshot.periodStart(), snapshot.periodEnd(), snapshot.reference(),
                    successor.authorId(), clock.instant()));
        }
        for (var reference : sources.references(sourceVersionId)) {
            sources.addReference(new ExternalReference(
                    UUID.randomUUID(), successor.id(), reference.uri(), reference.description(),
                    sources.nextReferencePosition(successor.id())));
        }
        for (var file : sources.files(sourceVersionId)) {
            sources.addFile(new VersionFile(
                    successor.id(), file.fileId(), file.originalName(), file.declaredMediaType(),
                    file.detectedMediaType(), file.sizeBytes(), file.sha256(),
                    sources.nextFilePosition(successor.id())));
        }
        audit.record(new AuditEventCommand(
                clock.instant(),
                successor.authorId(),
                null,
                UUID.randomUUID(),
                "EVIDENCE_CORRECTION",
                "EVIDENCE_VERSION",
                successor.id(),
                null,
                null,
                "SUCCESS",
                successor.revisionNumber() + "←" + sourceVersionId,
                java.util.Map.of("revisionNumber", successor.revisionNumber())));
        return new SuccessorVersion(successor.id(), successor.evidenceId(), successor.revisionNumber());
    }

    public record SuccessorVersion(UUID versionId, UUID evidenceId, int revisionNumber) {}
}
