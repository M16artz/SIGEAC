package ec.edu.unl.sec.evaluationmodel.application;

import ec.edu.unl.sec.evaluationmodel.domain.ModelVersion;
import ec.edu.unl.sec.evaluationmodel.domain.ModelVersionStatus;
import ec.edu.unl.sec.identityaccess.application.AuthorizationPort;
import ec.edu.unl.sec.identityaccess.application.CurrentActorPort;
import ec.edu.unl.sec.identityaccess.domain.OperationCode;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service public class ModelSuccessionService {
    private final EvaluationModelRepository repository;private final CurrentActorPort actors;private final AuthorizationPort auth;private final EvaluationModelAuditService audit;private final Clock clock=Clock.systemUTC();
    public ModelSuccessionService(EvaluationModelRepository repository,CurrentActorPort actors,AuthorizationPort auth,EvaluationModelAuditService audit){this.repository=repository;this.actors=actors;this.auth=auth;this.audit=audit;}
    @Transactional public ModelVersion create(UUID predecessorId,UUID correlation){var actor=actors.currentActor().orElseThrow(()->problem(ModelProblemException.Kind.UNAUTHORIZED,"AUTHENTICATION_REQUIRED","Se requiere autenticación."));if(actor.technical()||!auth.hasOperation(actor.identityId(),OperationCode.MODEL_MANAGE))throw problem(ModelProblemException.Kind.FORBIDDEN,"MODEL_SCOPE_DENIED","La operación no está permitida.");ModelVersion previous=repository.findVersion(predecessorId).orElseThrow(()->problem(ModelProblemException.Kind.NOT_FOUND,"MODEL_VERSION_NOT_FOUND","El recurso no existe o no está visible."));if(previous.status()==ModelVersionStatus.DRAFT)throw problem(ModelProblemException.Kind.CONFLICT,"INVALID_PREDECESSOR","Un borrador no puede ser predecesor.");ModelVersion next=previous.successor(repository.nextVersionNumber(previous.modelId()),actor.identityId(),clock.instant());repository.saveVersion(next,-1);audit.record(actor.identityId(),correlation,"MODEL_SUCCESSOR_CREATED","MODEL_VERSION",next.id(),Map.of("predecessorId",predecessorId.toString()));return next;}
    private static ModelProblemException problem(ModelProblemException.Kind kind,String code,String message){return new ModelProblemException(kind,code,message);}
}
