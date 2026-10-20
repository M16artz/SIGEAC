package ec.edu.unl.sec.identityaccess.api;

import ec.edu.unl.sec.identityaccess.application.IdentityAdministrationService;
import ec.edu.unl.sec.identityaccess.domain.IdentityAccessDeniedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class IdentityAdministrationController {
    private final IdentityAdministrationService administration;

    public IdentityAdministrationController(IdentityAdministrationService administration) {
        this.administration = administration;
    }

    @GetMapping("/users")
    public PageView<IdentityAdministrationService.UserView> listUsers(
            Authentication authentication,
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var result = administration.listUsers(actor(authentication, request), page, size);
        return new PageView<>(result.items(), result.page(), result.size(), result.total());
    }

    @PostMapping("/users")
    public ResponseEntity<ResourceView> createUser(
            Authentication authentication,
            HttpServletRequest request,
            @Valid @RequestBody UserCommand body) {
        UUID id = administration.createUser(
                actor(authentication, request),
                body.username(),
                body.displayName(),
                body.initialPassword(),
                Boolean.TRUE.equals(body.technical()),
                AccessRequestContext.correlation(request));
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create("/api/v1/users/" + id))
                .body(new ResourceView(id, 0, "ACTIVE"));
    }

    @PatchMapping("/users/{userId}")
    public ResourceView updateUserState(
            Authentication authentication,
            HttpServletRequest request,
            @PathVariable UUID userId,
            @RequestHeader("If-Match") String ifMatch,
            @Valid @RequestBody UserStateCommand body) {
        long expectedRevision = revision(ifMatch);
        long revision = administration.changeState(
                actor(authentication, request),
                userId,
                body.active(),
                expectedRevision,
                AccessRequestContext.correlation(request));
        return new ResourceView(userId, revision, body.active() ? "ACTIVE" : "DISABLED");
    }

    @GetMapping("/users/{userId}/assignments")
    public List<IdentityAdministrationService.AssignmentView> listAssignments(
            Authentication authentication, HttpServletRequest request, @PathVariable UUID userId) {
        return administration.listAssignments(actor(authentication, request), userId);
    }

    @PostMapping("/users/{userId}/assignments")
    public ResponseEntity<ResourceView> assignUserScope(
            Authentication authentication,
            HttpServletRequest request,
            @PathVariable UUID userId,
            @Valid @RequestBody AssignmentCommand body) {
        UUID id = administration.assign(
                actor(authentication, request),
                userId,
                body.roleCode(),
                body.careerId(),
                body.processId(),
                body.indicatorId(),
                body.validFrom(),
                body.validUntil(),
                body.exceptionReason(),
                AccessRequestContext.correlation(request));
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create("/api/v1/users/" + userId + "/assignments/" + id))
                .body(new ResourceView(id, 0, "ACTIVE"));
    }

    @PostMapping("/conflict-exceptions")
    public ResponseEntity<ResourceView> createConflictException(
            Authentication authentication,
            HttpServletRequest request,
            @Valid @RequestBody ConflictExceptionCommand body) {
        UUID id = administration.createException(
                actor(authentication, request),
                body.policyId(),
                body.targetUserId(),
                body.careerId(),
                body.processId(),
                body.indicatorId(),
                body.justification(),
                body.validFrom(),
                body.validTo(),
                AccessRequestContext.correlation(request));
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create("/api/v1/conflict-exceptions/" + id))
                .body(new ResourceView(id, 0, "ACTIVE"));
    }

    private static IdentityAdministrationService.Actor actor(
            Authentication authentication, HttpServletRequest request) {
        UUID identityId = AccessRequestContext.identityId(authentication);
        if (identityId == null) throw new IdentityAccessDeniedException("AUTHENTICATION_REQUIRED");
        return new IdentityAdministrationService.Actor(
                identityId, AccessRequestContext.correlation(request));
    }

    private static long revision(String ifMatch) {
        try {
            return Long.parseLong(ifMatch.replace("\"", "").strip());
        } catch (NumberFormatException invalid) {
            throw new IllegalArgumentException("El encabezado If-Match debe contener una revision valida.");
        }
    }

    public record UserCommand(
            @NotBlank @Size(min = 3, max = 100) String username,
            @NotBlank @Size(max = 200) String displayName,
            @NotBlank @Size(min = 12, max = 256) String initialPassword,
            Boolean technical) {}

    public record UserStateCommand(@NotNull Boolean active) {}

    public record AssignmentCommand(
            @NotBlank String roleCode,
            @NotNull UUID careerId,
            UUID processId,
            UUID indicatorId,
            @NotNull Instant validFrom,
            Instant validUntil,
            @Size(min = 10, max = 1000) String exceptionReason) {}

    public record ConflictExceptionCommand(
            @NotNull UUID policyId,
            @NotNull UUID targetUserId,
            @NotNull UUID careerId,
            UUID processId,
            UUID indicatorId,
            @NotBlank @Size(min = 10, max = 1000) String justification,
            @NotNull Instant validFrom,
            Instant validTo) {}

    public record ResourceView(UUID id, long version, String status) {}

    public record PageView<T>(List<T> items, int page, int size, long total) {}
}
