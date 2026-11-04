package ec.edu.unl.sec.evidence.application;

import ec.edu.unl.sec.audit.application.AuditEventCommand;
import ec.edu.unl.sec.audit.application.AuditPort;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class EvidenceAuditService {
    private final AuditPort audit;

    public EvidenceAuditService(AuditPort audit) {
        this.audit = audit;
    }

    public void record(String action, UUID actorId, UUID processId, UUID correlationId, String objectType,
            UUID objectId, String outcome, String reason, Map<String, String> details) {
        if (!EvidenceAuditPolicy.ALLOWED.contains(action)) {
            action = "EVIDENCE_AUDIT";
        }
        var safe = EvidenceAuditPolicy.sanitize(details);
        var map = new java.util.HashMap<String, Object>();
        if (safe != null) {
            safe.forEach(map::put);
        }
        audit.record(new AuditEventCommand(
                Instant.now(),
                actorId,
                null,
                correlationId,
                action,
                objectType,
                objectId,
                null,
                processId,
                outcome == null ? "SUCCESS" : outcome,
                reason,
                map));
    }
}
