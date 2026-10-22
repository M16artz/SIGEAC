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
import org.springframework.test.web.servlet.setup.*;
class SelfAssessmentControllerTest {
    MockMvc mvc; SelfAssessmentService service;
    @BeforeEach void setup() {
        service=mock(SelfAssessmentService.class);
        mvc=MockMvcBuilders.standaloneSetup(new SelfAssessmentController(service,mock(ProcessTransitionService.class)))
                .setControllerAdvice(new ProcessExceptionHandler(),new ApiProblemHandler()).build();
    }
    @Test void returnsRevisionEtag() throws Exception {
        var process=new SelfAssessmentProcess(null,UUID.randomUUID(),"P","Proceso",LocalDate.now(),LocalDate.now(),UUID.randomUUID(),1,ProcessState.PREPARATION,0,UUID.randomUUID(),Instant.now(),List.of());
        when(service.get(process.id())).thenReturn(process);
        mvc.perform(get("/processes/"+process.id())).andExpect(status().isOk()).andExpect(header().string("ETag","\"0\""));
    }
    @Test void missingEtagIs428AndUnquotedEtagIs400WithoutMutation() throws Exception {
        String path="/processes/"+UUID.randomUUID()+"/activate";
        mvc.perform(post(path)).andExpect(status().isPreconditionRequired()).andExpect(jsonPath("$.code").value("PRECONDITION_REQUIRED"));
        mvc.perform(post(path).header("If-Match","0")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_ETAG"));
        verifyNoInteractions(service);
    }
    @Test void malformedIdentifiersAndJsonAre400Not500() throws Exception {
        mvc.perform(get("/processes/not-a-uuid")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        mvc.perform(post("/processes").contentType("application/json").content("{broken"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        verifyNoInteractions(service);
    }
}
