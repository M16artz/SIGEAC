package ec.edu.unl.sec.evidence.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.unl.sec.evidence.application.ApplicabilityDecisionService;
import ec.edu.unl.sec.evidence.application.EvidenceProblemException;
import ec.edu.unl.sec.evidence.application.EvidenceSourceCoverageService;
import ec.edu.unl.sec.evidence.application.SubmitEvidenceService;
import ec.edu.unl.sec.evidence.domain.ApplicabilityDecision;
import ec.edu.unl.sec.evidence.domain.EvidenceVersion;
import ec.edu.unl.sec.identityaccess.application.SessionService;
import ec.edu.unl.sec.identityaccess.infrastructure.AccessProblemWriter;
import ec.edu.unl.sec.identityaccess.infrastructure.SessionAuthenticationFilter;
import ec.edu.unl.sec.shared.api.ApiProblemHandler;
import ec.edu.unl.sec.shared.infrastructure.CorrelationFilter;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class EvidenceSourceCoverageControllerTest {
    private static final UUID ACTOR = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID PROCESS = UUID.fromString("00000000-0000-0000-0000-000000000301");
    private static final UUID VERSION = UUID.fromString("00000000-0000-0000-0000-000000000501");
    private static final UUID SOURCE = UUID.fromString("00000000-0000-0000-0000-000000000601");
    private static final UUID COVERAGE = UUID.fromString("00000000-0000-0000-0000-000000000611");
    private static final String CSRF = "synthetic-csrf-token-with-32-characters";
    private static final String TOKEN = "opaque-session";

    private EvidenceSourceCoverageService coverage;
    private ApplicabilityDecisionService applicability;
    private SubmitEvidenceService submission;
    private MockMvc mvc;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        coverage = org.mockito.Mockito.mock(EvidenceSourceCoverageService.class);
        applicability = org.mockito.Mockito.mock(ApplicabilityDecisionService.class);
        submission = org.mockito.Mockito.mock(SubmitEvidenceService.class);
        SessionService sessions = org.mockito.Mockito.mock(SessionService.class);
        when(sessions.authenticateForRequest(TOKEN))
                .thenReturn(java.util.Optional.of(new SessionService.AuthenticatedSession(
                        UUID.randomUUID(), ACTOR, "Responsable sint\u00e9tico", false,
                        SessionService.sha256Hex(CSRF))));
        mapper = new ObjectMapper().findAndRegisterModules();
        var sessionFilter = new SessionAuthenticationFilter(sessions, new AccessProblemWriter(mapper));
        var sources = org.mockito.Mockito.mock(ec.edu.unl.sec.evidence.application.EvidenceSourceService.class);
        when(sources.snapshot(any())).thenReturn(ec.edu.unl.sec.evidence.application.EvidenceSourceService.Sources.empty());
        mvc = MockMvcBuilders.standaloneSetup(new EvidenceSourceCoverageController(coverage, applicability, submission, sources))
                .setControllerAdvice(new ApiProblemHandler(), new EvidenceProblemHandler())
                .addFilters(new CorrelationFilter(), sessionFilter)
                .build();
    }

    @Test
    void declaresCoverageOnTheDraftAndReturnsTheVersionRevision() throws Exception {
        var saved = new ec.edu.unl.sec.evidence.domain.SourceCoverage(
                COVERAGE, VERSION, SOURCE, "Nota sint\u00e9tica", ACTOR, Instant.parse("2026-10-20T15:00:00Z"));
        when(coverage.declare(eq(VERSION), eq("\"0\""), eq(SOURCE), eq("Nota sint\u00e9tica")))
                .thenReturn(new EvidenceSourceCoverageService.Declared(saved, 1));

        mvc.perform(post("/evidence-versions/" + VERSION + "/source-coverages")
                        .cookie(new Cookie("sec_session", TOKEN))
                        .header("X-CSRF-Token", CSRF)
                        .header("If-Match", "\"0\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(java.util.Map.of(
                                "requiredSourceId", SOURCE.toString(),
                                "coverageNote", "Nota sint\u00e9tica"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("ETag", "\"1\""))
                .andExpect(jsonPath("$.id").value(COVERAGE.toString()))
                .andExpect(jsonPath("$.requiredSourceId").value(SOURCE.toString()));
    }

    @Test
    void rejectsForeignOrIncompatibleRequiredSourcesAsUnprocessable() throws Exception {
        when(coverage.declare(any(), any(), any(), any()))
                .thenThrow(new EvidenceProblemException(EvidenceProblemException.Kind.INVALID,
                        "SOURCE_NOT_COMPATIBLE", "La fuente no pertenece al modelo fijado."));

        mvc.perform(post("/evidence-versions/" + VERSION + "/source-coverages")
                        .cookie(new Cookie("sec_session", TOKEN))
                        .header("X-CSRF-Token", CSRF)
                        .header("If-Match", "\"0\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(java.util.Map.of(
                                "requiredSourceId", SOURCE.toString(),
                                "coverageNote", "Nota sint\u00e9tica"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("SOURCE_NOT_COMPATIBLE"));
    }

    @Test
    void requiresStrongIfMatchForCoverageChanges() throws Exception {
        when(coverage.declare(any(), eq("\"9\""), any(), any()))
                .thenThrow(new EvidenceProblemException(EvidenceProblemException.Kind.PRECONDITION_FAILED,
                        "STALE_DRAFT", "El borrador cambi\u00f3."));

        mvc.perform(post("/evidence-versions/" + VERSION + "/source-coverages")
                        .cookie(new Cookie("sec_session", TOKEN))
                        .header("X-CSRF-Token", CSRF)
                        .header("If-Match", "\"9\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(java.util.Map.of(
                                "requiredSourceId", SOURCE.toString(),
                                "coverageNote", "Nota"))))
                .andExpect(status().isPreconditionFailed())
                .andExpect(jsonPath("$.code").value("STALE_DRAFT"));
    }

    @Test
    void removesCoverageOnDraftOnlyThroughTheService() throws Exception {
        when(coverage.remove(VERSION, COVERAGE, "\"0\"")).thenReturn(1L);
        mvc.perform(delete("/evidence-versions/" + VERSION + "/source-coverages/" + COVERAGE)
                        .cookie(new Cookie("sec_session", TOKEN))
                        .header("X-CSRF-Token", CSRF)
                        .header("If-Match", "\"0\""))
                .andExpect(status().isNoContent())
                .andExpect(header().string("ETag", "\"1\""));
        org.mockito.Mockito.verify(coverage).remove(VERSION, COVERAGE, "\"0\"");
    }

    @Test
    void rejectsApplicabilityDecisionsWithoutCompleteProvenance() throws Exception {
        mvc.perform(post("/processes/" + PROCESS + "/applicability-decisions")
                        .cookie(new Cookie("sec_session", TOKEN))
                        .header("X-CSRF-Token", CSRF)
                        .header("If-Match", "\"0\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(java.util.Map.of(
                                "subjectType", "REQUIRED_SOURCE",
                                "subjectId", SOURCE.toString(),
                                "decision", "NOT_APPLICABLE",
                                "ruleReference", "Regla",
                                "provenance", java.util.Map.of(
                                        "authorityAssignmentId", UUID.randomUUID().toString(),
                                        "legalBasis", "",
                                        "documentReference", "fixture://decision",
                                        "reason", "Justificaci\u00f3n completa sint\u00e9tica.")))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("AUTHORITY_PROVENANCE_INCOMPLETE"));
        org.mockito.Mockito.verifyNoInteractions(applicability);
    }

    @Test
    void recordsApplicabilityDecisionsWithTheGivenRevision() throws Exception {
        var decided = new ApplicabilityDecision(UUID.randomUUID(), PROCESS,
                ApplicabilityDecision.SubjectType.REQUIRED_SOURCE, SOURCE,
                ApplicabilityDecision.Decision.NOT_APPLICABLE, "Regla", "Justificaci\u00f3n completa sint\u00e9tica.",
                "Reglamento", "fixture://decision", ACTOR, Instant.parse("2026-10-20T15:00:00Z"), null, 0);
        when(applicability.decide(eq(PROCESS), any(ApplicabilityDecisionService.ApplicabilityInput.class)))
                .thenReturn(decided);

        mvc.perform(post("/processes/" + PROCESS + "/applicability-decisions")
                        .cookie(new Cookie("sec_session", TOKEN))
                        .header("X-CSRF-Token", CSRF)
                        .header("If-Match", "\"0\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(java.util.Map.of(
                                "subjectType", "REQUIRED_SOURCE",
                                "subjectId", SOURCE.toString(),
                                "decision", "NOT_APPLICABLE",
                                "ruleReference", "Regla",
                                "provenance", java.util.Map.of(
                                        "authorityAssignmentId", UUID.randomUUID().toString(),
                                        "legalBasis", "Reglamento",
                                        "documentReference", "fixture://decision",
                                        "reason", "Justificaci\u00f3n completa sint\u00e9tica.")))))
                .andExpect(status().isCreated())
                .andExpect(header().string("ETag", "\"0\""))
                .andExpect(jsonPath("$.decision").value("NOT_APPLICABLE"));
        org.mockito.Mockito.verify(applicability).decide(eq(PROCESS), any(ApplicabilityDecisionService.ApplicabilityInput.class));
    }

    @Test
    void submitsDraftsOnlyThroughTheService() throws Exception {
        EvidenceVersion submitted = new EvidenceVersion(
                VERSION, UUID.randomUUID(), 1, null,
                ec.edu.unl.sec.evidence.domain.EvidenceVersionStatus.SUBMITTED,
                "Informe", "Descripci\u00f3n", "Procedencia",
                java.time.LocalDate.of(2026, 1, 1), java.time.LocalDate.of(2026, 6, 30),
                ACTOR, Instant.parse("2026-10-20T15:00:00Z"), Instant.parse("2026-10-20T16:00:00Z"),
                1, List.of(new ec.edu.unl.sec.evidence.domain.VersionIndicator(
                        VERSION, UUID.fromString("00000000-0000-0000-0000-000000000401"), "IND-1", "Indicador", 1)));
        when(submission.submit(VERSION, "\"0\"")).thenReturn(submitted);

        mvc.perform(post("/evidence-versions/" + VERSION + "/submit")
                        .cookie(new Cookie("sec_session", TOKEN))
                        .header("X-CSRF-Token", CSRF)
                        .header("If-Match", "\"0\""))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("ETag"))
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
        assertThat(submitted.submittedAt()).isNotNull();
    }
}
