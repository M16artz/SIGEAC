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

class AcademicReconciliationControllerTest {
    final UUID process=UUID.randomUUID(),indicator=UUID.randomUUID(),actor=UUID.randomUUID(),id=UUID.randomUUID();
    final AcademicReconciliationService service=mock(AcademicReconciliationService.class);
    final FailureAuditPort failures=mock(FailureAuditPort.class);final String csrf="synthetic-reconciliation-csrf";
    MockMvc mvc;
    String path(){return "/processes/"+process+"/indicators/"+indicator;}
    Cookie cookie(){return new Cookie("SEC_SESSION","session");}
    String command(){return "{\"policyId\":\""+id+"\",\"selectedJudgementId\":\""+id+"\",\"participants\":[\""+actor+"\"],\"reason\":\"Fundamento suficiente de resolución\",\"authorityAssignmentId\":\""+id+"\"}";}
    @BeforeEach void setup(){
        var sessions=mock(SessionService.class);when(sessions.authenticateForRequest("session")).thenReturn(Optional.of(new SessionService.AuthenticatedSession(
                UUID.randomUUID(),actor,"Comité sintético",false,SessionService.sha256Hex(csrf))));
        mvc=MockMvcBuilders.standaloneSetup(new AcademicReconciliationController(service,failures,()->Optional.of(new CurrentActorPort.Actor(actor,false))))
                .setControllerAdvice(new ApiProblemHandler(),new AcademicProblemHandler()).addFilters(new CorrelationFilter(),
                        new SessionAuthenticationFilter(sessions,new AccessProblemWriter(new ObjectMapper().findAndRegisterModules()))).build();
    }
    @AfterEach void clear(){org.springframework.security.core.context.SecurityContextHolder.clearContext();}
    @Test void resolutionRequiresCsrfAndStrongPrecondition()throws Exception{
        var url=path()+"/academic-disagreements/"+id+"/resolution";
        mvc.perform(post(url).cookie(cookie()).header("X-CSRF-TOKEN",csrf).contentType(MediaType.APPLICATION_JSON).content(command())).andExpect(status().isPreconditionRequired());
        mvc.perform(post(url).cookie(cookie()).header("If-Match","\"1\"").contentType(MediaType.APPLICATION_JSON).content(command())).andExpect(status().isForbidden());
        mvc.perform(post(url).cookie(cookie()).header("X-CSRF-TOKEN",csrf).header("If-Match","W/\"1\"").contentType(MediaType.APPLICATION_JSON).content(command())).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void rejectsClientChosenActorAndAuditsDeniedCompetenceAfterRollback()throws Exception{
        var url=path()+"/academic-disagreements/"+id+"/resolution";
        mvc.perform(post(url).cookie(cookie()).header("X-CSRF-TOKEN",csrf).header("If-Match","\"1\"").contentType(MediaType.APPLICATION_JSON)
                .content(command().replace("}",",\"resolvedBy\":\""+actor+"\"}"))).andExpect(status().isBadRequest());
        when(service.resolve(any(),any(),any(),anyLong(),any(),any())).thenThrow(new AcademicProblemException(AcademicProblemException.Kind.FORBIDDEN,"RECONCILIATION_AUTHORITY_DENIED","Se requiere competencia vigente."));
        mvc.perform(post(url).cookie(cookie()).header("X-CSRF-TOKEN",csrf).header("If-Match","\"1\"").contentType(MediaType.APPLICATION_JSON).content(command()))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("RECONCILIATION_AUTHORITY_DENIED"));
        verify(failures).recordAfterRollback(argThat(e->e.action().equals("ACADEMIC_RECONCILIATION_REJECTED") && e.outcome().equals("REJECTED") && e.metadata().isEmpty()));
    }
    @Test void staleDisagreementMapsTo412WithoutRetry()throws Exception{
        when(service.open(any(),any(),anyLong(),any(),any())).thenThrow(new AcademicProblemException(AcademicProblemException.Kind.PRECONDITION_FAILED,"STALE_RECONCILIATION","Recargue antes de continuar."));
        mvc.perform(post(path()+"/academic-disagreements").cookie(cookie()).header("X-CSRF-TOKEN",csrf).header("If-Match","\"0\"")
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Fundamento suficiente de discrepancia\"}"))
                .andExpect(status().isPreconditionFailed()).andExpect(jsonPath("$.correlationId").isNotEmpty());
        verify(service,times(1)).open(eq(process),eq(indicator),eq(0L),any(),any());
    }
}
