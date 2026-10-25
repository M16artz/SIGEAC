package ec.edu.unl.sec.academicevaluation.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import ec.edu.unl.sec.academicevaluation.domain.*;
import ec.edu.unl.sec.academicevaluation.domain.AcademicReconciliation.*;
import ec.edu.unl.sec.identityaccess.application.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;

class AcademicReconciliationServiceTest {
    final AcademicEvaluationServiceTest base=new AcademicEvaluationServiceTest();
    final QuantitativeRepository quantitative=mock(QuantitativeRepository.class);
    final ReconciliationRepository repository=mock(ReconciliationRepository.class);
    final InstitutionalAuthorityPort authority=mock(InstitutionalAuthorityPort.class);
    AcademicReconciliationService service;ManualAcademicEvaluation first,second;
    final UUID assignment=UUID.randomUUID(),secondActor=UUID.randomUUID();
    @BeforeEach void setup() {
        base.setup();var otherOption=UUID.randomUUID();
        var model=base.models.publishedVersion(base.model).orElseThrow();
        var options=new ArrayList<>(base.scale.options());options.add(new ec.edu.unl.sec.evaluationmodel.domain.RatingScale.Option(otherOption,"OTHER","Otro juicio",2,null));
        var scale=new ec.edu.unl.sec.evaluationmodel.domain.RatingScale(base.scale.id(),base.scale.code(),base.scale.name(),options);
        when(base.models.publishedVersion(base.model)).thenReturn(Optional.of(new ec.edu.unl.sec.evaluationmodel.application.ModelCatalogPort.PublishedModel(
                model.modelId(),model.versionId(),model.versionNumber(),model.indicators(),scale,model.formula(),
                new ec.edu.unl.sec.evaluationmodel.domain.ModelConfiguration(model.configuration().indicators(),List.of(scale),model.configuration().rules()))));
        first=base.save(0,null);
        second=new ManualAcademicEvaluation(UUID.randomUUID(),base.process,base.indicator,base.model,base.rule.id(),base.scale.id(),otherOption,
                "Otro juicio","Método cualitativo sintético","Limitaciones de segundo juicio","Segundo fundamento académico distinto",first.supports(),secondActor,Instant.now(),1,null);
        when(base.repository.current(base.process,base.indicator)).thenReturn(List.of(first,second));
        when(base.repository.history(eq(base.process),eq(base.indicator),anyLong(),anyInt())).thenReturn(List.of(first,second));
        when(repository.policy(any(),any())).thenReturn(Optional.empty());when(repository.latest(any(),any())).thenReturn(Optional.empty());
        when(repository.resolution(any())).thenReturn(Optional.empty());when(repository.history(any(),any(),anyLong(),anyInt())).thenReturn(List.of());
        when(authority.isAuthorizedAssignment(any(),any(),any(),any())).thenReturn(true);
        doAnswer(call->{var p=(Policy)call.getArgument(0);when(repository.policy(base.process,base.indicator)).thenReturn(Optional.of(p));return null;}).when(repository).append(any(Policy.class));
        doAnswer(call->{var d=(Disagreement)call.getArgument(0);when(repository.latest(base.process,base.indicator)).thenReturn(Optional.of(d));return null;}).when(repository).append(any(Disagreement.class));
        doAnswer(call->{var r=(Resolution)call.getArgument(0);when(repository.resolution(r.disagreementId())).thenReturn(Optional.of(r));return null;}).when(repository).append(any(Resolution.class));
        service=new AcademicReconciliationService(base.service,base.models,base.repository,quantitative,repository,base.auth,authority,base.audit);
    }
    Policy policy(){return service.configure(base.process,base.indicator,0,new AcademicReconciliationService.PolicyCommand("Procedimiento sintético, versión 1",AcademicReconciliation.METHOD,
            "Fundamento de competencia sintético.","fixture://reconciliation-policy",assignment),UUID.randomUUID());}
    Disagreement open(){return service.open(base.process,base.indicator,0,"Diferencia explícita entre dos fundamentos académicos.",UUID.randomUUID());}
    Resolution resolve(Disagreement d,Policy p){return service.resolve(base.process,base.indicator,d.id(),d.revision(),new AcademicReconciliationService.ResolutionCommand(
            p.id(),first.id(),List.of(base.actor,secondActor),"Selección fundamentada conforme al procedimiento registrado.",assignment),UUID.randomUUID());}
    void rejected(String code,Runnable action){assertThatThrownBy(action::run).isInstanceOf(AcademicProblemException.class).extracting("code").isEqualTo(code);}
    @Test void recordsBothJudgementsAndAuthorizedResolutionWithoutEditingThem() {
        var p=policy();var d=open();assertThat(d.judgements()).extracting(Judgement::justification).contains(first.justification(),second.justification());
        var r=resolve(d,p);assertThat(r.selectedJudgement().id()).isEqualTo(first.id());
        var view=service.view(base.process,base.indicator,0,0,20);assertThat(view.status()).isEqualTo("RESOLVED");assertThat(view.result().id()).isEqualTo(first.id());
        assertThat(first.revision()).isEqualTo(1);assertThat(second.revision()).isEqualTo(1);
        verify(base.repository,times(1)).append(any());
    }
    @Test void cannotResolveWithoutConfiguredProcedureOrCompetentAssignment() {
        var d=open();rejected("RECONCILIATION_POLICY_REQUIRED",()->service.resolve(base.process,base.indicator,d.id(),1,
                new AcademicReconciliationService.ResolutionCommand(UUID.randomUUID(),first.id(),List.of(base.actor,secondActor),"Fundamento suficiente de resolución.",assignment),UUID.randomUUID()));
        when(authority.isAuthorizedAssignment(any(),any(),any(),any())).thenReturn(false);rejected("RECONCILIATION_AUTHORITY_DENIED",()->policy());
    }
    @Test void evaluatorsCanReadAndOpenButCannotGrantThemselvesResolutionAuthority() {
        when(base.auth.isAllowed(any(),eq(ec.edu.unl.sec.identityaccess.domain.OperationCode.ACADEMIC_RECONCILE),any())).thenReturn(false);
        assertThat(service.view(base.process,base.indicator,0,0,20).canReconcile()).isFalse();open();rejected("RECONCILIATION_AUTHORITY_DENIED",()->policy());
    }
    @Test void newJudgementMakesResolutionInapplicableAndPreservesOldDecision() {
        var p=policy();var d=open();var r=resolve(d,p);
        var successor=new ManualAcademicEvaluation(UUID.randomUUID(),second.processId(),second.indicatorId(),second.modelVersionId(),second.ruleId(),second.scaleId(),second.ratingOptionId(),
                second.ratingLabel(),second.method(),second.limitations(),"Fundamento de corrección de segundo evaluador.",second.supports(),second.evaluatorId(),Instant.now(),2,second.id());
        when(base.repository.current(base.process,base.indicator)).thenReturn(List.of(first,successor));
        assertThat(service.view(base.process,base.indicator,0,0,20).status()).isEqualTo("PENDING_RECONCILIATION");
        assertThat(service.view(base.process,base.indicator,0,0,20).result()).isNull();assertThat(repository.resolution(d.id())).contains(r);
    }
    @Test void invalidSupportBlocksResolutionAndFlagsHistoryWithoutDeletingJudgements() {
        var p=policy();var d=open();base.outcome("OBSERVED");
        var v=service.view(base.process,base.indicator,0,0,20);assertThat(v.status()).isEqualTo("REQUIRES_REVIEW");
        assertThat(v.history()).allSatisfy(j->assertThat(j.flags()).contains("SUPPORTS_INVALIDATED"));
        rejected("JUDGEMENTS_REQUIRE_REVIEW",()->resolve(d,p));
    }
    @Test void rejectsStaleRevisionMissingParticipantsAndForeignSelectedJudgement() {
        var p=policy();var d=open();rejected("DISAGREEMENT_ALREADY_OPEN",()->service.open(base.process,base.indicator,1,"Otra solicitud sobre la misma discrepancia.",UUID.randomUUID()));
        rejected("INVALID_RECONCILIATION_PARTICIPANTS",()->service.resolve(base.process,base.indicator,d.id(),1,
                new AcademicReconciliationService.ResolutionCommand(p.id(),first.id(),List.of(base.actor),"Fundamento suficiente de decisión.",assignment),UUID.randomUUID()));
        rejected("STALE_RECONCILIATION",()->service.resolve(base.process,base.indicator,d.id(),0,
                new AcademicReconciliationService.ResolutionCommand(p.id(),first.id(),List.of(base.actor,secondActor),"Fundamento suficiente de decisión.",assignment),UUID.randomUUID()));
        rejected("INVALID_SELECTED_JUDGEMENT",()->service.resolve(base.process,base.indicator,d.id(),1,
                new AcademicReconciliationService.ResolutionCommand(p.id(),UUID.randomUUID(),List.of(base.actor,secondActor),"Fundamento suficiente de decisión.",assignment),UUID.randomUUID()));
    }
    @Test void twoJudgementsFromDifferentPeopleAreRequired() {
        when(base.repository.current(base.process,base.indicator)).thenReturn(List.of(first));rejected("DISAGREEMENT_REQUIRES_TWO_EVALUATORS",()->open());
        assertThat(service.view(base.process,base.indicator,0,0,20).status()).isEqualTo("UNCONSOLIDATED");
    }
    @Test void changingProcedureInvalidatesApplicabilityWithoutEditingOldResolution() {
        var p=policy();var d=open();var r=resolve(d,p);
        var next=service.configure(base.process,base.indicator,1,new AcademicReconciliationService.PolicyCommand("Procedimiento sintético, versión 2",AcademicReconciliation.METHOD,
                "Nueva constancia de competencia sintética.","fixture://policy-2",assignment),UUID.randomUUID());
        assertThat(next.predecessorId()).isEqualTo(p.id());assertThat(repository.resolution(d.id())).contains(r);
        assertThat(service.view(base.process,base.indicator,0,0,20).result()).isNull();
        assertThat(service.open(base.process,base.indicator,1,"Nueva conciliación conforme al procedimiento actualizado.",UUID.randomUUID()).predecessorId()).isEqualTo(d.id());
    }
    @Test void resolutionIsSingleAndClosedOrForeignScopeCannotWrite() {
        var p=policy();var d=open();resolve(d,p);rejected("DISAGREEMENT_ALREADY_RESOLVED",()->resolve(d,p));
        base.state("CLOSED");rejected("ACADEMIC_PHASE_CLOSED",()->service.open(base.process,base.indicator,1,"Nueva solicitud que no está permitida.",UUID.randomUUID()));
        when(base.auth.isAllowed(any(),any(),any())).thenReturn(false);rejected("RESOURCE_NOT_FOUND",()->service.view(base.process,base.indicator,0,0,20));
    }
    @Test void quantitativeHistoryFlagsNewInputsAndInvalidatedDocumentarySupport() {
        var q=new AcademicCalculationServiceTest();q.setup();var inputs=q.capture();q.verify(inputs);var run=q.calculate(inputs);
        var service=new AcademicReconciliationService(q.base.service,q.base.models,q.base.repository,q.store,repository,q.base.auth,authority,q.base.audit);
        assertThat(service.view(q.base.process,q.base.indicator,0,0,20).status()).isEqualTo("UNCONSOLIDATED");
        q.variables.capture(q.base.process,q.base.indicator,1,q.command("2","4",inputs.id()),UUID.randomUUID());
        var changed=service.view(q.base.process,q.base.indicator,0,0,20);
        assertThat(changed.status()).isEqualTo("REQUIRES_REVIEW");assertThat(changed.history().getFirst().flags()).contains("INPUTS_SUPERSEDED");
        q.base.outcome("REJECTED");assertThat(service.view(q.base.process,q.base.indicator,0,0,20).history().getFirst().flags()).contains("SUPPORTS_INVALIDATED");
        assertThat(q.store.run(inputs.id())).contains(run);
    }
}
