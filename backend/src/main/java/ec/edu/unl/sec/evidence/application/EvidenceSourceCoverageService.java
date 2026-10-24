package ec.edu.unl.sec.evidence.application;

import ec.edu.unl.sec.evidence.domain.EvidenceVersion;
import ec.edu.unl.sec.evidence.domain.SourceCoverage;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Explicit coverage declared on drafts only; one coverage per required source
 * and version, always tied to the fixed model of the process.
 */
@Service
public class EvidenceSourceCoverageService {
    private final EvidenceDraftService drafts;
    private final EvidenceGovernanceRepository governance;
    private final RequiredSourceCatalogPort catalog;
    private final EvidenceContextPort context;
    private final EvidenceAuthorizationPort authorization;
    private final Clock clock;

    @Autowired
    public EvidenceSourceCoverageService(EvidenceDraftService drafts, EvidenceGovernanceRepository governance,
            RequiredSourceCatalogPort catalog, EvidenceContextPort context, EvidenceAuthorizationPort authorization) {
        this(drafts, governance, catalog, context, authorization, Clock.systemUTC());
    }

    EvidenceSourceCoverageService(EvidenceDraftService drafts, EvidenceGovernanceRepository governance,
            RequiredSourceCatalogPort catalog, EvidenceContextPort context, EvidenceAuthorizationPort authorization,
            Clock clock) {
        this.drafts = drafts;
        this.governance = governance;
        this.catalog = catalog;
        this.context = context;
        this.authorization = authorization;
        this.clock = clock;
    }

    @Transactional
    public Declared declare(UUID versionId, String ifMatch, UUID requiredSourceId, String coverageNote) {
        var version = drafts.getVersion(versionId);
        EvidenceDraftService.verifyEtag(ifMatch, version.lockRevision());
        requireDraft(version);
        drafts.requireEditableVersion(versionId, ifMatch);
        var process = context.process(processOf(version.evidenceId()))
                .orElseThrow(EvidenceDraftService::notFound);
        var entry = sources(process.fixedModelVersionId()).stream()
                .filter(source -> source.id().equals(requiredSourceId))
                .findFirst()
                .orElseThrow(EvidenceSourceCoverageService::incompatibleSource);
        var indicatorIds = version.indicators().stream()
                .map(ec.edu.unl.sec.evidence.domain.VersionIndicator::indicatorId)
                .toList();
        if (!indicatorIds.contains(entry.indicatorId())) throw incompatibleSource();
        if (governance.coverages(versionId).stream().anyMatch(coverage ->
                coverage.requiredSourceId().equals(requiredSourceId))) {
            throw new EvidenceProblemException(EvidenceProblemException.Kind.CONFLICT,
                    "SOURCE_COVERAGE_DUPLICATE",
                    "La fuente ya tiene una cobertura declarada en esta versiÃ³n.");
        }
        var actor = currentActor();
        advance(version);
        var saved = governance.saveCoverage(new SourceCoverage(
                null, versionId, requiredSourceId, coverageNote, actor, clock.instant()));
        return new Declared(saved, Math.incrementExact(version.lockRevision()));
    }

    @Transactional(readOnly = true)
    public List<SourceCoverage> coverages(UUID versionId) {
        drafts.getVersion(versionId);
        return governance.coverages(versionId);
    }

    @Transactional
    public long remove(UUID versionId, UUID coverageId, String ifMatch) {
        var version = drafts.getVersion(versionId);
        EvidenceDraftService.verifyEtag(ifMatch, version.lockRevision());
        requireDraft(version);
        drafts.requireEditableVersion(versionId, ifMatch);
        var coverage = governance.coverage(coverageId)
                .filter(stored -> stored.versionId().equals(versionId))
                .orElseThrow(EvidenceDraftService::notFound);
        advance(version);
        governance.deleteCoverage(coverage.id());
        return Math.incrementExact(version.lockRevision());
    }

    @Transactional(readOnly = true)
    public List<RequiredSourceState> requiredSources(UUID versionId) {
        var version = drafts.getVersion(versionId);
        var process = context.process(processOf(version.evidenceId()))
                .orElseThrow(EvidenceDraftService::notFound);
        var indicatorIds = version.indicators().stream()
                .map(ec.edu.unl.sec.evidence.domain.VersionIndicator::indicatorId)
                .toList();
        var covered = governance.coverages(versionId).stream()
                .map(SourceCoverage::requiredSourceId)
                .collect(java.util.stream.Collectors.toSet());
        return sources(process.fixedModelVersionId()).stream()
                .filter(source -> indicatorIds.contains(source.indicatorId()))
                .map(source -> new RequiredSourceState(
                        source.id(), source.indicatorId(), source.indicatorCode(), source.code(), source.name(),
                        source.sourceType(), source.required(), source.cardinalityMin(), source.cardinalityMax(),
                        covered.contains(source.id())))
                .toList();
    }

    private List<RequiredSourceCatalogPort.Entry> sources(UUID modelVersionId) {
        return catalog.model(modelVersionId)
                .map(RequiredSourceCatalogPort.ModelSources::sources)
                .orElseThrow(EvidenceSourceCoverageService::incompatibleSource);
    }

    private void advance(EvidenceVersion version) {
        if (!governance.advanceDraftRevision(version.id(), version.lockRevision())) {
            throw new EvidenceProblemException(EvidenceProblemException.Kind.PRECONDITION_FAILED,
                    "STALE_DRAFT", "El borrador cambió; recargue antes de continuar.");
        }
    }

    private UUID processOf(UUID evidenceId) {
        return drafts.getEvidence(evidenceId).evidence().processId();
    }

    private UUID currentActor() {
        return authorization.currentActor()
                .orElseThrow(() -> new EvidenceProblemException(
                        EvidenceProblemException.Kind.UNAUTHORIZED,
                        "AUTHENTICATION_REQUIRED",
                        "Se requiere una sesiÃ³n vigente."))
                .identityId();
    }

    private static void requireDraft(EvidenceVersion version) {
        if (version.status() != ec.edu.unl.sec.evidence.domain.EvidenceVersionStatus.DRAFT) {
            throw new EvidenceProblemException(EvidenceProblemException.Kind.CONFLICT,
                    "SOURCE_COVERAGE_IMMUTABLE",
                    "La cobertura solo puede declararse sobre borradores.");
        }
    }

    static EvidenceProblemException incompatibleSource() {
        return new EvidenceProblemException(
                EvidenceProblemException.Kind.INVALID,
                "SOURCE_NOT_COMPATIBLE",
                "La fuente no pertenece al modelo fijado de los indicadores de esta evidencia.");
    }

    public record RequiredSourceState(UUID id, UUID indicatorId, String indicatorCode, String code, String name,
            String sourceType, boolean required, int cardinalityMin, Integer cardinalityMax, boolean covered) {}

    public record Declared(SourceCoverage value, long versionRevision) {}
}
