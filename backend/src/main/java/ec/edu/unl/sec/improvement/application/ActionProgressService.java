package ec.edu.unl.sec.improvement.application;

import ec.edu.unl.sec.audit.application.AuditEventCommand;
import ec.edu.unl.sec.audit.application.AuditPort;
import ec.edu.unl.sec.improvement.domain.ActionDecision;
import ec.edu.unl.sec.improvement.domain.ActionProgress;
import ec.edu.unl.sec.improvement.domain.ImprovementAction;
import ec.edu.unl.sec.identityaccess.application.AuthorizationPort;
import ec.edu.unl.sec.identityaccess.application.CurrentActorPort;
import ec.edu.unl.sec.identityaccess.application.InstitutionalAuthorityPort;
import ec.edu.unl.sec.identityaccess.domain.AuthorizationScope;
import ec.edu.unl.sec.identityaccess.domain.OperationCode;
import ec.edu.unl.sec.selfassessment.application.DocumentaryProcessPort;
import ec.edu.unl.sec.shared.domain.DecisionProvenance;
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
 * P15 execution and verification. Approval, execution and verification are distinct capabilities;
 * progress is appended and never rewritten.
 */
@Service
public class ActionProgressService {
    public static final String CONTINUATION_POLICY = "IMPROVEMENT_CONTINUATION_V1";
    private static final String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";
    private static final Set<ImprovementAction.Transition> POST_CLOSURE_TRANSITIONS = Set.of(
            ImprovementAction.Transition.START, ImprovementAction.Transition.SUBMIT_VERIFICATION,
            ImprovementAction.Transition.VERIFY, ImprovementAction.Transition.REQUEST_CORRECTION);

    private final CurrentActorPort actors;
    private final AuthorizationPort auth;
    private final DocumentaryProcessPort processes;
    private final ImprovementRepository repository;
    private final InstitutionalAuthorityPort authority;
    private final AuditPort audit;
    private final DomainClock clock = DomainClock.systemUtc();

    public ActionProgressService(CurrentActorPort actors, AuthorizationPort auth, DocumentaryProcessPort processes,
            ImprovementRepository repository, InstitutionalAuthorityPort authority, AuditPort audit) {
        this.actors = Objects.requireNonNull(actors, "actors");
        this.auth = Objects.requireNonNull(auth, "auth");
        this.processes = Objects.requireNonNull(processes, "processes");
        this.repository = Objects.requireNonNull(repository, "repository");
        this.authority = Objects.requireNonNull(authority, "authority");
        this.audit = Objects.requireNonNull(audit, "audit");
    }

    public record ProgressCommand(String description, String achievedValue, List<UUID> evidenceVersionIds,
            Instant recordedAt) {}

    public record DecisionCommand(ImprovementAction.Transition action, Provenance provenance) {}

    public record Provenance(UUID authorityAssignmentId, String legalBasis, String documentReference, String reason) {}

    @Transactional
    public ActionProgress recordProgress(UUID actionId, long expectedRevision, ProgressCommand command,
            UUID correlationId) {
        var actor = actor(OperationCode.IMPROVEMENT_PROGRESS);
        var head = head(actionId);
        var context = lockScope(actor, OperationCode.IMPROVEMENT_PROGRESS, head.processId());
        if (head.revision() != expectedRevision)
            throw new ConcurrencyFailureException("stale improvement action: revision mismatch");
        if (head.state() == ImprovementAction.State.VERIFIED)
            throw problem(ImprovementProblemException.Kind.CONFLICT, "IMPROVEMENT_ACTION_CLOSED",
                    "Una acción verificada no admite nuevos avances.");
        if ("CLOSED".equals(context.state()) && head.state() != ImprovementAction.State.APPROVED
                && head.state() != ImprovementAction.State.IN_PROGRESS
                && head.state() != ImprovementAction.State.PENDING_VERIFICATION)
            throw problem(ImprovementProblemException.Kind.CONFLICT, "IMPROVEMENT_PROCESS_CLOSED",
                    "La política solo permite continuar acciones aprobadas después del cierre.");
        ActionProgress progress;
        try {
            progress = new ActionProgress(UUID.randomUUID(), head.id(),
                    new ActionProgress.Data(command.description(), command.achievedValue(),
                            command.evidenceVersionIds(), command.recordedAt()), actor, clock.now());
        } catch (IllegalArgumentException invalid) {
            throw problem(ImprovementProblemException.Kind.INVALID, "IMPROVEMENT_PROGRESS_INVALID",
                    invalid.getMessage());
        }
        repository.appendProgress(progress);
        advanceHead(head, head.state(), actor, expectedRevision);
        audit.record(new AuditEventCommand(clock.now(), actor, null, correlationId, "IMPROVEMENT_PROGRESS_RECORDED",
                "ImprovementAction", head.id(), context.careerId(), head.processId(), "SUCCESS", null,
                Map.<String, Object>of("progressId", progress.id().toString(), "policy", CONTINUATION_POLICY)));
        return progress;
    }

