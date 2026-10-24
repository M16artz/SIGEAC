package ec.edu.unl.sec.reporting.api;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.unl.sec.audit.application.FailureAuditPort;
import ec.edu.unl.sec.identityaccess.application.CurrentActorPort;
import ec.edu.unl.sec.identityaccess.application.SessionService;
import ec.edu.unl.sec.identityaccess.infrastructure.*;
import ec.edu.unl.sec.reporting.application.*;
import ec.edu.unl.sec.reporting.domain.DocumentaryClosure;
import ec.edu.unl.sec.shared.api.ApiProblemHandler;
import ec.edu.unl.sec.shared.infrastructure.CorrelationFilter;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class DocumentaryClosureControllerTest {
    private final UUID process=UUID.randomUUID(),actor=UUID.randomUUID(),id=UUID.randomUUID();
    private final DocumentaryClosureService closures=mock(DocumentaryClosureService.class);
    private final FailureAuditPort failures=mock(FailureAuditPort.class);
    private MockMvc mvc;
    private DocumentaryClosure closure;
    private static final String CSRF="synthetic-closure-csrf-token";
    private static final String BODY="{\"closurePolicyVersion\":\"DOCUMENTARY_STRICT_V1\",\"reason\":\"Cierre documental sintético con fuentes aceptadas.\"}";
    @BeforeEach
    void setUp() {
        closure=new DocumentaryClosure(new DocumentaryClosure.Content(id,process,UUID.randomUUID(),UUID.randomUUID(),"a".repeat(64),
                "DOCUMENTARY_CLOSED",9,"DOCUMENTARY_STRICT_V1",actor,Instant.parse("2026-10-24T12:00:00Z"),
                "Cierre documental sintético con fuentes aceptadas.",List.of()),"{}","b".repeat(64));
        var sessions=mock(SessionService.class);
        when(sessions.authenticateForRequest("session")).thenReturn(Optional.of(new SessionService.AuthenticatedSession(
                UUID.randomUUID(),actor,"Coordinador sintético",false,SessionService.sha256Hex(CSRF))));
        var mapper=new ObjectMapper().findAndRegisterModules();
        mvc=MockMvcBuilders.standaloneSetup(new DocumentaryClosureController(closures,failures,
                        ()->Optional.of(new CurrentActorPort.Actor(actor,false))))
                .setControllerAdvice(new ApiProblemHandler(),new ReportingProblemHandler())
                .addFilters(new CorrelationFilter(),new SessionAuthenticationFilter(sessions,new AccessProblemWriter(mapper))).build();
    }
    @AfterEach
    void clearContext() { org.springframework.security.core.context.SecurityContextHolder.clearContext(); }
    @Test
    void createsClosureWithLocationDigestAndFinalState() throws Exception {
        when(closures.close(eq(process),eq(7L),eq("DOCUMENTARY_STRICT_V1"),anyString(),any())).thenReturn(closure);
        mvc.perform(post(path()).cookie(cookie()).header("If-Match","\"7\"").header("X-CSRF-TOKEN",CSRF)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated()).andExpect(header().string("Location","/api/v1/documentary-closures/"+id))
                .andExpect(header().string("ETag","\""+closure.sha256()+"\""))
                .andExpect(jsonPath("$.data.state").value("DOCUMENTARY_CLOSED"));
    }
    @Test
    void returnsBlockersAndAuditsTheRejectedBoundaryAttempt() throws Exception {
        when(closures.close(any(),anyLong(),anyString(),anyString(),any())).thenThrow(new ReportingProblemException(
                ReportingProblemException.Kind.CONFLICT,"CLOSURE_BLOCKED","Fuentes pendientes.",Map.of("sourceIds","synthetic-source")));
        mvc.perform(post(path()).cookie(cookie()).header("If-Match","\"7\"").header("X-CSRF-TOKEN",CSRF)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.details.sourceIds").value("synthetic-source"));
        verify(failures).recordAfterRollback(argThat(event -> event.action().equals("DOCUMENTARY_CLOSURE_REJECTED")
                && event.reasonCode().equals("CLOSURE_BLOCKED") && event.outcome().equals("REJECTED")));
    }
    @Test
    void rejectsMissingPreconditionCsrfAndUnsupportedExceptionFields() throws Exception {
        mvc.perform(post(path()).cookie(cookie()).header("X-CSRF-TOKEN",CSRF).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isPreconditionRequired());
        mvc.perform(post(path()).cookie(cookie()).header("If-Match","\"7\"").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
        mvc.perform(post(path()).cookie(cookie()).header("If-Match","\"7\"").header("X-CSRF-TOKEN",CSRF).contentType(MediaType.APPLICATION_JSON)
                        .content(BODY.substring(0,BODY.length()-1)+",\"exceptionDecisionIds\":[]}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(closures);
    }
    @Test
    void readsConservedClosureWithoutExposingCanonicalPayload() throws Exception {
        when(closures.get(id)).thenReturn(closure);
        mvc.perform(get("/documentary-closures/"+id).cookie(cookie())).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control","no-store"))
                .andExpect(jsonPath("$.data.snapshotId").value(closure.content().snapshotId().toString()))
                .andExpect(jsonPath("$.payloadJson").doesNotExist());
    }
    private String path() { return "/processes/"+process+"/documentary-close"; }
    private Cookie cookie() { return new Cookie(SessionCookie.NAME,"session"); }
}
