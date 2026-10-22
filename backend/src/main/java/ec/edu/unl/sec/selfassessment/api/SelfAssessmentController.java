package ec.edu.unl.sec.selfassessment.api;

import ec.edu.unl.sec.selfassessment.application.ProcessProblemException;
import ec.edu.unl.sec.selfassessment.application.ProcessTransitionService;
import ec.edu.unl.sec.selfassessment.application.SelfAssessmentService;
import ec.edu.unl.sec.selfassessment.domain.ProcessAuthorization;
import ec.edu.unl.sec.selfassessment.domain.ProcessState;
import ec.edu.unl.sec.selfassessment.domain.SelfAssessmentProcess;
import ec.edu.unl.sec.shared.api.EntityTagSupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class SelfAssessmentController {
    private final SelfAssessmentService service;
    private final ProcessTransitionService transitions;

    public SelfAssessmentController(SelfAssessmentService service, ProcessTransitionService transitions) {
        this.service = service; this.transitions = transitions;
    }
    @GetMapping("/processes") public List<SelfAssessmentProcess> list() { return service.list(); }

    @PostMapping("/processes")
    public ResponseEntity<SelfAssessmentProcess> create(@Valid @RequestBody ProcessCommand command, HttpServletRequest request) {
        var process = service.create(command.careerId(), command.code(), command.name(), command.periodStart(), command.periodEnd(), command.modelVersionId(), correlation(request));
        return ResponseEntity.created(URI.create("/api/v1/processes/" + process.id())).eTag(tag(process)).body(process);
    }

    @GetMapping("/processes/{id}") public ResponseEntity<SelfAssessmentProcess> get(@PathVariable UUID id) {
        var process = service.get(id);
        return ResponseEntity.ok().eTag(tag(process)).body(process);
    }

    @PostMapping("/processes/{id}/authorizations")
    public ResponseEntity<ProcessAuthorization> authorize(@PathVariable UUID id,
            @RequestHeader(value = "If-Match", required = false) String match,
            @Valid @RequestBody AuthorizationCommand command, HttpServletRequest request) {
        var process = service.authorize(id, rev(match), command.authorityAssignmentId(), command.legalBasis(),
                command.documentReference(), command.reason(), correlation(request));
        return ResponseEntity.created(URI.create("/api/v1/processes/" + id + "/authorizations/" + process.authorization().id()))
                .eTag(tag(process)).body(process.authorization());
    }

    @PostMapping("/processes/{id}/activate")
    public ResponseEntity<SelfAssessmentProcess> activate(@PathVariable UUID id,
            @RequestHeader(value = "If-Match", required = false) String match, HttpServletRequest request) {
        var process = service.activate(id, rev(match), correlation(request));
        return ResponseEntity.ok().eTag(tag(process)).body(process);
    }

    @PostMapping("/processes/{id}/transitions")
    public ResponseEntity<SelfAssessmentProcess> transition(@PathVariable UUID id,
            @RequestHeader(value = "If-Match", required = false) String match,
            @Valid @RequestBody TransitionCommand command, HttpServletRequest request) {
        var process = transitions.transition(id, rev(match), command.target(), decision(command),
                command.reason(), correlation(request));
        return ResponseEntity.ok().eTag(tag(process)).body(process);
    }

    @PostMapping("/processes/{id}/reopen")
    public ResponseEntity<SelfAssessmentProcess> reopen(@PathVariable UUID id,
            @RequestHeader(value = "If-Match", required = false) String match,
            @Valid @RequestBody ReopenCommand command, HttpServletRequest request) {
        var process = transitions.transition(id, rev(match), ProcessState.REOPENED, decision(command),
                command.reason(), correlation(request));
        return ResponseEntity.ok().eTag(tag(process)).body(process);
    }

    static long rev(String value) { return EntityTagSupport.require(value); }
    static String tag(SelfAssessmentProcess process) { return EntityTagSupport.strong(process.revision()); }
    static ProcessTransitionService.Decision decision(TransitionCommand command) {
        return decision(command.authorityAssignmentId(), command.legalBasis(), command.documentReference());
    }
    static ProcessTransitionService.Decision decision(ReopenCommand command) {
        return decision(command.authorityAssignmentId(), command.legalBasis(), command.documentReference());
    }
    private static ProcessTransitionService.Decision decision(UUID assignment, String legalBasis, String document) {
        boolean empty = assignment == null && (legalBasis == null || legalBasis.isBlank())
                && (document == null || document.isBlank());
        if (empty) return null;
        if (assignment == null || legalBasis == null || legalBasis.isBlank()
                || document == null || document.isBlank())
            throw new ProcessProblemException(ProcessProblemException.Kind.INVALID, "AUTHORITY_PROVENANCE_INCOMPLETE",
                    "La decision requiere asignacion, fundamento y constancia.");
        return new ProcessTransitionService.Decision(assignment, legalBasis, document);
    }
    static UUID correlation(HttpServletRequest request) {
        Object value = request.getAttribute("sec.correlationId");
        try { return UUID.fromString(String.valueOf(value)); } catch (IllegalArgumentException exception) { return UUID.randomUUID(); }
    }

    public record ProcessCommand(@NotNull UUID careerId, @NotBlank @Size(max = 100) String code,
            @NotBlank @Size(max = 300) String name, @NotNull LocalDate periodStart, @NotNull LocalDate periodEnd, @NotNull UUID modelVersionId) {}
    public record TransitionCommand(@NotNull ProcessState target, @NotBlank @Size(min = 10, max = 2000) String reason,
            UUID authorityAssignmentId, @Size(max = 4000) String legalBasis,
            @Size(max = 2000) String documentReference) {}
    public record ReopenCommand(@NotBlank @Size(min = 10, max = 2000) String reason,
            UUID authorityAssignmentId, @Size(max = 4000) String legalBasis,
            @Size(max = 2000) String documentReference) {}
    public record AuthorizationCommand(@NotNull UUID authorityAssignmentId, @NotBlank @Size(max = 4000) String legalBasis,
            @NotBlank @Size(max = 2000) String documentReference, @NotBlank @Size(min = 10, max = 4000) String reason) {}
}
