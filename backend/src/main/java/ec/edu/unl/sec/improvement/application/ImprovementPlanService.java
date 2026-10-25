package ec.edu.unl.sec.improvement.application;

import ec.edu.unl.sec.audit.application.AuditEventCommand;
import ec.edu.unl.sec.audit.application.AuditPort;
import ec.edu.unl.sec.improvement.domain.ActionDecision;
import ec.edu.unl.sec.improvement.domain.ActionProgress;
import ec.edu.unl.sec.improvement.domain.ImprovementAction;
import ec.edu.unl.sec.improvement.domain.ImprovementPlan;
import ec.edu.unl.sec.identityaccess.application.AuthorizationPort;
import ec.edu.unl.sec.identityaccess.application.CurrentActorPort;
import ec.edu.unl.sec.identityaccess.domain.AuthorizationScope;
import ec.edu.unl.sec.identityaccess.domain.OperationCode;
import ec.edu.unl.sec.selfassessment.application.DocumentaryProcessPort;
import ec.edu.unl.sec.shared.domain.DomainClock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * P15 plans and actions: a plan is created from an issued report version, actions keep a revision
 * chain and the measurable commitments required by the specification.
 */
@Service
public class ImprovementPlanService {
    private static final Set<String> CLOSED_PHASES =
            Set.of("PREPARATION", "SUSPENDED", "CLOSING", "CLOSED");
    private static final Set<String> PLANNABLE_REPORT_STATES = Set.of("FINAL", "APPROVED");
    private static final String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";

    private final CurrentActorPort actors;
    private final AuthorizationPort auth;
    private final DocumentaryProcessPort processes;
    private final ImprovementRepository repository;
    private final AuditPort audit;
    private final DomainClock clock = DomainClock.systemUtc();

    public ImprovementPlanService(CurrentActorPort actors, AuthorizationPort auth,
            DocumentaryProcessPort processes, ImprovementRepository repository, AuditPort audit) {
        this.actors = Objects.requireNonNull(actors, "actors");
        this.auth = Objects.requireNonNull(auth, "auth");
        this.processes = Objects.requireNonNull(processes, "processes");
        this.repository = Objects.requireNonNull(repository, "repository");
        this.audit = Objects.requireNonNull(audit, "audit");
    }

    public record PlanCommand(String title, UUID reportVersionId) {}

    public record ActionCommand(UUID planId, List<UUID> findingIds, String title, String description,
            UUID responsibleAssignmentId, Instant dueAt, String target, String verificationMethod) {}

    public record ActionPage(List<ActionDetail> items, long totalElements) {}

    public record ActionDetail(ImprovementAction action, boolean overdue, List<ActionProgress> progress,
            List<ActionDecision> decisions) {}

    @Transactional
    public ImprovementPlan create(UUID processId, long expectedRevision, PlanCommand command, UUID correlationId) {
        var actor = actor(OperationCode.IMPROVEMENT_WRITE);
        var context = writeScope(actor, OperationCode.IMPROVEMENT_WRITE, processId);
        var current = repository.planTotal(processId);
        if (current != expectedRevision)
            throw new ConcurrencyFailureException("stale improvement plans: revision mismatch");
        if (command.reportVersionId() == null)
            throw problem(ImprovementProblemException.Kind.INVALID, "IMPROVEMENT_PLAN_INVALID",
                    "El plan de mejora requiere una versión de informe emitida.");
        var state = repository.reportVersionState(processId, command.reportVersionId())
                .orElseThrow(() -> problem(ImprovementProblemException.Kind.NOT_FOUND, RESOURCE_NOT_FOUND,
                        "La versión de informe indicada no existe en este proceso."));
        if (!PLANNABLE_REPORT_STATES.contains(state))
            throw problem(ImprovementProblemException.Kind.INVALID, "IMPROVEMENT_REPORT_NOT_FINAL",
                    "El plan de mejora requiere una versión de informe final o aprobada.");
        ImprovementPlan plan;
        try {
            plan = new ImprovementPlan(UUID.randomUUID(), processId, command.reportVersionId(), command.title(),
                    actor, clock.now());
        } catch (IllegalArgumentException invalid) {
            throw problem(ImprovementProblemException.Kind.INVALID, "IMPROVEMENT_PLAN_INVALID",
                    invalid.getMessage());
        }
        repository.appendPlan(plan);
        audit.record(new AuditEventCommand(clock.now(), actor, null, correlationId, "IMPROVEMENT_PLAN_CREATED",
                "ImprovementPlan", plan.id(), context.careerId(), processId, "SUCCESS", null,
                Map.<String, Object>of("reportVersionId", plan.reportVersionId().toString())));
        return plan;
    }