    @Transactional
    public ActionDecision decide(UUID actionId, long expectedRevision, DecisionCommand command, UUID correlationId) {
        var operation = capability(command.action());
        var actor = actor(operation);
        var head = head(actionId);
        var context = lockScope(actor, operation, head.processId());
        var scope = new AuthorizationScope(context.careerId(), head.processId(), null);
        if (head.revision() != expectedRevision)
            throw new ConcurrencyFailureException("stale improvement action: revision mismatch");
        if ("CLOSED".equals(context.state())) {
            if (!POST_CLOSURE_TRANSITIONS.contains(command.action())
                    || head.state() == ImprovementAction.State.PROPOSED
                    || head.state() == ImprovementAction.State.VERIFIED)
                throw problem(ImprovementProblemException.Kind.CONFLICT, "IMPROVEMENT_PROCESS_CLOSED",
                        "La política solo permite continuar acciones aprobadas después del cierre.");
        }
        ImprovementAction.State next;
        try {
            next = head.advance(command.action());
        } catch (IllegalStateException invalid) {
            throw problem(ImprovementProblemException.Kind.CONFLICT, "IMPROVEMENT_TRANSITION_INVALID",
                    "La transición indicada no procede desde el estado actual de la acción.");
        }
        if (!authority.isAuthorizedAssignment(command.provenance().authorityAssignmentId(), actor, operation, scope))
            throw problem(ImprovementProblemException.Kind.FORBIDDEN, "IMPROVEMENT_AUTHORITY_DENIED",
                    "La asignación indicada no detenta autoridad competente para esta decisión.");
        if ((command.action() == ImprovementAction.Transition.SUBMIT_VERIFICATION
                || command.action() == ImprovementAction.Transition.VERIFY)
                && repository.progressFor(head.id()).isEmpty())
            throw problem(ImprovementProblemException.Kind.CONFLICT, "IMPROVEMENT_PROGRESS_REQUIRED",
                    "La verificación requiere al menos un avance sustentado registrado.");
        DecisionProvenance provenance;
        try {
            provenance = new DecisionProvenance(actor, command.provenance().authorityAssignmentId(),
                    command.provenance().legalBasis(), command.provenance().documentReference(),
                    command.provenance().reason(), clock.now());
        } catch (IllegalArgumentException invalid) {
            throw problem(ImprovementProblemException.Kind.INVALID, "IMPROVEMENT_DECISION_INVALID",
                    invalid.getMessage());
        }
        var decision = new ActionDecision(UUID.randomUUID(), head.id(), command.action(), provenance);
        repository.appendDecision(decision);
        advanceHead(head, next, actor, expectedRevision);
        audit.record(new AuditEventCommand(clock.now(), actor, null, correlationId, "IMPROVEMENT_DECISION_RECORDED",
                "ImprovementAction", head.id(), context.careerId(), head.processId(), "SUCCESS", null,
                Map.<String, Object>of("action", command.action().name(), "state", next.name())));
        return decision;
    }

    private void advanceHead(ImprovementAction head, ImprovementAction.State next, UUID actor, long expectedRevision) {
        var advanced = new ImprovementAction(head.id(), head.processId(), head.data(), next, head.createdBy(),
                head.createdAt(), expectedRevision + 1);
        if (!repository.replaceHead(advanced, expectedRevision))
            throw new ConcurrencyFailureException("stale improvement action: revision mismatch");
    }

    private ImprovementAction head(UUID actionId) {
        return repository.findAction(actionId)
                .orElseThrow(() -> problem(ImprovementProblemException.Kind.NOT_FOUND, RESOURCE_NOT_FOUND,
                        "La acción de mejora indicada no existe."));
    }

    private OperationCode capability(ImprovementAction.Transition transition) {
        return switch (transition) {
            case APPROVE, RETURN, REQUEST_CORRECTION -> OperationCode.IMPROVEMENT_DECIDE;
            case START, SUBMIT_VERIFICATION -> OperationCode.IMPROVEMENT_ACTION_EXECUTE;
            case VERIFY -> OperationCode.IMPROVEMENT_ACTION_VERIFY;
        };
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

    private DocumentaryProcessPort.Context lockScope(UUID actor, OperationCode operation, UUID processId) {
        var context = processes.lock(processId)
                .orElseThrow(() -> problem(ImprovementProblemException.Kind.NOT_FOUND, RESOURCE_NOT_FOUND,
                        "El proceso indicado no existe."));
        if (!auth.isAllowed(actor, operation, new AuthorizationScope(context.careerId(), context.id(), null)))
            throw problem(ImprovementProblemException.Kind.NOT_FOUND, RESOURCE_NOT_FOUND,
                    "El recurso solicitado no existe.");
        return context;
    }

    private ImprovementProblemException problem(ImprovementProblemException.Kind kind, String code, String message) {
        return new ImprovementProblemException(kind, code, message);
    }
}
