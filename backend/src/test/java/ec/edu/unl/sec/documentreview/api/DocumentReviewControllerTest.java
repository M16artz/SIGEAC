package ec.edu.unl.sec.documentreview.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.unl.sec.documentreview.application.DocumentReviewProblemException;
import ec.edu.unl.sec.documentreview.application.DocumentReviewService;
import ec.edu.unl.sec.documentreview.application.ReviewSubmissionPort;
import ec.edu.unl.sec.documentreview.domain.DocumentReviewDecision;
import ec.edu.unl.sec.documentreview.domain.ReviewCoverageOutcome;
import ec.edu.unl.sec.identityaccess.application.SessionService;
import ec.edu.unl.sec.identityaccess.infrastructure.AccessProblemWriter;
import ec.edu.unl.sec.identityaccess.infrastructure.SessionAuthenticationFilter;
import ec.edu.unl.sec.shared.api.ApiProblemHandler;
import ec.edu.unl.sec.shared.infrastructure.CorrelationFilter;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * T051: review-inbox and coverage-projection API behaviour: the inbox lists
 * only pending submissions, decisions require If-Match and validated outcomes,
 * version review reports PENDING_REVIEW without decisions and corrections
 * create successors linked to the triggering decision.
 */
class DocumentReviewControllerTest {
    private static final UUID ACTOR = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID PROCESS = UUID.fromString("00000000-0000-0000-0000-000000000301");
    private static final UUID CAREER = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private static final UUID EVIDENCE = UUID.fromString("00000000-0000-0000-0000-000000000401");
    private static final UUID VERSION = UUID.fromString("00000000-0000-0000-0000-000000000501");
    private static final UUID SOURCE = UUID.fromString("00000000-0000-0000-0000-000000000601");
    private static final UUID SOURCE_TWO = UUID.fromString("00000000-0000-0000-0000-000000000602");
    private static final UUID SUCCESSOR = UUID.fromString("00000000-0000-0000-0000-000000000502");
    private static final String CSRF = "synthetic-csrf-token-with-32-characters";
    private static final String TOKEN = "opaque-session";

