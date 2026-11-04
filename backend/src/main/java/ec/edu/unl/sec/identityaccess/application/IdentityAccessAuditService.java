package ec.edu.unl.sec.identityaccess.application;

import ec.edu.unl.sec.audit.application.AuditEventCommand;
import ec.edu.unl.sec.audit.application.AuditPort;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdentityAccessAuditService {
    /** Synthetic technical actor used only for events without an authenticated identity. */
    public static final UUID SYSTEM_ACTOR = new UUID(0L, 0L);

    private static final Set<String> ACTIONS = Set.of(
            "SESSION_LOGIN",
            "SESSION_LOGIN_FAILED",
            "SESSION_LOGIN_LIMITED",
            "SESSION_LOGOUT",
            "IDENTITY_CREATED",
            "IDENTITY_DISABLED",
            "IDENTITY_ENABLED",
            "ASSIGNMENT_CREATED",
            "ASSIGNMENT_REVOKED",
            "ASSIGNMENT_REJECTED",
            "CONFLICT_EXCEPTION_CREATED",
            "CONFLICT_REJECTED",
            "ACCESS_DENIED",
            "AUDIT_QUERY");

    private final AuditPort auditPort;
    private final Clock clock;

    @Autowired
    public IdentityAccessAuditService(AuditPort auditPort) {
        this(auditPort, Clock.systemUTC());
    }

    IdentityAccessAuditService(AuditPort auditPort, Clock clock) {
        this.auditPort = auditPort;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public UUID record(
            UUID actorUserId,
            UUID correlationId,
            String action,
            String objectType,
            UUID objectId,
            UUID careerId,
            UUID processId,
            String outcome,
            String reasonCode,
            Map<String, Object> metadata) {
        return auditPort.record(command(
                actorUserId,
                correlationId,
                action,
                objectType,
                objectId,
                careerId,
                processId,
                outcome,
                reasonCode,
                metadata));
    }

    public UUID rejection(
            UUID actorUserId,
            UUID correlationId,
            String action,
            String objectType,
            UUID objectId,
            UUID careerId,
            UUID processId,
            String reasonCode,
            Map<String, Object> metadata) {
        return auditPort.recordRejection(command(
                actorUserId,
                correlationId,
                action,
                objectType,
                objectId,
                careerId,
                processId,
                "REJECTED",
                reasonCode,
                metadata));
    }

    private AuditEventCommand command(
            UUID actorUserId,
            UUID correlationId,
            String action,
            String objectType,
            UUID objectId,
            UUID careerId,
            UUID processId,
            String outcome,
            String reasonCode,
            Map<String, Object> metadata) {
        if (!ACTIONS.contains(action)) throw new IllegalArgumentException("Unsupported audit action");
        if (!Set.of("SUCCESS", "REJECTED", "FAILED").contains(outcome)) {
            throw new IllegalArgumentException("Unsupported audit outcome");
        }
        UUID userActor = SYSTEM_ACTOR.equals(actorUserId) ? null : actorUserId;
        UUID technicalActor = SYSTEM_ACTOR.equals(actorUserId) ? SYSTEM_ACTOR : null;
        return new AuditEventCommand(
                Instant.now(clock),
                userActor,
                technicalActor,
                correlationId == null ? UUID.randomUUID() : correlationId,
                action,
                objectType,
                objectId == null ? SYSTEM_ACTOR : objectId,
                careerId,
                processId,
                outcome,
                reasonCode,
                sanitize(metadata));
    }

    private static Map<String, Object> sanitize(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) return Map.of();
        Map<String, Object> safe = new LinkedHashMap<>();
        metadata.forEach((key, value) -> {
            String normalized = key.toLowerCase(Locale.ROOT);
            if (normalized.contains("password")
                    || normalized.contains("token")
                    || normalized.contains("secret")
                    || normalized.contains("hash")) {
                return;
            }
            safe.put(key, value);
        });
        return safe;
    }
}
