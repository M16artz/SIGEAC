package ec.edu.unl.sec.reporting.api;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.unl.sec.identityaccess.application.SessionService;
import ec.edu.unl.sec.identityaccess.infrastructure.AccessProblemWriter;
import ec.edu.unl.sec.identityaccess.infrastructure.SessionAuthenticationFilter;
import ec.edu.unl.sec.reporting.application.DocumentarySnapshotService;
import ec.edu.unl.sec.reporting.domain.DocumentaryProgressCalculator;
import ec.edu.unl.sec.reporting.domain.DocumentarySnapshot;
import ec.edu.unl.sec.shared.api.ApiProblemHandler;
import ec.edu.unl.sec.shared.infrastructure.CorrelationFilter;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class DocumentarySnapshotControllerTest {
    private final UUID process=UUID.randomUUID(),id=UUID.randomUUID();
    private final DocumentarySnapshotService snapshots=mock(DocumentarySnapshotService.class);
    private MockMvc mvc;
    private DocumentarySnapshot snapshot;
    private static final String CSRF="synthetic-csrf-token-for-snapshot";
    private static final String BODY="{\"reason\":\"Corte documental sintético de seguimiento.\"}";

    @BeforeEach
    void setUp() {
        var content=new DocumentarySnapshot.Content(id,process,UUID.randomUUID(),UUID.randomUUID(),"ACTIVE",8,UUID.randomUUID(),
                Instant.parse("2026-10-24T12:00:00Z"),"Corte documental sintético de seguimiento.",List.of(),
                new DocumentaryProgressCalculator.Progress(DocumentaryProgressCalculator.Status.NOT_CONFIGURED,0,0,0,"SOURCE_COVERAGE_V1"),List.of(),List.of());
        snapshot=new DocumentarySnapshot(content,"{}","a".repeat(64));
        var sessions=mock(SessionService.class);
        when(sessions.authenticateForRequest("session")).thenReturn(Optional.of(new SessionService.AuthenticatedSession(
                UUID.randomUUID(),content.createdBy(),"Coordinador sintético",false,SessionService.sha256Hex(CSRF))));
        var json=new ObjectMapper().findAndRegisterModules();
        mvc=MockMvcBuilders.standaloneSetup(new DocumentarySnapshotController(snapshots))
                .setControllerAdvice(new ApiProblemHandler(),new ReportingProblemHandler())
                .addFilters(new CorrelationFilter(),new SessionAuthenticationFilter(sessions,new AccessProblemWriter(json))).build();
    }

    @Test
    void createsSnapshotWithStrongDigestLocationAndSafeTextCorrelation() throws Exception {
        when(snapshots.create(eq(process),eq(7L),anyString(),any())).thenReturn(snapshot);
        mvc.perform(post(path()).cookie(cookie()).header("X-CSRF-Token",CSRF).header("If-Match","\"7\"")
                        .header("X-Correlation-ID","snapshot-test-correlation").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated()).andExpect(header().string("Location","/api/v1/documentary-snapshots/"+id))
                .andExpect(header().string("ETag","\""+snapshot.sha256()+"\""))
                .andExpect(header().string("Cache-Control","no-store"))
                .andExpect(jsonPath("$.header.processRevision").value(8))
                .andExpect(jsonPath("$.data.reason").value(snapshot.content().reason()))
                .andExpect(jsonPath("$.payloadJson").doesNotExist());
    }

    @Test
    void rejectsMissingWeakAndOverflowingEtagsWithoutCallingService() throws Exception {
        mvc.perform(post(path()).cookie(cookie()).header("X-CSRF-Token",CSRF).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isPreconditionRequired());
        for (String etag : List.of("W/\"7\"","\"9999999999999999999999999\""))
            mvc.perform(post(path()).cookie(cookie()).header("X-CSRF-Token",CSRF).header("If-Match",etag)
                    .contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isUnprocessableContent());
        verifyNoInteractions(snapshots);
    }

    @Test
    void rejectsUnknownHistoricalCutoffAndMissingCsrf() throws Exception {
        mvc.perform(post(path()).cookie(cookie()).header("If-Match","\"7\"").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
        mvc.perform(post(path()).cookie(cookie()).header("X-CSRF-Token",CSRF).header("If-Match","\"7\"")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Corte documental sintético.\",\"cutoffAt\":\"2020-01-01T00:00:00Z\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(snapshots);
    }

    @Test
    void retrievesStoredContentAndPagesItsHistory() throws Exception {
        when(snapshots.get(id)).thenReturn(snapshot);
        when(snapshots.history(process,1,5)).thenReturn(new DocumentarySnapshotService.Page(List.of(snapshot.header()),1,5,false));
        mvc.perform(get("/documentary-snapshots/"+id).cookie(cookie())).andExpect(status().isOk())
                .andExpect(header().string("ETag","\""+snapshot.sha256()+"\""))
                .andExpect(jsonPath("$.data.progress.requiredSourceTotal").value(0));
        mvc.perform(get(path()).cookie(cookie()).param("page","1").param("size","5"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.items[0].id").value(id.toString()));
    }

    @Test
    void mapsConcurrentTransactionConflictsToRetryableProblem() throws Exception {
        when(snapshots.create(any(),anyLong(),anyString(),any())).thenThrow(new org.springframework.dao.CannotSerializeTransactionException("conflict"));
        mvc.perform(post(path()).cookie(cookie()).header("X-CSRF-Token",CSRF).header("If-Match","\"7\"")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isPreconditionFailed()).andExpect(jsonPath("$.code").value("STALE_PROCESS"));
    }

    private String path() { return "/processes/"+process+"/documentary-snapshots"; }
    @org.junit.jupiter.api.AfterEach
    void clearSecurityContext() { org.springframework.security.core.context.SecurityContextHolder.clearContext(); }
    private Cookie cookie() { return new Cookie(ec.edu.unl.sec.identityaccess.infrastructure.SessionCookie.NAME,"session"); }
}
