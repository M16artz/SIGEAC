package ec.edu.unl.sec.identityaccess.application;

import ec.edu.unl.sec.identityaccess.domain.*;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionService {
    private final IdentityAccessRepository repository;
    private final IdentityAccessProperties properties;
    private final IdentityAccessAuditService audit;
    private final Clock clock;
    private final BCryptPasswordEncoder encoder;
    private final SecureRandom random = new SecureRandom();

    public SessionService(
            IdentityAccessRepository repository,
            IdentityAccessProperties properties,
            IdentityAccessAuditService audit) {
        this(repository, properties, audit, Clock.systemUTC());
    }

    SessionService(
            IdentityAccessRepository repository,
            IdentityAccessProperties properties,
            IdentityAccessAuditService audit,
            Clock clock) {
        this.repository = repository;
        this.properties = properties;
        this.audit = audit;
        this.clock = clock;
        this.encoder = new BCryptPasswordEncoder(properties.getBcryptStrength());
    }

    @Transactional(noRollbackFor = AuthenticationRejected.class)
    public CreatedSession login(String username, String password, String source, UUID correlationId) {
        Instant now = clock.instant();
        String normalized = AccessIdentity.normalize(username);
        String subject = hmac(normalized);
        String sourceKey = hmac(source == null || source.isBlank() ? "unknown" : source);

        if (repository.countFailures(subject, now.minus(properties.getFailedAttemptWindow()))
                >= properties.getFailedAttemptLimit()) {
            audit.rejection(
                    IdentityAccessAuditService.SYSTEM_ACTOR,
                    correlationId,
                    "SESSION_LOGIN_LIMITED",
                    "ACCESS_IDENTITY",
                    null,
                    null,
                    null,
                    "AUTHENTICATION_LIMITED",
                    Map.of("subjectKey", subject));
            throw new AuthenticationRejected("AUTHENTICATION_LIMITED");
        }

        Optional<IdentityAccessRepository.LoginRecord> found = repository.findLogin(normalized);
        boolean valid = found.isPresent()
                && found.get().active()
                && encoder.matches(password == null ? "" : password, found.get().bcryptHash());
        repository.recordAttempt(subject, sourceKey, now, valid, correlationId);

        if (!valid) {
            UUID actor = found.map(IdentityAccessRepository.LoginRecord::identityId)
                    .orElse(IdentityAccessAuditService.SYSTEM_ACTOR);
            audit.rejection(
                    actor,
                    correlationId,
                    "SESSION_LOGIN_FAILED",
                    "ACCESS_IDENTITY",
                    found.map(IdentityAccessRepository.LoginRecord::identityId).orElse(null),
                    null,
                    null,
                    "AUTHENTICATION_FAILED",
                    Map.of("subjectKey", subject));
            if (repository.countFailures(subject, now.minus(properties.getFailedAttemptWindow()))
                    >= properties.getFailedAttemptLimit()) {
                audit.rejection(
                        IdentityAccessAuditService.SYSTEM_ACTOR,
                        correlationId,
                        "SESSION_LOGIN_LIMITED",
                        "ACCESS_IDENTITY",
                        found.map(IdentityAccessRepository.LoginRecord::identityId).orElse(null),
                        null,
                        null,
                        "AUTHENTICATION_LIMITED",
                        Map.of("subjectKey", subject));
                throw new AuthenticationRejected("AUTHENTICATION_LIMITED");
            }
            throw new AuthenticationRejected("AUTHENTICATION_FAILED");
        }

        String token = randomToken();
        String csrf = randomToken();
        Instant expires = now.plus(properties.getSessionIdleTimeout());
        AccessSession session = new AccessSession(
                UUID.randomUUID(),
                found.get().identityId(),
                sha256Hex(token),
                sha256Hex(csrf),
                now,
                now,
                expires,
                null);
        repository.saveSession(session);
        audit.record(
                found.get().identityId(),
                correlationId,
                "SESSION_LOGIN",
                "ACCESS_SESSION",
                session.id(),
                null,
                null,
                "SUCCESS",
                "AUTHENTICATED",
                Map.of("technical", found.get().technical()));
        return new CreatedSession(token, csrf, expires, found.get().identityId());
    }

    @Transactional
    public Optional<AuthenticatedSession> authenticateForRequest(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        Instant now = clock.instant();
        Optional<IdentityAccessRepository.SessionIdentity> found = repository.findSession(sha256Hex(token));
        if (found.isEmpty()) return Optional.empty();
        IdentityAccessRepository.SessionIdentity session = found.get();
        boolean active = session.identityActive()
                && session.revokedAt() == null
                && now.isBefore(session.expiresAt())
                && session.lastActivityAt().plus(properties.getSessionIdleTimeout()).isAfter(now);
        if (!active) return Optional.empty();
        repository.touchSession(session.sessionId(), now, now.plus(properties.getSessionIdleTimeout()));
        return Optional.of(new AuthenticatedSession(
                session.sessionId(),
                session.identityId(),
                session.displayName(),
                session.technical(),
                session.csrfHash()));
    }

    @Transactional
    public void logout(String token, UUID identityId, UUID correlationId) {
        if (token == null || token.isBlank()) return;
        repository.revokeSession(sha256Hex(token), clock.instant());
        if (identityId != null) {
            audit.record(
                    identityId,
                    correlationId,
                    "SESSION_LOGOUT",
                    "ACCESS_SESSION",
                    identityId,
                    null,
                    null,
                    "SUCCESS",
                    "SESSION_REVOKED",
                    Map.of());
        }
    }

    public String encodePassword(String raw) {
        return encoder.encode(raw);
    }

    public Duration idleTimeout() {
        return properties.getSessionIdleTimeout();
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String sha256Hex(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String hmac(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    properties.getSubjectHmacSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public record CreatedSession(String token, String csrfToken, Instant expiresAt, UUID identityId) {}

    public record AuthenticatedSession(
            UUID sessionId, UUID identityId, String displayName, boolean technical, String csrfHash) {}

    public static final class AuthenticationRejected extends RuntimeException {
        private final String code;

        public AuthenticationRejected(String code) {
            super("Authentication was not accepted");
            this.code = code;
        }

        public String code() {
            return code;
        }
    }
}
