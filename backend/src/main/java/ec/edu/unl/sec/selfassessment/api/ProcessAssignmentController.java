package ec.edu.unl.sec.selfassessment.api;

import ec.edu.unl.sec.selfassessment.application.ProcessAssignmentService;
import ec.edu.unl.sec.selfassessment.application.ProcessPlanningService;
import ec.edu.unl.sec.selfassessment.application.ProcessProblemException;
import ec.edu.unl.sec.selfassessment.domain.ProcessAssignment;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/processes/{processId}/assignments")
public class ProcessAssignmentController {
    private final ProcessAssignmentService service;
    private final ProcessPlanningService planning;

    public ProcessAssignmentController(ProcessAssignmentService service, ProcessPlanningService planning) {
        this.service = service; this.planning = planning;
    }

    @GetMapping
    public List<ProcessAssignment> list(@PathVariable UUID processId) { return service.list(processId); }

    @PostMapping
    public ResponseEntity<ProcessAssignment> assign(@PathVariable UUID processId,
            @RequestHeader(value = "If-Match", required = false) String match,
            @Valid @RequestBody Command body, HttpServletRequest request) {
        long revision = SelfAssessmentController.rev(match);
        if (body.identityId() != null && body.userId() != null && !body.identityId().equals(body.userId()))
            throw new ProcessProblemException(ProcessProblemException.Kind.INVALID, "ASSIGNMENT_IDENTITY_CONFLICT",
                    "identityId y userId deben identificar a la misma persona.");
        UUID userId = body.userId() != null ? body.userId() : body.identityId();
        if (body.teamId() != null || body.userId() != null) {
            var planned = planning.assignParticipant(processId, revision, body.indicatorId(), userId,
                    body.teamId(), body.function(), body.validFrom(), body.validTo(), body.reason(),
                    SelfAssessmentController.correlation(request));
            return ResponseEntity.created(URI.create("/api/v1/processes/" + processId + "/assignments/"
                    + planned.value().id())).eTag(ec.edu.unl.sec.shared.api.EntityTagSupport.strong(
                            planned.processRevision())).body(planned.value());
        }
        var assignment = service.assign(processId, revision, body.indicatorId(), userId, body.function(),
                body.validFrom(), body.validTo(), body.reason(), SelfAssessmentController.correlation(request));
        return ResponseEntity.created(URI.create("/api/v1/processes/" + processId + "/assignments/" + assignment.id()))
                .eTag(ec.edu.unl.sec.shared.api.EntityTagSupport.strong(revision + 1)).body(assignment);
    }

    public record Command(@NotNull UUID indicatorId, UUID identityId, UUID userId, UUID teamId,
            @NotNull ProcessAssignment.Function function, @NotNull Instant validFrom, @NotNull Instant validTo,
            @NotBlank @Size(max = 2000) String reason) {}
}
