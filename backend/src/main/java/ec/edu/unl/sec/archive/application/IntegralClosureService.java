package ec.edu.unl.sec.archive.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.unl.sec.archive.domain.IntegralClosure;
import ec.edu.unl.sec.archive.domain.IntegralClosurePolicy;
import ec.edu.unl.sec.audit.application.AuditEventCommand;
import ec.edu.unl.sec.audit.application.AuditPort;
import ec.edu.unl.sec.identityaccess.application.AuthorizationPort;
import ec.edu.unl.sec.identityaccess.application.CurrentActorPort;
import ec.edu.unl.sec.identityaccess.application.InstitutionalAuthorityPort;
import ec.edu.unl.sec.identityaccess.domain.AuthorizationScope;
import ec.edu.unl.sec.identityaccess.domain.OperationCode;
import ec.edu.unl.sec.selfassessment.application.DocumentaryProcessPort;
import ec.edu.unl.sec.shared.domain.DomainClock;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** P16 integral closure: revalidates policy evidence, seals the snapshot and keeps the history. */
@Service
public class IntegralClosureService {
    private static final String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";
    private static final Set<String> EXEMPTIBLE_CODES = Set.of("OVERDUE_ACTION");

    private final CurrentActorPort actors;
    private final AuthorizationPort auth;
    private final InstitutionalAuthorityPort authority;
    private final DocumentaryProcessPort processes;
    private final ClosureEvidencePort evidence;
    private final IntegralClosureRepository repository;
    private final AuditPort audit;
    private final ObjectMapper json;
    private final DomainClock clock = DomainClock.systemUtc();

    public IntegralClosureService(CurrentActorPort actors, AuthorizationPort auth,
            InstitutionalAuthorityPort authority, DocumentaryProcessPort processes, ClosureEvidencePort evidence,
            IntegralClosureRepository repository, AuditPort audit, ObjectMapper json) {
        this.actors = Objects.requireNonNull(actors, "actors");
        this.auth = Objects.requireNonNull(auth, "auth");
        this.authority = Objects.requireNonNull(authority, "authority");
        this.processes = Objects.requireNonNull(processes, "processes");
        this.evidence = Objects.requireNonNull(evidence, "evidence");
        this.repository = Objects.requireNonNull(repository, "repository");
        this.audit = Objects.requireNonNull(audit, "audit");
        this.json = Objects.requireNonNull(json, "json");
    }

    public record Provenance(UUID authorityAssignmentId, String legalBasis, String documentReference,
            String reason) {}

    public record CloseCommand(String closurePolicyVersion, Provenance provenance,
            List<UUID> exceptionDecisionIds) {}

    public record Preview(UUID processId, long revision, String policyVersion, String closureType,
            List<String> blockers, List<String> warnings, boolean eligible) {}

    public record ClosureView(UUID id, UUID processId, String type, String policyVersion, String sha256,
            List<UUID> pendingIds, List<UUID> exceptionDecisionIds, UUID createdBy, Instant createdAt) {}

    public record ClosurePage(List<ClosureView> items, int page, int size, long totalElements) {}

    @Transactional(readOnly = true)
    public Preview preview(UUID processId, String closureType) {
        var actor = actor(OperationCode.PROCESS_READ);
        var context = readContext(actor, processId);
        if ("DOCUMENTARY_CLOSURE".equals(closureType)) {
            return documentaryPreview(processId, context);
        }
        if (!"INTEGRAL_CLOSURE".equals(closureType)) {
            throw problem(ArchiveProblemException.Kind.INVALID, "UNSUPPORTED_CLOSURE_TYPE",
                    "El tipo de cierre debe ser DOCUMENTARY_CLOSURE o INTEGRAL_CLOSURE.");
        }
        var policy = IntegralClosurePolicy.STRICT;
        var evaluation = evaluate(context, Set.of());
        return new Preview(processId, context.revision(), policy.code(), "INTEGRAL_CLOSURE",
                evaluation.blockers().stream().map(IntegralClosurePolicy.Blocker::message).toList(),
                evaluation.warnings(), evaluation.eligible());
    }

