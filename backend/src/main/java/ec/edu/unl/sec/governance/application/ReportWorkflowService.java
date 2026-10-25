package ec.edu.unl.sec.governance.application;

import ec.edu.unl.sec.audit.application.AuditEventCommand;
import ec.edu.unl.sec.audit.application.AuditPort;
import ec.edu.unl.sec.governance.domain.ApprovalDecision;
import ec.edu.unl.sec.governance.domain.ObservationReply;
import ec.edu.unl.sec.governance.domain.ReportObservation;
import ec.edu.unl.sec.governance.domain.ReportSocialization;
import ec.edu.unl.sec.governance.domain.ReportVersion;
import ec.edu.unl.sec.identityaccess.application.AuthorizationPort;
import ec.edu.unl.sec.identityaccess.application.CurrentActorPort;
import ec.edu.unl.sec.identityaccess.application.InstitutionalAuthorityPort;
import ec.edu.unl.sec.identityaccess.domain.AuthorizationScope;
import ec.edu.unl.sec.identityaccess.domain.OperationCode;
import ec.edu.unl.sec.academicevaluation.application.AcademicConsolidationPort;
import ec.edu.unl.sec.selfassessment.application.DocumentaryProcessPort;
import ec.edu.unl.sec.shared.domain.DecisionProvenance;
import ec.edu.unl.sec.shared.domain.DomainClock;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * P14 report workflow: immutable emission, observation cycle and authority decision. Emitted
 * bytes never change; only revision-guarded processing state advances.
 */
@Service
public class ReportWorkflowService {
    private static final Set<String> CLOSED_PHASES = Set.of("PREPARATION", "SUSPENDED", "CLOSING", "CLOSED");
    private static final String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";

    private final CurrentActorPort actors;
    private final AuthorizationPort auth;
    private final DocumentaryProcessPort processes;
    private final AcademicConsolidationPort academic;
    private final FindingRepository findings;
    private final ReportRepository reports;
    private final InstitutionalAuthorityPort authority;
    private final AuditPort audit;
    private final DomainClock clock;

    public static final String DISCLAIMER =
            "Este informe apoya la autoevaluación y no constituye acreditación.";

    public ReportWorkflowService(CurrentActorPort actors, AuthorizationPort auth, DocumentaryProcessPort processes,
            AcademicConsolidationPort academic, FindingRepository findings, ReportRepository reports,
            InstitutionalAuthorityPort authority, AuditPort audit) {
        this.actors = Objects.requireNonNull(actors, "actors");
        this.auth = Objects.requireNonNull(auth, "auth");
        this.processes = Objects.requireNonNull(processes, "processes");
        this.academic = Objects.requireNonNull(academic, "academic");
        this.findings = Objects.requireNonNull(findings, "findings");
        this.reports = Objects.requireNonNull(reports, "reports");
        this.authority = Objects.requireNonNull(authority, "authority");
        this.audit = Objects.requireNonNull(audit, "audit");
        this.clock = DomainClock.systemUtc();
    }

    public record ReportCommand(String kind, UUID consolidationId, String templateReference, boolean templateValidated,
            String content, UUID predecessorVersionId) {}

    public record SocializationCommand(Instant occurredAt, List<UUID> participantAssignmentIds, String recordReference) {}

    public record ObservationCommand(String sectionReference, String text) {}

    public record ReplyCommand(String response, UUID correctedVersionId) {}

    public record Provenance(UUID authorityAssignmentId, String legalBasis, String documentReference, String reason) {}

    public record ApprovalDecisionCommand(String decision, Provenance provenance) {}

    public record ReportVersionDetail(ReportVersion version, long revision, String state,
            List<ReportSocialization> socializations, List<ReportObservation> observations,
            List<ObservationReply> replies, List<ApprovalDecision> decisions, String disclaimer) {
        public String contentSha256() { return version.contentSha256(); }
    }

