package ec.edu.unl.sec.improvement.api;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import ec.edu.unl.sec.audit.application.AuditEventCommand;
import ec.edu.unl.sec.audit.application.FailureAuditPort;
import ec.edu.unl.sec.improvement.application.ActionProgressService;
import ec.edu.unl.sec.improvement.application.ImprovementPlanService;
import ec.edu.unl.sec.improvement.application.ImprovementProblemException;
import ec.edu.unl.sec.improvement.domain.ActionDecision;
import ec.edu.unl.sec.improvement.domain.ActionProgress;
import ec.edu.unl.sec.improvement.domain.ImprovementAction;
import ec.edu.unl.sec.improvement.domain.ImprovementPlan;
import ec.edu.unl.sec.identityaccess.application.CurrentActorPort;
import ec.edu.unl.sec.shared.api.EntityTagSupport;
import ec.edu.unl.sec.shared.domain.DecisionProvenance;
import ec.edu.unl.sec.shared.infrastructure.CorrelationFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
public class ImprovementController {
    private final ImprovementPlanService plans;
    private final ActionProgressService progress;
    private final FailureAuditPort failures;
    private final CurrentActorPort actors;

    public ImprovementController(ImprovementPlanService plans, ActionProgressService progress,
            FailureAuditPort failures, CurrentActorPort actors) {
        this.plans = plans;
        this.progress = progress;
        this.failures = failures;
        this.actors = actors;
    }

    @PostMapping("/processes/{processId}/improvement-plans")
    public ResponseEntity<PlanView> createPlan(@PathVariable UUID processId,
            @RequestHeader(name = "If-Match", required = false) String etag,
            @Valid @RequestBody PlanRequest command, HttpServletRequest request) {
        UUID correlation = correlation(request);
        long expected = EntityTagSupport.require(etag);
        ImprovementPlan plan;
        try {
            plan = plans.create(processId, expected, new ImprovementPlanService.PlanCommand(command.title(),
                    command.reportVersionId()), correlation);
        } catch (ImprovementProblemException rejected) {
            rejection("IMPROVEMENT_PLAN_REJECTED", processId, correlation, rejected.code());
            throw rejected;
        } catch (ConcurrencyFailureException stale) {
            rejection("IMPROVEMENT_PLAN_REJECTED", processId, correlation, "STALE_WORKFLOW");
            throw stale;
        }
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
                .eTag(EntityTagSupport.strong(expected + 1))
                .body(new PlanView(plan.id(), expected + 1, plan.createdAt(), plan.createdBy(),
                        new PlanPayload(plan.title(), plan.reportVersionId())));
    }

    @GetMapping("/processes/{processId}/improvement-actions")
    public ResponseEntity<ActionPage> list(@PathVariable UUID processId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        var result = plans.list(processId, page, size);
        var items = result.items().stream().map(this::view).toList();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .eTag(EntityTagSupport.strong(result.totalElements()))
                .body(new ActionPage(items, page, size, result.totalElements()));
    }

    @PostMapping("/processes/{processId}/improvement-actions")
    public ResponseEntity<ActionView> createAction(@PathVariable UUID processId,
            @RequestHeader(name = "If-Match", required = false) String etag,
            @Valid @RequestBody ActionRequest command, HttpServletRequest request) {
        UUID correlation = correlation(request);
        EntityTagSupport.require(etag);
        ImprovementAction action;
        try {
            action = plans.createAction(processId, action(command), correlation);
        } catch (ImprovementProblemException rejected) {
            rejection("IMPROVEMENT_ACTION_REJECTED", processId, correlation, rejected.code());
            throw rejected;
        } catch (ConcurrencyFailureException stale) {
            rejection("IMPROVEMENT_ACTION_REJECTED", processId, correlation, "STALE_WORKFLOW");
            throw stale;
        }
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
                .eTag(EntityTagSupport.strong(action.revision()))
                .body(view(new ImprovementPlanService.ActionDetail(action, action.overdue(Instant.now()), List.of(),
                        List.of())));
    }

    @GetMapping("/improvement-actions/{actionId}")
    public ResponseEntity<ActionView> view(@PathVariable UUID actionId) {
        var detail = plans.view(actionId);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .eTag(EntityTagSupport.strong(detail.action().revision())).body(view(detail));
    }

