package ec.edu.unl.sec.audit.api;

import ec.edu.unl.sec.audit.application.AuditQueryPort;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditEventView(
        UUID id,
        Instant occurredAt,
        UUID correlationId,
        String action,
        String objectType,
        UUID objectId,
        String outcome,
        String reasonCode,
        UUID scopeCareerId,
        UUID scopeProcessId) {
    public static AuditEventView from(AuditQueryPort.AuditEventView view) {
        return new AuditEventView(
                view.id(),
                view.occurredAt(),
                view.correlationId(),
                view.action(),
                view.objectType(),
                view.objectId(),
                view.outcome(),
                view.reasonCode(),
                view.scopeCareerId(),
                view.scopeProcessId());
    }
}