    public record VersionPage(List<ReportVersion> items, long totalElements, long revision) {}

    @Transactional
    public ReportVersion create(UUID processId, long expectedRevision, ReportCommand command, UUID correlationId) {
        var actor = requireWriter(OperationCode.REPORT_WRITE);
        var context = writeScope(actor, OperationCode.REPORT_WRITE, processId);
        var consolidation = findings.latestConsolidation(processId)
                .orElseThrow(() -> problem(GovernanceProblemException.Kind.INVALID, "REPORT_CONSOLIDATION_NOT_FOUND",
                        "No existe una consolidación registrada sobre la cual emitir informe."));
        var kind = parseKind(command.kind());
        if (reports.hasApprovedFinal(processId))
            throw problem(GovernanceProblemException.Kind.CONFLICT, "REPORT_ALREADY_APPROVED",
                    "El informe final ya fue aprobado y la cadena permanece cerrada.");
        UUID predecessorId = null;
        var latest = reports.latest(processId);
        if (kind == ReportVersion.Kind.PRELIMINARY) {
            if (latest.isPresent())
                throw problem(GovernanceProblemException.Kind.CONFLICT, "REPORT_PRELIMINARY_STATIC",
                        "El preliminar ya fue emitido; las correcciones se registran como versiones finales.");
            if (expectedRevision != 0) throw new ConcurrencyFailureException("stale preliminary emission");
        } else {
            if (command.predecessorVersionId() == null)
                throw problem(GovernanceProblemException.Kind.INVALID, "REPORT_PREDECESSOR_REQUIRED",
                        "La versión final requiere encadenar una versión predecesora.");
            var predecessor = reports.findVersion(command.predecessorVersionId())
                    .filter(v -> v.processId().equals(processId))
                    .orElseThrow(() -> problem(GovernanceProblemException.Kind.NOT_FOUND, "REPORT_VERSION_NOT_FOUND",
                            "La versión predecesora indicada no existe."));
            if (!reports.advanceProcessing(predecessor.id(), expectedRevision, null))
                throw new ConcurrencyFailureException("stale emission: revision mismatch");
            predecessorId = predecessor.id();
        }
        ReportCommand used = command;
        var version = new ReportVersion(UUID.randomUUID(), processId, new ReportVersion.Data(kind, consolidation.id(),
                used.templateReference(), used.templateValidated(), used.content(), predecessorId),
                actor, clock.now(), predecessorId, latest.map(v -> v.versionNumber() + 1).orElse(1L), 1L,
                sha256(used.content()));
        reports.appendVersion(version);
        audit.record(new AuditEventCommand(clock.now(), actor, null, correlationId, "REPORT_VERSION_EMITTED",
                "ReportVersion", version.id(), context.careerId(), processId, "SUCCESS", null,
                Map.<String, Object>of("kind", kind.name(), "versionNumber", version.versionNumber())));
        return version;
    }

    @Transactional
    public ReportObservation observe(UUID versionId, long expectedRevision, ObservationCommand command, UUID correlationId) {
        var actor = requireWriter(OperationCode.REPORT_OBSERVE);
        var version = loadVersion(versionId);
        var context = writeScope(actor, OperationCode.REPORT_OBSERVE, version.processId());
        if (!reports.advanceProcessing(versionId, expectedRevision, null))
            throw new ConcurrencyFailureException("stale observation: revision mismatch");
        var observation = new ReportObservation(UUID.randomUUID(), versionId, command.sectionReference(),
                command.text(), actor, clock.now());
        reports.appendObservation(observation);
        audit.record(new AuditEventCommand(clock.now(), actor, null, correlationId, "REPORT_OBSERVATION_RECORDED",
                "ReportObservation", observation.id(), context.careerId(), version.processId(), "SUCCESS", null, Map.of()));
        return observation;
    }

