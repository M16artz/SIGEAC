package ec.edu.unl.sec.evidence.application;

import ec.edu.unl.sec.audit.application.AuditEventCommand;
import ec.edu.unl.sec.audit.application.AuditPort;
import ec.edu.unl.sec.evidence.domain.EvidenceVersion;
import ec.edu.unl.sec.filestorage.domain.FileAsset;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Immutable submission: a complete draft becomes submitted atomically and the
 * successful audit event shares the same transaction. The version content then
 * stops changing.
 */
@Service
public class SubmitEvidenceService {
    private final EvidenceDraftService drafts;
    private final EvidenceGovernanceRepository governance;
    private final EvidenceSourceService sources;
    private final EvidenceContextPort context;
    private final AuditPort audit;
    private final Clock clock;

    @Autowired
    public SubmitEvidenceService(EvidenceDraftService drafts, EvidenceGovernanceRepository governance,
            EvidenceSourceService sources, EvidenceContextPort context, AuditPort audit) {
        this(drafts, governance, sources, context, audit, Clock.systemUTC());
    }

    SubmitEvidenceService(EvidenceDraftService drafts, EvidenceGovernanceRepository governance,
            EvidenceSourceService sources, EvidenceContextPort context, AuditPort audit, Clock clock) {
        this.drafts = drafts;
        this.governance = governance;
        this.sources = sources;
        this.context = context;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public EvidenceVersion submit(UUID versionId, String ifMatch) {
        var version = drafts.getVersion(versionId);
        EvidenceDraftService.verifyEtag(ifMatch, version.lockRevision());
        if (version.status() != ec.edu.unl.sec.evidence.domain.EvidenceVersionStatus.DRAFT) {
            throw new EvidenceProblemException(EvidenceProblemException.Kind.CONFLICT,
                    "EVIDENCE_VERSION_IMMUTABLE",
                    "La versión ya está remitida y no admite cambios.");
        }
        var details = drafts.getEvidence(version.evidenceId());
        var process = context.process(details.evidence().processId())
                .orElseThrow(EvidenceDraftService::notFound);
        requireActive(process);
        var snapshot = sources.snapshot(versionId);
        requireCompleteSources(snapshot);
        var submittedAt = clock.instant();
        if (!governance.markSubmitted(versionId, submittedAt, version.lockRevision())) {
            throw new EvidenceProblemException(EvidenceProblemException.Kind.PRECONDITION_FAILED,
                    "STALE_DRAFT",
                    "El borrador cambió; recargue antes de continuar.");
        }
        audit.record(new AuditEventCommand(
                submittedAt,
                version.authorId(),
                null,
                details.evidence().correlationId(),
                "EVIDENCE_SUBMITTED",
                "EVIDENCE_VERSION",
                version.id(),
                process.careerId(),
                process.id(),
                "SUCCESS",
                null,
                Map.of("revision", version.revisionNumber(),
                        "files", snapshot.files().size(),
                        "references", snapshot.externalReferences().size())));
        return version.submit(submittedAt);
    }

    private static void requireCompleteSources(EvidenceSourceService.Sources snapshot) {
        if (snapshot.files().isEmpty() && snapshot.externalReferences().isEmpty()) {
            throw new EvidenceProblemException(EvidenceProblemException.Kind.CONFLICT,
                    "SOURCE_REQUIRED",
                    "Adjunta al menos un archivo o una referencia externa antes de remitir.");
        }
        if (snapshot.files().stream().anyMatch(file -> file.status() != FileAsset.Status.AVAILABLE)) {
            throw new EvidenceProblemException(EvidenceProblemException.Kind.CONFLICT,
                    "FILE_NOT_AVAILABLE",
                    "Todos los archivos deben estar disponibles y verificables antes de remitir.");
        }
    }

    private static void requireActive(EvidenceContextPort.ProcessContext process) {
        if (!"ACTIVE".equals(process.state())) {
            throw new EvidenceProblemException(EvidenceProblemException.Kind.CONFLICT,
                    "PROCESS_NOT_ACTIVE",
                    "El proceso debe estar activo para registrar o modificar evidencias.");
        }
    }
}
