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
    MockMvc mvc; SelfAssessmentService service; ProcessTransitionService transitions;
    @BeforeEach void setup() {
        service=mock(SelfAssessmentService.class);
        transitions=mock(ProcessTransitionService.class);
        mvc=MockMvcBuilders.standaloneSetup(new SelfAssessmentController(service,transitions))
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
    @Test void forwardsAuthorityProvenanceAndRevisionForEveryGovernedTransition() throws Exception {
        UUID id=UUID.randomUUID(), assignment=UUID.randomUUID();
        var decision=new ProcessTransitionService.Decision(assignment,"Designacion sintetica","fixture://decision");
        for (var target:List.of(ProcessState.SUSPENDED,ProcessState.ACTIVE,ProcessState.REOPENED)) {
            var process=new SelfAssessmentProcess(id,UUID.randomUUID(),"P","Proceso",LocalDate.now(),LocalDate.now(),
                    UUID.randomUUID(),1,target,5,UUID.randomUUID(),Instant.now(),List.of());
            when(transitions.transition(eq(id),eq(4L),eq(target),eq(decision),eq("Decision justificada"),any()))
                    .thenReturn(process);
            String path="/processes/"+id+(target==ProcessState.REOPENED?"/reopen":"/transitions");
            String body="""
                    {"target":"%s","reason":"Decision justificada","authorityAssignmentId":"%s",
                     "legalBasis":"Designacion sintetica","documentReference":"fixture://decision"}
                    """.formatted(target,assignment);
            mvc.perform(post(path).header("If-Match","\"4\"").contentType("application/json").content(body))
                    .andExpect(status().isOk()).andExpect(header().string("ETag","\"5\""));
            verify(transitions).transition(eq(id),eq(4L),eq(target),eq(decision),eq("Decision justificada"),any());
        }
    }
    @Test void rejectsIncompleteDecisionProvenanceWithoutMutating() throws Exception {
        for (String suffix:List.of("/transitions","/reopen")) {
            mvc.perform(post("/processes/"+UUID.randomUUID()+suffix).header("If-Match","\"4\"")
                    .contentType("application/json").content("""
                        {"target":"SUSPENDED","reason":"Decision justificada","legalBasis":"Designacion"}
                        """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("AUTHORITY_PROVENANCE_INCOMPLETE"));
        }
        verifyNoInteractions(service,transitions);
    }
}