    @Transactional
    public ObservationReply reply(UUID observationId, long expectedRevision, ReplyCommand command, UUID correlationId) {
        var actor = requireWriter(OperationCode.REPORT_WRITE);
        var observation = reports.findObservation(observationId)
                .orElseThrow(() -> problem(GovernanceProblemException.Kind.NOT_FOUND, "REPORT_OBSERVATION_NOT_FOUND",
                        "La observación indicada no existe."));
        if (reports.repliesForVersion(observation.versionId()).stream()
                .anyMatch(r -> r.observationId().equals(observationId)))
            throw problem(GovernanceProblemException.Kind.CONFLICT, "REPORT_OBSERVATION_ALREADY_ANSWERED",
                    "La observación ya cuenta con una respuesta registrada.");
        var version = loadVersion(observation.versionId());
        var context = writeScope(actor, OperationCode.REPORT_WRITE, version.processId());
        if (command.correctedVersionId() != null) {
            var correction = reports.findVersion(command.correctedVersionId())
                    .filter(v -> v.processId().equals(version.processId()))
                    .filter(v -> !v.createdAt().isBefore(observation.createdAt()));
            if (correction.isEmpty())
                throw problem(GovernanceProblemException.Kind.INVALID, "REPORT_INVALID_CORRECTION_LINK",
                        "La versión de corrección debe existir y derivar de una observación previa.");
        }
        if (!reports.advanceProcessing(observation.versionId(), expectedRevision, null))
            throw new ConcurrencyFailureException("stale reply: revision mismatch");
        var reply = new ObservationReply(UUID.randomUUID(), observationId, command.response(),
                command.correctedVersionId(), actor, clock.now());
        reports.appendReply(reply);
        audit.record(new AuditEventCommand(clock.now(), actor, null, correlationId, "REPORT_OBSERVATION_REPLIED",
                "ObservationReply", reply.id(), context.careerId(), version.processId(), "SUCCESS", null, Map.of()));
        return reply;
    }

    @Transactional
    public ReportSocialization socialize(UUID versionId, long expectedRevision, SocializationCommand command,
            UUID correlationId) {
        var actor = requireWriter(OperationCode.REPORT_SOCIALIZE);
        var version = loadVersion(versionId);
        var context = writeScope(actor, OperationCode.REPORT_SOCIALIZE, version.processId());
        if (!reports.advanceProcessing(versionId, expectedRevision, null))
            throw new ConcurrencyFailureException("stale socialization: revision mismatch");
        var socialization = new ReportSocialization(UUID.randomUUID(), versionId, command.occurredAt(),
                command.participantAssignmentIds(), command.recordReference(), actor, clock.now());
        reports.appendSocialization(socialization);
        audit.record(new AuditEventCommand(clock.now(), actor, null, correlationId, "REPORT_SOCIALIZED",
                "ReportSocialization", socialization.id(), context.careerId(), version.processId(), "SUCCESS", null, Map.of()));
        return socialization;
    }

