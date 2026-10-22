package ec.edu.unl.sec.evaluationmodel.application;

import ec.edu.unl.sec.evaluationmodel.domain.*;
import ec.edu.unl.sec.identityaccess.application.AuthorizationPort;
import ec.edu.unl.sec.identityaccess.application.CurrentActorPort;
import ec.edu.unl.sec.identityaccess.domain.OperationCode;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EvaluationModelService {
    private final EvaluationModelRepository repository;
    private final CurrentActorPort actors;
    private final AuthorizationPort authorization;
    private final EvaluationModelProperties properties;
    private final EvaluationModelAuditService audit;
    private final Clock clock;
    private final ValidatedStrategyPort strategies;
    public EvaluationModelService(EvaluationModelRepository repository,CurrentActorPort actors,AuthorizationPort authorization,
            EvaluationModelProperties properties,EvaluationModelAuditService audit){this(repository,actors,authorization,properties,audit,java.util.Set::of);}
    @Autowired public EvaluationModelService(EvaluationModelRepository repository,CurrentActorPort actors,AuthorizationPort authorization,
            EvaluationModelProperties properties,EvaluationModelAuditService audit,ValidatedStrategyPort strategies){this(repository,actors,authorization,properties,audit,Clock.systemUTC(),strategies);}
    EvaluationModelService(EvaluationModelRepository repository,CurrentActorPort actors,AuthorizationPort authorization,
            EvaluationModelProperties properties,EvaluationModelAuditService audit,Clock clock){this(repository,actors,authorization,properties,audit,clock,java.util.Set::of);}
    private EvaluationModelService(EvaluationModelRepository repository,CurrentActorPort actors,AuthorizationPort authorization,
            EvaluationModelProperties properties,EvaluationModelAuditService audit,Clock clock,ValidatedStrategyPort strategies){this.repository=repository;this.actors=actors;this.authorization=authorization;this.properties=properties;this.audit=audit;this.clock=clock;this.strategies=strategies;}

    @Transactional public EvaluationModel createModel(String name,String description,UUID correlation){
        UUID actor=actor(OperationCode.MODEL_MANAGE); EvaluationModel model=new EvaluationModel(null,name,description,actor,clock.instant());
        repository.saveModel(model); audit.record(actor,correlation,"MODEL_CREATED","EVALUATION_MODEL",model.id(),Map.of()); return model;
    }
    @Transactional(readOnly=true) public List<EvaluationModel> listModels(){actor(OperationCode.MODEL_READ);return repository.listModels();}
    @Transactional public ModelVersion createDraft(UUID modelId,String source,List<ModelNode> criteria,RatingScale scale,ProgressFormula formula,UUID correlation){
        UUID actor=actor(OperationCode.MODEL_MANAGE); if(repository.findModel(modelId).isEmpty()) throw notFound();
        ModelVersion version=new ModelVersion(null,modelId,repository.nextVersionNumber(modelId),ModelVersionStatus.DRAFT,source,actor,null,null,0,clock.instant(),null,criteria,scale,formula);
        requireLegacy(version); repository.saveVersion(version,-1); audit.record(actor,correlation,"MODEL_VERSION_CREATED","MODEL_VERSION",version.id(),Map.of("versionNumber",version.versionNumber())); return version;
    }
    @Transactional(readOnly=true) public List<ModelVersion> listVersions(UUID modelId){actor(OperationCode.MODEL_READ);if(repository.findModel(modelId).isEmpty())throw notFound();return repository.listVersions(modelId);}
    @Transactional public ModelVersion createDraft(UUID modelId,String source,List<ModelNode> nodes,ModelConfiguration configuration,UUID correlation){
        UUID actor=actor(OperationCode.MODEL_MANAGE);if(repository.findModel(modelId).isEmpty())throw notFound();
        if(configuration==null)throw configurationRequired();
        ModelVersion version=new ModelVersion(null,modelId,repository.nextVersionNumber(modelId),ModelVersionStatus.DRAFT,source,actor,null,null,0,clock.instant(),null,nodes,null,null,configuration);
        repository.saveVersion(version,-1);audit.record(actor,correlation,"MODEL_VERSION_CREATED","MODEL_VERSION",version.id(),Map.of("versionNumber",version.versionNumber()));return version;
    }
    @Transactional public ModelVersion get(UUID id,UUID correlation){UUID actor=actor(OperationCode.MODEL_READ);ModelVersion v=find(id);audit.record(actor,correlation,"MODEL_VERSION_READ","MODEL_VERSION",id,Map.of());return v;}
    @Transactional public ModelVersion replace(UUID id,long expected,String source,List<ModelNode> criteria,RatingScale scale,ProgressFormula formula,UUID correlation){
        UUID actor=actor(OperationCode.MODEL_MANAGE); ModelVersion current=find(id); checkRevision(current,expected); requireLegacy(current);
        try{ModelVersion changed=current.replaceDraft(source,criteria,scale,formula);requireLegacy(changed);repository.saveVersion(changed,expected);audit.record(actor,correlation,"MODEL_DRAFT_UPDATED","MODEL_VERSION",id,Map.of());return changed;}
        catch(IllegalStateException immutable){audit.reject(actor,correlation,id,"MODEL_VERSION_IMMUTABLE");throw problem(ModelProblemException.Kind.CONFLICT,"MODEL_VERSION_IMMUTABLE","La versión publicada es inmutable.");}
    }
    @Transactional public ModelVersion publish(UUID id,long expected,UUID approver,UUID correlation){
        return publish(id,expected,approver,correlation,false);
    }
    @Transactional public ModelVersion publishConfigured(UUID id,long expected,UUID approver,UUID correlation){
        return publish(id,expected,approver,correlation,true);
    }
    @Transactional public ModelVersion replaceConfigured(UUID id,long expected,String source,List<ModelNode> nodes,ModelConfiguration configuration,UUID correlation){
        UUID actor=actor(OperationCode.MODEL_MANAGE);ModelVersion current=find(id);checkRevision(current,expected);
        if(current.configuration()==null||configuration==null)throw configurationRequired();
        if(current.status()!=ModelVersionStatus.DRAFT){audit.reject(actor,correlation,id,"MODEL_VERSION_IMMUTABLE");throw problem(ModelProblemException.Kind.CONFLICT,"MODEL_VERSION_IMMUTABLE","La versión publicada es inmutable.");}
        var changed=current.replaceConfiguration(source,nodes,configuration);repository.saveVersion(changed,expected);
        audit.record(actor,correlation,"MODEL_DRAFT_UPDATED","MODEL_VERSION",id,Map.of());return changed;
    }
    private ModelVersion publish(UUID id,long expected,UUID approver,UUID correlation,boolean configured){
        UUID actor=actor(OperationCode.MODEL_PUBLISH); ModelVersion current=find(id);checkRevision(current,expected);
        if(configured){if(current.configuration()==null)throw configurationRequired();}else requireLegacy(current);
        if(!actor.equals(approver)) throw problem(ModelProblemException.Kind.FORBIDDEN,"APPROVER_IDENTITY_MISMATCH","El aprobador debe ser la identidad autenticada.");
        if(current.status()!=ModelVersionStatus.DRAFT){audit.reject(actor,correlation,id,"MODEL_VERSION_IMMUTABLE");throw problem(ModelProblemException.Kind.CONFLICT,"MODEL_VERSION_IMMUTABLE","La versión publicada es inmutable.");}
        List<String> issues=PublicationValidator.validate(current,approver,properties.formulaSchema(),java.util.Set.copyOf(strategies.validatedStrategies()));
        if(!issues.isEmpty()){audit.reject(actor,correlation,id,"MODEL_PUBLICATION_INVALID");throw new PublicationRejected(issues);}
        ModelVersion published=current.publish(approver,clock.instant()); repository.saveVersion(published,expected);
        if(current.predecessorId()!=null){repository.findVersion(current.predecessorId()).filter(v->v.status()==ModelVersionStatus.PUBLISHED).ifPresent(previous->repository.saveVersion(previous.supersede(),previous.revision()));}
        audit.record(actor,correlation,"MODEL_PUBLISHED","MODEL_VERSION",id,Map.of("versionNumber",published.versionNumber()));return published;
    }
    private UUID actor(OperationCode operation){var current=actors.currentActor().orElseThrow(()->problem(ModelProblemException.Kind.UNAUTHORIZED,"AUTHENTICATION_REQUIRED","Se requiere autenticación."));if(current.technical()||!authorization.hasOperation(current.identityId(),operation))throw problem(ModelProblemException.Kind.FORBIDDEN,"MODEL_SCOPE_DENIED","La operación no está permitida.");return current.identityId();}
    private ModelVersion find(UUID id){return repository.findVersion(id).orElseThrow(EvaluationModelService::notFound);}
    private static ModelProblemException notFound(){return problem(ModelProblemException.Kind.NOT_FOUND,"MODEL_VERSION_NOT_FOUND","El recurso no existe o no está visible.");}
    private static void checkRevision(ModelVersion v,long expected){if(v.revision()!=expected)throw problem(ModelProblemException.Kind.PRECONDITION,"STALE_MODEL_VERSION","La versión cambió; recargue antes de continuar.");}
    public static void requireLegacy(ModelVersion version){
        if(version.configuration()!=null||ModelHierarchy.inspect(version.criteria()).entries().stream().anyMatch(e->e.node().nodeType()!=null))
            throw problem(ModelProblemException.Kind.CONFLICT,"MODEL_VERSION_REQUIRES_V2","La estructura de esta versión requiere el contrato de modelos v2.");
    }
    private static ModelProblemException configurationRequired(){return problem(ModelProblemException.Kind.INVALID,"MODEL_CONFIGURATION_REQUIRED","Se requiere la configuración por indicador.");}
    private static ModelProblemException problem(ModelProblemException.Kind kind,String code,String message){return new ModelProblemException(kind,code,message);}
    public static final class PublicationRejected extends ModelProblemException { private final List<String> issues; public PublicationRejected(List<String> issues){super(Kind.INVALID,"MODEL_PUBLICATION_INVALID","La versión no cumple todas las condiciones de publicación.");this.issues=List.copyOf(issues);}public List<String> issues(){return issues;}}
}
