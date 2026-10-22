package ec.edu.unl.sec.selfassessment.api;

import ec.edu.unl.sec.selfassessment.application.ProcessPlanningService;
import ec.edu.unl.sec.selfassessment.domain.ProcessActivity;
import ec.edu.unl.sec.selfassessment.domain.ProcessTeam;
import ec.edu.unl.sec.selfassessment.domain.TeamMembership;
import ec.edu.unl.sec.shared.api.EntityTagSupport;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProcessGovernanceController {
    private final ProcessPlanningService planning;

    public ProcessGovernanceController(ProcessPlanningService planning) { this.planning = planning; }

    @GetMapping("/processes/{processId}/teams")
    public List<ProcessTeam> teams(@PathVariable UUID processId) { return planning.teams(processId); }

    @PostMapping("/processes/{processId}/teams")
    public ResponseEntity<ProcessTeam> createTeam(@PathVariable UUID processId,
            @RequestHeader(value = "If-Match", required = false) String match,
            @Valid @RequestBody TeamCommand command, HttpServletRequest request) {
        var planned = planning.createTeam(processId, SelfAssessmentController.rev(match), command.name(),
                command.processRole(), command.validFrom(), command.validUntil(),
                SelfAssessmentController.correlation(request));
        return ResponseEntity.created(URI.create("/api/v1/teams/" + planned.value().id()))
                .eTag(EntityTagSupport.strong(planned.processRevision())).body(planned.value());
    }

    @GetMapping("/teams/{teamId}")
    public ProcessTeam team(@PathVariable UUID teamId) { return planning.team(teamId); }

    @GetMapping("/teams/{teamId}/memberships")
    public List<TeamMembership> memberships(@PathVariable UUID teamId) { return planning.memberships(teamId); }

    @PostMapping("/teams/{teamId}/memberships")
    public ResponseEntity<TeamMembership> addMember(@PathVariable UUID teamId,
            @RequestHeader(value = "If-Match", required = false) String match,
            @Valid @RequestBody MemberCommand command, HttpServletRequest request) {
        var planned = planning.addMember(teamId, SelfAssessmentController.rev(match), command.userId(),
                command.roleAssignmentId(), command.validFrom(), command.validUntil(),
                SelfAssessmentController.correlation(request));
        return ResponseEntity.created(URI.create("/api/v1/teams/" + teamId + "/memberships/" + planned.value().id()))
                .eTag(EntityTagSupport.strong(planned.processRevision())).body(planned.value());
    }

    @GetMapping("/processes/{processId}/activities")
    public List<ProcessActivity> activities(@PathVariable UUID processId) { return planning.activities(processId); }

    @PostMapping("/processes/{processId}/activities")
    public ResponseEntity<ProcessActivity> createActivity(@PathVariable UUID processId,
            @RequestHeader(value = "If-Match", required = false) String match,
            @Valid @RequestBody ActivityCommand command, HttpServletRequest request) {
        var planned = planning.createActivity(processId, SelfAssessmentController.rev(match), input(command),
                SelfAssessmentController.correlation(request));
        return ResponseEntity.created(URI.create("/api/v1/activities/" + planned.value().id()))
                .eTag(EntityTagSupport.strong(planned.processRevision())).body(planned.value());
    }

    @GetMapping("/activities/{activityId}")
    public ProcessActivity activity(@PathVariable UUID activityId) { return planning.activity(activityId); }

    @PatchMapping("/activities/{activityId}")
    public ResponseEntity<ProcessActivity> updateActivity(@PathVariable UUID activityId,
            @RequestHeader(value = "If-Match", required = false) String match,
            @Valid @RequestBody ActivityCommand command, HttpServletRequest request) {
        var planned = planning.updateActivity(activityId, SelfAssessmentController.rev(match), input(command),
                SelfAssessmentController.correlation(request));
        return ResponseEntity.ok().eTag(EntityTagSupport.strong(planned.processRevision())).body(planned.value());
    }

    private static ProcessPlanningService.ActivityInput input(ActivityCommand command) {
        return new ProcessPlanningService.ActivityInput(command.indicatorId(), command.requiredSourceId(),
                command.assignmentId(), command.title(), command.dueAt(), command.dependencyIds(),
                command.exceptionReason(), command.exceptionAuthorityAssignmentId(), command.exceptionLegalBasis());
    }

    public record TeamCommand(@NotBlank @Size(max = 300) String name,
            @NotBlank @Size(max = 100) String processRole, @NotNull Instant validFrom,
            @NotNull Instant validUntil) {}

    public record MemberCommand(@NotNull UUID userId, @NotNull UUID roleAssignmentId,
            @NotNull Instant validFrom, @NotNull Instant validUntil) {}

    public record ActivityCommand(@NotBlank @Size(max = 300) String title, @NotNull UUID indicatorId,
            UUID requiredSourceId, @NotNull UUID assignmentId, @NotNull Instant dueAt, List<UUID> dependencyIds,
            @Size(max = 4000) String exceptionReason, UUID exceptionAuthorityAssignmentId,
            @Size(max = 4000) String exceptionLegalBasis) {}
}
