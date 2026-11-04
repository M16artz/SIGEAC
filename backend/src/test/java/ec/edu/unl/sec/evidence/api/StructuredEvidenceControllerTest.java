package ec.edu.unl.sec.evidence.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.unl.sec.evidence.application.EvidenceProblemException;
import ec.edu.unl.sec.evidence.application.StructuredEvidenceService;
import ec.edu.unl.sec.evidence.domain.StructuredEvidence;
import ec.edu.unl.sec.identityaccess.application.SessionService;
import ec.edu.unl.sec.identityaccess.infrastructure.AccessProblemWriter;
import ec.edu.unl.sec.identityaccess.infrastructure.SessionAuthenticationFilter;
import ec.edu.unl.sec.shared.api.ApiProblemHandler;
import ec.edu.unl.sec.shared.infrastructure.CorrelationFilter;
import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class StructuredEvidenceControllerTest {
    private static final UUID ACTOR = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID VERSION = UUID.fromString("00000000-0000-0000-0000-000000000501");
    private static final UUID SOURCE = UUID.fromString("00000000-0000-0000-0000-000000000601");
    private static final String CSRF = "synthetic-csrf-token-with-32-characters";
    private static final String TOKEN = "opaque-session";

    private StructuredEvidenceService snapshots;
    private MockMvc mvc;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        snapshots = org.mockito.Mockito.mock(StructuredEvidenceService.class);
        SessionService sessions = org.mockito.Mockito.mock(SessionService.class);
        when(sessions.authenticateForRequest(TOKEN))
                .thenReturn(java.util.Optional.of(new SessionService.AuthenticatedSession(
                        UUID.randomUUID(), ACTOR, "Responsable sint\u00e9tico", false,
                        SessionService.sha256Hex(CSRF))));
        mapper = new ObjectMapper().findAndRegisterModules();
        var sessionFilter = new SessionAuthenticationFilter(sessions, new AccessProblemWriter(mapper));
        mvc = MockMvcBuilders.standaloneSetup(new StructuredEvidenceController(snapshots))
                .setControllerAdvice(new ApiProblemHandler(), new EvidenceProblemHandler())
                .addFilters(new CorrelationFilter(), sessionFilter)
                .build();
    }

    @Test
    void registersTypedSnapshotsForStructuredSources() throws Exception {
        var snapshot = structured();
        when(snapshots.register(eq(VERSION), eq("\"0\""),
                any(StructuredEvidenceService.StructuredCommand.class)))
                .thenReturn(new StructuredEvidenceService.Register(snapshot, 0));

        mvc.perform(post("/evidence-versions/" + VERSION + "/structured-sources")
                        .cookie(new Cookie("sec_session", TOKEN))
                        .header("X-CSRF-Token", CSRF)
                        .header("If-Match", "\"0\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(java.util.Map.of(
                                "requiredSourceId", SOURCE.toString(),
                                "kind", "STRUCTURED_DATA",
                                "value", 42.5,
                                "unit", "m2",
                                "periodStart", "2026-01-01",
                                "periodEnd", "2026-06-30"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("ETag", "\"0\""))
                .andExpect(jsonPath("$.id").value(snapshot.id().toString()))
                .andExpect(jsonPath("$.kind").value("STRUCTURED_DATA"))
                .andExpect(jsonPath("$.value").value(42.5))
                .andExpect(jsonPath("$.valueType").value("NUMBER"))
                .andExpect(jsonPath("$.unit").value("m2"))
                .andExpect(jsonPath("$.periodStart").value("2026-01-01"));
    }

    @Test
    void requiresIfMatchBeforeRegisteringSnapshots() throws Exception {
        when(snapshots.register(eq(VERSION), eq(null), any(StructuredEvidenceService.StructuredCommand.class)))
                .thenThrow(new EvidenceProblemException(EvidenceProblemException.Kind.PRECONDITION_REQUIRED,
                        "PRECONDITION_REQUIRED", "Se requiere If-Match."));

        mvc.perform(post("/evidence-versions/" + VERSION + "/structured-sources")
                        .cookie(new Cookie("sec_session", TOKEN))
                        .header("X-CSRF-Token", CSRF)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(java.util.Map.of(
                                "requiredSourceId", SOURCE.toString(),
                                "kind", "STRUCTURED_DATA",
                                "value", 42.5,
                                "periodStart", "2026-01-01",
                                "periodEnd", "2026-06-30"))))
                .andExpect(status().isPreconditionRequired())
                .andExpect(jsonPath("$.code").value("PRECONDITION_REQUIRED"));
    }

    @Test
    void rejectsDuplicateAndIncompatibleSnapshotsThroughTheService() throws Exception {
        when(snapshots.register(eq(VERSION), eq("\"0\""), any(StructuredEvidenceService.StructuredCommand.class)))
                .thenThrow(new EvidenceProblemException(EvidenceProblemException.Kind.CONFLICT,
                        "STRUCTURED_SNAPSHOT_DUPLICATE", "La versi\u00f3n ya tiene un snapshot para la fuente."))
                .thenThrow(new EvidenceProblemException(EvidenceProblemException.Kind.INVALID,
                        "SOURCE_NOT_COMPATIBLE", "La fuente no admite la modalidad."));

        mvc.perform(post("/evidence-versions/" + VERSION + "/structured-sources")
                        .cookie(new Cookie("sec_session", TOKEN))
                        .header("X-CSRF-Token", CSRF)
                        .header("If-Match", "\"0\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STRUCTURED_SNAPSHOT_DUPLICATE"));

        mvc.perform(post("/evidence-versions/" + VERSION + "/structured-sources")
                        .cookie(new Cookie("sec_session", TOKEN))
                        .header("X-CSRF-Token", CSRF)
                        .header("If-Match", "\"0\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("SOURCE_NOT_COMPATIBLE"));
    }

    @Test
    void listsSnapshotsOfTheVersion() throws Exception {
        when(snapshots.snapshots(VERSION)).thenReturn(List.of(structured()));

        mvc.perform(get("/evidence-versions/" + VERSION + "/structured-sources")
                        .cookie(new Cookie("sec_session", TOKEN))
                        .header("X-CSRF-Token", CSRF))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].requiredSourceId").value(SOURCE.toString()))
                .andExpect(jsonPath("$[0].value").value(42.5));
    }

    private String requestBody() throws Exception {
        return mapper.writeValueAsString(java.util.Map.of(
                "requiredSourceId", SOURCE.toString(),
                "kind", "STRUCTURED_DATA",
                "value", 42.5,
                "unit", "m2",
                "periodStart", "2026-01-01",
                "periodEnd", "2026-06-30"));
    }

    private static StructuredEvidence structured() {
        return new StructuredEvidence(UUID.randomUUID(), VERSION, SOURCE, StructuredEvidence.Kind.STRUCTURED_DATA,
                StructuredEvidence.TypedValue.number(new BigDecimal("42.5")), "m2",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30), null, ACTOR,
                Instant.parse("2026-10-20T15:00:00Z"));
    }
}
