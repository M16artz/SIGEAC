package ec.edu.unl.sec.academicevaluation.api;

import ec.edu.unl.sec.academicevaluation.application.AcademicReconciliationService;
import ec.edu.unl.sec.academicevaluation.application.AcademicProblemException;
import ec.edu.unl.sec.academicevaluation.domain.AcademicReconciliation.*;
import ec.edu.unl.sec.audit.application.*;
import ec.edu.unl.sec.identityaccess.application.CurrentActorPort;
import ec.edu.unl.sec.shared.api.EntityTagSupport;
import ec.edu.unl.sec.shared.infrastructure.CorrelationFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
public class AcademicReconciliationController {
    private final AcademicReconciliationService reconciliation;
    private final FailureAuditPort failures;private final CurrentActorPort actors;
    public AcademicReconciliationController(AcademicReconciliationService reconciliation,FailureAuditPort failures,CurrentActorPort actors) {
        this.reconciliation = reconciliation;this.failures=failures;this.actors=actors;
    }
    @GetMapping("/processes/{processId}/indicators/{indicatorId}/academic-reconciliation")
    public ResponseEntity<AcademicReconciliationService.View> view(@PathVariable UUID processId,@PathVariable UUID indicatorId,
            @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="0") int casePage,@RequestParam(defaultValue="20") int size) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(reconciliation.view(processId,indicatorId,page,casePage,size));
    }
    @PostMapping("/processes/{processId}/indicators/{indicatorId}/academic-reconciliation-policies")
    public ResponseEntity<Policy> policy(@PathVariable UUID processId,@PathVariable UUID indicatorId,
            @RequestHeader(name="If-Match",required=false) String tag,@Valid @RequestBody PolicyCommand command,HttpServletRequest request) {
        var correlation=correlation(request);
        var result=attempt(processId,indicatorId,correlation,()->reconciliation.configure(processId,indicatorId,EntityTagSupport.require(tag),
                new AcademicReconciliationService.PolicyCommand(command.procedureReference(),command.method(),command.legalBasis(),command.documentReference(),command.authorityAssignmentId()),correlation));
        return ResponseEntity.status(201).cacheControl(CacheControl.noStore()).eTag(EntityTagSupport.strong(result.revision())).body(result);
    }
    @PostMapping("/processes/{processId}/indicators/{indicatorId}/academic-disagreements")
    public ResponseEntity<Disagreement> open(@PathVariable UUID processId,@PathVariable UUID indicatorId,
            @RequestHeader(name="If-Match",required=false) String tag,@Valid @RequestBody OpenCommand command,HttpServletRequest request) {
        var correlation=correlation(request);
        var result=attempt(processId,indicatorId,correlation,()->reconciliation.open(processId,indicatorId,EntityTagSupport.require(tag),command.reason(),correlation));
        return ResponseEntity.status(201).cacheControl(CacheControl.noStore()).eTag(EntityTagSupport.strong(result.revision())).body(result);
    }
    @PostMapping("/processes/{processId}/indicators/{indicatorId}/academic-disagreements/{id}/resolution")
    public ResponseEntity<Resolution> resolve(@PathVariable UUID processId,@PathVariable UUID indicatorId,@PathVariable UUID id,
            @RequestHeader(name="If-Match",required=false) String tag,@Valid @RequestBody ResolutionCommand command,HttpServletRequest request) {
        var correlation=correlation(request);
        return ResponseEntity.status(201).cacheControl(CacheControl.noStore()).body(attempt(processId,indicatorId,correlation,
                ()->reconciliation.resolve(processId,indicatorId,id,EntityTagSupport.require(tag),new AcademicReconciliationService.ResolutionCommand(
                        command.policyId(),command.selectedJudgementId(),command.participants(),command.reason(),command.authorityAssignmentId()),correlation)));
    }
    @PostMapping("/academic-evaluations/disagreements/{indicatorId}")
    public void reconcile(@PathVariable UUID indicatorId, @RequestBody(required = false) Map<String, Object> note) {
        throw new ec.edu.unl.sec.shared.api.DomainProblemException(org.springframework.http.HttpStatus.NOT_IMPLEMENTED,
                "RECONCILIATION_SCOPE_REQUIRED", "Utilice la conciliación por proceso e indicador con procedimiento autorizado.");
    }
    private <T>T attempt(UUID process,UUID indicator,UUID correlation,Supplier<T> action) {
        try{return action.get();}
        catch(AcademicProblemException ex){reject(process,indicator,correlation,ex.code());throw ex;}
        catch(org.springframework.dao.ConcurrencyFailureException ex){reject(process,indicator,correlation,"STALE_RECONCILIATION");throw ex;}
    }
    private void reject(UUID process,UUID indicator,UUID correlation,String code) {
        actors.currentActor().ifPresent(actor->failures.recordAfterRollback(new AuditEventCommand(Instant.now(),actor.technical()?null:actor.identityId(),actor.technical()?actor.identityId():null,
                correlation,"ACADEMIC_RECONCILIATION_REJECTED","INDICATOR",indicator,null,process,"REJECTED",code,Map.of())));
    }
    private static UUID correlation(HttpServletRequest request){try{return UUID.fromString(String.valueOf(request.getAttribute(CorrelationFilter.CORRELATION_ATTRIBUTE)));}catch(IllegalArgumentException ex){return UUID.randomUUID();}}
    public record PolicyCommand(@NotBlank @Size(min=3,max=300) String procedureReference,@NotBlank String method,
            @NotBlank @Size(min=10,max=4000) String legalBasis,@NotBlank @Size(min=3,max=1000) String documentReference,@NotNull UUID authorityAssignmentId) {
        @com.fasterxml.jackson.annotation.JsonAnySetter public void unknown(String k,Object v){throw new IllegalArgumentException("Unsupported policy field");}
    }
    public record OpenCommand(@NotBlank @Size(min=10,max=4000) String reason) {
        @com.fasterxml.jackson.annotation.JsonAnySetter public void unknown(String k,Object v){throw new IllegalArgumentException("Unsupported disagreement field");}
    }
    public record ResolutionCommand(@NotNull UUID policyId,@NotNull UUID selectedJudgementId,@NotEmpty @Size(max=101) List<@NotNull UUID> participants,
            @NotBlank @Size(min=10,max=4000) String reason,@NotNull UUID authorityAssignmentId) {
        @com.fasterxml.jackson.annotation.JsonAnySetter public void unknown(String k,Object v){throw new IllegalArgumentException("Unsupported resolution field");}
    }
}
