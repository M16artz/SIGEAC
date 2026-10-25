package ec.edu.unl.sec.academicevaluation.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import ec.edu.unl.sec.academicevaluation.domain.ManualAcademicEvaluation;
import ec.edu.unl.sec.audit.application.*;
import ec.edu.unl.sec.documentreview.application.DocumentReviewProgressPort;
import ec.edu.unl.sec.evaluationmodel.application.ModelCatalogPort;
import ec.edu.unl.sec.evaluationmodel.domain.*;
import ec.edu.unl.sec.evidence.application.*;
import ec.edu.unl.sec.identityaccess.application.*;
import ec.edu.unl.sec.selfassessment.application.DocumentaryProcessPort;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AcademicEvaluationServiceTest {
    final UUID process=UUID.randomUUID(), indicator=UUID.randomUUID(), actor=UUID.randomUUID(), model=UUID.randomUUID(),
            career=UUID.randomUUID(), source=UUID.randomUUID(), version=UUID.randomUUID(), decision=UUID.randomUUID();
    final CurrentActorPort actors=mock(CurrentActorPort.class);
    final AuthorizationPort auth=mock(AuthorizationPort.class);
    final DocumentaryProcessPort processes=mock(DocumentaryProcessPort.class);
    final ModelCatalogPort models=mock(ModelCatalogPort.class);
    final RequiredSourceCatalogPort catalog=mock(RequiredSourceCatalogPort.class);
    final EvidenceProgressPort evidence=mock(EvidenceProgressPort.class);
    final DocumentReviewProgressPort reviews=mock(DocumentReviewProgressPort.class);
    final ManualEvaluationRepository repository=mock(ManualEvaluationRepository.class);
    final AuditPort audit=mock(AuditPort.class);
    final AcademicEvaluationService service=new AcademicEvaluationService(actors,auth,processes,models,catalog,evidence,reviews,repository,audit);
    final RatingScale scale=new RatingScale(null,"SCALE","Escala sintética",List.of(new RatingScale.Option(null,"OK","Cumple",1,null)));
    final ValuationRule rule=new ValuationRule(null,indicator,ValuationRule.Type.MANUAL_QUALITATIVE,"Juicio cualitativo sintético",
            null,null,null,scale.id(),List.of(),List.of(),"Registrar limitaciones",ValuationRule.Status.VALIDATED);

    @BeforeEach void setup() {
        when(actors.currentActor()).thenReturn(Optional.of(new CurrentActorPort.Actor(actor,false)));
        when(auth.hasOperation(any(),any())).thenReturn(true);
        when(auth.isAllowed(any(),any(),any())).thenReturn(true);
        state("DOCUMENTARY_CLOSED");
        model(rule,IndicatorDefinition.Nature.QUALITATIVE);
        when(catalog.model(model)).thenReturn(Optional.of(new RequiredSourceCatalogPort.ModelSources(model,List.of(
                new RequiredSourceCatalogPort.Entry(source,indicator,"I1","F1","Fuente","EXTERNAL_LINK",true,1,1)))));
        when(evidence.applicability(process)).thenReturn(new EvidenceProgressPort.Applicability(Set.of(),Set.of()));
        when(evidence.currentCoverages(process,Set.of(source))).thenReturn(List.of(new EvidenceProgressPort.CurrentCoverage(UUID.randomUUID(),version,source)));
        outcome("ACCEPTED");
        when(repository.latest(any(),any(),any())).thenReturn(Optional.empty());
        when(repository.history(any(),any(),anyLong(),anyInt())).thenReturn(List.of());
    }
    void state(String state) {
        var context=new DocumentaryProcessPort.Context(process,career,model,state,7,List.of(indicator));
        when(processes.context(process)).thenReturn(Optional.of(context));
        when(processes.lock(process)).thenReturn(Optional.of(context));
    }
    void model(ValuationRule rule,IndicatorDefinition.Nature nature) {
        var definition=new IndicatorDefinition(indicator,nature,"Estándar sintético",List.of(),"Período sintético",scale.id(),rule.id(),List.of());
        when(models.publishedVersion(model)).thenReturn(Optional.of(new ModelCatalogPort.PublishedModel(UUID.randomUUID(),model,1,
                List.of(new ModelCatalogPort.Indicator(indicator,"I1","Indicador")),scale,null,
                new ModelConfiguration(List.of(definition),List.of(scale),List.of(rule)))));
    }
    void outcome(String outcome) { when(reviews.latestOutcomes(Set.of(version))).thenReturn(List.of(new DocumentReviewProgressPort.Outcome(version,source,decision,outcome))); }
    AcademicEvaluationService.Command command(UUID predecessor) { return new AcademicEvaluationService.Command(scale.options().getFirst().id(),List.of(version),
            "Justificación académica sintética.","Sin limitaciones adicionales declaradas.",predecessor); }
    ManualAcademicEvaluation save(long revision,UUID predecessor) { return service.evaluate(process,indicator,revision,command(predecessor),UUID.randomUUID()); }
    void rejected(String code,Runnable action) { assertThatThrownBy(action::run).isInstanceOf(AcademicProblemException.class).extracting("code").isEqualTo(code); }

    @Test void savesManualJudgementWithExactRuleScaleAndDecisionWithoutChangingProcess() {
        var value=save(0,null);
        assertThat(value.ruleId()).isEqualTo(rule.id());
        assertThat(value.method()).isEqualTo(rule.methodDefinition());
        assertThat(value.supports()).containsExactly(new ManualAcademicEvaluation.Support(version,source,decision));
        assertThat(value.revision()).isEqualTo(1);
        verify(repository).append(value); verify(audit).record(any(AuditEventCommand.class));
        verify(processes,never()).reserveSnapshot(any(),anyLong());
        verify(processes,never()).completeClosure(any(),anyLong(),any(),any(),any());
    }
    @Test void correctionAppendsOnlyOwnPredecessor() {
        var prior=save(0,null); when(repository.latest(process,indicator,actor)).thenReturn(Optional.of(prior));
        var next=save(1,prior.id());
        assertThat(next.revision()).isEqualTo(2); assertThat(next.predecessorId()).isEqualTo(prior.id());
        assertThat(prior.revision()).isEqualTo(1); verify(repository).append(prior); verify(repository).append(next);
    }
    @Test void staleOrForeignPredecessorCannotOverwriteJudgement() {
        var prior=save(0,null); when(repository.latest(process,indicator,actor)).thenReturn(Optional.of(prior));
        rejected("STALE_EVALUATION",()->save(0,null));
        rejected("STALE_EVALUATION",()->save(1,UUID.randomUUID()));
        verify(repository,times(1)).append(any());
    }
    @ParameterizedTest @ValueSource(strings={"OBSERVED","REJECTED","PENDING_REVIEW"})
    void rejectsUnacceptedSupports(String outcome) { outcome(outcome); rejected("INVALID_SUPPORTS",()->save(0,null)); verify(repository,never()).append(any()); }
    @Test void rejectsForeignOrSupersededVersionsAndDuplicateSupports() {
        rejected("INVALID_SUPPORTS",()->service.evaluate(process,indicator,0,new AcademicEvaluationService.Command(
                scale.options().getFirst().id(),List.of(UUID.randomUUID()),"Justificación suficiente","Limitaciones suficientes",null),UUID.randomUUID()));
        rejected("INVALID_SUPPORTS",()->service.evaluate(process,indicator,0,new AcademicEvaluationService.Command(
                scale.options().getFirst().id(),List.of(version,version),"Justificación suficiente","Limitaciones suficientes",null),UUID.randomUUID()));
    }
    @Test void excludedIndicatorsDoNotSupplySupport() {
        when(evidence.applicability(process)).thenReturn(new EvidenceProgressPort.Applicability(Set.of(indicator),Set.of()));
        rejected("INVALID_SUPPORTS",()->save(0,null));
    }
    @Test void rejectsTechnicalAccountBeforeReadingProcess() {
        when(actors.currentActor()).thenReturn(Optional.of(new CurrentActorPort.Actor(actor,true)));
        rejected("EVALUATION_DENIED",()->save(0,null)); verify(processes,never()).context(any());
    }
    @Test void hidesForeignScopeBeforeLockOrHistory() {
        when(auth.isAllowed(any(),any(),any())).thenReturn(false);
        rejected("RESOURCE_NOT_FOUND",()->save(0,null)); rejected("RESOURCE_NOT_FOUND",()->service.view(process,indicator,0,20));
        verify(processes,never()).lock(any()); verifyNoInteractions(repository);
    }
    @ParameterizedTest @ValueSource(strings={"DRAFT","PREPARATION","SUSPENDED","CLOSING","CLOSED"})
    void rejectsInactiveAcademicPhases(String state) { state(state); rejected("ACADEMIC_PHASE_CLOSED",()->save(0,null)); verify(repository,never()).append(any()); }
    @Test void rejectsUnvalidatedOrQuantitativeRule() {
        model(new ValuationRule(rule.id(),indicator,rule.ruleType(),rule.methodDefinition(),null,null,null,scale.id(),List.of(),List.of(),"Limitaciones",ValuationRule.Status.DRAFT),IndicatorDefinition.Nature.QUALITATIVE);
        rejected("MANUAL_RULE_UNAVAILABLE",()->save(0,null));
        model(rule,IndicatorDefinition.Nature.QUANTITATIVE); rejected("MANUAL_RULE_UNAVAILABLE",()->save(0,null));
    }
    @Test void validatesScaleAndTexts() {
        rejected("INVALID_RATING",()->service.evaluate(process,indicator,0,new AcademicEvaluationService.Command(UUID.randomUUID(),List.of(version),"Justificación suficiente","Limitaciones suficientes",null),UUID.randomUUID()));
        rejected("INVALID_EVALUATION",()->service.evaluate(process,indicator,0,new AcademicEvaluationService.Command(scale.options().getFirst().id(),List.of(version),"corta","",null),UUID.randomUUID()));
    }
    @Test void historyReturnsOwnRevisionAndOnlyOnePage() {
        var value=save(0,null); when(repository.latest(process,indicator,actor)).thenReturn(Optional.of(value));
        when(repository.history(process,indicator,0,2)).thenReturn(List.of(value,value));
        var page=service.view(process,indicator,0,1);
        assertThat(page.hasNext()).isTrue(); assertThat(page.items()).hasSize(1); assertThat(page.ownLatestId()).isEqualTo(value.id());
        assertThat(page.acceptedSupports()).hasSize(1); assertThat(page.method()).isEqualTo(rule.methodDefinition());
        rejected("INVALID_PAGE",()->service.view(process,indicator,-1,1));
    }
}
