package ec.edu.unl.sec.evaluationmodel.api;

import ec.edu.unl.sec.audit.application.AuditQueryPort;
import ec.edu.unl.sec.identityaccess.application.AuthorizationPort;
import ec.edu.unl.sec.identityaccess.application.CurrentActorPort;
import ec.edu.unl.sec.identityaccess.domain.OperationCode;
import ec.edu.unl.sec.evaluationmodel.application.ModelProblemException;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController @RequestMapping
public class ModelAuditController {
    private final CurrentActorPort actors;private final AuthorizationPort authorization;private final AuditQueryPort queries;
    public ModelAuditController(CurrentActorPort actors,AuthorizationPort authorization,AuditQueryPort queries){this.actors=actors;this.authorization=authorization;this.queries=queries;}
    @GetMapping("/model-audit-events") public Page search(@RequestParam(required=false) UUID modelId,@RequestParam(required=false) UUID versionId,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){var actor=actors.currentActor().orElseThrow(()->new ModelProblemException(ModelProblemException.Kind.UNAUTHORIZED,"AUTHENTICATION_REQUIRED","Se requiere autenticación."));if(actor.technical()||!authorization.hasOperation(actor.identityId(),OperationCode.MODEL_AUDIT_READ))throw new ModelProblemException(ModelProblemException.Kind.FORBIDDEN,"MODEL_AUDIT_DENIED","La auditoría no está permitida.");int safePage=Math.max(0,page),safeSize=Math.min(100,Math.max(1,size));UUID object=versionId!=null?versionId:modelId;String type=versionId!=null?"MODEL_VERSION":modelId!=null?"EVALUATION_MODEL":null;return new Page(queries.search(List.of(),true,type,object,safePage*safeSize,safeSize),safePage,safeSize,queries.count(List.of(),true,type,object));}
    public record Page(List<AuditQueryPort.AuditEventView> items,int page,int size,long total){}
}
