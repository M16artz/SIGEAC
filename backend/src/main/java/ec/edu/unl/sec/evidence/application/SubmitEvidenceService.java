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
    private final RequiredSourceCatalogPort catalog;
    private final Clock clock;

    @Autowired
    public SubmitEvidenceService(EvidenceDraftService drafts, EvidenceGovernanceRepository governance,
            EvidenceSourceService sources, EvidenceContextPort context, AuditPort audit, RequiredSourceCatalogPort catalog) {
        this(drafts, governance, sources, context, audit, catalog, Clock.systemUTC());
    }

    SubmitEvidenceService(EvidenceDraftService drafts, EvidenceGovernanceRepository governance,
            EvidenceSourceService sources, EvidenceContextPort context, AuditPort audit, RequiredSourceCatalogPort catalog, Clock clock) {
        this.drafts = drafts;
        this.governance = governance;
        this.sources = sources;
        this.context = context;
        this.audit = audit;
        this.catalog = catalog;
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
        var process = context.processForWrite(details.evidence().processId())
                .orElseThrow(EvidenceDraftService::notFound);
        requireActive(process);
        var snapshot = sources.snapshot(versionId);
        var structured = governance.structuredSnapshots(versionId);
        requireCompleteSources(snapshot, !structured.isEmpty());
        requireCoverage(version, process, snapshot, structured);
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
                        "references", snapshot.externalReferences().size(), "structured", structured.size())));
        return version.submit(submittedAt);
    }

    private static void requireCompleteSources(EvidenceSourceService.Sources snapshot, boolean hasStructured) {
        if (snapshot.files().isEmpty() && snapshot.externalReferences().isEmpty() && !hasStructured) {
            throw new EvidenceProblemException(EvidenceProblemException.Kind.CONFLICT,
                    "SOURCE_REQUIRED",
                    "Registra un archivo, enlace, dato estructurado o referencia institucional antes de remitir.");
        }
        if (snapshot.files().stream().anyMatch(file -> file.status() != FileAsset.Status.AVAILABLE)) {
            throw new EvidenceProblemException(EvidenceProblemException.Kind.CONFLICT,
                    "FILE_NOT_AVAILABLE",
                    "Todos los archivos deben estar disponibles y verificables antes de remitir.");
        }
    }

    private void requireCoverage(EvidenceVersion version, EvidenceContextPort.ProcessContext process,
            EvidenceSourceService.Sources content, java.util.List<ec.edu.unl.sec.evidence.domain.StructuredEvidence> structured) {
        var coverages = governance.coverages(version.id());
        if (coverages.isEmpty()) throw invalid("SOURCE_COVERAGE_REQUIRED", "Declara las fuentes cubiertas antes de remitir.");
        var model = catalog.model(process.fixedModelVersionId()).orElseThrow(EvidenceSourceCoverageService::incompatibleSource);
        for (var coverage : coverages) {
            var source = model.sources().stream().filter(s -> s.id().equals(coverage.requiredSourceId()))
                    .findFirst().orElseThrow(EvidenceSourceCoverageService::incompatibleSource);
            if (version.indicators().stream().noneMatch(i -> i.indicatorId().equals(source.indicatorId())))
                throw EvidenceSourceCoverageService.incompatibleSource();
            if ("FILE".equals(source.sourceType())) {
                if (content.files().isEmpty()) throw invalid("SOURCE_CONTENT_REQUIRED", "La fuente cubierta requiere un archivo.");
            } else if ("EXTERNAL_LINK".equals(source.sourceType())) {
                if (content.externalReferences().isEmpty()) throw invalid("SOURCE_CONTENT_REQUIRED", "La fuente cubierta requiere un enlace.");
            } else {
                var value = structured.stream().filter(s -> s.requiredSourceId().equals(source.id()))
                        .findFirst().orElseThrow(() -> invalid("STRUCTURED_SOURCE_REQUIRED", "La fuente cubierta requiere su snapshot tipado."));
                if (!value.kind().name().equals(source.sourceType())) throw EvidenceSourceCoverageService.incompatibleSource();
                if (value.periodStart().isBefore(version.coveredFrom()) || value.periodEnd().isAfter(version.coveredTo()))
                    throw invalid("STRUCTURED_PERIOD_INVALID", "El periodo del snapshot debe estar dentro del periodo de la evidencia.");
                if (value.kind() == ec.edu.unl.sec.evidence.domain.StructuredEvidence.Kind.INSTITUTIONAL_REFERENCE
                        && (value.reference() == null || value.reference().isBlank()))
                    throw invalid("INSTITUTIONAL_REFERENCE_REQUIRED", "La fuente requiere la referencia institucional explicita.");
            }
        }
    }

    private static EvidenceProblemException invalid(String code, String message) {
        return new EvidenceProblemException(EvidenceProblemException.Kind.INVALID, code, message);
    }

    private static void requireActive(EvidenceContextPort.ProcessContext process) {
        if (!"ACTIVE".equals(process.state())) {
            throw new EvidenceProblemException(EvidenceProblemException.Kind.CONFLICT,
                    "PROCESS_NOT_ACTIVE",
                    "El proceso debe estar activo para registrar o modificar evidencias.");
        }
    }
}