    @Transactional
    public ApprovalDecision decide(UUID versionId, long expectedRevision, ApprovalDecisionCommand command,
            UUID correlationId) {
        var actor = requireWriter(OperationCode.REPORT_APPROVE);
        var decision = parseDecision(command.decision());
        var version = loadVersion(versionId);
        var context = writeScope(actor, OperationCode.REPORT_APPROVE, version.processId());
        var processing = reports.processing(versionId)
                .orElseThrow(() -> problem(GovernanceProblemException.Kind.NOT_FOUND, RESOURCE_NOT_FOUND,
                        "El estado de procesamiento de la versión no está disponible."));
        if (!reports.decisions(versionId).isEmpty())
            throw problem(GovernanceProblemException.Kind.CONFLICT, "REPORT_DECISION_ALREADY_RECORDED",
                    "La versión ya registra una decisión y la cadena es inmutable.");
        if (!"FINAL".equals(processing.state()))
            throw problem(GovernanceProblemException.Kind.CONFLICT, "REPORT_NOT_FINAL",
                    "Solo una versión FINAL admite decisión institucional de aprobación.");
        if (!authority.isAuthorizedAssignment(command.provenance().authorityAssignmentId(), actor,
                OperationCode.REPORT_APPROVE, new AuthorizationScope(context.careerId(), version.processId(), null)))
            throw problem(GovernanceProblemException.Kind.FORBIDDEN, "REPORT_AUTHORITY_DENIED",
                    "La asignación indicada no detenta autoridad competente sobre esta versión.");
        if (!reports.advanceProcessing(versionId, expectedRevision, decision.name()))
            throw new ConcurrencyFailureException("stale decision: revision mismatch");
        var decisionRecord = new ApprovalDecision(UUID.randomUUID(), versionId, decision,
                new DecisionProvenance(actor, command.provenance().authorityAssignmentId(),
                        command.provenance().legalBasis(), command.provenance().documentReference(),
                        command.provenance().reason(), clock.now()));
        reports.appendDecision(decisionRecord);
        audit.record(new AuditEventCommand(clock.now(), actor, null, correlationId, "REPORT_DECISION_RECORDED",
                "ApprovalDecision", decisionRecord.id(), context.careerId(), version.processId(), "SUCCESS", null,
                Map.<String, Object>of("decision", decisionRecord.decision().name())));
        return decisionRecord;
    }

    public ReportVersionDetail viewVersion(UUID versionId) {
        return reports.findVersion(versionId).map(v -> viewVersion(v.processId(), versionId))
                .orElseThrow(() -> problem(GovernanceProblemException.Kind.NOT_FOUND, "REPORT_VERSION_NOT_FOUND",
                        "La versión indicada no existe."));
    }

    public ReportVersionDetail versionDetail(UUID processId, UUID versionId) {
        return viewVersion(processId, versionId);
    }

    public VersionPage listVersions(UUID processId, long offset, long limit) {
        var actor = requireWriter(OperationCode.REPORT_READ);
        readScope(actor, OperationCode.REPORT_READ, processId);
        if (limit < 1 || limit > 100 || offset < 0)
            throw problem(GovernanceProblemException.Kind.INVALID, "REPORT_PAGE_LIMIT_EXCEEDED",
                    "La paginación de versiones admite hasta 100 elementos.");
        var items = reports.versions(processId, offset, limit);
        var revision = items.stream().map(v -> reports.processing(v.id())).flatMap(Optional::stream)
                .map(ReportRepository.Processing::revision).mapToLong(Long::longValue).max().orElse(0L);
        return new VersionPage(items, reports.totalVersions(processId), revision);
    }

    public long processingRevision(UUID versionId) {
        return reports.processing(versionId).map(ReportRepository.Processing::revision).orElse(0L);
    }

    public ReportVersionDetail viewVersion(UUID processId, UUID versionId) {
        var actor = requireWriter(OperationCode.REPORT_READ);
        readScope(actor, OperationCode.REPORT_READ, processId);
        var version = reports.findVersion(versionId).filter(v -> v.processId().equals(processId))
                .orElseThrow(() -> problem(GovernanceProblemException.Kind.NOT_FOUND, "REPORT_VERSION_NOT_FOUND",
                        "La versión indicada no existe en este proceso."));
        var processing = reports.processing(versionId)
                .orElseThrow(() -> problem(GovernanceProblemException.Kind.NOT_FOUND, RESOURCE_NOT_FOUND,
                        "El estado de procesamiento de la versión no está disponible."));
        var observations = reports.observations(versionId);
        var replies = reports.repliesForVersion(versionId);
        var decisions = reports.decisions(versionId);
        var state = stateFor(version, processing, observations, replies, decisions);
        return new ReportVersionDetail(version, processing.revision(), state, reports.socializations(versionId),
                observations, replies, decisions, DISCLAIMER);
    }