    @PatchMapping("/improvement-actions/{actionId}")
    public ResponseEntity<ActionView> revise(@PathVariable UUID actionId,
            @RequestHeader(name = "If-Match", required = false) String etag,
            @Valid @RequestBody ActionRequest command, HttpServletRequest request) {
        UUID correlation = correlation(request);
        long expected = EntityTagSupport.require(etag);
        ImprovementAction action;
        try {
            action = plans.revise(actionId, expected, action(command), correlation);
        } catch (ImprovementProblemException rejected) {
            rejection("IMPROVEMENT_ACTION_REJECTED", actionId, correlation, rejected.code());
            throw rejected;
        } catch (ConcurrencyFailureException stale) {
            rejection("IMPROVEMENT_ACTION_REJECTED", actionId, correlation, "STALE_WORKFLOW");
            throw stale;
        }
        var detail = plans.view(action.id());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .eTag(EntityTagSupport.strong(action.revision())).body(view(detail));
    }

    @PostMapping("/improvement-actions/{actionId}/progress")
    public ResponseEntity<ProgressView> recordProgress(@PathVariable UUID actionId,
            @RequestHeader(name = "If-Match", required = false) String etag,
            @Valid @RequestBody ProgressRequest command, HttpServletRequest request) {
        UUID correlation = correlation(request);
        long expected = EntityTagSupport.require(etag);
        ActionProgress recorded;
        try {
            recorded = progress.recordProgress(actionId, expected, new ActionProgressService.ProgressCommand(
                    command.description(), command.achievedValue(), command.evidenceVersionIds(),
                    command.recordedAt()), correlation);
        } catch (ImprovementProblemException rejected) {
            rejection("IMPROVEMENT_PROGRESS_REJECTED", actionId, correlation, rejected.code());
            throw rejected;
        } catch (ConcurrencyFailureException stale) {
            rejection("IMPROVEMENT_PROGRESS_REJECTED", actionId, correlation, "STALE_WORKFLOW");
            throw stale;
        }
        return eventResult(recorded.id(), recorded.actionId(), expected, recorded.recordedBy(), recorded.createdAt(),
                recorded.data());
    }

    @PostMapping("/improvement-actions/{actionId}/decisions")
    public ResponseEntity<DecisionView> decide(@PathVariable UUID actionId,
            @RequestHeader(name = "If-Match", required = false) String etag,
            @Valid @RequestBody DecisionRequest command, HttpServletRequest request) {
        UUID correlation = correlation(request);
        long expected = EntityTagSupport.require(etag);
        ActionDecision decision;
        try {
            decision = progress.decide(actionId, expected, new ActionProgressService.DecisionCommand(
                    transition(command.action()), new ActionProgressService.Provenance(
                            command.provenance().authorityAssignmentId(), command.provenance().legalBasis(),
                            command.provenance().documentReference(), command.provenance().reason())), correlation);
        } catch (ImprovementProblemException rejected) {
            rejection("IMPROVEMENT_DECISION_REJECTED", actionId, correlation, rejected.code());
            throw rejected;
        } catch (ConcurrencyFailureException stale) {
            rejection("IMPROVEMENT_DECISION_REJECTED", actionId, correlation, "STALE_WORKFLOW");
            throw stale;
        }
        DecisionProvenance provenance = decision.provenance();
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
                .eTag(EntityTagSupport.strong(expected + 1))
                .body(new DecisionView(decision.id(), decision.actionId(), expected + 1, provenance.decidedAt(),
                        provenance.decidedBy(), decision.action().name(), provenance));
    }

    private ResponseEntity<ProgressView> eventResult(UUID id, UUID actionId, long expected, UUID createdBy,
            Instant createdAt, ActionProgress.Data data) {
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
                .eTag(EntityTagSupport.strong(expected + 1))
                .body(new ProgressView(id, actionId, expected + 1, createdAt, createdBy, data));
    }

    private ActionView view(ImprovementPlanService.ActionDetail detail) {
        var action = detail.action();
        var data = action.data();
        return new ActionView(action.id(), action.revision(), action.createdAt(), action.createdBy(),
                new ActionPayload(data.planId(), data.findingIds(), data.title(), data.description(),
                        data.responsibleAssignmentId(), data.dueAt(), data.target(), data.verificationMethod()),
                action.state().name(), detail.overdue(), detail.progress(), detail.decisions());
    }

