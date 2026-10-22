package ec.edu.unl.sec.evaluationmodel.application;

import static org.assertj.core.api.Assertions.*;import static org.mockito.Mockito.*;
import ec.edu.unl.sec.evaluationmodel.domain.*;import ec.edu.unl.sec.evaluationmodel.support.*;import ec.edu.unl.sec.identityaccess.application.*;import java.time.Instant;import java.util.*;import org.junit.jupiter.api.Test;
class ModelComparisonServiceTest {
 @Test void detectsChangesToIndicatorRulesAndScalesEvenWhenTheTreeDoesNotChange(){
  var fixture=ConfiguredModelFixtures.first();var configuration=fixture.configuration();var old=ModelFixtures.draft();
  var from=new ModelVersion(null,old.modelId(),1,ModelVersionStatus.PUBLISHED,fixture.source(),old.authorId(),ModelFixtures.APPROVER,null,0,old.createdAt(),Instant.now(),fixture.nodes(),null,null,configuration);
  var rule=configuration.rules().getLast();var changedRule=new ValuationRule(rule.id(),rule.indicatorNodeId(),rule.ruleType(),rule.methodDefinition()+" corregido",rule.formulaDefinition(),rule.strategyKey(),rule.strategyVersion(),rule.scaleId(),rule.variables(),rule.thresholds(),rule.missingDataPolicy(),rule.status());
  var scale=configuration.scales().getFirst();var option=scale.options().getFirst();var changedScale=new RatingScale(scale.id(),scale.code(),scale.name(),List.of(new RatingScale.Option(option.id(),option.code(),"Etiqueta corregida",option.position(),option.declaredValue()),scale.options().getLast()));
  var changed=new ModelConfiguration(configuration.indicators(),List.of(changedScale,configuration.scales().getLast()),List.of(configuration.rules().getFirst(),changedRule));
  var to=new ModelVersion(null,from.modelId(),2,ModelVersionStatus.PUBLISHED,from.source(),from.authorId(),ModelFixtures.APPROVER,from.id(),0,from.createdAt(),Instant.now(),from.criteria(),null,null,changed);
  var repo=new InMemoryEvaluationModelRepository();repo.saveVersion(from,-1);repo.saveVersion(to,-1);
  CurrentActorPort actors=()->Optional.of(new CurrentActorPort.Actor(ModelFixtures.AUTHOR,false));var auth=mock(AuthorizationPort.class);when(auth.hasOperation(any(),any())).thenReturn(true);
  var result=new ModelComparisonService(repo,actors,auth,mock(EvaluationModelAuditService.class)).compare(from.id(),to.id(),UUID.randomUUID());
  assertThat(result.changes()).filteredOn(c->c.key().startsWith("INDICATOR:")).hasSize(2).allMatch(c->c.kind()==ModelComparisonService.Kind.MODIFIED);
  assertThat(repo.findVersion(from.id())).contains(from);
 }
 @Test void detectsReparentingAtAnyDepthWithoutChangingTheV1ComparisonShape(){
  var repo=new InMemoryEvaluationModelRepository();var original=ModelFixtures.draft();
  var indicator=new ModelNode(null,"I-1","Indicador","",1,List.of(),"INDICATOR");
  var rootsA=List.of(new ModelNode(null,"C-1","Criterio","",1,List.of(new ModelNode(null,"G-1","Grupo","",1,List.of(indicator),"GROUP"),new ModelNode(null,"G-2","Grupo","",2,List.of(),"GROUP")),"CRITERION"));
  var rootsB=List.of(new ModelNode(null,"C-1","Criterio","",1,List.of(new ModelNode(null,"G-1","Grupo","",1,List.of(),"GROUP"),new ModelNode(null,"G-2","Grupo","",2,List.of(indicator),"GROUP")),"CRITERION"));
  var from=original.replaceDraft(original.source(),rootsA,original.scale(),original.formula());
  var to=new ModelVersion(null,from.modelId(),2,ModelVersionStatus.DRAFT,from.source(),from.authorId(),null,from.id(),0,from.createdAt(),null,rootsB,from.scale(),from.formula());
  repo.saveVersion(from,-1);repo.saveVersion(to,-1);
  CurrentActorPort actors=()->Optional.of(new CurrentActorPort.Actor(ModelFixtures.AUTHOR,false));var auth=mock(AuthorizationPort.class);when(auth.hasOperation(any(),any())).thenReturn(true);
  var result=new ModelComparisonService(repo,actors,auth,mock(EvaluationModelAuditService.class)).compare(from.id(),to.id(),UUID.randomUUID());
  assertThat(result.changes()).anyMatch(c->c.key().equals("INDICATOR:I-1")&&c.kind()==ModelComparisonService.Kind.MODIFIED);
  assertThat(result.changes()).filteredOn(c->c.key().equals("CRITERION:C-1")).allMatch(c->c.kind()==ModelComparisonService.Kind.UNCHANGED);
 }
 @Test void classifiesKnownChangesWithoutMutatingVersions(){var repo=new InMemoryEvaluationModelRepository();var a=ModelFixtures.draft().publish(ModelFixtures.APPROVER,Instant.now());var changedIndicator=new ModelNode(null,"IND-1","Nombre modificado","",1,List.of());var b=new ModelVersion(null,a.modelId(),2,ModelVersionStatus.PUBLISHED,a.source(),ModelFixtures.AUTHOR,ModelFixtures.APPROVER,a.id(),0,Instant.now(),Instant.now(),List.of(new ModelNode(null,"CRI-1","Criterio sintético","",1,List.of(new ModelNode(null,"EST-1","Estándar sintético","",1,List.of(changedIndicator))))),a.scale(),a.formula());repo.saveVersion(a,-1);repo.saveVersion(b,-1);CurrentActorPort actors=()->Optional.of(new CurrentActorPort.Actor(ModelFixtures.AUTHOR,false));AuthorizationPort auth=mock(AuthorizationPort.class);when(auth.hasOperation(any(),any())).thenReturn(true);var result=new ModelComparisonService(repo,actors,auth,mock(EvaluationModelAuditService.class)).compare(a.id(),b.id(),UUID.randomUUID());assertThat(result.changes()).anyMatch(c->c.key().equals("INDICATOR:IND-1")&&c.kind()==ModelComparisonService.Kind.MODIFIED);assertThat(repo.findVersion(a.id()).orElseThrow()).isEqualTo(a);}
}
