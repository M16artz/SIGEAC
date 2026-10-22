package ec.edu.unl.sec.selfassessment.application;
import static org.assertj.core.api.Assertions.*;import static org.mockito.Mockito.*;import ec.edu.unl.sec.evaluationmodel.application.*;import ec.edu.unl.sec.identityaccess.application.*;import ec.edu.unl.sec.selfassessment.support.*;import java.time.*;import java.util.*;import org.junit.jupiter.api.Test;
class SelfAssessmentServiceTest {
    @Test void exposesOnlyIndicatorsCoveredByTheCurrentScope() {
        var repo=new InMemorySelfAssessmentRepository(); UUID actor=UUID.randomUUID(), first=UUID.randomUUID(), second=UUID.randomUUID();
        var process=new ec.edu.unl.sec.selfassessment.domain.SelfAssessmentProcess(null,UUID.randomUUID(),"P","Proceso",LocalDate.now(),LocalDate.now(),UUID.randomUUID(),1,ec.edu.unl.sec.selfassessment.domain.ProcessState.ACTIVE,1,actor,Instant.now(),List.of(new ec.edu.unl.sec.selfassessment.domain.SelfAssessmentProcess.IndicatorSnapshot(first,"I-1","Visible"),new ec.edu.unl.sec.selfassessment.domain.SelfAssessmentProcess.IndicatorSnapshot(second,"I-2","Oculto")));
        repo.save(process,-1);
        var auth=mock(AuthorizationPort.class);
        when(auth.isAllowed(eq(actor),eq(ec.edu.unl.sec.identityaccess.domain.OperationCode.EVIDENCE_COLLECT),eq(new ec.edu.unl.sec.identityaccess.domain.AuthorizationScope(process.careerId(),process.id(),first)))).thenReturn(true);
        var service=new SelfAssessmentService(repo,mock(ModelCatalogPort.class),()->Optional.of(new CurrentActorPort.Actor(actor,false)),auth,mock(SelfAssessmentAuditService.class),mock(InstitutionalAuthorityPort.class),mock(CareerAdministrationService.class));
        assertThat(service.get(process.id()).indicators()).extracting("id").containsExactly(first);
        assertThat(service.list()).hasSize(1);
        reset(auth);
        assertThat(service.list()).isEmpty();
        assertThatThrownBy(()->service.get(process.id())).isInstanceOf(ProcessProblemException.class).extracting("code").isEqualTo("PROCESS_NOT_FOUND");
    }
    @Test void verifiesTheSpecificAuthorityAssignmentBeforeActivation() {
        var repo=new InMemorySelfAssessmentRepository();
        UUID actor=UUID.randomUUID(), career=UUID.randomUUID(), model=UUID.randomUUID(), version=UUID.randomUUID(), indicator=UUID.randomUUID(), assignment=UUID.randomUUID();
        var models=mock(ModelCatalogPort.class);
        when(models.publishedVersion(version)).thenReturn(Optional.of(new ModelCatalogPort.PublishedModel(model,version,1,List.of(new ModelCatalogPort.Indicator(indicator,"I","Indicador")),null,null)));
        var auth=mock(AuthorizationPort.class); when(auth.isAllowed(any(),any(),any())).thenReturn(true);
        var authority=mock(InstitutionalAuthorityPort.class);
        var service=new SelfAssessmentService(repo,models,()->Optional.of(new CurrentActorPort.Actor(actor,false)),auth,mock(SelfAssessmentAuditService.class),authority,mock(CareerAdministrationService.class));
        var preparation=service.create(career,"P-2","Proceso autorizado",LocalDate.now(),LocalDate.now().plusDays(10),version,UUID.randomUUID());
        assertThatThrownBy(()->service.authorize(preparation.id(),0,assignment,"Designación sintética","fixture://authorization","Inicio autorizado sintético",UUID.randomUUID()))
                .isInstanceOf(ProcessProblemException.class).extracting("code").isEqualTo("PROCESS_AUTHORITY_DENIED");
        assertThat(repo.find(preparation.id()).orElseThrow().authorization()).isNull();
        when(authority.isAuthorizedAssignment(eq(assignment),eq(actor),eq(ec.edu.unl.sec.identityaccess.domain.OperationCode.PROCESS_AUTHORIZE),any())).thenReturn(true);
        var authorized=service.authorize(preparation.id(),0,assignment,"Designación sintética","fixture://authorization","Inicio autorizado sintético",UUID.randomUUID());
        assertThat(authorized.authorization().authorityAssignmentId()).isEqualTo(assignment);
        assertThat(authorized.authorization().authorizedBy()).isEqualTo(actor);
        var active=service.activate(preparation.id(),authorized.revision(),UUID.randomUUID());
        assertThat(active.state()).isEqualTo(ec.edu.unl.sec.selfassessment.domain.ProcessState.ACTIVE);
        assertThat(active.indicators()).extracting("id").containsExactly(indicator);
        assertThat(active.authorization()).isEqualTo(authorized.authorization());
    }
    @Test void activationRequiresAnExplicitAuthorization() {
        var repo = new InMemorySelfAssessmentRepository();
        UUID actor=UUID.randomUUID(), career=UUID.randomUUID(), model=UUID.randomUUID(), version=UUID.randomUUID(), indicator=UUID.randomUUID();
        ModelCatalogPort models=mock(ModelCatalogPort.class);
        when(models.publishedVersion(version)).thenReturn(Optional.of(new ModelCatalogPort.PublishedModel(model,version,1,List.of(new ModelCatalogPort.Indicator(indicator,"I","Indicador")),null,null)));
        AuthorizationPort auth=mock(AuthorizationPort.class);
        when(auth.isAllowed(any(),any(),any())).thenReturn(true);
        var service=new SelfAssessmentService(repo,models,()->Optional.of(new CurrentActorPort.Actor(actor,false)),auth,mock(SelfAssessmentAuditService.class),mock(InstitutionalAuthorityPort.class),mock(CareerAdministrationService.class));
        var draft=service.create(career,"P-1","Proceso",LocalDate.now(),LocalDate.now().plusDays(10),version,UUID.randomUUID());
        assertThatThrownBy(() -> service.activate(draft.id(),0,UUID.randomUUID()))
                .isInstanceOf(ProcessProblemException.class)
                .satisfies(error -> assertThat(((ProcessProblemException) error).code()).isEqualTo("AUTHORIZATION_MISSING"));
        assertThat(repo.find(draft.id()).orElseThrow().state().name()).isIn("DRAFT", "PREPARATION");
    }
}
