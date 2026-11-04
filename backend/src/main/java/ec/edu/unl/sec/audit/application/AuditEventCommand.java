package ec.edu.unl.sec.audit.application;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditEventCommand(
        Instant occurredAt,
        UUID actorUserId,
        UUID technicalAccountId,
        UUID correlationId,
        String action,
        String objectType,
        UUID objectId,
        UUID scopeCareerId,
        UUID scopeProcessId,
        String outcome,
        String reasonCode,
        Map<String, Object> metadata) {
    public AuditEventCommand {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        if (actorUserId == null && technicalAccountId == null) {
            throw new IllegalArgumentException("An audit actor is required");
        }
    }
}
