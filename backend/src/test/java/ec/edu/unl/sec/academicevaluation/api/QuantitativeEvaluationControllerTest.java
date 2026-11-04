package ec.edu.unl.sec.academicevaluation.api;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.unl.sec.academicevaluation.application.*;
import ec.edu.unl.sec.audit.application.FailureAuditPort;
import ec.edu.unl.sec.identityaccess.application.*;
import ec.edu.unl.sec.identityaccess.infrastructure.*;
import ec.edu.unl.sec.shared.api.ApiProblemHandler;
import ec.edu.unl.sec.shared.infrastructure.CorrelationFilter;
import jakarta.servlet.http.Cookie;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class QuantitativeEvaluationControllerTest {
    final UUID process=UUID.randomUUID(),indicator=UUID.randomUUID(),actor=UUID.randomUUID(),input=UUID.randomUUID();
    final AcademicCalculationService calculations=mock(AcademicCalculationService.class);
    final VariableValueService variables=mock(VariableValueService.class);
    final FailureAuditPort failures=mock(FailureAuditPort.class);
    final String csrf="synthetic-quantitative-csrf";
    MockMvc mvc;
    String path(){return "/processes/"+process+"/indicators/"+indicator;}
    Cookie cookie(){return new Cookie("SEC_SESSION","session");}
    @BeforeEach void setup() {
        var sessions=mock(SessionService.class);
        when(sessions.authenticateForRequest("session")).thenReturn(Optional.of(new SessionService.AuthenticatedSession(
                UUID.randomUUID(),actor,"Evaluador sintético",false,SessionService.sha256Hex(csrf))));
        mvc=MockMvcBuilders.standaloneSetup(new QuantitativeEvaluationController(calculations,variables,failures,
                ()->Optional.of(new CurrentActorPort.Actor(actor,false))))
                .setControllerAdvice(new ApiProblemHandler(),new AcademicProblemHandler())
                .addFilters(new CorrelationFilter(),new SessionAuthenticationFilter(sessions,new AccessProblemWriter(new ObjectMapper().findAndRegisterModules()))).build();
    }
    @AfterEach void clear(){org.springframework.security.core.context.SecurityContextHolder.clearContext();}
    String calculation(){return "{\"inputSetId\":\""+input+"\",\"justification\":\"Justificación cuantitativa suficiente\",\"limitations\":\"Limitaciones cuantitativas declaradas\"}";}
    @Test void requiresStrongPreconditionAndCsrfForCalculation() throws Exception {
        mvc.perform(post(path()+"/quantitative-calculations").cookie(cookie()).header("X-CSRF-TOKEN",csrf).contentType(MediaType.APPLICATION_JSON).content(calculation())).andExpect(status().isPreconditionRequired());
        mvc.perform(post(path()+"/quantitative-calculations").cookie(cookie()).header("If-Match","\"1\"").contentType(MediaType.APPLICATION_JSON).content(calculation())).andExpect(status().isForbidden());
        mvc.perform(post(path()+"/quantitative-calculations").cookie(cookie()).header("X-CSRF-TOKEN",csrf).header("If-Match","W/\"1\"").contentType(MediaType.APPLICATION_JSON).content(calculation())).andExpect(status().isBadRequest());
        verifyNoInteractions(calculations);
    }
    @Test void unverifiedCalculationProduces422AndSafeRejectionAudit() throws Exception {
        when(calculations.calculate(any(),any(),any(),anyLong(),any(),any(),any())).thenThrow(new AcademicProblemException(AcademicProblemException.Kind.INVALID,"VARIABLES_UNVERIFIED","Debe verificar las variables."));
        mvc.perform(post(path()+"/quantitative-calculations").cookie(cookie()).header("X-CSRF-TOKEN",csrf).header("If-Match","\"1\"").contentType(MediaType.APPLICATION_JSON).content(calculation()))
                .andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.code").value("VARIABLES_UNVERIFIED"));
        verify(failures).recordAfterRollback(argThat(event->event.reasonCode().equals("VARIABLES_UNVERIFIED") && event.outcome().equals("REJECTED") && event.metadata().isEmpty()));
    }
    @Test void doesNotAcceptClientVerifierIdentityOrUnknownCalculationFields() throws Exception {
        mvc.perform(post(path()+"/quantitative-inputs/"+input+"/verification").cookie(cookie()).header("X-CSRF-TOKEN",csrf).header("If-Match","\"1\"")
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Verificación suficiente de datos\",\"verifiedBy\":\""+actor+"\"}" )).andExpect(status().isBadRequest());
        mvc.perform(post(path()+"/quantitative-calculations").cookie(cookie()).header("X-CSRF-TOKEN",csrf).header("If-Match","\"1\"")
                .contentType(MediaType.APPLICATION_JSON).content(calculation().replace("}",",\"strategy\":\"VALIDATED\"}"))).andExpect(status().isBadRequest());
        verifyNoInteractions(calculations,variables);
    }
    @Test void staleVerificationProduces412AndNoSilentRetry() throws Exception {
        when(variables.verify(any(),any(),any(),anyLong(),any(),any())).thenThrow(new AcademicProblemException(AcademicProblemException.Kind.PRECONDITION_FAILED,"STALE_VARIABLE_SET","Recargue las variables."));
        mvc.perform(post(path()+"/quantitative-inputs/"+input+"/verification").cookie(cookie()).header("X-CSRF-TOKEN",csrf).header("If-Match","\"1\"")
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Cifras y fuentes verificadas\"}" )).andExpect(status().isPreconditionFailed());
        verify(variables,times(1)).verify(eq(process),eq(indicator),eq(input),eq(1L),anyString(),any());
    }
}