    private String stateFor(ReportVersion version, ReportRepository.Processing processing,
            List<ReportObservation> observations, List<ObservationReply> replies, List<ApprovalDecision> decisions) {
        if (!decisions.isEmpty())
            return "APPROVED".equals(decisions.get(decisions.size() - 1).decision().name()) ? "APPROVED" : "RETURNED";
        var answered = replies.stream().map(ObservationReply::observationId).collect(java.util.stream.Collectors.toSet());
        if (observations.stream().anyMatch(o -> !answered.contains(o.id()))) return "UNDER_OBSERVATION";
        return processing.state();
    }

    private ReportVersion loadVersion(UUID versionId) {
        return reports.findVersion(versionId)
                .orElseThrow(() -> problem(GovernanceProblemException.Kind.NOT_FOUND, "REPORT_VERSION_NOT_FOUND",
                        "La versión indicada no existe."));
    }

    private UUID requireWriter(OperationCode operation) {
        var actorRecord = actors.currentActor()
                .filter(a -> !a.technical())
                .orElseThrow(() -> problem(GovernanceProblemException.Kind.FORBIDDEN, "REPORT_ACTOR_REQUIRED",
                        "Solo sesiones de usuario activas pueden operar el ciclo de informe."));
        if (!auth.hasOperation(actorRecord.identityId(), operation)) throw hiddenAccess();
        return actorRecord.identityId();
    }

    private DocumentaryProcessPort.Context writeScope(UUID actor, OperationCode operation, UUID processId) {
        var context = processes.lock(processId)
                .orElseThrow(() -> problem(GovernanceProblemException.Kind.NOT_FOUND, RESOURCE_NOT_FOUND,
                        "El proceso indicado no existe."));
        if (!auth.isAllowed(actor, operation, new AuthorizationScope(context.careerId(), context.id(), null)))
            throw hiddenAccess();
        if (CLOSED_PHASES.contains(context.state()))
            throw problem(GovernanceProblemException.Kind.CONFLICT, "REPORT_PHASE_CLOSED",
                    "La fase del proceso no admite modificaciones al informe.");
        return context;
    }

    private DocumentaryProcessPort.Context readScope(UUID actor, OperationCode operation, UUID processId) {
        if (!auth.hasOperation(actor, operation)) throw hiddenAccess();
        var context = processes.context(processId)
                .orElseThrow(() -> problem(GovernanceProblemException.Kind.NOT_FOUND, RESOURCE_NOT_FOUND,
                        "El proceso indicado no existe."));
        if (!auth.isAllowed(actor, operation, new AuthorizationScope(context.careerId(), context.id(), null)))
            throw hiddenAccess();
        return context;
    }

    private GovernanceProblemException hiddenAccess() {
        return problem(GovernanceProblemException.Kind.NOT_FOUND, RESOURCE_NOT_FOUND,
                "El recurso solicitado no existe.");
    }

    private GovernanceProblemException problem(GovernanceProblemException.Kind kind, String code, String message) {
        return new GovernanceProblemException(kind, code, message);
    }

    private ReportVersion.Kind parseKind(String value) {
        try {
            return ReportVersion.Kind.valueOf(value);
        } catch (RuntimeException e) {
            throw problem(GovernanceProblemException.Kind.INVALID, "REPORT_KIND_UNSUPPORTED",
                    "La clase de versión indicada no es emitible.");
        }
    }

    private static final Set<String> SUPPORTED_DECISIONS = Set.of("APPROVED", "RETURNED");

    private ApprovalDecision.Decision parseDecision(String value) {
        if (value == null || !SUPPORTED_DECISIONS.contains(value))
            throw problem(GovernanceProblemException.Kind.INVALID, "REPORT_DECISION_UNSUPPORTED",
                    "La decisión indicada no es compatible con el flujo institucional.");
        return ApprovalDecision.Decision.valueOf(value);
    }

    private static String sha256(String content) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("sha256 unavailable", e);
        }
    }
}
