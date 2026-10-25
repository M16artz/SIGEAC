package ec.edu.unl.sec.academicevaluation.api;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.unl.sec.academicevaluation.application.*;
import ec.edu.unl.sec.academicevaluation.domain.ManualAcademicEvaluation;
import ec.edu.unl.sec.identityaccess.application.SessionService;
import ec.edu.unl.sec.identityaccess.infrastructure.*;
import ec.edu.unl.sec.shared.api.ApiProblemHandler;
import ec.edu.unl.sec.shared.infrastructure.CorrelationFilter;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AcademicEvaluationControllerTest {
    final UUID process=UUID.randomUUID(), indicator=UUID.randomUUID(), actor=UUID.randomUUID(), option=UUID.randomUUID(), version=UUID.randomUUID();
    final AcademicEvaluationService service=mock(AcademicEvaluationService.class);
    final ec.edu.unl.sec.audit.application.FailureAuditPort failures=mock(ec.edu.unl.sec.audit.application.FailureAuditPort.class);
    final String csrf="synthetic-manual-evaluation-csrf";
    MockMvc mvc;
    String body() { return "{\"ratingOptionId\":\""+option+"\",\"evidenceVersionIds\":[\""+version+"\"],\"justification\":\"Justificación académica suficiente\",\"limitations\":\"Limitaciones académicas declaradas\",\"predecessorId\":null}"; }
    String path() { return "/processes/"+process+"/indicators/"+indicator+"/manual-academic-evaluations"; }
    Cookie cookie() { return new Cookie("SEC_SESSION","session"); }
    @BeforeEach void setup() {
        var sessions=mock(SessionService.class);
        when(sessions.authenticateForRequest("session")).thenReturn(Optional.of(new SessionService.AuthenticatedSession(
                UUID.randomUUID(),actor,"Evaluador sintético",false,SessionService.sha256Hex(csrf))));
        mvc=MockMvcBuilders.standaloneSetup(new AcademicEvaluationController(service,failures,
                        () -> Optional.of(new ec.edu.unl.sec.identityaccess.application.CurrentActorPort.Actor(actor,false))),
                        new AcademicReconciliationController(mock(AcademicReconciliationService.class),failures,
                                () -> Optional.of(new ec.edu.unl.sec.identityaccess.application.CurrentActorPort.Actor(actor,false))))
                .setControllerAdvice(new ApiProblemHandler(),new AcademicProblemHandler())
                .addFilters(new CorrelationFilter(),new SessionAuthenticationFilter(sessions,new AccessProblemWriter(new ObjectMapper().findAndRegisterModules()))).build();
    }
    @AfterEach void clear() { org.springframework.security.core.context.SecurityContextHolder.clearContext(); }
    @Test void savesWithStrongPreconditionAndNoCache() throws Exception {
        var result=new ManualAcademicEvaluation(UUID.randomUUID(),process,indicator,UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),option,
                "Cumple","Método cualitativo","Limitaciones registradas","Justificación registrada",
                List.of(new ManualAcademicEvaluation.Support(version,UUID.randomUUID(),UUID.randomUUID())),actor,Instant.now(),1,null);
        when(service.evaluate(eq(process),eq(indicator),eq(0L),any(),any())).thenReturn(result);
        mvc.perform(post(path()).cookie(cookie()).header("X-CSRF-TOKEN",csrf).header("If-Match","\"0\"")
                        .contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isCreated()).andExpect(header().string("ETag","\"1\""))
                .andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.method").value("Método cualitativo"));
    }
    @Test void rejectsAbsentWeakPreconditionMissingCsrfAndUnknownFields() throws Exception {
        mvc.perform(post(path()).cookie(cookie()).header("X-CSRF-TOKEN",csrf).contentType(MediaType.APPLICATION_JSON).content(body())).andExpect(status().isPreconditionRequired());
        mvc.perform(post(path()).cookie(cookie()).header("X-CSRF-TOKEN",csrf).header("If-Match","W/\"0\"").contentType(MediaType.APPLICATION_JSON).content(body())).andExpect(status().isBadRequest());
        mvc.perform(post(path()).cookie(cookie()).header("If-Match","\"0\"").contentType(MediaType.APPLICATION_JSON).content(body())).andExpect(status().isForbidden());
        mvc.perform(post(path()).cookie(cookie()).header("X-CSRF-TOKEN",csrf).header("If-Match","\"0\"")
                .contentType(MediaType.APPLICATION_JSON).content(body().replace("\"predecessorId\":null","\"predecessorId\":null,\"evaluatorId\":\""+actor+"\""))).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @Test void returnsOwnRevisionForHistoryAndMapsStaleError() throws Exception {
        when(service.view(process,indicator,0,20)).thenReturn(new AcademicEvaluationService.View(process,indicator,UUID.randomUUID(),
                "DOCUMENTARY_CLOSED","Método",List.of(),List.of(),2,UUID.randomUUID(),List.of(),0,20,false));
        mvc.perform(get(path()).cookie(cookie())).andExpect(status().isOk()).andExpect(header().string("ETag","\"2\""));
        when(service.evaluate(any(),any(),anyLong(),any(),any())).thenThrow(new AcademicProblemException(AcademicProblemException.Kind.PRECONDITION_FAILED,"STALE_EVALUATION","Recargue antes de continuar."));
        mvc.perform(post(path()).cookie(cookie()).header("X-CSRF-TOKEN",csrf).header("If-Match","\"0\"").contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isPreconditionFailed()).andExpect(jsonPath("$.code").value("STALE_EVALUATION")).andExpect(jsonPath("$.correlationId").isNotEmpty());
        verify(failures).recordAfterRollback(argThat(event -> event.action().equals("ACADEMIC_MANUAL_EVALUATION_REJECTED")
                && event.outcome().equals("REJECTED") && event.reasonCode().equals("STALE_EVALUATION") && event.metadata().isEmpty()));
    }
    @Test void provisionalEndpointsCannotClaimSuccessfulCalculationOrReconciliation() throws Exception {
        for (String operation:List.of("variables","calculate","evaluate"))
            mvc.perform(post("/academic-evaluations/indicators/"+indicator+"/"+operation).cookie(cookie()).header("X-CSRF-TOKEN",csrf))
                    .andExpect(status().isNotImplemented());
        mvc.perform(post("/academic-evaluations/disagreements/"+indicator).cookie(cookie()).header("X-CSRF-TOKEN",csrf)).andExpect(status().isNotImplemented());
        verifyNoInteractions(service);
    }
}
