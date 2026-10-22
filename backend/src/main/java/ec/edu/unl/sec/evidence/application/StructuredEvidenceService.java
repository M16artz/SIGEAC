package ec.edu.unl.sec.evidence.application;

import ec.edu.unl.sec.evidence.domain.EvidenceVersion;
import ec.edu.unl.sec.evidence.domain.EvidenceVersionStatus;
import ec.edu.unl.sec.evidence.domain.SourceCoverage;
import ec.edu.unl.sec.evidence.domain.StructuredEvidence;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Typed snapshots for structured-data and institutional-reference modalities
 * (FR-069 / AC-X16). Snapshots require an already declared coverage for the
 * same version and required source, keep value, optional unit, mandated period
 * and optional reference, and share draft-only mutability with files.
 */
@Service
public class StructuredEvidenceService {
    private final EvidenceDraftService drafts;
    private final EvidenceGovernanceRepository governance;
    private final RequiredSourceCatalogPort catalog;
    private final EvidenceContextPort context;
    private final EvidenceAuthorizationPort authorization;
    private final Clock clock;

    @Autowired
    public StructuredEvidenceService(EvidenceDraftService drafts, EvidenceGovernanceRepository governance,
            RequiredSourceCatalogPort catalog, EvidenceContextPort context, EvidenceAuthorizationPort authorization) {
        this(drafts, governance, catalog, context, authorization, Clock.systemUTC());
    }

    StructuredEvidenceService(EvidenceDraftService drafts, EvidenceGovernanceRepository governance,
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
    public Register register(UUID versionId, String ifMatch, StructuredCommand command) {
        var version = drafts.getVersion(versionId);
        EvidenceDraftService.verifyEtag(ifMatch, version.lockRevision());
        requireDraft(version);
        var process = context.process(processOf(version.evidenceId()))
                .orElseThrow(EvidenceDraftService::notFound);
        if (governance.coverages(versionId).stream()
                .noneMatch(item -> item.requiredSourceId().equals(command.requiredSourceId()))) {
            throw notCovered();
        }
        var entry = sources(process.fixedModelVersionId()).stream()
                .filter(source -> source.id().equals(command.requiredSourceId()))
                .findFirst()
                .orElseThrow(EvidenceSourceCoverageService::incompatibleSource);
        var indicatorIds = version.indicators().stream()
                .map(ec.edu.unl.sec.evidence.domain.VersionIndicator::indicatorId)
                .toList();
        if (!indicatorIds.contains(entry.indicatorId())) {
            throw EvidenceSourceCoverageService.incompatibleSource();
        }
        if (!entry.sourceType().equals(command.kind().name())) {
            throw new EvidenceProblemException(EvidenceProblemException.Kind.INVALID,
                    "SOURCE_NOT_COMPATIBLE",
                    "La fuente no admite la modalidad de snapshot solicitada.");
        }
        if (governance.structuredSnapshot(versionId, command.requiredSourceId()).isPresent()) {
            throw new EvidenceProblemException(EvidenceProblemException.Kind.CONFLICT,
                    "STRUCTURED_SNAPSHOT_DUPLICATE",
                    "La versi\u00f3n ya tiene un snapshot para la fuente requerida.");
        }
        var value = typedValue(command.value());
        if (command.periodStart() == null || command.periodEnd() == null
                || command.periodStart().isAfter(command.periodEnd())) {
            throw new EvidenceProblemException(EvidenceProblemException.Kind.INVALID,
                    "STRUCTURED_PERIOD_INVALID",
                    "El per\u00edodo del snapshot es inv\u00e1lido.");
        }
        if (command.unit() != null && command.unit().strip().length() > 100) {
            throw metadataInvalid("La unidad no puede exceder cien caracteres.");
        }
        if (command.reference() != null && command.reference().strip().length() > 2000) {
            throw metadataInvalid("La referencia no puede exceder dos mil caracteres.");
        }
        var snapshot = new StructuredEvidence(
                null, versionId, command.requiredSourceId(), command.kind(), value, command.unit(),
                command.periodStart(), command.periodEnd(), command.reference(), currentActor(), clock.instant());
        var saved = governance.saveStructured(snapshot);
        return new Register(saved == null ? snapshot : saved, version.lockRevision());
    }

    @Transactional(readOnly = true)
    public List<StructuredEvidence> snapshots(UUID versionId) {
        drafts.getVersion(versionId);
        return governance.structuredSnapshots(versionId);
    }

    private List<RequiredSourceCatalogPort.Entry> sources(UUID modelVersionId) {
        return catalog.model(modelVersionId)
                .map(RequiredSourceCatalogPort.ModelSources::sources)
                .orElseThrow(EvidenceSourceCoverageService::incompatibleSource);
    }

    private UUID processOf(UUID evidenceId) {
        return drafts.getEvidence(evidenceId).evidence().processId();
    }

    private UUID currentActor() {
        return authorization.currentActor()
                .orElseThrow(() -> new EvidenceProblemException(
                        EvidenceProblemException.Kind.UNAUTHORIZED,
                        "AUTHENTICATION_REQUIRED",
                        "Se requiere una sesi\u00f3n vigente."))
                .identityId();
    }

    private static void requireDraft(EvidenceVersion version) {
        if (version.status() != EvidenceVersionStatus.DRAFT) {
            throw new EvidenceProblemException(EvidenceProblemException.Kind.CONFLICT,
                    "STRUCTURED_SNAPSHOT_IMMUTABLE",
                    "El snapshot solo puede registrarse sobre borradores.");
        }
    }

    private static EvidenceProblemException notCovered() {
        return new EvidenceProblemException(EvidenceProblemException.Kind.CONFLICT,
                "STRUCTURED_SOURCE_NOT_COVERED",
                "La fuente requiere una cobertura declarada antes de registrar el snapshot.");
    }

    private static EvidenceProblemException metadataInvalid(String message) {
        return new EvidenceProblemException(EvidenceProblemException.Kind.INVALID,
                "STRUCTURED_METADATA_INVALID", message);
    }

    private static StructuredEvidence.TypedValue typedValue(Object value) {
        Objects.requireNonNull(value, "value required");
        if (value instanceof Number number) {
            return StructuredEvidence.TypedValue.number(new BigDecimal(number.toString()));
        }
        if (value instanceof Boolean flag) {
            return StructuredEvidence.TypedValue.flag(flag);
        }
        var text = value.toString().strip();
        if (text.isEmpty()) {
            throw new EvidenceProblemException(EvidenceProblemException.Kind.INVALID,
                    "STRUCTURED_VALUE_INVALID",
                    "El valor del snapshot no puede estar vac\u00edo.");
        }
        if (text.length() > 4000) {
            throw new EvidenceProblemException(EvidenceProblemException.Kind.INVALID,
                    "STRUCTURED_VALUE_INVALID",
                    "El valor del snapshot no puede exceder cuatro mil caracteres.");
        }
        return StructuredEvidence.TypedValue.text(text);
    }

    public record StructuredCommand(UUID requiredSourceId, StructuredEvidence.Kind kind, Object value, String unit,
            LocalDate periodStart, LocalDate periodEnd, String reference) {}

    public record Register(StructuredEvidence value, long versionRevision) {}
}
