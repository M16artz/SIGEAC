package ec.edu.unl.sec.documentreview.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.edu.unl.sec.documentreview.application.CoverageReviewProjectionService;
import ec.edu.unl.sec.documentreview.application.CoverageReviewProjectionService.EvidenceLineage;
import ec.edu.unl.sec.documentreview.application.CoverageReviewProjectionService.SourceReviewState;
import ec.edu.unl.sec.documentreview.application.CoverageReviewProjectionService.VersionLineage;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * D5 domain separation and rectification rules (T050): decisions derive the
 * global status from coverage outcomes, keep the previous decision when
 * rectifying (FR-024), never encode an academic level (FR-023) and leave no
 * structural way to record acceptance for the whole version at once.
 */
class CompleteDocumentReviewTest {
    private static final UUID VERSION = UUID.fromString("00000000-0000-0000-0000-000000000c01");
    private static final UUID SOURCE_ONE = UUID.fromString("00000000-0000-0000-0000-000000000c11");
    private static final UUID SOURCE_TWO = UUID.fromString("00000000-0000-0000-0000-000000000c12");
    private static final UUID REVIEWER = UUID.fromString("00000000-0000-0000-0000-000000000c21");
    private static final Instant NOW = Instant.parse("2026-10-23T12:00:00Z");

    @Test
    void derivesObservationWhenOneCoverageIsAcceptedAndAnotherObserved() {
        var outcomes = List.of(
                accepted(SOURCE_ONE, "Cobertura completa y verificada."),
                observed(SOURCE_TWO));
        var decision = new DocumentReviewDecision(
                null, VERSION, DocumentReviewDecision.derive(outcomes),
                "Observación documental registrada por cobertura, sin implicación académica.",
                REVIEWER, NOW, false, null, null, null, 0, outcomes);
        assertThat(decision.decision()).isEqualTo(DocumentReviewDecision.Decision.OBSERVED);
    }

    @Test
    void rejectsWholeVersionDecisionsThatAreNotDerivedFromOutcomes() {
        var outcomes = List.of(accepted(SOURCE_ONE, "Cobertura completa y verificada."));
        assertThatThrownBy(() -> new DocumentReviewDecision(
                null, VERSION, DocumentReviewDecision.Decision.OBSERVED, "Justificación de diez caracteres sintético.",
                REVIEWER, NOW, false, null, null, null, 0, outcomes))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void requiresDistinctOutcomesForEveryDeclaredCoverage() {
        var outcome = accepted(SOURCE_ONE, "Cobertura completa y verificada.");
        assertThatThrownBy(() -> new DocumentReviewDecision(
                null, VERSION, DocumentReviewDecision.derive(List.of(outcome)),
                "Revisión documental duplicada para el guard de coberturas.",
                REVIEWER, NOW, false, null, null, null, 0, List.of(outcome, outcome)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate outcome source");
    }

    @Test
    void requiresFoundationForObservedOrRejectedCoverageOutcomes() {
        assertThatThrownBy(() -> observedWithoutFoundation())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("outcome note");
    }

    @Test
    void recordsRectificationWithoutOverwritingThePreviousDecision() {
        var first = decision(
                List.of(accepted(SOURCE_ONE, "Cobertura completa y verificada."),
                        observed(SOURCE_TWO)),
                List.of(
                        accepted(SOURCE_ONE, "Cobertura completa y verificada."),
                        observed(SOURCE_TWO)));
        var rectified = first.next(
                "Rectificación: la segunda cobertura continúa observada.",
                false, null, REVIEWER, NOW.plusSeconds(60),
                List.of(accepted(SOURCE_ONE, "Cobertura completa y verificada."),
                        observed(SOURCE_TWO)));
        assertThat(rectified.revision()).isEqualTo(1);
        assertThat(rectified.predecessorId()).isEqualTo(first.id());
        assertThat(rectified.versionId()).isEqualTo(first.versionId());
        assertThat(first.predecessorId()).isNull();
    }

    @Test
    void blocksSelfReviewWithoutAnExplicitAuditableException() {
        var outcomes = List.of(accepted(SOURCE_ONE, "Cobertura completa y verificada."));
        assertThatThrownBy(() -> new DocumentReviewDecision(
                null, VERSION, DocumentReviewDecision.derive(outcomes), "Justificación de diez caracteres.",
                REVIEWER, NOW, true, null, null, null, 0, outcomes))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exception reason");
        var excepted = new DocumentReviewDecision(
                null, VERSION, DocumentReviewDecision.derive(outcomes), "Justificación de diez caracteres.",
                REVIEWER, NOW, true, "Excepción autorizada por la coordinación del proceso.",
                null, null, 0, outcomes);
        assertThat(excepted.selfReviewException()).isTrue();
    }

    @Test
    void projectsPerCoverageResultsAndGlobalStatusWithoutAcademicValuation() {
        var outcomes = List.of(
                accepted(SOURCE_ONE, "Cobertura completa y verificada."),
                observed(SOURCE_TWO));
        var decision = new DocumentReviewDecision(
                null, VERSION, DocumentReviewDecision.derive(outcomes),
                "Se observa la cobertura documental en curso.",
                REVIEWER, NOW, false, null, null, null, 0, outcomes);
        var projection = new CoverageReviewProjectionService().project(
                new EvidenceLineage(UUID.randomUUID(), List.of(
                        new VersionLineage(VERSION, 1, "SUBMITTED", List.of(SOURCE_ONE, SOURCE_TWO),
                                Optional.of(decision)))));
        var states = projection.states();
        assertThat(states.get(0).status())
                .isEqualTo(CoverageReviewProjectionService.VersionStatus.ACCEPTED);
        assertThat(states.get(1).status())
                .isEqualTo(CoverageReviewProjectionService.VersionStatus.OBSERVED);
        assertThat(projection.sources()).containsExactlyInAnyOrder(SOURCE_ONE, SOURCE_TWO);
    }

    private static DocumentReviewDecision decision(
            List<ReviewCoverageOutcome> first, List<ReviewCoverageOutcome> outcomes) {
        return new DocumentReviewDecision(
                null, VERSION, DocumentReviewDecision.derive(outcomes),
                "Revisión documental de la versión remitida.",
                REVIEWER, NOW, false, null, null, null, 0, first);
    }

    private static ReviewCoverageOutcome accepted(UUID source, String note) {
        return new ReviewCoverageOutcome(source, ReviewCoverageOutcome.Outcome.ACCEPTED, note);
    }

    private static ReviewCoverageOutcome observed(UUID source) {
        return new ReviewCoverageOutcome(source, ReviewCoverageOutcome.Outcome.OBSERVED,
                "Observación documental de la cobertura para corregir.");
    }

    private static DocumentReviewDecision observedWithoutFoundation() {
        return new DocumentReviewDecision(
                null, VERSION, DocumentReviewDecision.derive(List.of(
                        new ReviewCoverageOutcome(SOURCE_ONE, ReviewCoverageOutcome.Outcome.OBSERVED, null))),
                "Justificación global de diez caracteres.", REVIEWER, NOW, false, null, null, null, 0,
                List.of(new ReviewCoverageOutcome(SOURCE_ONE, ReviewCoverageOutcome.Outcome.OBSERVED, null)));
    }
}
