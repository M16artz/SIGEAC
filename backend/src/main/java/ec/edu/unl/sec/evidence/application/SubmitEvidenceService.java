package ec.edu.unl.sec.evidence.application;

import ec.edu.unl.sec.evidence.domain.EvidenceVersion;
import java.time.Clock;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Immutable submission: a draft becomes submitted only when at least one
 * explicit source coverage exists; the version content then stops changing.
 */
@Service
public class SubmitEvidenceService {
    private final EvidenceDraftService drafts;
    private final EvidenceGovernanceRepository governance;
    private final EvidenceContextPort context;
    private final Clock clock;

    @Autowired
    public SubmitEvidenceService(EvidenceDraftService drafts, EvidenceGovernanceRepository governance,
            EvidenceContextPort context) {
        this(drafts, governance, context, Clock.systemUTC());
    }

    SubmitEvidenceService(EvidenceDraftService drafts, EvidenceGovernanceRepository governance,
            EvidenceContextPort context, Clock clock) {
        this.drafts = drafts;
        this.governance = governance;
        this.context = context;
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
        var process = context.process(processOf(version.evidenceId()))
                .orElseThrow(EvidenceDraftService::notFound);
        requireActive(process);
        if (governance.coverages(versionId).isEmpty()) {
            throw new EvidenceProblemException(EvidenceProblemException.Kind.CONFLICT,
                    "COVERAGE_REQUIRED",
                    "Declara al menos una cobertura de fuente antes de remitir la evidencia.");
        }
        var submittedAt = clock.instant();
        if (!governance.markSubmitted(versionId, submittedAt, version.lockRevision())) {
            throw new EvidenceProblemException(EvidenceProblemException.Kind.PRECONDITION_FAILED,
                    "STALE_DRAFT",
                    "El borrador cambió; recargue antes de continuar.");
        }
        return version.submit(submittedAt);
    }

    private UUID processOf(UUID evidenceId) {
        return drafts.getEvidence(evidenceId).evidence().processId();
    }

    private static void requireActive(EvidenceContextPort.ProcessContext process) {
        if (!"ACTIVE".equals(process.state())) {
            throw new EvidenceProblemException(EvidenceProblemException.Kind.CONFLICT,
                    "PROCESS_NOT_ACTIVE",
                    "El proceso debe estar activo para registrar o modificar evidencias.");
        }
    }
}
