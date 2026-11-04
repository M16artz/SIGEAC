package ec.edu.unl.sec.audit.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AuditQueryPort {
    List<AuditEventView> search(
            List<UUID> careerIds,
            boolean includeUnscoped,
            String objectType,
            UUID objectId,
            int offset,
            int limit);

    long count(List<UUID> careerIds, boolean includeUnscoped, String objectType, UUID objectId);

    record AuditEventView(
            UUID id,
            Instant occurredAt,
            UUID correlationId,
            String action,
            String objectType,
            UUID objectId,
            String outcome,
            String reasonCode,
            UUID scopeCareerId,
            UUID scopeProcessId) {}
}
