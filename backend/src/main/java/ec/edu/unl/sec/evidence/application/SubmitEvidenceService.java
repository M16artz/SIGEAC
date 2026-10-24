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
    private final EvidenceDownloadService downloads;
    private final Clock clock;

    @Autowired
    public SubmitEvidenceService(EvidenceDraftService drafts, EvidenceGovernanceRepository governance,
            EvidenceSourceService sources, EvidenceContextPort context, AuditPort audit,
            RequiredSourceCatalogPort catalog, EvidenceDownloadService downloads) {
        this(drafts, governance, sources, context, audit, catalog, downloads, Clock.systemUTC());
    }

    SubmitEvidenceService(EvidenceDraftService drafts, EvidenceGovernanceRepository governance,
            EvidenceSourceService sources, EvidenceContextPort context, AuditPort audit,
            RequiredSourceCatalogPort catalog, EvidenceDownloadService downloads, Clock clock) {
        this.drafts = drafts;
        this.governance = governance;
        this.sources = sources;
        this.context = context;
        this.audit = audit;
        this.catalog = catalog;
        this.downloads = downloads;
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
        drafts.requireEditableVersion(versionId, ifMatch);
        var details = drafts.getEvidence(version.evidenceId());
        var process = context.process(details.evidence().processId())
                .orElseThrow(EvidenceDraftService::notFound);
        requireActive(process);
        var snapshot = sources.snapshot(versionId);
        requireCompleteSources(version, process, snapshot);
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

    private void requireCompleteSources(EvidenceVersion version, EvidenceContextPort.ProcessContext process,
            EvidenceSourceService.Sources snapshot) {
        var coverages = governance.coverages(version.id());
        if (coverages.isEmpty()) {
            throw new EvidenceProblemException(EvidenceProblemException.Kind.CONFLICT,
                    "COVERAGE_REQUIRED", "Declara al menos una cobertura de fuente antes de remitir.");
        }
        var model = catalog.model(process.fixedModelVersionId()).filter(m -> m.versionId().equals(process.fixedModelVersionId()))
                .orElseThrow(EvidenceSourceCoverageService::incompatibleSource);
        var structured = governance.structuredSnapshots(version.id());
        for (var coverage : coverages) {
            var source = model.sources().stream().filter(s -> s.id().equals(coverage.requiredSourceId()))
                    .filter(s -> version.indicators().stream().anyMatch(i -> i.indicatorId().equals(s.indicatorId())))
                    .findFirst().orElseThrow(EvidenceSourceCoverageService::incompatibleSource);
            if (!version.id().equals(coverage.versionId())) throw EvidenceSourceCoverageService.incompatibleSource();
            switch (source.sourceType()) {
                case "FILE" -> { if (snapshot.files().isEmpty()) throw sourceRequired(); }
                case "EXTERNAL_LINK" -> { if (snapshot.externalReferences().isEmpty()) throw sourceRequired(); }
                case "STRUCTURED_DATA", "INSTITUTIONAL_REFERENCE" -> {
                    if (structured.stream().noneMatch(s -> s.versionId().equals(version.id())
                            && s.requiredSourceId().equals(source.id()) && s.kind().name().equals(source.sourceType()))) {
                        throw new EvidenceProblemException(EvidenceProblemException.Kind.CONFLICT,
                                "STRUCTURED_SOURCE_REQUIRED", "Completa el dato o referencia institucional de la fuente declarada.");
                    }
                }
                default -> throw EvidenceSourceCoverageService.incompatibleSource();
            }
        }
        if (structured.stream().anyMatch(s -> coverages.stream().noneMatch(c -> c.requiredSourceId().equals(s.requiredSourceId())))) {
            throw new EvidenceProblemException(EvidenceProblemException.Kind.CONFLICT,
                    "STRUCTURED_SOURCE_NOT_COVERED", "Cada dato estructurado debe conservar su cobertura explícita.");
        }
        if (snapshot.files().stream().anyMatch(file -> file.status() != FileAsset.Status.AVAILABLE)) {
            throw new EvidenceProblemException(EvidenceProblemException.Kind.CONFLICT,
                    "FILE_NOT_AVAILABLE",
                    "Todos los archivos deben estar disponibles y verificables antes de remitir.");
        }
        for (var file : snapshot.files()) downloads.prepare(version.id(), file.snapshot().fileId());
    }

    private static EvidenceProblemException sourceRequired() {
        return new EvidenceProblemException(EvidenceProblemException.Kind.CONFLICT,
                "SOURCE_REQUIRED", "Falta el archivo o enlace correspondiente a una fuente declarada.");
    }

    private static void requireActive(EvidenceContextPort.ProcessContext process) {
        if (!"ACTIVE".equals(process.state())) {
            throw new EvidenceProblemException(EvidenceProblemException.Kind.CONFLICT,
                    "PROCESS_NOT_ACTIVE",
                    "El proceso debe estar activo para registrar o modificar evidencias.");
        }
    }
}