    private Preview documentaryPreview(UUID processId, DocumentaryProcessPort.Context context) {
        var blockers = new ArrayList<String>();
        if (!"ACTIVE".equals(context.state())) {
            blockers.add("El cierre documental se ejecuta sobre un proceso activo.");
        }
        if (!evidence.documentaryClosures(processId).isEmpty()) {
            blockers.add("El proceso ya conserva un cierre documental.");
        }
        return new Preview(processId, context.revision(), "DOCUMENTARY_STRICT_V1", "DOCUMENTARY_CLOSURE",
                blockers, List.of("La cobertura aceptada se revalida al ejecutar el cierre documental."),
                blockers.isEmpty());
    }

    @Transactional
    public IntegralClosure close(UUID processId, long expectedRevision, CloseCommand command, UUID correlationId) {
        var actor = actor(OperationCode.PROCESS_INTEGRAL_CLOSE);
        if (command == null || command.provenance() == null)
            throw problem(ArchiveProblemException.Kind.INVALID, "INTEGRAL_CLOSURE_INVALID",
                    "El cierre integral requiere procedencia institucional.");
        var policy = policy(command.closurePolicyVersion());
        var context = processes.lock(processId)
                .orElseThrow(() -> problem(ArchiveProblemException.Kind.NOT_FOUND, RESOURCE_NOT_FOUND,
                        "El proceso indicado no existe."));
        scope(actor, OperationCode.PROCESS_INTEGRAL_CLOSE, context);
        if (context.revision() != expectedRevision)
            throw new ConcurrencyFailureException("stale process revision");
        if ("CLOSED".equals(context.state()))
            throw problem(ArchiveProblemException.Kind.CONFLICT, "CLOSURE_ALREADY_EXISTS",
                    "El proceso ya se encuentra cerrado integralmente.");
        if (!"DOCUMENTARY_CLOSED".equals(context.state()))
            throw problem(ArchiveProblemException.Kind.CONFLICT, "CLOSURE_STATE_INVALID",
                    "El cierre integral requiere un proceso en cierre documental.");
        var exceptions = exceptions(processId, command.exceptionDecisionIds());
        var evaluation = evaluate(context, exceptions.isEmpty() ? Set.of() : EXEMPTIBLE_CODES);
        if (!evaluation.eligible()) {
            var message = evaluation.blockers().stream().map(IntegralClosurePolicy.Blocker::message)
                    .reduce((first, second) -> first + " " + second).orElse("El cierre integral no es elegible.");
            throw problem(ArchiveProblemException.Kind.CONFLICT, "CLOSURE_BLOCKED", message);
        }
        provenance(actor, context, command.provenance(), OperationCode.PROCESS_INTEGRAL_CLOSE);
        var documentary = evidence.documentaryClosures(processId).getFirst();
        var report = evidence.finalReport(processId).orElse(null);
        var now = clock.now();
        var payload = payload(context, documentary, report, evaluation, exceptions,
                command.provenance(), actor, now);
        String encoded;
        try {
            encoded = json.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize integral closure", exception);
        }
        var closure = new IntegralClosure(UUID.randomUUID(), processId, documentary.id(), policy.code(), encoded,
                sha256(encoded), evaluation.pendingIds(), exceptions, actor, now);
        repository.append(closure);
        processes.completeIntegralClosure(processId, expectedRevision, actor, command.provenance().reason(),
                        correlationId, command.provenance().authorityAssignmentId(),
                        command.provenance().legalBasis(), command.provenance().documentReference())
                .orElseThrow(() -> new ConcurrencyFailureException("stale process revision"));
        audit.record(new AuditEventCommand(now, actor, null, correlationId, "INTEGRAL_CLOSURE_CREATED",
                "INTEGRAL_CLOSURE", closure.id(), context.careerId(), processId, "SUCCESS", null,
                Map.of("policyVersion", closure.policyVersion(), "sha256", closure.sha256(),
                        "exceptions", closure.exceptionDecisionIds().size())));
        return closure;
    }

