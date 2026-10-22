package ec.edu.unl.sec.selfassessment.api;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import ec.edu.unl.sec.selfassessment.application.*;
import ec.edu.unl.sec.selfassessment.domain.*;
import ec.edu.unl.sec.shared.api.ApiProblemHandler;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
class CareerControllerTest {
    CareerAdministrationService service; MockMvc mvc;
    @BeforeEach void setup() {service=mock(CareerAdministrationService.class);mvc=MockMvcBuilders.standaloneSetup(new CareerController(service)).setControllerAdvice(new CareerCatalogProblemHandler(),new ApiProblemHandler()).build();}
    @Test void returnsThePlannedNestedDtoWithStrongRevision() throws Exception {
        UUID id=UUID.randomUUID();var career=new Career(id,new Career.Data("C","Carrera",UUID.randomUUID(),LocalDate.now(),null),Career.Status.ACTIVE,1,UUID.randomUUID(),Instant.now(),false);
        when(service.getCareer(id)).thenReturn(career);
        mvc.perform(get("/careers/"+id)).andExpect(status().isOk()).andExpect(header().string("ETag","\"1\""))
                .andExpect(jsonPath("$.data.code").value("C")).andExpect(jsonPath("$.status").value("ACTIVE"));
    }
    @Test void requiresEtagBeforeUpdatingAndNeverExposesADeleteOperation() throws Exception {
        String path="/careers/"+UUID.randomUUID();
        mvc.perform(patch(path).contentType("application/json").content("{\"code\":\"C\",\"name\":\"Carrera\",\"academicUnitId\":\""+UUID.randomUUID()+"\",\"validFrom\":\"2026-01-01\"}"))
                .andExpect(status().isPreconditionRequired());
        mvc.perform(delete(path)).andExpect(status().isMethodNotAllowed()); verifyNoInteractions(service);
    }
    @Test void rejectsExtraFieldsRatherThanTrustingClientIdentityOrRevision() throws Exception {
        mvc.perform(post("/academic-units").contentType("application/json").content("{\"code\":\"U\",\"name\":\"Unidad\",\"createdBy\":\"forged\"}"))
                .andExpect(status().isBadRequest());verifyNoInteractions(service);
    }
}
