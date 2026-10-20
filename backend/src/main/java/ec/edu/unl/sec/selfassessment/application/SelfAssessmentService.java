package ec.edu.unl.sec.selfassessment.application;

import ec.edu.unl.sec.evaluationmodel.application.ModelCatalogPort;
import ec.edu.unl.sec.identityaccess.application.AuthorizationPort;
import ec.edu.unl.sec.identityaccess.application.CurrentActorPort;
import ec.edu.unl.sec.identityaccess.domain.AuthorizationScope;
import ec.edu.unl.sec.identityaccess.domain.OperationCode;
import ec.edu.unl.sec.selfassessment.domain.ProcessState;
import ec.edu.unl.sec.selfassessment.domain.SelfAssessmentProcess;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SelfAssessmentService {
    private final SelfAssessmentRepository repository; private final ModelCatalogPort models;
    private final CurrentActorPort actors; private final AuthorizationPort authorization;
    private final SelfAssessmentAuditService audit;
    public SelfAssessmentService(SelfAssessmentRepository repository, ModelCatalogPort models, CurrentActorPort actors,
            AuthorizationPort authorization, SelfAssessmentAuditService audit) { this.repository=repository;this.models=models;this.actors=actors;this.authorization=authorization;this.audit=audit; }

    @Transactional public SelfAssessmentProcess create(UUID careerId,String code,String name,LocalDate start,LocalDate end,UUID versionId,UUID correlation) {
        UUID actor=actor(OperationCode.PROCESS_MANAGE,new AuthorizationScope(careerId,null,null));
        var model=models.publishedVersion(versionId).orElseThrow(()->problem(ProcessProblemException.Kind.INVALID,"MODEL_VERSION_NOT_PUBLISHED","La versión del modelo no está publicada o visible."));
        var process=new SelfAssessmentProcess(null,careerId,code,name,start,end,model.versionId(),model.versionNumber(),ProcessState.DRAFT,0,actor,Instant.now(),List.of());
        repository.save(process,-1);audit.record(actor,correlation,"PROCESS_CREATED",process.id(),"SUCCESS",null);return process;
    }
    @Transactional public SelfAssessmentProcess activate(UUID id,long expected,UUID correlation) {
        var process=find(id);UUID actor=actor(OperationCode.PROCESS_MANAGE,new AuthorizationScope(process.careerId(),process.id(),null));check(process,expected);
        var model=models.publishedVersion(process.modelVersionId()).orElseThrow(()->problem(ProcessProblemException.Kind.CONFLICT,"MODEL_VERSION_UNAVAILABLE","La versión fijada ya no está disponible."));
        var snapshot=model.indicators().stream().map(i->new SelfAssessmentProcess.IndicatorSnapshot(i.id(),i.code(),i.name())).toList();
        var active=process.activate(snapshot);repository.save(active,expected);repository.recordTransition(id,process.state(),active.state(),actor,"ACTIVATION",correlation,active.revision());audit.record(actor,correlation,"PROCESS_ACTIVATED",id,"SUCCESS",null);return active;
    }
    @Transactional(readOnly=true) public List<SelfAssessmentProcess> list(){var current=current();return repository.list().stream().filter(p->allowed(current,p)).toList();}
    @Transactional(readOnly=true) public SelfAssessmentProcess get(UUID id){var p=find(id);if(!allowed(current(),p))throw problem(ProcessProblemException.Kind.FORBIDDEN,"PROCESS_SCOPE_DENIED","La operación no está permitida.");return p;}
    private CurrentActorPort.Actor current(){return actors.currentActor().orElseThrow(()->problem(ProcessProblemException.Kind.UNAUTHORIZED,"AUTHENTICATION_REQUIRED","Se requiere autenticación."));}
    private boolean allowed(CurrentActorPort.Actor actor,SelfAssessmentProcess p){if(actor.technical())return false;var scope=new AuthorizationScope(p.careerId(),p.id(),null);return authorization.isAllowed(actor.identityId(),OperationCode.PROCESS_MANAGE,scope)||authorization.isAllowed(actor.identityId(),OperationCode.PROCESS_COORDINATE,scope);}
    private UUID actor(OperationCode op,AuthorizationScope scope){var actor=current();if(actor.technical()||!authorization.isAllowed(actor.identityId(),op,scope))throw problem(ProcessProblemException.Kind.FORBIDDEN,"PROCESS_SCOPE_DENIED","La operación no está permitida.");return actor.identityId();}
    SelfAssessmentProcess find(UUID id){return repository.find(id).orElseThrow(()->problem(ProcessProblemException.Kind.NOT_FOUND,"PROCESS_NOT_FOUND","El proceso no existe o no está visible."));}
    static void check(SelfAssessmentProcess p,long expected){if(p.revision()!=expected)throw problem(ProcessProblemException.Kind.PRECONDITION,"STALE_PROCESS","El proceso cambió; recargue antes de continuar.");}
    static ProcessProblemException problem(ProcessProblemException.Kind k,String c,String m){return new ProcessProblemException(k,c,m);}
}
