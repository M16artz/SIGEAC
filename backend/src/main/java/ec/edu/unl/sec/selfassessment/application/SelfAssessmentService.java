package ec.edu.unl.sec.selfassessment.application;

import ec.edu.unl.sec.evaluationmodel.application.ModelApplicabilityService;
import ec.edu.unl.sec.evaluationmodel.application.ModelCatalogPort;
import ec.edu.unl.sec.identityaccess.application.AuthorizationPort;
import ec.edu.unl.sec.identityaccess.application.CurrentActorPort;
import ec.edu.unl.sec.identityaccess.application.InstitutionalAuthorityPort;
import ec.edu.unl.sec.identityaccess.domain.AuthorizationScope;
import ec.edu.unl.sec.identityaccess.domain.OperationCode;
import ec.edu.unl.sec.selfassessment.domain.ProcessState;
import ec.edu.unl.sec.selfassessment.domain.ProcessAuthorization;
import ec.edu.unl.sec.selfassessment.domain.SelfAssessmentProcess;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SelfAssessmentService {
    private final SelfAssessmentRepository repository; private final ModelCatalogPort models;
    private final CurrentActorPort actors; private final AuthorizationPort authorization;
    private final SelfAssessmentAuditService audit;
    private final InstitutionalAuthorityPort authority;
    private final CareerAdministrationService careers;
    private final ModelApplicabilityService applicability;
    @Autowired
    public SelfAssessmentService(SelfAssessmentRepository repository, ModelCatalogPort models, CurrentActorPort actors,
            AuthorizationPort authorization, SelfAssessmentAuditService audit, InstitutionalAuthorityPort authority,
            CareerAdministrationService careers, ModelApplicabilityService applicability) { this.repository=repository;this.models=models;this.actors=actors;this.authorization=authorization;this.audit=audit;this.authority=authority;this.careers=careers;this.applicability=applicability; }
    public SelfAssessmentService(SelfAssessmentRepository repository, ModelCatalogPort models, CurrentActorPort actors,
            AuthorizationPort authorization, SelfAssessmentAuditService audit, InstitutionalAuthorityPort authority,
            CareerAdministrationService careers) { this(repository,models,actors,authorization,audit,authority,careers,null); }

    @Transactional public SelfAssessmentProcess create(UUID careerId,String code,String name,LocalDate start,LocalDate end,UUID versionId,UUID correlation) {
        UUID actor=actor(OperationCode.PROCESS_MANAGE,new AuthorizationScope(careerId,null,null));
        careers.requireActiveCareer(careerId,start,end);
        var model=models.publishedVersion(versionId).orElseThrow(()->problem(ProcessProblemException.Kind.INVALID,"MODEL_VERSION_NOT_PUBLISHED","La versiÃ³n del modelo no estÃ¡ publicada o visible."));
        var process=new SelfAssessmentProcess(null,careerId,code,name,start,end,model.versionId(),model.versionNumber(),ProcessState.PREPARATION,0,actor,Instant.now(),List.of());
        repository.save(process,-1);audit.record(actor,correlation,"PROCESS_CREATED",process,"SUCCESS",null);return process;
    }
    @Transactional public SelfAssessmentProcess activate(UUID id,long expected,UUID correlation) {
        var current=current();var process=find(id);
        if(current.technical() || !authorization.isAllowed(current.identityId(),OperationCode.PROCESS_MANAGE,new AuthorizationScope(process.careerId(),process.id(),null)))
            throw problem(ProcessProblemException.Kind.NOT_FOUND,"PROCESS_NOT_FOUND","El proceso no existe o no estÃ¡ visible.");
        UUID actor=current.identityId();check(process,expected);
        careers.requireActiveCareer(process.careerId(),process.periodStart(),process.periodEnd());
        if (applicability != null) applicability.assertUsableForActivation(process.modelVersionId(), process.careerId(), process.periodStart());
        if (!process.inPreparation()) throw problem(ProcessProblemException.Kind.CONFLICT,"INVALID_PROCESS_TRANSITION","Solo se puede activar un proceso en preparaciÃ³n.");
        if (process.authorization() == null) throw problem(ProcessProblemException.Kind.CONFLICT,
                "AUTHORIZATION_MISSING", "El proceso permanece en preparaciÃ³n hasta registrar una autorizaciÃ³n institucional verificada.");
        var model=models.publishedVersion(process.modelVersionId()).orElseThrow(()->problem(ProcessProblemException.Kind.CONFLICT,"MODEL_VERSION_UNAVAILABLE","La versiÃ³n fijada ya no estÃ¡ disponible."));
        var snapshot=model.indicators().stream().map(i->new SelfAssessmentProcess.IndicatorSnapshot(i.id(),i.code(),i.name())).toList();
        if (snapshot.isEmpty()) throw problem(ProcessProblemException.Kind.CONFLICT,"MODEL_HAS_NO_INDICATORS","El modelo no contiene indicadores configurados.");
        var active=process.activate(snapshot);repository.save(active,expected);repository.recordTransition(id,process.state(),active.state(),actor,"ACTIVATION",correlation,active.revision());audit.record(actor,correlation,"PROCESS_ACTIVATED",active,"SUCCESS",null);return active;
    }
    @Transactional
    public SelfAssessmentProcess authorize(UUID id, long expected, UUID assignmentId, String legalBasis,
            String documentReference, String reason, UUID correlation) {
        var actor = current();
        var process = find(id);
        var scope = new AuthorizationScope(process.careerId(), id, null);
        if (actor.technical() || !authorization.isAllowed(actor.identityId(), OperationCode.PROCESS_AUTHORIZE, scope))
            throw problem(ProcessProblemException.Kind.NOT_FOUND,"PROCESS_NOT_FOUND","El proceso no existe o no estÃ¡ visible.");
        if (actor.technical() || !authority.isAuthorizedAssignment(assignmentId, actor.identityId(), OperationCode.PROCESS_AUTHORIZE, scope))
            throw problem(ProcessProblemException.Kind.FORBIDDEN, "PROCESS_AUTHORITY_DENIED", "La asignaciÃ³n indicada no autoriza esta decisiÃ³n.");
        check(process, expected);
        if (!process.inPreparation() || process.authorization() != null)
            throw problem(ProcessProblemException.Kind.CONFLICT, "AUTHORIZATION_ALREADY_FIXED", "La autorizaciÃ³n ya estÃ¡ fijada o el proceso no estÃ¡ en preparaciÃ³n.");
        var decision = new ProcessAuthorization(null, assignmentId, legalBasis, documentReference, reason, actor.identityId(), Instant.now());
        var changed = process.authorize(decision);
        repository.save(changed, expected);
        repository.saveAuthorization(id, decision);
        audit.record(actor.identityId(), correlation, "PROCESS_AUTHORIZATION_RECORDED", changed, "SUCCESS", null);
        return changed;
    }
    @Transactional(readOnly=true) public List<SelfAssessmentProcess> list(){var current=current();return repository.list().stream().filter(p->allowed(current,p)).map(p->visibleSnapshot(current,p)).toList();}
    @Transactional(readOnly=true) public SelfAssessmentProcess get(UUID id){var actor=current();var p=find(id);if(!allowed(actor,p))throw problem(ProcessProblemException.Kind.NOT_FOUND,"PROCESS_NOT_FOUND","El proceso no existe o no estÃ¡ visible.");return visibleSnapshot(actor,p);}
    private CurrentActorPort.Actor current(){return actors.currentActor().orElseThrow(()->problem(ProcessProblemException.Kind.UNAUTHORIZED,"AUTHENTICATION_REQUIRED","Se requiere autenticaciÃ³n."));}
    private boolean manages(CurrentActorPort.Actor actor, SelfAssessmentProcess p) {
        var scope = new AuthorizationScope(p.careerId(), p.id(), null);
        return authorization.isAllowed(actor.identityId(), OperationCode.PROCESS_MANAGE, scope)
                || authorization.isAllowed(actor.identityId(), OperationCode.PROCESS_COORDINATE, scope)
                || authorization.isAllowed(actor.identityId(), OperationCode.PROCESS_AUTHORIZE, scope);
    }
    private boolean canReadIndicator(CurrentActorPort.Actor actor, SelfAssessmentProcess p, UUID indicatorId) {
        var scope = new AuthorizationScope(p.careerId(), p.id(), indicatorId);
        return authorization.isAllowed(actor.identityId(), OperationCode.EVIDENCE_COLLECT, scope)
                || authorization.isAllowed(actor.identityId(), OperationCode.PROGRESS_READ, scope)
                || authorization.isAllowed(actor.identityId(), OperationCode.EVIDENCE_REVIEW, scope)
                || authorization.isAllowed(actor.identityId(), OperationCode.INDICATOR_EVALUATE, scope);
    }
    private boolean allowed(CurrentActorPort.Actor actor,SelfAssessmentProcess p){return !actor.technical() && (manages(actor,p) || p.indicators().stream().anyMatch(i->canReadIndicator(actor,p,i.id())));}
    private SelfAssessmentProcess visibleSnapshot(CurrentActorPort.Actor actor, SelfAssessmentProcess process) {
        if (manages(actor, process)) return process;
        var visible = process.indicators().stream().filter(i->canReadIndicator(actor,process,i.id())).toList();
        return new SelfAssessmentProcess(process.id(), process.careerId(), process.code(), process.name(), process.periodStart(),
                process.periodEnd(), process.modelVersionId(), process.modelVersionNumber(), process.state(), process.revision(),
                process.createdBy(), process.createdAt(), visible, null);
    }
    private UUID actor(OperationCode op,AuthorizationScope scope){var actor=current();if(actor.technical()||!authorization.isAllowed(actor.identityId(),op,scope))throw problem(ProcessProblemException.Kind.FORBIDDEN,"PROCESS_SCOPE_DENIED","La operaciÃ³n no estÃ¡ permitida.");return actor.identityId();}
    SelfAssessmentProcess find(UUID id){return repository.find(id).orElseThrow(()->problem(ProcessProblemException.Kind.NOT_FOUND,"PROCESS_NOT_FOUND","El proceso no existe o no estÃ¡ visible."));}
    static void check(SelfAssessmentProcess p,long expected){if(p.revision()!=expected)throw problem(ProcessProblemException.Kind.PRECONDITION,"STALE_PROCESS","El proceso cambiÃ³; recargue antes de continuar.");}
    static ProcessProblemException problem(ProcessProblemException.Kind k,String c,String m){return new ProcessProblemException(k,c,m);}
}
