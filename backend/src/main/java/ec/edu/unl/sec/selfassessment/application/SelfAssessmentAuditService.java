package ec.edu.unl.sec.selfassessment.application;
import ec.edu.unl.sec.audit.application.*;
import ec.edu.unl.sec.selfassessment.domain.SelfAssessmentProcess;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
@Service
public class SelfAssessmentAuditService {
    private final AuditPort audit;
    public SelfAssessmentAuditService(AuditPort audit) { this.audit=audit; }
    public void record(UUID actor, UUID correlation, String action, SelfAssessmentProcess process, String outcome, String reason) {
        var command=new AuditEventCommand(Instant.now(),actor,null,correlation==null?UUID.randomUUID():correlation,
                action,"SELF_ASSESSMENT_PROCESS",process.id(),process.careerId(),process.id(),outcome,reason,Map.of());
        if("SUCCESS".equals(outcome)) audit.record(command); else audit.recordRejection(command);
    }
}
