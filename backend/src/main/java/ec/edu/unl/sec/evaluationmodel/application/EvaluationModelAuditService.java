package ec.edu.unl.sec.evaluationmodel.application;

import ec.edu.unl.sec.audit.application.AuditEventCommand;
import ec.edu.unl.sec.audit.application.AuditPort;
import java.time.Clock;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EvaluationModelAuditService {
    private static final Set<String> ACTIONS = Set.of("MODEL_CREATED","MODEL_VERSION_CREATED","MODEL_DRAFT_UPDATED",
            "MODEL_PUBLISHED","MODEL_SUCCESSOR_CREATED","MODEL_VERSION_READ","MODEL_VERSIONS_COMPARED","MODEL_OPERATION_REJECTED");
    private final AuditPort audit;
    private final Clock clock;
    @Autowired public EvaluationModelAuditService(AuditPort audit){this(audit,Clock.systemUTC());}
    EvaluationModelAuditService(AuditPort audit,Clock clock){this.audit=audit;this.clock=clock;}
    public UUID record(UUID actor,UUID correlation,String action,String type,UUID object,Map<String,Object> metadata){
        if(!ACTIONS.contains(action)) throw new IllegalArgumentException("unsupported model audit action");
        return audit.record(new AuditEventCommand(clock.instant(),actor,null,correlation==null?UUID.randomUUID():correlation,
                action,type,object,null,null,"SUCCESS",null,sanitize(metadata)));
    }
    public UUID reject(UUID actor,UUID correlation,UUID object,String reason){
        return audit.recordRejection(new AuditEventCommand(clock.instant(),actor,null,correlation==null?UUID.randomUUID():correlation,
                "MODEL_OPERATION_REJECTED","MODEL_VERSION",object,null,null,"REJECTED",reason,Map.of()));
    }
    private static Map<String,Object> sanitize(Map<String,Object> input){return input==null?Map.of():input.entrySet().stream()
            .filter(e->!e.getKey().toLowerCase().matches(".*(secret|token|password|hash).*"))
            .collect(java.util.stream.Collectors.toUnmodifiableMap(Map.Entry::getKey,Map.Entry::getValue));}
}
