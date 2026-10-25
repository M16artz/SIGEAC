package ec.edu.unl.sec.academicevaluation.api;

import ec.edu.unl.sec.academicevaluation.application.AcademicEvaluationService;
import ec.edu.unl.sec.academicevaluation.domain.ManualAcademicEvaluation;
import ec.edu.unl.sec.shared.api.DomainProblemException;
import ec.edu.unl.sec.shared.api.EntityTagSupport;
import ec.edu.unl.sec.shared.infrastructure.CorrelationFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
public class AcademicEvaluationController {
    private final AcademicEvaluationService evaluation;
    private final ec.edu.unl.sec.audit.application.FailureAuditPort failures;
    private final ec.edu.unl.sec.identityaccess.application.CurrentActorPort actors;
    public AcademicEvaluationController(AcademicEvaluationService evaluation,
            ec.edu.unl.sec.audit.application.FailureAuditPort failures,
            ec.edu.unl.sec.identityaccess.application.CurrentActorPort actors) {
        this.evaluation=evaluation; this.failures=failures; this.actors=actors;
    }

    @GetMapping("/processes/{processId}/indicators/{indicatorId}/manual-academic-evaluations")
    public ResponseEntity<AcademicEvaluationService.View> view(@PathVariable UUID processId, @PathVariable UUID indicatorId,
            @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size) {
        var view=evaluation.view(processId, indicatorId, page, size);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).eTag(EntityTagSupport.strong(view.ownRevision())).body(view);
    }
    @PostMapping("/processes/{processId}/indicators/{indicatorId}/manual-academic-evaluations")
    public ResponseEntity<ManualAcademicEvaluation> evaluate(@PathVariable UUID processId, @PathVariable UUID indicatorId,
            @RequestHeader(name="If-Match", required=false) String etag, @Valid @RequestBody EvaluateRequest command,
            HttpServletRequest request) {
        var raw=request.getAttribute(CorrelationFilter.CORRELATION_ATTRIBUTE);
        UUID correlation;
        try { correlation=UUID.fromString(String.valueOf(raw)); } catch (IllegalArgumentException ex) { correlation=UUID.randomUUID(); }
        ManualAcademicEvaluation result;
        try {
            result=evaluation.evaluate(processId, indicatorId, EntityTagSupport.require(etag),
                    new AcademicEvaluationService.Command(command.ratingOptionId(), command.evidenceVersionIds(),
                            command.justification(), command.limitations(), command.predecessorId()), correlation);
        } catch (ec.edu.unl.sec.academicevaluation.application.AcademicProblemException rejected) {
            rejection(processId, indicatorId, correlation, rejected.code());
            throw rejected;
        } catch (org.springframework.dao.ConcurrencyFailureException stale) {
            rejection(processId, indicatorId, correlation, "STALE_EVALUATION");
            throw stale;
        }
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
                .eTag(EntityTagSupport.strong(result.revision())).body(result);
    }
    @PostMapping({"/academic-evaluations/indicators/{indicatorId}/variables",
            "/academic-evaluations/indicators/{indicatorId}/calculate", "/academic-evaluations/indicators/{indicatorId}/evaluate"})
    public void unavailable(@PathVariable UUID indicatorId) {
        throw new DomainProblemException(HttpStatus.NOT_IMPLEMENTED,"ACADEMIC_WORKFLOW_UNAVAILABLE",
                "La operación provisional no está implementada; utilice el flujo manual por proceso e indicador.");
    }
    private void rejection(UUID processId, UUID indicatorId, UUID correlation, String code) {
        // The service proxy has rolled back before the separate rejection audit starts.
        actors.currentActor().ifPresent(actor -> failures.recordAfterRollback(new ec.edu.unl.sec.audit.application.AuditEventCommand(
                java.time.Instant.now(), actor.technical()?null:actor.identityId(), actor.technical()?actor.identityId():null,
                correlation, "ACADEMIC_MANUAL_EVALUATION_REJECTED", "INDICATOR", indicatorId, null, processId,
                "REJECTED", code, java.util.Map.of())));
    }
    public record EvaluateRequest(@NotNull UUID ratingOptionId,
            @NotEmpty @Size(max=100) List<@NotNull UUID> evidenceVersionIds,
            @NotBlank @Size(min=10,max=4000) String justification,
            @NotBlank @Size(min=10,max=4000) String limitations, UUID predecessorId) {
        @com.fasterxml.jackson.annotation.JsonAnySetter public void unknown(String key, Object value) {
            throw new IllegalArgumentException("Campo no admitido");
        }
    }
}