    @Transactional
    public ImprovementAction createAction(UUID processId, ActionCommand command, UUID correlationId) {
        var actor = actor(OperationCode.IMPROVEMENT_WRITE);
        var context = writeScope(actor, OperationCode.IMPROVEMENT_WRITE, processId);
        var data = validate(command, processId, context);
        var action = new ImprovementAction(UUID.randomUUID(), processId, data, ImprovementAction.State.PROPOSED,
                actor, clock.now(), 1L);
        repository.appendAction(action, 0L);
        audit.record(new AuditEventCommand(clock.now(), actor, null, correlationId, "IMPROVEMENT_ACTION_PROPOSED",
                "ImprovementAction", action.id(), context.careerId(), processId, "SUCCESS", null,
                Map.<String, Object>of("planId", data.planId().toString(),
                        "findings", data.findingIds().size(), "dueAt", data.dueAt().toString())));
        return action;
    }

    @Transactional
    public ImprovementAction revise(UUID actionId, long expectedRevision, ActionCommand command, UUID correlationId) {
        var actor = actor(OperationCode.IMPROVEMENT_WRITE);
        var head = repository.findAction(actionId)
                .orElseThrow(() -> problem(ImprovementProblemException.Kind.NOT_FOUND, RESOURCE_NOT_FOUND,
                        "La acción de mejora indicada no existe."));
        var context = writeScope(actor, OperationCode.IMPROVEMENT_WRITE, head.processId());
        if (head.revision() != expectedRevision)
            throw new ConcurrencyFailureException("stale improvement action: revision mismatch");
        if (head.state() == ImprovementAction.State.VERIFIED)
            throw problem(ImprovementProblemException.Kind.CONFLICT, "IMPROVEMENT_ACTION_CLOSED",
                    "Una acción verificada no admite nuevas revisiones.");
        var data = validate(command, head.processId(), context);
        var revised = new ImprovementAction(head.id(), head.processId(), data, ImprovementAction.State.PROPOSED,
                head.createdBy(), head.createdAt(), head.revision() + 1);
        if (!repository.replaceHead(revised, expectedRevision))
            throw new ConcurrencyFailureException("stale improvement action: revision mismatch");
        audit.record(new AuditEventCommand(clock.now(), actor, null, correlationId, "IMPROVEMENT_ACTION_REVISED",
                "ImprovementAction", revised.id(), context.careerId(), head.processId(), "SUCCESS", null,
                Map.<String, Object>of("revision", revised.revision())));
        return revised;
    }

    @Transactional(readOnly = true)
    public ActionPage list(UUID processId, int page, int size) {
        var actor = actor(OperationCode.IMPROVEMENT_READ);
        readScope(actor, OperationCode.IMPROVEMENT_READ, processId);
        if (page < 0 || size < 1 || size > 100)
            throw problem(ImprovementProblemException.Kind.INVALID, "IMPROVEMENT_PAGE_LIMIT_EXCEEDED",
                    "La paginación de acciones admite hasta 100 elementos.");
        var now = clock.now();
        var items = repository.actions(processId, (long) page * size, size).stream()
                .map(action -> new ActionDetail(action, action.overdue(now), repository.progressFor(action.id()),
                        repository.decisionsFor(action.id())))
                .toList();
        return new ActionPage(items, repository.actionTotal(processId));
    }

    @Transactional(readOnly = true)
    public ActionDetail view(UUID actionId) {
        var actor = actor(OperationCode.IMPROVEMENT_READ);
        var head = repository.findAction(actionId)
                .orElseThrow(() -> problem(ImprovementProblemException.Kind.NOT_FOUND, RESOURCE_NOT_FOUND,
                        "La acción de mejora indicada no existe."));
        readScope(actor, OperationCode.IMPROVEMENT_READ, head.processId());
        return new ActionDetail(head, head.overdue(clock.now()), repository.progressFor(actionId),
                repository.decisionsFor(actionId));
    }

