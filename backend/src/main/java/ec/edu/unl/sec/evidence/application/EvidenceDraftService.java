package ec.edu.unl.sec.evidence.application;

import ec.edu.unl.sec.evidence.domain.Evidence;
import ec.edu.unl.sec.evidence.domain.EvidenceVersion;
import ec.edu.unl.sec.evidence.domain.VersionIndicator;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EvidenceDraftService {
    private final EvidenceDraftRepository repository;
    private final EvidenceAuthorizationPort authorization;
    private final EvidenceContextPort context;
    private final Clock clock;

    @Autowired
    public EvidenceDraftService(
            EvidenceDraftRepository repository,
            EvidenceAuthorizationPort authorization,
            EvidenceContextPort context) {
        this(repository, authorization, context, Clock.systemUTC());
    }

    EvidenceDraftService(
            EvidenceDraftRepository repository,
            EvidenceAuthorizationPort authorization,
            EvidenceContextPort context,
            Clock clock) {
        this.repository = repository;
        this.authorization = authorization;
        this.context = context;
        this.clock = clock;
    }

    public EvidenceDetails create(CreateCommand command, UUID correlationId) {
        var actor = actor();
        var process = process(command.processId());
        UUID versionId = UUID.randomUUID();
        var indicators = authorizeIndicators(actor, process, versionId, command.indicatorIds());
        requireActive(process);
        var evidence = new Evidence(null, process.id(), actor.identityId(), clock.instant(), correlationId, 0).advance();
        var version = new EvidenceVersion(
                versionId,
                evidence.id(),
                1,
                null,
                null,
                command.title(),
                command.description(),
                command.provenance(),
                command.coveredFrom(),
                command.coveredTo(),
                actor.identityId(),
                clock.instant(),
                null,
                0,
                indicators);
        repository.create(evidence, version);
        return new EvidenceDetails(evidence, version);
    }

    public List<EvidenceDetails> list(UUID processId, Optional<UUID> indicatorId) {
        var actor = actor();
        var process = process(processId);
        return repository.listByProcess(processId).stream()
                .filter(item -> indicatorId.isEmpty()
                        || item.latestVersion().indicators().stream()
                                .anyMatch(indicator -> indicator.indicatorId().equals(indicatorId.get())))
                .filter(item -> visible(actor, process, item.latestVersion()))
                .toList();
    }

    public EvidenceDetails getEvidence(UUID evidenceId) {
        var details = repository.findEvidence(evidenceId).orElseThrow(EvidenceDraftService::notFound);
        authorizeDirect(details.evidence(), details.latestVersion());
        return details;
    }

    public EvidenceVersion getVersion(UUID versionId) {
        var version = repository.findVersion(versionId).orElseThrow(EvidenceDraftService::notFound);
        var evidence = repository.findEvidence(version.evidenceId())
                .map(EvidenceDetails::evidence)
                .orElseThrow(EvidenceDraftService::notFound);
        authorizeDirect(evidence, version);
        return version;
    }

    public EvidenceVersion update(UUID versionId, String ifMatch, UpdateCommand command, UUID correlationId) {
        var current = getVersion(versionId);
        verifyEtag(ifMatch, current.lockRevision());
        var evidence = repository.findEvidence(current.evidenceId())
                .map(EvidenceDetails::evidence)
                .orElseThrow(EvidenceDraftService::notFound);
        var actor = actor();
        var process = process(evidence.processId());
        var indicators = authorizeIndicators(actor, process, current.id(), command.indicatorIds());
        requireActive(process);
        if (!current.authorId().equals(actor.identityId())) throw notFound();
        var updated = current.update(
                command.title(),
                command.description(),
                command.provenance(),
                command.coveredFrom(),
                command.coveredTo(),
                indicators);
        repository.update(updated, current.lockRevision());
        return updated;
    }

    public EvidenceVersion requireEditableVersion(UUID versionId, String ifMatch) {
        var version = getVersion(versionId);
        verifyEtag(ifMatch, version.lockRevision());
        if (version.status() != ec.edu.unl.sec.evidence.domain.EvidenceVersionStatus.DRAFT) {
            throw new EvidenceProblemException(EvidenceProblemException.Kind.CONFLICT,
                    "DRAFT_REQUIRED", "La versión remitida no admite cambios.");
        }
        var evidence = repository.findEvidence(version.evidenceId()).orElseThrow(EvidenceDraftService::notFound).evidence();
        requireActive(process(evidence.processId()));
        if (!version.authorId().equals(actor().identityId())) throw notFound();
        return version;
    }

    private void authorizeDirect(Evidence evidence, EvidenceVersion version) {
        var actor = actor();
        var process = context.process(evidence.processId()).orElseThrow(EvidenceDraftService::notFound);
        if (!visible(actor, process, version)) throw notFound();
    }

    private boolean visible(
            EvidenceAuthorizationPort.Actor actor,
            EvidenceContextPort.ProcessContext process,
            EvidenceVersion version) {
        return !actor.technical()
                && version.indicators().stream().allMatch(indicator ->
                        authorization.canCollect(actor.identityId(), process.careerId(), process.id(), indicator.indicatorId())
                                && context.hasCurrentResponsibleAssignment(
                                        process.id(), indicator.indicatorId(), actor.identityId(), clock.instant()));
    }

    private List<VersionIndicator> authorizeIndicators(
            EvidenceAuthorizationPort.Actor actor,
            EvidenceContextPort.ProcessContext process,
            UUID versionId,
            List<UUID> indicatorIds) {
        if (indicatorIds == null || indicatorIds.isEmpty()) throw outOfScope();
        var available = process.indicators().stream().collect(java.util.stream.Collectors.toMap(
                EvidenceContextPort.Indicator::id, indicator -> indicator));
        var unique = indicatorIds.stream().distinct().toList();
        if (unique.size() != indicatorIds.size()) throw outOfScope();
        var snapshots = new java.util.ArrayList<VersionIndicator>();
        for (int index = 0; index < unique.size(); index++) {
            UUID indicatorId = unique.get(index);
            var indicator = available.get(indicatorId);
            if (indicator == null
                    || actor.technical()
                    || !authorization.canCollect(actor.identityId(), process.careerId(), process.id(), indicatorId)
                    || !context.hasCurrentResponsibleAssignment(process.id(), indicatorId, actor.identityId(), clock.instant())) {
                throw outOfScope();
            }
            snapshots.add(new VersionIndicator(versionId, indicatorId, indicator.code(), indicator.title(), index + 1));
        }
        return List.copyOf(snapshots);
    }

    private EvidenceAuthorizationPort.Actor actor() {
        return authorization.currentActor().orElseThrow(() -> new EvidenceProblemException(
                EvidenceProblemException.Kind.UNAUTHORIZED,
                "AUTHENTICATION_REQUIRED",
                "Se requiere una sesión vigente."));
    }

    private EvidenceContextPort.ProcessContext process(UUID processId) {
        return context.process(processId).orElseThrow(EvidenceDraftService::notFound);
    }

    private static EvidenceProblemException outOfScope() {
        return new EvidenceProblemException(
                EvidenceProblemException.Kind.INVALID,
                "INDICATOR_OUT_OF_SCOPE",
                "Uno o más indicadores no pertenecen al ámbito vigente.");
    }

    static EvidenceProblemException notFound() {
        return new EvidenceProblemException(
                EvidenceProblemException.Kind.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                "No se encontró el recurso solicitado.");
    }

    private static void requireActive(EvidenceContextPort.ProcessContext process) {
        if (!"ACTIVE".equals(process.state())) {
            throw new EvidenceProblemException(EvidenceProblemException.Kind.CONFLICT,
                    "PROCESS_NOT_ACTIVE", "El proceso debe estar activo para registrar o modificar evidencias.");
        }
    }

    static void verifyEtag(String value, long current) {
        if (value == null || value.isBlank()) {
            throw new EvidenceProblemException(
                    EvidenceProblemException.Kind.PRECONDITION_REQUIRED,
                    "PRECONDITION_REQUIRED",
                    "Se requiere If-Match.");
        }
        if (!value.matches("\\\"[0-9]+\\\"")) {
            throw new EvidenceProblemException(
                    EvidenceProblemException.Kind.INVALID,
                    "INVALID_ETAG",
                    "If-Match no contiene una revisión válida.");
        }
        long revision;
        try {
            revision = Long.parseLong(value.substring(1, value.length() - 1));
        } catch (NumberFormatException exception) {
            throw new EvidenceProblemException(
                    EvidenceProblemException.Kind.INVALID,
                    "INVALID_ETAG",
                    "If-Match no contiene una revisión válida.");
        }
        if (revision != current) {
            throw new EvidenceProblemException(
                    EvidenceProblemException.Kind.PRECONDITION_FAILED,
                    "STALE_DRAFT",
                    "El borrador cambió; recargue antes de continuar.");
        }
    }

    public record CreateCommand(
            UUID processId,
            String title,
            String description,
            String provenance,
            LocalDate coveredFrom,
            LocalDate coveredTo,
            List<UUID> indicatorIds) {}

    public record UpdateCommand(
            String title,
            String description,
            String provenance,
            LocalDate coveredFrom,
            LocalDate coveredTo,
            List<UUID> indicatorIds) {}

    public record EvidenceDetails(Evidence evidence, EvidenceVersion latestVersion) {}
}
