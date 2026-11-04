package ec.edu.unl.sec.academicevaluation.api;

import ec.edu.unl.sec.academicevaluation.application.*;
import ec.edu.unl.sec.academicevaluation.domain.QuantitativeEvaluation.*;
import ec.edu.unl.sec.audit.application.*;
import ec.edu.unl.sec.identityaccess.application.CurrentActorPort;
import ec.edu.unl.sec.shared.api.EntityTagSupport;
import ec.edu.unl.sec.shared.infrastructure.CorrelationFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/processes/{processId}/indicators/{indicatorId}")
public class QuantitativeEvaluationController {
    private final AcademicCalculationService calculations;private final VariableValueService variables;
    private final FailureAuditPort failures;private final CurrentActorPort actors;
    public QuantitativeEvaluationController(AcademicCalculationService calculations,VariableValueService variables,FailureAuditPort failures,CurrentActorPort actors) {
        this.calculations=calculations;this.variables=variables;this.failures=failures;this.actors=actors;
    }
    @GetMapping("/quantitative-evaluations")
    public ResponseEntity<AcademicCalculationService.View> view(@PathVariable UUID processId,@PathVariable UUID indicatorId,
            @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(calculations.view(processId,indicatorId,page,size));
    }
    @GetMapping("/quantitative-inputs/{id}")
    public ResponseEntity<Inputs> get(@PathVariable UUID processId,@PathVariable UUID indicatorId,@PathVariable UUID id) {
        var result=variables.get(processId,indicatorId,id);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).eTag(EntityTagSupport.strong(result.revision())).body(result);
    }
    @PostMapping("/quantitative-inputs")
    public ResponseEntity<Inputs> capture(@PathVariable UUID processId,@PathVariable UUID indicatorId,
            @RequestHeader(name="If-Match",required=false) String tag,@Valid @RequestBody CaptureCommand command,HttpServletRequest request) {
        var correlation=correlation(request);
        var values=new LinkedHashMap<String,VariableValueService.ValueInput>();
        command.variables().forEach((key,v)->values.put(key,new VariableValueService.ValueInput(v.value(),v.unit(),v.evidenceVersionIds())));
        var result=attempt(processId,indicatorId,correlation,()->variables.capture(processId,indicatorId,EntityTagSupport.require(tag),
                new VariableValueService.Command(command.periodStart(),command.periodEnd(),values,command.predecessorId()),correlation));
        return ResponseEntity.status(201).cacheControl(CacheControl.noStore()).eTag(EntityTagSupport.strong(result.revision())).body(result);
    }
    @PostMapping("/quantitative-inputs/{id}/verification")
    public ResponseEntity<Verification> verify(@PathVariable UUID processId,@PathVariable UUID indicatorId,@PathVariable UUID id,
            @RequestHeader(name="If-Match",required=false) String tag,@Valid @RequestBody VerificationCommand command,HttpServletRequest request) {
        var correlation=correlation(request);
        return ResponseEntity.status(201).cacheControl(CacheControl.noStore()).body(attempt(processId,indicatorId,correlation,
                ()->variables.verify(processId,indicatorId,id,EntityTagSupport.require(tag),command.reason(),correlation)));
    }
    @PostMapping("/quantitative-calculations")
    public ResponseEntity<Run> calculate(@PathVariable UUID processId,@PathVariable UUID indicatorId,
            @RequestHeader(name="If-Match",required=false) String tag,@Valid @RequestBody CalculationCommand command,HttpServletRequest request) {
        var correlation=correlation(request);
        return ResponseEntity.status(201).cacheControl(CacheControl.noStore()).body(attempt(processId,indicatorId,correlation,
                ()->calculations.calculate(processId,indicatorId,command.inputSetId(),EntityTagSupport.require(tag),command.justification(),command.limitations(),correlation)));
    }
    private <T>T attempt(UUID process,UUID indicator,UUID correlation,Supplier<T> action) {
        try {return action.get();}
        catch(AcademicProblemException ex) {reject(process,indicator,correlation,ex.code());throw ex;}
        catch(org.springframework.dao.ConcurrencyFailureException ex) {reject(process,indicator,correlation,"STALE_VARIABLE_SET");throw ex;}
    }
    private void reject(UUID process,UUID indicator,UUID correlation,String code) {
        actors.currentActor().ifPresent(actor->failures.recordAfterRollback(new AuditEventCommand(Instant.now(),actor.technical()?null:actor.identityId(),
                actor.technical()?actor.identityId():null,correlation,"ACADEMIC_QUANTITATIVE_REJECTED","INDICATOR",indicator,null,process,"REJECTED",code,Map.of())));
    }
    private static UUID correlation(HttpServletRequest request) {try{return UUID.fromString(String.valueOf(request.getAttribute(CorrelationFilter.CORRELATION_ATTRIBUTE)));}catch(IllegalArgumentException ex){return UUID.randomUUID();}}
    public record ValueCommand(@NotBlank @Size(max=20) String value,@NotBlank @Size(max=100) String unit,
            @NotEmpty @Size(max=100) List<@NotNull UUID> evidenceVersionIds) {
        @com.fasterxml.jackson.annotation.JsonAnySetter public void unknown(String k,Object v) {throw new IllegalArgumentException("Unsupported variable field");}
    }
    public record CaptureCommand(@NotNull LocalDate periodStart,@NotNull LocalDate periodEnd,
            @NotNull @Size(min=2,max=2) Map<String,@NotNull @Valid ValueCommand> variables,UUID predecessorId) {
        @com.fasterxml.jackson.annotation.JsonAnySetter public void unknown(String k,Object v) {throw new IllegalArgumentException("Unsupported capture field");}
    }
    public record VerificationCommand(@NotBlank @Size(min=10,max=4000) String reason) {
        @com.fasterxml.jackson.annotation.JsonAnySetter public void unknown(String k,Object v) {throw new IllegalArgumentException("Unsupported verification field");}
    }
    public record CalculationCommand(@NotNull UUID inputSetId,@NotBlank @Size(min=10,max=4000) String justification,@NotBlank @Size(min=10,max=4000) String limitations) {
        @com.fasterxml.jackson.annotation.JsonAnySetter public void unknown(String k,Object v) {throw new IllegalArgumentException("Unsupported calculation field");}
    }
}