    private ImprovementPlanService.ActionCommand action(ActionRequest command) {
        return new ImprovementPlanService.ActionCommand(command.planId(), command.findingIds(), command.title(),
                command.description(), command.responsibleAssignmentId(), command.dueAt(), command.target(),
                command.verificationMethod());
    }

    private ImprovementAction.Transition transition(String value) {
        try {
            return ImprovementAction.Transition.valueOf(value);
        } catch (RuntimeException invalid) {
            throw new ImprovementProblemException(ImprovementProblemException.Kind.INVALID,
                    "IMPROVEMENT_TRANSITION_UNSUPPORTED", "La transición indicada no es admitida.");
        }
    }

    private void rejection(String action, UUID objectId, UUID correlation, String code) {
        actors.currentActor().ifPresent(actor -> failures.recordAfterRollback(new AuditEventCommand(
                Instant.now(), actor.technical() ? null : actor.identityId(),
                actor.technical() ? actor.identityId() : null, correlation, action, "ImprovementAction", objectId,
                null, null, "REJECTED", code, java.util.Map.of())));
    }

    private UUID correlation(HttpServletRequest request) {
        var raw = request.getAttribute(CorrelationFilter.CORRELATION_ATTRIBUTE);
        try {
            return UUID.fromString(String.valueOf(raw));
        } catch (IllegalArgumentException ex) {
            return UUID.randomUUID();
        }
    }

    public record PlanRequest(@NotBlank @Size(max = 300) String title, @NotNull UUID reportVersionId) {
        @JsonAnySetter public void unknown(String key, Object value) { throw new IllegalArgumentException("Campo no admitido"); }
    }

    public record ActionRequest(@NotNull UUID planId, @NotEmpty @Size(max = 500) List<UUID> findingIds,
            @NotBlank @Size(max = 300) String title, @NotBlank @Size(min = 10, max = 4000) String description,
            @NotNull UUID responsibleAssignmentId, @NotNull Instant dueAt,
            @NotBlank @Size(max = 4000) String target,
            @NotBlank @Size(max = 4000) String verificationMethod) {
        @JsonAnySetter public void unknown(String key, Object value) { throw new IllegalArgumentException("Campo no admitido"); }
    }

    public record ProgressRequest(@NotBlank @Size(min = 10, max = 4000) String description,
            @NotBlank @Size(max = 2000) String achievedValue,
            @NotEmpty @Size(max = 500) List<UUID> evidenceVersionIds, @NotNull Instant recordedAt) {
        @JsonAnySetter public void unknown(String key, Object value) { throw new IllegalArgumentException("Campo no admitido"); }
    }

    public record DecisionRequest(@NotBlank String action, @Valid Provenance provenance) {
        @JsonAnySetter public void unknown(String key, Object value) { throw new IllegalArgumentException("Campo no admitido"); }
        public record Provenance(@NotNull UUID authorityAssignmentId, @NotBlank @Size(max = 4000) String legalBasis,
                @NotBlank @Size(max = 2000) String documentReference,
                @NotBlank @Size(min = 10, max = 4000) String reason) {}
    }

    public record PlanPayload(String title, UUID reportVersionId) {}

    public record PlanView(UUID id, long revision, Instant createdAt, UUID createdBy, PlanPayload data) {}

    public record ActionPayload(UUID planId, List<UUID> findingIds, String title, String description,
            UUID responsibleAssignmentId, Instant dueAt, String target, String verificationMethod) {}

    public record ActionView(UUID id, long revision, Instant createdAt, UUID createdBy, ActionPayload data, String state,
            boolean overdue, List<ActionProgress> progress, List<ActionDecision> decisions) {}

    public record ActionPage(List<ActionView> items, int page, int size, long totalElements) {}

    public record ProgressView(UUID id, UUID actionId, long revision, Instant createdAt, UUID createdBy,
            ActionProgress.Data data) {}

    public record DecisionView(UUID id, UUID actionId, long revision, Instant createdAt, UUID createdBy, String action,
            DecisionProvenance provenance) {}
}
