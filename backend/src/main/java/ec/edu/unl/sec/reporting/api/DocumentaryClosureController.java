package ec.edu.unl.sec.reporting.api;

import ec.edu.unl.sec.reporting.application.DocumentaryClosureService;
import ec.edu.unl.sec.reporting.application.ReportingProblemException;
import ec.edu.unl.sec.reporting.domain.DocumentaryClosure;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class DocumentaryClosureController {
    private final DocumentaryClosureService closures;
    private final ec.edu.unl.sec.audit.application.FailureAuditPort failures;
    private final ec.edu.unl.sec.identityaccess.application.CurrentActorPort actors;
    public DocumentaryClosureController(DocumentaryClosureService closures,
            ec.edu.unl.sec.audit.application.FailureAuditPort failures,
            ec.edu.unl.sec.identityaccess.application.CurrentActorPort actors) {
        this.closures=closures;this.failures=failures;this.actors=actors;
    }
    @PostMapping("/processes/{processId}/documentary-close")
    public ResponseEntity<ClosureView> close(@PathVariable UUID processId,@RequestHeader(name="If-Match",required=false) String ifMatch,
            @Valid @RequestBody ClosureCommand command,HttpServletRequest request) {
        var correlation=DocumentarySnapshotController.correlation(request);
        DocumentaryClosure closure;
        try {
            closure=closures.close(processId,DocumentarySnapshotController.revision(ifMatch),command.closurePolicyVersion(),command.reason(),correlation);
        } catch (ReportingProblemException rejected) {
            recordRejection(processId,correlation,rejected.code());
            throw rejected;
        } catch (org.springframework.dao.ConcurrencyFailureException stale) {
            recordRejection(processId,correlation,"STALE_PROCESS");
            throw stale;
        }
        return ResponseEntity.created(URI.create("/api/v1/documentary-closures/"+closure.content().id()))
                .cacheControl(CacheControl.noStore()).eTag("\""+closure.sha256()+"\"")
                .body(new ClosureView(closure.content(),closure.sha256()));
    }
    @GetMapping("/processes/{processId}/documentary-closures")
    public ResponseEntity<DocumentaryClosureService.Page> history(@PathVariable UUID processId,
            @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,@RequestParam Map<String,String> parameters) {
        if (!Set.of("page","size").containsAll(parameters.keySet())) throw new ReportingProblemException(
                ReportingProblemException.Kind.INVALID,"UNSUPPORTED_FILTER","El historial admite únicamente página y tamaño.");
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(closures.history(processId,page,size));
    }
    @GetMapping("/documentary-closures/{closureId}")
    public ResponseEntity<ClosureView> get(@PathVariable UUID closureId) {
        var closure=closures.get(closureId);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).eTag("\""+closure.sha256()+"\"")
                .body(new ClosureView(closure.content(),closure.sha256()));
    }
    private void recordRejection(UUID processId,UUID correlation,String code) {
        // The transactional service has already rolled back before the boundary records this attempt.
        actors.currentActor().ifPresent(actor -> failures.recordAfterRollback(new ec.edu.unl.sec.audit.application.AuditEventCommand(
                java.time.Instant.now(),actor.technical()?null:actor.identityId(),actor.technical()?actor.identityId():null,correlation,
                "DOCUMENTARY_CLOSURE_REJECTED","SELF_ASSESSMENT_PROCESS",processId,null,processId,"REJECTED",code,Map.of())));
    }
    public record ClosureCommand(@NotBlank @Size(max=100) String closurePolicyVersion,@NotBlank @Size(min=10,max=2000) String reason) {
        @com.fasterxml.jackson.annotation.JsonAnySetter
        public void unsupported(String key,Object value) { throw new IllegalArgumentException("No se admiten excepciones sin un flujo autorizador implementado."); }
    }
    public record ClosureView(DocumentaryClosure.Content data,String sha256) {}
}