    private ImprovementAction.Data validate(ActionCommand command, UUID processId,
            DocumentaryProcessPort.Context context) {
        var plan = repository.findPlan(command.planId())
                .filter(candidate -> candidate.processId().equals(processId))
                .orElseThrow(() -> problem(ImprovementProblemException.Kind.NOT_FOUND, RESOURCE_NOT_FOUND,
                        "El plan de mejora indicado no existe en este proceso."));
        var requested = command.findingIds() == null ? List.<UUID>of() : command.findingIds();
        if (requested.isEmpty() || requested.stream().anyMatch(Objects::isNull)
                || requested.stream().distinct().count() != requested.size())
            throw problem(ImprovementProblemException.Kind.INVALID, "IMPROVEMENT_FINDING_REQUIRED",
                    "La acción requiere hallazgos distintos y registrados.");
        var found = repository.findings(processId, requested);
        if (!found.containsAll(requested))
            throw problem(ImprovementProblemException.Kind.NOT_FOUND, "IMPROVEMENT_FINDING_NOT_FOUND",
                    "Uno de los hallazgos indicados no existe en este proceso.");
        var now = clock.now();
        repository.activeAssignment(command.responsibleAssignmentId())
                .filter(assignment -> assignment.covers(context.careerId(), processId, now))
                .orElseThrow(() -> problem(ImprovementProblemException.Kind.INVALID,
                        "IMPROVEMENT_RESPONSIBLE_UNAVAILABLE",
                        "El responsable indicado no tiene una asignación vigente sobre este proceso."));
        try {
            return new ImprovementAction.Data(plan.id(), requested, command.title(), command.description(),
                    command.responsibleAssignmentId(), command.dueAt(), command.target(),
                    command.verificationMethod());
        } catch (IllegalArgumentException invalid) {
            throw problem(ImprovementProblemException.Kind.INVALID, "IMPROVEMENT_ACTION_INVALID",
                    invalid.getMessage());
        }
    }

    private UUID actor(OperationCode operation) {
        var actor = actors.currentActor()
                .filter(candidate -> !candidate.technical())
                .orElseThrow(() -> problem(ImprovementProblemException.Kind.FORBIDDEN,
                        "IMPROVEMENT_ACTOR_REQUIRED",
                        "Solo sesiones de usuario activas pueden operar la mejora continua."));
        if (!auth.hasOperation(actor.identityId(), operation))
            throw problem(ImprovementProblemException.Kind.FORBIDDEN, "IMPROVEMENT_CAPABILITY_REQUIRED",
                    "La operación requiere una capacidad de mejora vigente.");
        return actor.identityId();
    }

    private DocumentaryProcessPort.Context writeScope(UUID actor, OperationCode operation, UUID processId) {
        var context = processes.lock(processId)
                .orElseThrow(() -> problem(ImprovementProblemException.Kind.NOT_FOUND, RESOURCE_NOT_FOUND,
                        "El proceso indicado no existe."));
        if (!auth.isAllowed(actor, operation, new AuthorizationScope(context.careerId(), context.id(), null)))
            throw hiddenAccess();
        if (CLOSED_PHASES.contains(context.state()))
            throw problem(ImprovementProblemException.Kind.CONFLICT, "IMPROVEMENT_PROCESS_CLOSED",
                    "La fase del proceso no admite nuevos planes, acciones ni revisiones.");
        return context;
    }

    private DocumentaryProcessPort.Context readScope(UUID actor, OperationCode operation, UUID processId) {
        var context = processes.context(processId)
                .orElseThrow(() -> problem(ImprovementProblemException.Kind.NOT_FOUND, RESOURCE_NOT_FOUND,
                        "El proceso indicado no existe."));
        if (!auth.isAllowed(actor, operation, new AuthorizationScope(context.careerId(), context.id(), null)))
            throw hiddenAccess();
        return context;
    }

    private ImprovementProblemException hiddenAccess() {
        return problem(ImprovementProblemException.Kind.NOT_FOUND, RESOURCE_NOT_FOUND,
                "El recurso solicitado no existe.");
    }

    private ImprovementProblemException problem(ImprovementProblemException.Kind kind, String code, String message) {
        return new ImprovementProblemException(kind, code, message);
    }
}