    @Transactional(readOnly = true)
    public ClosurePage history(UUID processId, int page, int size) {
        var actor = actor(OperationCode.ARCHIVE_READ);
        var context = readContext(actor, processId);
        if (page < 0 || size < 1 || size > 100)
            throw problem(ArchiveProblemException.Kind.INVALID, "INVALID_PAGE", "La paginación admite hasta 100 elementos.");
        var items = new ArrayList<ClosureView>();
        for (var documentary : evidence.documentaryClosures(processId)) {
            items.add(new ClosureView(documentary.id(), processId, "DOCUMENTARY_CLOSURE",
                    documentary.policyVersion(), documentary.sha256(), List.of(), List.of(),
                    documentary.closedBy(), documentary.closedAt()));
        }
        for (var integral : repository.closures(processId, 0, 1000)) {
            items.add(view(integral));
        }
        items.sort((left, right) -> right.createdAt().compareTo(left.createdAt()));
        long total = items.size();
        int from = Math.min(page * size, items.size());
        int to = Math.min(from + size, items.size());
        return new ClosurePage(List.copyOf(items.subList(from, to)), page, size, total);
    }

    private ClosureView view(IntegralClosure closure) {
        return new ClosureView(closure.id(), closure.processId(), "INTEGRAL_CLOSURE", closure.policyVersion(),
                closure.sha256(), closure.pendingIds(), closure.exceptionDecisionIds(), closure.closedBy(),
                closure.closedAt());
    }

    private IntegralClosurePolicy.Evaluation evaluate(DocumentaryProcessPort.Context context, Set<String> excepted) {
        var documentary = evidence.documentaryClosures(context.id()).stream().findFirst().orElse(null);
        var report = evidence.finalReport(context.id()).orElse(null);
        return IntegralClosurePolicy.STRICT.evaluate(new IntegralClosurePolicy.Evidence(context.state(),
                documentary == null ? null : documentary.id(), report == null ? null : report.state(),
                evidence.actions(context.id()), clock.now(), excepted));
    }

    private Map<String, Object> payload(DocumentaryProcessPort.Context context,
            ClosureEvidencePort.DocumentaryClosureEvidence documentary, ClosureEvidencePort.ReportEvidence report,
            IntegralClosurePolicy.Evaluation evaluation, List<UUID> exceptions,
            Provenance provenance, UUID actor, Instant closedAt) {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("closureType", "INTEGRAL_CLOSURE");
        payload.put("policyVersion", IntegralClosurePolicy.STRICT.code());
        payload.put("processId", context.id());
        payload.put("careerId", context.careerId());
        payload.put("modelVersionId", context.modelVersionId());
        payload.put("processRevision", context.revision());
        payload.put("documentaryClosure", Map.of("id", documentary.id(), "snapshotId", documentary.snapshotId(),
                "policyVersion", documentary.policyVersion(), "sha256", documentary.sha256(),
                "closedAt", documentary.closedAt()));
        payload.put("finalReport", report == null ? null : Map.of("versionId", report.versionId(),
                "versionNumber", report.versionNumber(), "state", report.state()));
        payload.put("pendingIds", evaluation.pendingIds().stream().map(UUID::toString).toList());
        payload.put("exceptionDecisionIds", exceptions.stream().map(UUID::toString).toList());
        payload.put("warnings", evaluation.warnings());
        payload.put("provenance", Map.of("authorityAssignmentId", provenance.authorityAssignmentId(),
                "legalBasis", provenance.legalBasis(), "documentReference", provenance.documentReference(),
                "reason", provenance.reason()));
        payload.put("closedBy", actor);
        payload.put("closedAt", closedAt);
        return payload;
    }