    private DocumentReviewService reviews;
    private MockMvc mvc;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        reviews = org.mockito.Mockito.mock(DocumentReviewService.class);
        SessionService sessions = org.mockito.Mockito.mock(SessionService.class);
        when(sessions.authenticateForRequest(TOKEN))
                .thenReturn(Optional.of(new SessionService.AuthenticatedSession(
                        UUID.randomUUID(), ACTOR, "Responsable sint\u00e9tico", false,
                        SessionService.sha256Hex(CSRF))));
        mapper = new ObjectMapper().findAndRegisterModules();
        var sessionFilter = new SessionAuthenticationFilter(sessions, new AccessProblemWriter(mapper));
        mvc = MockMvcBuilders.standaloneSetup(new DocumentReviewController(reviews))
                .setControllerAdvice(new ApiProblemHandler(), new DocumentReviewProblemHandler())
                .addFilters(new CorrelationFilter(), sessionFilter)
                .build();
    }

    @Test
    void listsReviewerInboxEntriesWithETag() throws Exception {
        var pending = new ReviewSubmissionPort.PendingSubmission(
                VERSION, EVIDENCE, PROCESS, CAREER, 1, "Informe institucional", ACTOR,
                Instant.parse("2026-10-20T15:00:00Z"),
                List.of(UUID.fromString("00000000-0000-0000-0000-000000000402")),
                List.of(SOURCE));
        when(reviews.inbox(PROCESS)).thenReturn(List.of(pending));

        mvc.perform(get("/processes/" + PROCESS + "/document-review/inbox")
                        .cookie(new Cookie("sec_session", TOKEN))
                        .header("X-CSRF-Token", CSRF))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"1\""))
                .andExpect(jsonPath("$[0].versionId").value(VERSION.toString()))
                .andExpect(jsonPath("$[0].revisionNumber").value(1))
                .andExpect(jsonPath("$[0].coveredRequiredSourceIds[0]").value(SOURCE.toString()));
    }

    @Test
    void recordsCoverageDecisionsWithDerivedGlobalStatus() throws Exception {
        var outcomes = List.of(accepted(SOURCE, "Cobertura completa y verificada."));
        var decided = new DocumentReviewDecision(
                null, VERSION, DocumentReviewDecision.derive(outcomes),
                "Revisi\u00f3n documental aprobada con fundamento suficiente.",
                ACTOR, Instant.parse("2026-10-20T16:00:00Z"), false, null, null, null, 0, outcomes);
        when(reviews.decide(eq(VERSION), any(DocumentReviewService.ReviewInput.class))).thenReturn(decided);

        mvc.perform(post("/evidence-versions/" + VERSION + "/document-review")
                        .cookie(new Cookie("sec_session", TOKEN))
                        .header("X-CSRF-Token", CSRF)
                        .header("If-Match", "\"0\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "selfReviewException", false,
                                "justification", "Revisi\u00f3n documental aprobada con fundamento suficiente.",
                                "outcomes", List.of(Map.of(
                                        "requiredSourceId", SOURCE.toString(),
                                        "outcome", "ACCEPTED",
                                        "note", "Cobertura completa y verificada."))))))
                .andExpect(status().isCreated())
                .andExpect(header().string("ETag", "\"0\""))
                .andExpect(jsonPath("$.decision").value("ACCEPTED"))
                .andExpect(jsonPath("$.outcomes[0].outcome").value("ACCEPTED"));
    }

    @Test
    void requiresIfMatchBeforeRecordingAnyDecision() throws Exception {
        mvc.perform(post("/evidence-versions/" + VERSION + "/document-review")
                        .cookie(new Cookie("sec_session", TOKEN))
                        .header("X-CSRF-Token", CSRF)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "selfReviewException", false,
                                "justification", "Revisi\u00f3n documental aprobada con fundamento.",
                                "outcomes", List.of(Map.of(
                                        "requiredSourceId", SOURCE.toString(),
                                        "outcome", "ACCEPTED",
                                        "note", "Cobertura completa y verificada."))))))
                .andExpect(status().isPreconditionRequired())
                .andExpect(jsonPath("$.code").value("PRECONDITION_REQUIRED"));
        verifyNoInteractions(reviews);
    }

    @Test
    void rejectsOutcomeValuesThatAreNotDocumentaryOutcomes() throws Exception {
        mvc.perform(post("/evidence-versions/" + VERSION + "/document-review")
                        .cookie(new Cookie("sec_session", TOKEN))
                        .header("X-CSRF-Token", CSRF)
                        .header("If-Match", "\"0\"")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "selfReviewException", false,
                                "justification", "Revisi\u00f3n documental aprobada con fundamento.",
                                "outcomes", List.of(Map.of(
                                        "requiredSourceId", SOURCE.toString(),
                                        "outcome", "GRADED",
                                        "note", "Valoraci\u00f3n acad\u00e9mica prohibida."))))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_OUTCOME"));
        verifyNoInteractions(reviews);
    }

    @Test
    void reportsPendingReviewWhenTheVersionHasNoDecisionYet() throws Exception {
        when(reviews.versionReview(VERSION)).thenReturn(Optional.empty());

        mvc.perform(get("/evidence-versions/" + VERSION + "/document-review")
                        .cookie(new Cookie("sec_session", TOKEN))
                        .header("X-CSRF-Token", CSRF))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decisionId").doesNotExist())
                .andExpect(jsonPath("$.decision").value("PENDING_REVIEW"))
                .andExpect(jsonPath("$.outcomes").isEmpty());
    }

    @Test
    void createsCorrectiveSuccessorsLinkedToTheTriggeringDecision() throws Exception {
        var decided = java.util.List.of(accepted(SOURCE, "Cobertura observada por el revisor."));
        var decision = new DocumentReviewDecision(
                null, VERSION, DocumentReviewDecision.derive(decided),
                "Revisi\u00f3n documental observada con fundamento suficiente.",
                ACTOR, Instant.parse("2026-10-20T16:00:00Z"), false, null, null, null, 0, decided);
        when(reviews.versionReview(VERSION)).thenReturn(Optional.of(decision));
        when(reviews.correct(decision.id(), 0)).thenReturn(
                new DocumentReviewService.Correction(VERSION, SUCCESSOR, 2));

        mvc.perform(post("/document-review-decisions/" + decision.id() + "/correction")
                        .cookie(new Cookie("sec_session", TOKEN))
                        .header("X-CSRF-Token", CSRF)
                        .header("If-Match", "\"0\""))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        "/api/v1/evidence-versions/" + SUCCESSOR))
                .andExpect(jsonPath("$.correctedVersionId").value(VERSION.toString()))
                .andExpect(jsonPath("$.successorVersionId").value(SUCCESSOR.toString()))
                .andExpect(jsonPath("$.successorRevision").value(2));
    }

    @Test
    void rejectsStaleOrDoubleCorrectionGuardsFromUrls() throws Exception {
        var decided = List.of(accepted(SOURCE, "Cobertura observada por el revisor."));
        var decision = new DocumentReviewDecision(
                null, VERSION, DocumentReviewDecision.derive(decided),
                "Revisi\u00f3n documental observada con fundamento suficiente.",
                ACTOR, Instant.parse("2026-10-20T16:00:00Z"), false, null, null, SUCCESSOR, 0, decided);
        when(reviews.correct(decision.id(), 5)).thenThrow(new DocumentReviewProblemException(
                DocumentReviewProblemException.Kind.PRECONDITION_FAILED,
                "STALE_REVIEW", "La decisi\u00f3n cambi\u00f3."));

        mvc.perform(post("/document-review-decisions/" + decision.id() + "/correction")
                        .cookie(new Cookie("sec_session", TOKEN))
                        .header("X-CSRF-Token", CSRF)
                        .header("If-Match", "\"5\""))
                .andExpect(status().isPreconditionFailed())
                .andExpect(jsonPath("$.code").value("STALE_REVIEW"));
    }

    @Test
    void exposesDecisionHistoryForTheProcess() throws Exception {
        var outcomes = List.of(accepted(SOURCE, "Cobertura revisada por el revisor."),
                observed(SOURCE_TWO));
        var decision = new DocumentReviewDecision(
                null, VERSION, DocumentReviewDecision.derive(outcomes),
                "Revisi\u00f3n documental observada con fundamento suficiente.",
                ACTOR, Instant.parse("2026-10-20T16:00:00Z"), false, null, null, null, 0, outcomes);
        when(reviews.history(PROCESS)).thenReturn(List.of(decision));

        mvc.perform(get("/processes/" + PROCESS + "/document-review-decisions")
                        .cookie(new Cookie("sec_session", TOKEN))
                        .header("X-CSRF-Token", CSRF))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].decision").value("OBSERVED"))
                .andExpect(jsonPath("$[0].revision").value(0));
        assertThat(decision.successorVersionId()).isNull();
    }

    private static ReviewCoverageOutcome accepted(UUID source, String note) {
        return new ReviewCoverageOutcome(source, ReviewCoverageOutcome.Outcome.ACCEPTED, note);
    }

    private static ReviewCoverageOutcome observed(UUID source) {
        return new ReviewCoverageOutcome(source, ReviewCoverageOutcome.Outcome.OBSERVED,
                "Observación documental registrada para corregir.");
    }
}
