package ec.edu.unl.sec.selfassessment.api;
import ec.edu.unl.sec.selfassessment.application.ProcessAssignmentService;
import ec.edu.unl.sec.selfassessment.domain.ProcessAssignment;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.net.URI;
import java.time.Instant;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/processes/{processId}/assignments")
public class ProcessAssignmentController {
    private final ProcessAssignmentService service;
    public ProcessAssignmentController(ProcessAssignmentService service) { this.service=service; }
    @GetMapping public List<ProcessAssignment> list(@PathVariable UUID processId) { return service.list(processId); }
    @PostMapping public ResponseEntity<ProcessAssignment> assign(@PathVariable UUID processId,
            @RequestHeader(value="If-Match",required=false) String match, @Valid @RequestBody Command body, HttpServletRequest request) {
        long revision=SelfAssessmentController.rev(match);
        var assignment=service.assign(processId,revision,body.indicatorId(),body.identityId(),body.function(),body.validFrom(),body.validTo(),body.reason(),SelfAssessmentController.correlation(request));
        return ResponseEntity.created(URI.create("/api/v1/processes/"+processId+"/assignments/"+assignment.id()))
                .eTag(ec.edu.unl.sec.shared.api.EntityTagSupport.strong(revision+1)).body(assignment);
    }
    public record Command(@NotNull UUID indicatorId,@NotNull UUID identityId,@NotNull ProcessAssignment.Function function,
            @NotNull Instant validFrom,@NotNull Instant validTo,@NotBlank @Size(max=2000) String reason) {}
}
