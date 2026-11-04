package ec.edu.unl.sec.evaluationmodel.application;

import ec.edu.unl.sec.evaluationmodel.domain.*;
import ec.edu.unl.sec.identityaccess.application.AuthorizationPort;
import ec.edu.unl.sec.identityaccess.application.CurrentActorPort;
import ec.edu.unl.sec.identityaccess.domain.OperationCode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service public class ModelComparisonService {
    private final EvaluationModelRepository repository;private final CurrentActorPort actors;private final AuthorizationPort auth;private final EvaluationModelAuditService audit;
    public ModelComparisonService(EvaluationModelRepository repository,CurrentActorPort actors,AuthorizationPort auth,EvaluationModelAuditService audit){this.repository=repository;this.actors=actors;this.auth=auth;this.audit=audit;}
    @Transactional public Comparison compare(UUID fromId,UUID toId,UUID correlation){var actor=actors.currentActor().orElseThrow(()->problem(ModelProblemException.Kind.UNAUTHORIZED,"AUTHENTICATION_REQUIRED","Se requiere autenticación."));if(actor.technical()||!auth.hasOperation(actor.identityId(),OperationCode.MODEL_READ))throw problem(ModelProblemException.Kind.FORBIDDEN,"MODEL_SCOPE_DENIED","La operación no está permitida.");var from=repository.findVersion(fromId).orElseThrow(ModelComparisonService::hidden);var to=repository.findVersion(toId).orElseThrow(ModelComparisonService::hidden);if(!from.modelId().equals(to.modelId()))throw problem(ModelProblemException.Kind.CONFLICT,"MODEL_COMPARISON_MISMATCH","Las versiones pertenecen a modelos distintos.");Map<String,Location> left=flatten(from),right=flatten(to);List<Change> changes=new ArrayList<>();java.util.LinkedHashSet<String> keys=new java.util.LinkedHashSet<>(left.keySet());keys.addAll(right.keySet());for(String key:keys){Location a=left.get(key),b=right.get(key);Kind kind=a==null?Kind.ADDED:b==null?Kind.REMOVED:Objects.equals(a,b)?Kind.UNCHANGED:Kind.MODIFIED;changes.add(new Change(key,kind,a==null?null:a.item(),b==null?null:b.item()));}audit.record(actor.identityId(),correlation,"MODEL_VERSIONS_COMPARED","MODEL_VERSION",toId,Map.of("from",fromId.toString()));return new Comparison(fromId,toId,List.copyOf(changes));}
    private static Map<String,Location> flatten(ModelVersion version){
        Map<String,Location> out=new LinkedHashMap<>();
        Map<UUID,String> keys=new LinkedHashMap<>();
        for(var entry:ModelHierarchy.inspect(version.criteria()).entries()){
            var node=entry.node();String key=entry.nodeType()+":"+node.code();
            var configuration=version.configuration();
            var definition=configuration==null?null:configuration.indicators().stream().filter(i->i.nodeId().equals(node.id())).findFirst().orElse(null);
            var scale=definition==null?null:configuration.scales().stream().filter(s->s.id().equals(definition.ratingScaleId())).findFirst().orElse(null);
            var rule=definition==null?null:configuration.rules().stream().filter(r->r.id().equals(definition.valuationRuleId())).findFirst().orElse(null);
            out.put(key,new Location(new Item(entry.nodeType(),node.code(),node.name(),node.description(),node.position()),keys.get(entry.parentId()),definition,scale,rule));
            keys.put(node.id(),key);
        }
        return out;
    }
    private record Location(Item item,String parentKey,IndicatorDefinition definition,RatingScale scale,ValuationRule rule){}
    private static ModelProblemException hidden(){return problem(ModelProblemException.Kind.NOT_FOUND,"MODEL_VERSION_NOT_FOUND","El recurso no existe o no está visible.");}
    private static ModelProblemException problem(ModelProblemException.Kind kind,String code,String message){return new ModelProblemException(kind,code,message);}
    public enum Kind{ADDED,REMOVED,MODIFIED,UNCHANGED}
    public record Item(String type,String code,String name,String description,int position){}
    public record Change(String key,Kind kind,Item from,Item to){}
    public record Comparison(UUID fromVersionId,UUID toVersionId,List<Change> changes){}
}
