package ec.edu.unl.sec.identityaccess.api;

import ec.edu.unl.sec.identityaccess.application.IdentityAccessProperties;
import ec.edu.unl.sec.identityaccess.application.IdentityAdministrationService;
import ec.edu.unl.sec.identityaccess.application.SessionService;
import ec.edu.unl.sec.identityaccess.infrastructure.SessionCookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class SessionController {
    private final SessionService sessions;
    private final IdentityAdministrationService administration;
    private final IdentityAccessProperties properties;

    public SessionController(
            SessionService sessions,
            IdentityAdministrationService administration,
            IdentityAccessProperties properties) {
        this.sessions = sessions;
        this.administration = administration;
        this.properties = properties;
    }

    @PostMapping("/sessions")
    public ResponseEntity<SessionView> createSession(
            @Valid @RequestBody LoginRequest body, HttpServletRequest request) {
        String source = request.getRemoteAddr();
        SessionService.CreatedSession created =
                sessions.login(body.username(), body.password(), source, AccessRequestContext.correlation(request));
        var cookie = SessionCookie.create(
                created.token(), properties.getSessionIdleTimeout(), properties.isCookieSecure());
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new SessionView(created.identityId(), created.expiresAt(), created.csrfToken()));
    }

    @DeleteMapping("/sessions")
    public ResponseEntity<Void> revokeCurrentSession(Authentication authentication, HttpServletRequest request) {
        UUID identityId = AccessRequestContext.identityId(authentication);
        SessionCookie.read(request)
                .ifPresent(token -> sessions.logout(
                        token, identityId, AccessRequestContext.correlation(request)));
        var cookie = SessionCookie.clear(properties.isCookieSecure());
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    @GetMapping("/me")
    public IdentityAdministrationService.CurrentIdentity me(Authentication authentication) {
        UUID identityId = AccessRequestContext.identityId(authentication);
        if (identityId == null) {
            throw new ec.edu.unl.sec.identityaccess.domain.IdentityAccessDeniedException("AUTHENTICATION_REQUIRED");
        }
        return administration.currentIdentity(identityId);
    }

    public record LoginRequest(
            @NotBlank @Size(min = 3, max = 100) String username,
            @NotBlank @Size(min = 12, max = 256) String password) {}

    public record SessionView(UUID userId, Instant expiresAt, String csrfToken) {}
}