    private List<UUID> exceptions(UUID processId, List<UUID> requested) {
        if (requested == null || requested.isEmpty()) return List.of();
        var distinct = new LinkedHashSet<UUID>();
        for (var id : requested) {
            if (id == null || !distinct.add(id) || !evidence.authorizedDecision(processId, id))
                throw problem(ArchiveProblemException.Kind.INVALID, "INVALID_CLOSURE_EXCEPTION",
                        "Cada excepción del cierre debe referenciar una decisión autorizada del proceso.");
        }
        return List.copyOf(distinct);
    }

    private IntegralClosurePolicy policy(String version) {
        try {
            return IntegralClosurePolicy.of(version);
        } catch (IllegalArgumentException invalid) {
            throw problem(ArchiveProblemException.Kind.INVALID, "UNSUPPORTED_CLOSURE_POLICY",
                    "Solo se admite la política integral estricta versionada.");
        }
    }

    private void provenance(UUID actor, DocumentaryProcessPort.Context context, Provenance provenance,
            OperationCode operation) {
        if (provenance.authorityAssignmentId() == null)
            throw problem(ArchiveProblemException.Kind.FORBIDDEN, "ARCHIVE_AUTHORITY_DENIED",
                    "La decisión requiere una asignación autorizadora vigente, fundamento y constancia.");
        if (!authority.isAuthorizedAssignment(provenance.authorityAssignmentId(), actor, operation,
                new AuthorizationScope(context.careerId(), context.id(), null)))
            throw problem(ArchiveProblemException.Kind.FORBIDDEN, "ARCHIVE_AUTHORITY_DENIED",
                    "La asignación indicada no autoriza el cierre integral.");
        try {
            new ec.edu.unl.sec.selfassessment.domain.ProcessAuthorization(null,
                    provenance.authorityAssignmentId(), provenance.legalBasis(), provenance.documentReference(),
                    provenance.reason(), actor, clock.now());
        } catch (IllegalArgumentException invalid) {
            throw problem(ArchiveProblemException.Kind.INVALID, "INTEGRAL_CLOSURE_INVALID",
                    "La procedencia del cierre está incompleta.");
        }
    }

    private UUID actor(OperationCode operation) {
        var actor = actors.currentActor().filter(candidate -> !candidate.technical())
                .orElseThrow(() -> problem(ArchiveProblemException.Kind.FORBIDDEN, "ARCHIVE_ACTOR_REQUIRED",
                        "Solo sesiones de usuario activas pueden operar el cierre y archivo."));
        if (!auth.hasOperation(actor.identityId(), operation))
            throw problem(ArchiveProblemException.Kind.FORBIDDEN, "ARCHIVE_CAPABILITY_REQUIRED",
                    "La operación requiere una capacidad de cierre o archivo vigente.");
        return actor.identityId();
    }

    private DocumentaryProcessPort.Context readContext(UUID actor, UUID processId) {
        var context = processes.context(processId)
                .orElseThrow(() -> problem(ArchiveProblemException.Kind.NOT_FOUND, RESOURCE_NOT_FOUND,
                        "El proceso indicado no existe."));
        if (!auth.isAllowed(actor, OperationCode.PROCESS_READ,
                new AuthorizationScope(context.careerId(), context.id(), null))) throw hidden();
        return context;
    }

    private void scope(UUID actor, OperationCode operation, DocumentaryProcessPort.Context context) {
        if (!auth.isAllowed(actor, operation, new AuthorizationScope(context.careerId(), context.id(), null)))
            throw hidden();
    }

    private ArchiveProblemException hidden() {
        return problem(ArchiveProblemException.Kind.NOT_FOUND, RESOURCE_NOT_FOUND,
                "El recurso solicitado no existe.");
    }

    private ArchiveProblemException problem(ArchiveProblemException.Kind kind, String code, String message) {
        return new ArchiveProblemException(kind, code, message);
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
