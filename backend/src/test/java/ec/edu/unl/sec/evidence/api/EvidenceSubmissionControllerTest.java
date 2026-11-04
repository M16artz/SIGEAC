package ec.edu.unl.sec.evidence.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.unl.sec.evidence.application.ApplicabilityDecisionService;
import ec.edu.unl.sec.evidence.application.EvidenceDownloadService;
import ec.edu.unl.sec.evidence.application.EvidenceProblemException;
import ec.edu.unl.sec.evidence.application.EvidenceSourceCoverageService;
import ec.edu.unl.sec.evidence.application.EvidenceSourceService;
import ec.edu.unl.sec.evidence.application.SubmitEvidenceService;
import ec.edu.unl.sec.evidence.domain.EvidenceVersion;
import ec.edu.unl.sec.evidence.domain.EvidenceVersionStatus;
import ec.edu.unl.sec.evidence.domain.VersionIndicator;
import ec.edu.unl.sec.identityaccess.application.SessionService;
import ec.edu.unl.sec.identityaccess.infrastructure.AccessProblemWriter;
import ec.edu.unl.sec.identityaccess.infrastructure.SessionAuthenticationFilter;
import ec.edu.unl.sec.shared.api.ApiProblemHandler;
import ec.edu.unl.sec.shared.infrastructure.CorrelationFilter;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class EvidenceSubmissionControllerTest {
    private static final String CSRF = "synthetic-csrf-token-with-32-characters";
    private static final UUID VERSION = UUID.fromString("00000000-0000-0000-0000-000000000801");
    private static final UUID FILE = UUID.fromString("00000000-0000-0000-0000-000000000802");
    private static final byte[] CONTENT = "synthetic evidence".getBytes(StandardCharsets.UTF_8);
    private static final String SHA = "a".repeat(64);

    private SubmitEvidenceService submission;
    private EvidenceSourceService sources;
    private EvidenceDownloadService downloads;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        submission = mock(SubmitEvidenceService.class);
        sources = mock(EvidenceSourceService.class);
        downloads = mock(EvidenceDownloadService.class);
        var sessions = mock(SessionService.class);
        when(sessions.authenticateForRequest("synthetic-session")).thenReturn(Optional.of(
                new SessionService.AuthenticatedSession(UUID.randomUUID(), UUID.randomUUID(), "Responsable", false,
                        SessionService.sha256Hex(CSRF))));
        mvc = MockMvcBuilders.standaloneSetup(
                        new EvidenceSourceCoverageController(mock(EvidenceSourceCoverageService.class),
                                mock(ApplicabilityDecisionService.class), submission, sources),
                        new EvidenceDownloadController(downloads))
                .setControllerAdvice(new ApiProblemHandler(), new EvidenceProblemHandler())
                .addFilters(new CorrelationFilter(), new SessionAuthenticationFilter(sessions,
                        new AccessProblemWriter(new ObjectMapper().findAndRegisterModules()))).build();
    }

    @Test
    void submitsWithIfMatchAndReturnsTheImmutableSnapshotWithoutAnEtag() throws Exception {
        when(submission.submit(VERSION, "\"0\"")).thenReturn(version(EvidenceVersionStatus.SUBMITTED, 1));
        when(sources.snapshot(VERSION)).thenReturn(EvidenceSourceService.Sources.empty());

        mvc.perform(post("/evidence-versions/" + VERSION + "/submit").cookie(session())
                        .header("X-CSRF-TOKEN", CSRF).header("If-Match", "\"0\""))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("ETag"))
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.createdBy").value(version(EvidenceVersionStatus.SUBMITTED, 1).authorId().toString()));
    }

    @Test
    void submitRequiresCsrfAndIfMatchWithSafeProblems() throws Exception {
        mvc.perform(post("/evidence-versions/" + VERSION + "/submit").cookie(session()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(submission);

        when(submission.submit(VERSION, null)).thenThrow(new EvidenceProblemException(
                EvidenceProblemException.Kind.PRECONDITION_REQUIRED, "PRECONDITION_REQUIRED", "Se requiere If-Match."));
        mvc.perform(post("/evidence-versions/" + VERSION + "/submit").cookie(session())
                        .header("X-CSRF-TOKEN", CSRF))
                .andExpect(status().is(428))
                .andExpect(jsonPath("$.code").value("PRECONDITION_REQUIRED"));
    }

    @Test
    void downloadsExactVerifiedBytesWithSafeHeaders() throws Exception {
        when(downloads.prepare(VERSION, FILE)).thenReturn(new EvidenceDownloadService.Download(
                "synthetic evidence.txt", "text/plain", CONTENT.length, SHA, CONTENT));

        mvc.perform(get("/evidence-versions/" + VERSION + "/files/" + FILE + "/content").cookie(session()))
                .andExpect(status().isOk())
                .andExpect(content().bytes(CONTENT))
                .andExpect(header().longValue("Content-Length", CONTENT.length))
                .andExpect(header().string("Content-Type", "text/plain"))
                .andExpect(header().string("X-Content-SHA256", SHA))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("synthetic evidence.txt")))
                .andExpect(header().doesNotExist("X-Storage-Key"));
    }

    @Test
    void integrityFailuresReturnNoBytesAndNoMetadata() throws Exception {
        when(downloads.prepare(VERSION, FILE)).thenThrow(new EvidenceProblemException(
                EvidenceProblemException.Kind.CONFLICT, "FILE_INTEGRITY_FAILED",
                "El archivo no está disponible o no supera la verificación de integridad."));

        mvc.perform(get("/evidence-versions/" + VERSION + "/files/" + FILE + "/content").cookie(session()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("FILE_INTEGRITY_FAILED"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("synthetic evidence"))));
    }

    @Test
    void unauthorizedDownloadsRevealNothing() throws Exception {
        when(downloads.prepare(VERSION, FILE)).thenThrow(new EvidenceProblemException(
                EvidenceProblemException.Kind.UNAUTHORIZED, "AUTHENTICATION_REQUIRED",
                "Se requiere una sesión vigente."));
        mvc.perform(get("/evidence-versions/" + VERSION + "/files/" + FILE + "/content"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("synthetic evidence"))));
    }

    private static EvidenceVersion version(EvidenceVersionStatus status, long revision) {
        return new EvidenceVersion(VERSION, UUID.fromString("00000000-0000-0000-0000-000000000811"), 1, null,
                status, "Informe sintetico", "Descripcion sintetica", "Procedencia sintetica",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30),
                UUID.fromString("00000000-0000-0000-0000-000000000812"), Instant.parse("2026-10-20T15:00:00Z"),
                status == EvidenceVersionStatus.SUBMITTED ? Instant.parse("2026-10-20T16:00:00Z") : null,
                revision, List.of(new VersionIndicator(VERSION,
                        UUID.fromString("00000000-0000-0000-0000-000000000813"), "IND-1", "Indicador", 1)));
    }

    private static Cookie session() {
        return new Cookie("SEC_SESSION", "synthetic-session");
    }
}
