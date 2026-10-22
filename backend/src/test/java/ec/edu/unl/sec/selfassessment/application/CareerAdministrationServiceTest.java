package ec.edu.unl.sec.selfassessment.application;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import ec.edu.unl.sec.audit.application.AuditPort;
import ec.edu.unl.sec.identityaccess.application.*;
import ec.edu.unl.sec.identityaccess.domain.*;
import ec.edu.unl.sec.selfassessment.domain.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
class CareerAdministrationServiceTest {
    CareerCatalogRepository repository; AuthorizationPort auth; CareerAdministrationService service;
    UUID actor=UUID.randomUUID(), unitId=UUID.randomUUID(), id=UUID.randomUUID(); Career career;
    @BeforeEach void setup() {
        repository=mock(CareerCatalogRepository.class); auth=mock(AuthorizationPort.class);
        when(auth.hasOperation(actor,OperationCode.CAREER_ADMIN)).thenReturn(true);
        when(repository.findUnit(unitId)).thenReturn(Optional.of(new AcademicUnit(unitId,new AcademicUnit.Data("U","Unidad sintética"),Career.Status.ACTIVE,1,actor,Instant.now())));
        career=new Career(id,new Career.Data("C","Carrera sintética",unitId,LocalDate.of(2020,1,1),null),Career.Status.ACTIVE,1,actor,Instant.now(),false);
        when(repository.findCareer(id)).thenReturn(Optional.of(career));
        service=new CareerAdministrationService(repository,()->Optional.of(new CurrentActorPort.Actor(actor,false)),auth,mock(AuditPort.class));
    }
    @Test void rejectsNormalizedDuplicatesAndStaleMutations() {
        when(repository.careerCodeExists("C",null)).thenReturn(true);
        assertThatThrownBy(()->service.createCareer(new Career.Data(" c ","Otra",unitId,LocalDate.now(),null),UUID.randomUUID())).isInstanceOf(CareerCatalogException.class).extracting("code").isEqualTo("DUPLICATE_CODE");
        assertThatThrownBy(()->service.updateCareer(id,0,career.data(),UUID.randomUUID())).isInstanceOf(CareerCatalogException.class).extracting("code").isEqualTo("STALE_REVISION");
        verify(repository,never()).saveCareer(any(),anyLong(),any(),anyString());
    }
    @Test void stateChangesAreVersionedAndAudited() {
        var disabled=service.changeCareerState(id,1,Career.Status.DISABLED,"Desactivación sintética",UUID.randomUUID());
        assertThat(disabled.status()).isEqualTo(Career.Status.DISABLED); assertThat(disabled.revision()).isEqualTo(2);
        verify(repository).saveCareer(eq(disabled),eq(1L),eq(actor),eq("Desactivación sintética"));
    }
    @Test void foreignDirectIdsAreHiddenAndCollectionsUseOnlyAuthorizedCareers() {
        when(auth.hasOperation(actor,OperationCode.CAREER_ADMIN)).thenReturn(false);
        when(auth.hasOperation(actor,OperationCode.CAREER_READ)).thenReturn(true);
        when(auth.authorizedCareerIds(actor,OperationCode.CAREER_READ)).thenReturn(Set.of(id));
        service.careers(0,20,"",null,null);
        verify(repository).careers(eq(Set.of(id)),eq(0),eq(20),eq(""),isNull(),isNull());
        assertThatThrownBy(()->service.getCareer(id)).isInstanceOf(CareerCatalogException.class).extracting("code").isEqualTo("RESOURCE_NOT_FOUND");
    }
    @Test void blocksNewProcessesForInactiveCareersAndUnitDeactivationWithActiveCareers() {
        when(repository.findCareer(id)).thenReturn(Optional.of(career.state(Career.Status.DISABLED)));
        assertThatThrownBy(()->service.requireActiveCareer(id,LocalDate.now(),LocalDate.now()))
                .isInstanceOf(CareerCatalogException.class).extracting("code").isEqualTo("CAREER_INACTIVE");
        when(repository.hasActiveCareers(unitId)).thenReturn(true);
        assertThatThrownBy(()->service.changeUnitState(unitId,1,Career.Status.DISABLED,"Motivo sintético válido",UUID.randomUUID()))
                .isInstanceOf(CareerCatalogException.class).extracting("code").isEqualTo("UNIT_HAS_ACTIVE_CAREERS");
    }
    @Test void validatesPaginationAndRejectsTechnicalIdentities() {
        assertThatThrownBy(()->service.careers(-1,20,"",null,null)).isInstanceOf(CareerCatalogException.class).extracting("code").isEqualTo("INVALID_PAGE");
        var technical=new CareerAdministrationService(repository,()->Optional.of(new CurrentActorPort.Actor(actor,true)),auth,mock(AuditPort.class));
        assertThatThrownBy(technical::access).isInstanceOf(CareerCatalogException.class).extracting("status").isEqualTo(403);
    }
}
