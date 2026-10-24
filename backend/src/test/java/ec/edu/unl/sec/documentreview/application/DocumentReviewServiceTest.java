package ec.edu.unl.sec.documentreview.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ec.edu.unl.sec.audit.application.AuditPort;
import ec.edu.unl.sec.documentreview.domain.DocumentReviewDecision;
import ec.edu.unl.sec.documentreview.domain.ReviewCoverageOutcome;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Documentary review service rules (T053, T119): authorized reviewers,
 * self-review blocked without an explicit auditable exception, revision
 * chains for rectification and append-only outcomes per declared coverage.
 */
class DocumentReviewServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-23T12:00:00Z");
    private static final UUID REVIEWER = UUID.fromString("00000000-0000-0000-0000-000000000c21");
    private static final UUID SUBMITTER = UUID.fromString("00000000-0000-0000-0000-000000000c31");
    private static final UUID CAREER = UUID.fromString("00000000-0000-0000-0000-000000000c41");
    private static final UUID PROCESS = UUID.fromString("00000000-0000-0000-0000-000000000c42");
    private static final UUID EVIDENCE = UUID.fromString("00000000-0000-0000-0000-000000000c43");
    private static final UUID VERSION = UUID.fromString("00000000-0000-0000-0000-000000000c01");
    private static final UUID SOURCE = UUID.fromString("00000000-0000-0000-0000-000000000c11");

    private ReviewSubmissionPort submissions;
    private DocumentReviewRepository repository;
    private DocumentReviewAuthorizationPort authorization;
    private SubmissionCorrectionPort correction;
    private AuditPort audit;
    private DocumentReviewService service;
    private ReviewSubmissionPort.SubmittedVersion submission;

    @BeforeEach
    void setUp() {
        submissions = org.mockito.Mockito.mock(ReviewSubmissionPort.class);
        repository = org.mockito.Mockito.mock(DocumentReviewRepository.class);
        authorization = org.mockito.Mockito.mock(DocumentReviewAuthorizationPort.class);
        correction = org.mockito.Mockito.mock(SubmissionCorrectionPort.class);
        audit = org.mockito.Mockito.mock(AuditPort.class);
        service = new DocumentReviewService(submissions, repository, authorization, correction, audit,
                Clock.fixed(NOW, ZoneOffset.UTC));
        submission = new ReviewSubmissionPort.SubmittedVersion(
                VERSION, EVIDENCE, PROCESS, CAREER, 1, SUBMITTER,
                List.of(UUID.randomUUID()), List.of(SOURCE));
        when(submissions.submission(VERSION)).thenReturn(Optional.of(submission));
        when(submissions.latestRevisionNumber(EVIDENCE)).thenReturn(1L);
        when(authorization.currentActor()).thenReturn(Optional.of(
                new DocumentReviewAuthorizationPort.Actor(REVIEWER, false)));
        when(authorization.canReview(REVIEWER, CAREER, PROCESS)).thenReturn(true);
        when(submissions.career(PROCESS)).thenReturn(Optional.of(CAREER));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void recordsADerivedDecisionWithAppendOnlyChain() {
        var decided = service.decide(VERSION, new DocumentReviewService.ReviewInput(
                0, false, null, "Revisión documental aprobada con fundamento suficiente.",
                List.of(new ReviewCoverageOutcome(SOURCE, ReviewCoverageOutcome.Outcome.ACCEPTED,
                        "Cobertura completa y verificada."))));

        assertThat(decided.revision()).isZero();
        assertThat(decided.predecessorId()).isNull();
        assertThat(decided.successorVersionId()).isNull();
        assertThat(decided.decision()).isEqualTo(DocumentReviewDecision.Decision.ACCEPTED);
        verify(audit).record(any());
        var saved = org.mockito.ArgumentCaptor.forClass(DocumentReviewDecision.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().versionId()).isEqualTo(VERSION);
        assertThat(saved.getValue().outcomes().getFirst().requiredSourceId()).isEqualTo(SOURCE);
    }

    @Test
    void appendsRectificationsToTheSameVersion() {
        var first = decisionOf(REVIEWER, List.of(new ReviewCoverageOutcome(SOURCE,
                ReviewCoverageOutcome.Outcome.OBSERVED, "Cobertura observada por el revisor sintético.")));
        when(repository.latest(VERSION)).thenReturn(Optional.of(first));
        when(repository.decision(first.id())).thenReturn(Optional.of(first));

        var rectified = service.decide(VERSION, new DocumentReviewService.ReviewInput(
                1, false, null, "Rectificación documental tras corrección del responsable.",
                List.of(new ReviewCoverageOutcome(SOURCE, ReviewCoverageOutcome.Outcome.ACCEPTED,
                        "Cobertura completa y verificada."))
        ));

        assertThat(rectified.revision()).isEqualTo(1);
        assertThat(rectified.predecessorId()).isEqualTo(first.id());
        assertThat(rectified.decision()).isEqualTo(DocumentReviewDecision.Decision.ACCEPTED);
    }

    @Test
    void rejectsUnauthorizedOrTechnicalReviewers() {
        when(authorization.canReview(any(), any(), any())).thenReturn(false);
        assertForbidden(() -> service.decide(VERSION, input(0)), "REVIEW_NOT_AUTHORIZED");

        when(authorization.currentActor()).thenReturn(Optional.of(
                new DocumentReviewAuthorizationPort.Actor(UUID.randomUUID(), true)));
        assertForbidden(() -> service.inbox(PROCESS), "REVIEW_NOT_AUTHORIZED");
    }

    @Test
    void blocksSelfReviewWithoutAnExplicitException() {
        when(authorization.currentActor()).thenReturn(Optional.of(
                new DocumentReviewAuthorizationPort.Actor(SUBMITTER, false)));
        when(authorization.canReview(SUBMITTER, CAREER, PROCESS)).thenReturn(true);
        assertForbidden(() -> service.decide(VERSION, input(0)), "SELF_REVIEW_FORBIDDEN");

        var excepted = service.decide(VERSION, new DocumentReviewService.ReviewInput(
                0, true, "Excepción autorizada por la coordinación del proceso.",
                "Revisión excepcional documentada con decisión competente.",
                List.of(new ReviewCoverageOutcome(SOURCE, ReviewCoverageOutcome.Outcome.ACCEPTED,
                        "Cobertura completa y verificada."))));
        assertThat(excepted.selfReviewException()).isTrue();
    }

    @Test
    void rejectsStaleDecisionRevisions() {
        var first = decisionOf(REVIEWER, List.of(new ReviewCoverageOutcome(SOURCE,
                ReviewCoverageOutcome.Outcome.REJECTED, "Cobertura rechazada por el revisor sintético.")));
        when(repository.latest(VERSION)).thenReturn(Optional.of(first));
        assertProblem(() -> service.decide(VERSION, input(0)),
                DocumentReviewProblemException.Kind.PRECONDITION_FAILED, "STALE_REVIEW");
    }

    @Test
    void rejectsSupersededVersionsAndUnknownCoverages() {
        when(submissions.latestRevisionNumber(EVIDENCE)).thenReturn(2L);
        assertProblem(() -> service.decide(VERSION, input(0)),
                DocumentReviewProblemException.Kind.CONFLICT, "VERSION_SUPERSEDED");
        when(submissions.latestRevisionNumber(EVIDENCE)).thenReturn(1L);
        assertProblem(() -> service.decide(VERSION, new DocumentReviewService.ReviewInput(
                0, false, null, "Revisión documental aprobada con fundamento suficiente.",
                List.of(new ReviewCoverageOutcome(UUID.randomUUID(),
                        ReviewCoverageOutcome.Outcome.ACCEPTED, "Cobertura completa y verificada.")))),
                DocumentReviewProblemException.Kind.INVALID, "OUTCOME_WITHOUT_COVERAGE");
    }

    @Test
    void createsCorrectionSuccessorsLinkedToTheTriggeringDecision() {
        var decision = decisionOf(REVIEWER, List.of(new ReviewCoverageOutcome(SOURCE,
                ReviewCoverageOutcome.Outcome.OBSERVED, "Cobertura observada por el revisor sintético.")));
        when(repository.decision(decision.id())).thenReturn(Optional.of(decision));
        when(correction.correct(VERSION, decision.id())).thenReturn(
                new SubmissionCorrectionPort.Successor(
                        UUID.fromString("00000000-0000-0000-0000-000000000c02"), EVIDENCE, 2));

        var correctionResult = service.correct(decision.id(), 0);

        assertThat(correctionResult.correctedVersionId()).isEqualTo(VERSION);
        assertThat(correctionResult.successorRevision()).isEqualTo(2);
        verify(correction).correct(VERSION, decision.id());
        verify(audit).record(any());
    }

    @Test
    void rejectsDoubleCorrectionsAndStaleLinks() {
        var decided = decisionOf(REVIEWER, List.of(new ReviewCoverageOutcome(SOURCE,
                ReviewCoverageOutcome.Outcome.OBSERVED, "Cobertura observada por el revisor sintético.")));
        var linked = new DocumentReviewDecision(
                decided.id(), decided.versionId(), decided.decision(), decided.justification(),
                decided.reviewerId(), decided.reviewedAt(), decided.selfReviewException(),
                decided.exceptionReason(), null,
                UUID.fromString("00000000-0000-0000-0000-000000000c02"), decided.revision(),
                decided.outcomes());
        when(repository.decision(decided.id())).thenReturn(Optional.of(linked));

        assertProblem(() -> service.correct(decided.id(), 0),
                DocumentReviewProblemException.Kind.CONFLICT, "SUCCESSOR_EXISTS");

        when(repository.decision(decided.id())).thenReturn(Optional.of(decided));
        assertProblem(() -> service.correct(decided.id(), 9),
                DocumentReviewProblemException.Kind.PRECONDITION_FAILED, "STALE_REVIEW");
        verifyNoInteractions(correction);
        verify(audit, never()).record(any());
    }

    private static DocumentReviewService.ReviewInput input(long expected) {
        return new DocumentReviewService.ReviewInput(expected, false, null,
                "Revisión documental aprobada con fundamento suficiente.",
                List.of(new ReviewCoverageOutcome(SOURCE, ReviewCoverageOutcome.Outcome.ACCEPTED,
                        "Cobertura completa y verificada.")));
    }

    private DocumentReviewDecision decisionOf(UUID reviewer, List<ReviewCoverageOutcome> outcomes) {
        return new DocumentReviewDecision(
                null, VERSION, DocumentReviewDecision.derive(outcomes),
                "Revisión documental inicial con fundamento suficiente.",
                reviewer, NOW.minusSeconds(600), false, null, null, null, 0, outcomes);
    }

    private static void assertForbidden(java.util.function.Supplier<?> operation, String code) {
        assertProblem(operation, DocumentReviewProblemException.Kind.FORBIDDEN, code);
    }

    private static void assertProblem(java.util.function.Supplier<?> operation,
            DocumentReviewProblemException.Kind kind, String code) {
        assertThatThrownBy(operation::get)
                .isInstanceOf(DocumentReviewProblemException.class)
                .satisfies(error -> {
                    assertThat(((DocumentReviewProblemException) error).kind()).isEqualTo(kind);
                    assertThat(((DocumentReviewProblemException) error).code()).isEqualTo(code);
                });
    }
}
