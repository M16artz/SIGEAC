package ec.edu.unl.sec.academicevaluation.application;

import ec.edu.unl.sec.academicevaluation.domain.*;
import ec.edu.unl.sec.academicevaluation.domain.AcademicReconciliation.*;
import ec.edu.unl.sec.audit.application.*;
import ec.edu.unl.sec.evaluationmodel.application.ModelCatalogPort;
import ec.edu.unl.sec.evaluationmodel.domain.IndicatorDefinition;
import ec.edu.unl.sec.identityaccess.application.*;
import ec.edu.unl.sec.identityaccess.domain.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AcademicReconciliationService {
    private final AcademicEvaluationService academic;private final ModelCatalogPort models;
    private final ManualEvaluationRepository manual;private final QuantitativeRepository quantitative;
    private final ReconciliationRepository repository;private final AuthorizationPort authorization;
    private final InstitutionalAuthorityPort authority;private final AuditPort audit;
    public AcademicReconciliationService(AcademicEvaluationService academic,ModelCatalogPort models,ManualEvaluationRepository manual,
            QuantitativeRepository quantitative,ReconciliationRepository repository,AuthorizationPort authorization,
            InstitutionalAuthorityPort authority,AuditPort audit) {
        this.academic=academic;this.models=models;this.manual=manual;this.quantitative=quantitative;this.repository=repository;
        this.authorization=authorization;this.authority=authority;this.audit=audit;
    }
    @Transactional(readOnly=true)
    public View view(UUID p,UUID i,int page,int casePage,int size) {
        if(page<0 || casePage<0 || size<1 || size>100) throw invalid("INVALID_PAGE","Página o tamaño inválidos.");
        var scope=academic.scope(p,i,false);var source=source(scope,i);var policy=repository.policy(p,i).orElse(null);
        var latest=repository.latest(p,i).orElse(null);var resolution=latest==null?null:repository.resolution(latest.id()).orElse(null);
        String status=status(source.current(),latest,resolution,policy);
        var history=history(source,scope,i,(long)page*size,size+1);
        var cases=repository.history(p,i,(long)casePage*size,size+1);
        var caseViews=cases.stream().limit(size).map(d->new CaseView(d,repository.resolution(d.id()).orElse(null),
                latest!=null && latest.id().equals(d.id()) && "RESOLVED".equals(status))).toList();
        return new View(p,i,scope.actorId(),scope.process().state(),allowed(scope,i),status,
                "RESOLVED".equals(status)?resolution.selectedJudgement():null,source.current(),policy,latest,resolution,
                history.stream().limit(size).toList(),page,history.size()>size,caseViews,casePage,cases.size()>size,size);
    }
    @Transactional
    public Policy configure(UUID p,UUID i,long expected,PolicyCommand command,UUID correlation) {
        var scope=academic.scope(p,i,true);requireAuthority(scope,i,command.authorityAssignmentId());
        var previous=repository.policy(p,i).orElse(null);
        if(expected!=(previous==null?0:previous.revision())) throw stale();
        Policy policy;
        try {policy=new Policy(UUID.randomUUID(),p,i,expected+1,previous==null?null:previous.id(),command.procedureReference(),command.method(),
                command.legalBasis(),command.documentReference(),command.authorityAssignmentId(),scope.actorId(),Instant.now());}
        catch(IllegalArgumentException ex){throw invalid("INVALID_RECONCILIATION_POLICY","Registre procedimiento, método soportado, fundamento y constancia válidos.");}
        repository.append(policy);audit(scope,policy.id(),"ACADEMIC_RECONCILIATION_POLICY_RECORDED",correlation);return policy;
    }
    @Transactional
    public Disagreement open(UUID p,UUID i,long expected,String reason,UUID correlation) {
        var scope=academic.scope(p,i,true);var source=source(scope,i);var previous=repository.latest(p,i).orElse(null);
        if(expected!=(previous==null?0:previous.revision())) throw stale();
        var current=source.current();
        if(current.size()<2 || current.stream().map(Judgement::authorId).distinct().count()<2)
            throw invalid("DISAGREEMENT_REQUIRES_TWO_EVALUATORS","Se requieren juicios de al menos dos evaluadores distintos.");
        var fingerprint=AcademicReconciliation.fingerprint(current);
        if(previous!=null && previous.fingerprint().equals(fingerprint)) {
            var resolution=repository.resolution(previous.id()).orElse(null);
            if(resolution==null) throw conflict("DISAGREEMENT_ALREADY_OPEN","La discrepancia actual ya está registrada.");
            if(repository.policy(p,i).map(Policy::id).filter(resolution.policy().id()::equals).isPresent() && current.stream().allMatch(j->j.flags().isEmpty()))
                throw conflict("DISAGREEMENT_ALREADY_RESOLVED","La discrepancia actual ya tiene una resolución aplicable.");
        }
        Disagreement disagreement;
        try {disagreement=new Disagreement(UUID.randomUUID(),p,i,expected+1,previous==null?null:previous.id(),fingerprint,current,reason,scope.actorId(),Instant.now());}
        catch(IllegalArgumentException ex){throw invalid("INVALID_DISAGREEMENT","La discrepancia exige fundamento de 10 a 4000 caracteres.");}
        repository.append(disagreement);audit(scope,disagreement.id(),"ACADEMIC_DISAGREEMENT_RECORDED",correlation);return disagreement;
    }
    @Transactional
    public Resolution resolve(UUID p,UUID i,UUID id,long expected,ResolutionCommand command,UUID correlation) {
        var scope=academic.scope(p,i,true);requireAuthority(scope,i,command.authorityAssignmentId());
        var disagreement=repository.latest(p,i).filter(d->d.id().equals(id) && d.revision()==expected).orElseThrow(AcademicReconciliationService::stale);
        if(repository.resolution(id).isPresent()) throw conflict("DISAGREEMENT_ALREADY_RESOLVED","La resolución existente es inmutable.");
        var policy=repository.policy(p,i).orElseThrow(()->invalid("RECONCILIATION_POLICY_REQUIRED","Registre primero el procedimiento autorizado."));
        if(!policy.id().equals(command.policyId())) throw stale();
        var current=source(scope,i).current();
        if(!disagreement.fingerprint().equals(AcademicReconciliation.fingerprint(current))) throw stale();
        if(current.stream().anyMatch(j->!j.flags().isEmpty())) throw conflict("JUDGEMENTS_REQUIRE_REVIEW","Hay juicios o sustentos que requieren revisión; no se puede consolidar.");
        var selected=current.stream().filter(j->j.id().equals(command.selectedJudgementId())).findFirst()
                .orElseThrow(()->invalid("INVALID_SELECTED_JUDGEMENT","Seleccione un juicio vigente incluido en la discrepancia."));
        var participants=command.participants();var required=current.stream().map(Judgement::authorId).collect(Collectors.toSet());required.add(scope.actorId());
        if(participants==null || participants.size()!=required.size() || !required.equals(new HashSet<>(participants)))
            throw invalid("INVALID_RECONCILIATION_PARTICIPANTS","Identifique a los evaluadores participantes y a la persona que resuelve.");
        Resolution resolution;
        try {resolution=new Resolution(UUID.randomUUID(),p,i,id,policy,selected,participants,command.reason(),command.authorityAssignmentId(),scope.actorId(),Instant.now(),disagreement.fingerprint());}
        catch(IllegalArgumentException ex){throw invalid("INVALID_RECONCILIATION_RESOLUTION","La resolución exige fundamento de 10 a 4000 caracteres.");}
        repository.append(resolution);audit(scope,resolution.id(),"ACADEMIC_RECONCILIATION_RESOLVED",correlation);return resolution;
    }
    private Source source(AcademicEvaluationService.Scope scope,UUID i) {
        var model=models.publishedVersion(scope.process().modelVersionId()).orElseThrow(()->invalid("MODEL_UNAVAILABLE","No se encontró el modelo fijado."));
        var definition=Optional.ofNullable(model.configuration()).stream().flatMap(c->c.indicators().stream()).filter(d->d.nodeId().equals(i)).findFirst()
                .orElseThrow(()->invalid("INDICATOR_CONFIGURATION_REQUIRED","El indicador no tiene configuración académica."));
        var accepted=academic.accepted(scope.process(),i);boolean qualitative=definition.nature()==IndicatorDefinition.Nature.QUALITATIVE;
        List<Judgement> current=qualitative?manual.current(scope.process().id(),i).stream().map(v->manual(v,accepted,Set.of(v.id()),definition)).toList()
                :quantitative.current(scope.process().id(),i).stream().map(v->quantitative(v,accepted,Set.of(v.id()),definition)).toList();
        return new Source(qualitative,definition,accepted,current.stream().sorted(Comparator.comparing(Judgement::id)).toList());
    }
    private List<Judgement> history(Source source,AcademicEvaluationService.Scope scope,UUID i,long offset,int limit) {
        var ids=source.current().stream().map(Judgement::id).collect(Collectors.toSet());
        return source.qualitative()?manual.history(scope.process().id(),i,offset,limit).stream().map(v->manual(v,source.accepted(),ids,source.definition())).toList()
                :quantitative.history(scope.process().id(),i,offset,limit).stream().map(v->quantitative(v,source.accepted(),ids,source.definition())).toList();
    }
    private Judgement manual(ManualAcademicEvaluation v,List<ManualAcademicEvaluation.Support> accepted,Set<UUID> current,IndicatorDefinition d) {
        var flags=flags(v.id(),v.ruleId(),v.scaleId(),v.supports(),accepted,current,d);
        return new Judgement("MANUAL",v.id(),v.evaluatorId(),v.modelVersionId(),v.ruleId(),v.scaleId(),v.ratingOptionId(),v.ratingLabel(),null,
                v.method(),v.justification(),v.limitations(),v.evaluatedAt(),v.supports(),flags);
    }
    private Judgement quantitative(QuantitativeEvaluation.Run v,List<ManualAcademicEvaluation.Support> accepted,Set<UUID> current,IndicatorDefinition d) {
        var supports=v.inputs().variables().stream().flatMap(x->x.supports().stream()).distinct().toList();
        var flags=flags(v.id(),v.rule().id(),v.scale().id(),supports,accepted,current,d);
        if(!quantitative.latest(v.inputs().processId(),v.inputs().indicatorId(),v.inputs().capturedBy()).map(QuantitativeEvaluation.Inputs::id).filter(v.inputs().id()::equals).isPresent()) flags.add("INPUTS_SUPERSEDED");
        return new Judgement("QUANTITATIVE",v.id(),v.executedBy(),v.inputs().modelVersionId(),v.rule().id(),v.scale().id(),v.ratingOptionId(),v.ratingLabel(),v.result().toPlainString(),
                v.rule().methodDefinition(),v.justification(),v.limitations(),v.executedAt(),supports,flags);
    }
    private static ArrayList<String> flags(UUID id,UUID rule,UUID scale,List<ManualAcademicEvaluation.Support> supports,
            List<ManualAcademicEvaluation.Support> accepted,Set<UUID> current,IndicatorDefinition d) {
        var flags=new ArrayList<String>();
        if(!current.contains(id)) flags.add("SUPERSEDED");
        if(!Objects.equals(rule,d.valuationRuleId()) || !Objects.equals(scale,d.ratingScaleId())) flags.add("RULE_MISMATCH");
        if(supports.isEmpty() || !accepted.containsAll(supports)) flags.add("SUPPORTS_INVALIDATED");return flags;
    }
    private boolean allowed(AcademicEvaluationService.Scope scope,UUID i) {
        return authorization.isAllowed(scope.actorId(),OperationCode.ACADEMIC_RECONCILE,new AuthorizationScope(scope.process().careerId(),scope.process().id(),i));
    }
    private void requireAuthority(AcademicEvaluationService.Scope scope,UUID i,UUID assignment) {
        if(!allowed(scope,i) || assignment==null || !authority.isAuthorizedAssignment(assignment,scope.actorId(),OperationCode.ACADEMIC_RECONCILE,
                new AuthorizationScope(scope.process().careerId(),scope.process().id(),i)))
            throw new AcademicProblemException(AcademicProblemException.Kind.FORBIDDEN,"RECONCILIATION_AUTHORITY_DENIED","Se requiere una asignación vigente competente para conciliar este indicador.");
    }
    private static String status(List<Judgement> current,Disagreement latest,Resolution resolution,Policy policy) {
        if(current.isEmpty()) return "NO_EVALUATION";
        if(current.stream().anyMatch(j->!j.flags().isEmpty())) return "REQUIRES_REVIEW";
        if(latest!=null && resolution!=null && policy!=null && resolution.policy().id().equals(policy.id())
                && resolution.fingerprint().equals(AcademicReconciliation.fingerprint(current))) return "RESOLVED";
        if(latest!=null || current.stream().map(j->j.ratingOptionId()+":"+j.result()).distinct().count()>1) return "PENDING_RECONCILIATION";
        return "UNCONSOLIDATED";
    }
    private void audit(AcademicEvaluationService.Scope scope,UUID id,String action,UUID correlation) {
        audit.record(new AuditEventCommand(Instant.now(),scope.actorId(),null,correlation,action,"ACADEMIC_RECONCILIATION",id,
                scope.process().careerId(),scope.process().id(),"SUCCESS",null,Map.of()));
    }
    private static AcademicProblemException invalid(String code,String message){return new AcademicProblemException(AcademicProblemException.Kind.INVALID,code,message);}
    private static AcademicProblemException conflict(String code,String message){return new AcademicProblemException(AcademicProblemException.Kind.CONFLICT,code,message);}
    private static AcademicProblemException stale(){return new AcademicProblemException(AcademicProblemException.Kind.PRECONDITION_FAILED,"STALE_RECONCILIATION","La discrepancia, los juicios o el procedimiento cambiaron; recargue antes de continuar.");}
    private record Source(boolean qualitative,IndicatorDefinition definition,List<ManualAcademicEvaluation.Support> accepted,List<Judgement> current) {}
    public record PolicyCommand(String procedureReference,String method,String legalBasis,String documentReference,UUID authorityAssignmentId) {}
    public record ResolutionCommand(UUID policyId,UUID selectedJudgementId,List<UUID> participants,String reason,UUID authorityAssignmentId) {}
    public record CaseView(Disagreement disagreement,Resolution resolution,boolean applicable) {}
    public record View(UUID processId,UUID indicatorId,UUID actorId,String processState,boolean canReconcile,String status,Judgement result,
            List<Judgement> current,Policy policy,Disagreement latestCase,Resolution resolution,List<Judgement> history,int page,boolean hasNext,
            List<CaseView> cases,int casePage,boolean hasNextCase,int size) {}
}
