package ec.edu.unl.sec.evidence.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.unl.sec.evidence.application.EvidenceDraftService;
import ec.edu.unl.sec.evidence.application.EvidenceProblemException;
import ec.edu.unl.sec.evidence.domain.Evidence;
import ec.edu.unl.sec.evidence.domain.EvidenceVersion;
import ec.edu.unl.sec.evidence.domain.VersionIndicator;
import ec.edu.unl.sec.identityaccess.application.SessionService;
import ec.edu.unl.sec.identityaccess.infrastructure.AccessProblemWriter;
import ec.edu.unl.sec.identityaccess.infrastructure.SessionAuthenticationFilter;
import ec.edu.unl.sec.shared.api.ApiProblemHandler;
import ec.edu.unl.sec.shared.infrastructure.CorrelationFilter;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class EvidenceDraftControllerTest {
    private static final UUID ACTOR = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID PROCESS = UUID.fromString("00000000-0000-0000-0000-000000000301");
    private static final UUID INDICATOR = UUID.fromString("00000000-0000-0000-0000-000000000401");
    private static final String TOKEN = "opaque-session";
    private static final String CSRF = "synthetic-csrf-token-with-32-characters";

    private EvidenceDraftService service;
    private MockMvc mvc;
    private EvidenceDraftService.EvidenceDetails details;

    @BeforeEach
    void setUp() {
        service = mock(EvidenceDraftService.class);
        SessionService sessions = mock(SessionService.class);
        when(sessions.authenticateForRequest(TOKEN))
                .thenReturn(Optional.of(new SessionService.AuthenticatedSession(
                        UUID.randomUUID(), ACTOR, "Responsable sintético", false, SessionService.sha256Hex(CSRF))));
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        var sessionFilter = new SessionAuthenticationFilter(sessions, new AccessProblemWriter(mapper));
        mvc = MockMvcBuilders.standaloneSetup(new EvidenceDraftController(service))
                .setControllerAdvice(new ApiProblemHandler(), new EvidenceProblemHandler())
                .addFilters(new CorrelationFilter(), sessionFilter)
                .build();
        details = details();
    }

    @Test
    void listsAndGetsOnlyServiceAuthorizedRepresentations() throws Exception {
        when(service.list(PROCESS, Optional.empty())).thenReturn(List.of(details));
        when(service.getEvidence(details.evidence().id())).thenReturn(details);
        when(service.getVersion(details.latestVersion().id())).thenReturn(details.latestVersion());

        mvc.perform(get("/evidences").param("processId", PROCESS.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(details.evidence().id().toString()));
        mvc.perform(get("/evidences/" + details.evidence().id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latestVersion.id").value(details.latestVersion().id().toString()));
        mvc.perform(get("/evidence-versions/" + details.latestVersion().id()))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"0\""));
    }

    @Test
    void createsDraftWithLocationAndStrongEtag() throws Exception {
        when(service.create(any(), any())).thenReturn(details);

        mvc.perform(post("/evidences")
                        .cookie(new Cookie("SEC_SESSION", TOKEN))
                        .header("X-CSRF-TOKEN", CSRF)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody()))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location", "/api/v1/evidence-versions/" + details.latestVersion().id()))
                .andExpect(header().string("ETag", "\"0\""));
    }

    @Test
    void rejectsStateChangeWithoutValidCsrfToken() throws Exception {
        mvc.perform(post("/evidences")
                        .cookie(new Cookie("SEC_SESSION", TOKEN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_TOKEN_INVALID"));
    }

    @Test
    void patchesUsingTheExactIfMatchHeaderAndReturnsTheNewEtag() throws Exception {
        EvidenceVersion updated = details.latestVersion().update(
                "Título actualizado",
                "Descripción actualizada",
                "Procedencia actualizada",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 6, 30),
                details.latestVersion().indicators());
        when(service.update(eq(updated.id()), eq("\"0\""), any(), any())).thenReturn(updated);

        mvc.perform(patch("/evidence-versions/" + updated.id())
                        .cookie(new Cookie("SEC_SESSION", TOKEN))
                        .header("X-CSRF-TOKEN", CSRF)
                        .header("If-Match", "\"0\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody()))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"1\""));

        verify(service).update(eq(updated.id()), eq("\"0\""), any(), any());
    }

    @Test
    void hidesOutOfScopeDirectIdsAsNotFound() throws Exception {
        UUID hidden = UUID.randomUUID();
        when(service.getEvidence(hidden)).thenThrow(problem(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND"));

        mvc.perform(get("/evidences/" + hidden))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void preservesMissingAndStalePreconditionResponses() throws Exception {
        UUID versionId = details.latestVersion().id();
        when(service.update(eq(versionId), eq(null), any(), any()))
                .thenThrow(problem(HttpStatus.PRECONDITION_REQUIRED, "PRECONDITION_REQUIRED"));
        when(service.update(eq(versionId), eq("\"9\""), any(), any()))
                .thenThrow(problem(HttpStatus.PRECONDITION_FAILED, "STALE_DRAFT"));

        mvc.perform(patch("/evidence-versions/" + versionId)
                        .cookie(new Cookie("SEC_SESSION", TOKEN))
                        .header("X-CSRF-TOKEN", CSRF)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody()))
                .andExpect(status().isPreconditionRequired())
                .andExpect(jsonPath("$.code").value("PRECONDITION_REQUIRED"));
        mvc.perform(patch("/evidence-versions/" + versionId)
                        .cookie(new Cookie("SEC_SESSION", TOKEN))
                        .header("X-CSRF-TOKEN", CSRF)
                        .header("If-Match", "\"9\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody()))
                .andExpect(status().isPreconditionFailed())
                .andExpect(jsonPath("$.code").value("STALE_DRAFT"));
    }

    private static EvidenceDraftService.EvidenceDetails details() {
        UUID evidenceId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        Evidence evidence = new Evidence(evidenceId, PROCESS, ACTOR, Instant.parse("2026-10-20T15:00:00Z"), UUID.randomUUID(), 1);
        EvidenceVersion version = new EvidenceVersion(
                versionId,
                evidenceId,
                1,
                null,
                null,
                "Informe sintético",
                "Descripción sintética",
                "Procedencia sintética",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 6, 30),
                ACTOR,
                Instant.parse("2026-10-20T15:00:00Z"),
                null,
                0,
                List.of(new VersionIndicator(versionId, INDICATOR, "IND-1", "Indicador 1", 1)));
        return new EvidenceDraftService.EvidenceDetails(evidence, version);
    }

    private static EvidenceProblemException problem(HttpStatus status, String code) {
        EvidenceProblemException.Kind kind = switch (status) {
            case NOT_FOUND -> EvidenceProblemException.Kind.NOT_FOUND;
            case PRECONDITION_REQUIRED -> EvidenceProblemException.Kind.PRECONDITION_REQUIRED;
            case PRECONDITION_FAILED -> EvidenceProblemException.Kind.PRECONDITION_FAILED;
            default -> EvidenceProblemException.Kind.INVALID;
        };
        return new EvidenceProblemException(kind, code, "Operación rechazada de forma segura.");
    }

    private static String createBody() {
        return """
                {"processId":"%s","title":"Informe sintético","description":"Descripción sintética",\
                "provenance":"Procedencia sintética","coveredFrom":"2026-01-01","coveredTo":"2026-06-30",\
                "indicatorIds":["%s"]}
                """.formatted(PROCESS, INDICATOR);
    }

    private static String updateBody() {
        return """
                {"title":"Título actualizado","description":"Descripción actualizada",\
                "provenance":"Procedencia actualizada","coveredFrom":"2026-01-01","coveredTo":"2026-06-30",\
                "indicatorIds":["%s"]}
                """.formatted(INDICATOR);
    }
}
