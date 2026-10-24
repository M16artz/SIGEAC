package ec.edu.unl.sec.documentreview.application;

import static org.assertj.core.api.Assertions.assertThat;

import ec.edu.unl.sec.documentreview.application.CoverageReviewProjectionService.EvidenceLineage;
import ec.edu.unl.sec.documentreview.application.CoverageReviewProjectionService.Projection;
import ec.edu.unl.sec.documentreview.application.CoverageReviewProjectionService.VersionStatus;
import ec.edu.unl.sec.documentreview.application.CoverageReviewProjectionService.VersionLineage;
import ec.edu.unl.sec.documentreview.domain.DocumentReviewDecision;
import ec.edu.unl.sec.documentreview.domain.ReviewCoverageOutcome;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Projection semantics for tracking (T119): AC-X06 derives per-coverage
 * states and a global observed status without any academic valuation, and
 * AC-X07 keeps an accepted version valid only until a successor is submitted.
 */
class CoverageReviewProjectionTest {
    private static final UUID EVIDENCE = UUID.fromString("00000000-0000-0000-0000-000000000d01");
    private static final UUID VERSION_ONE = UUID.fromString("00000000-0000-0000-0000-000000000d11");
    private static final UUID VERSION_TWO = UUID.fromString("00000000-0000-0000-0000-000000000d12");
    private static final UUID VERSION_THREE = UUID.fromString("00000000-0000-0000-0000-000000000d13");
    private static final UUID REVIEWER = UUID.fromString("00000000-0000-0000-0000-000000000c21");
    private static final UUID SOURCE_ONE = UUID.fromString("00000000-0000-0000-0000-000000000d21");
    private static final UUID SOURCE_TWO = UUID.fromString("00000000-0000-0000-0000-000000000d22");
    private static final Instant NOW = Instant.parse("2026-10-23T12:00:00Z");

    private final CoverageReviewProjectionService projection = new CoverageReviewProjectionService();

    @Test
    void projectsObservedCoverageWithoutInferringAcademicValuation() {
        var accepted = decision(VERSION_ONE, ReviewCoverageOutcome.Outcome.ACCEPTED,
                ReviewCoverageOutcome.Outcome.OBSERVED);
        var result = projection.project(new EvidenceLineage(EVIDENCE, List.of(
                new VersionLineage(VERSION_ONE, 1, "SUBMITTED", List.of(SOURCE_ONE, SOURCE_TWO),
                        Optional.of(accepted)))));
        assertThat(result.states().size()).isEqualTo(2);
        var bySourceZip = result.states().stream().map(state -> state.status()).toList();
        assertThat(bySourceZip).containsExactly(VersionStatus.ACCEPTED, VersionStatus.OBSERVED);
        var fieldNames = java.util.Arrays.stream(
                        CoverageReviewProjectionService.Projection.class.getDeclaredFields())
                .map(java.lang.reflect.Field::getName)
                .collect(Collectors.toSet());
        assertThat(fieldNames).noneMatch(name -> name.toLowerCase().contains("academic"))
                .as("la proyección documental no transporta valoración académica");
        assertThat(projection.documentReviewStatus(accepted.outcomes()))
                .isEqualTo(DocumentReviewDecision.Decision.OBSERVED);
    }

    @Test
    void acceptedVersionStaysValidWhileItsSuccessorIsOnlyADraft() {
        var accepted = decision(VERSION_ONE, ReviewCoverageOutcome.Outcome.ACCEPTED,
                ReviewCoverageOutcome.Outcome.ACCEPTED);
        var result = projection.project(new EvidenceLineage(EVIDENCE, List.of(
                new VersionLineage(VERSION_TWO, 2, "DRAFT", List.of(SOURCE_ONE, SOURCE_TWO), Optional.empty()),
                new VersionLineage(VERSION_ONE, 1, "SUBMITTED", List.of(SOURCE_ONE, SOURCE_TWO),
                        Optional.of(accepted)))));
        assertThat(result.states().stream().map(state -> state.status()))
                .containsExactly(VersionStatus.ACCEPTED, VersionStatus.ACCEPTED)
                .as("AC-X07: un borrador sucesor no anula la aceptación vigente de v1");
    }

    @Test
    void successorsSubmissionVoidsThePendingAcceptance() {
        var accepted = decision(VERSION_ONE, ReviewCoverageOutcome.Outcome.ACCEPTED,
                ReviewCoverageOutcome.Outcome.ACCEPTED);
        var result = projection.project(new EvidenceLineage(EVIDENCE, List.of(
                new VersionLineage(VERSION_TWO, 2, "SUBMITTED", List.of(SOURCE_ONE, SOURCE_TWO), Optional.empty()),
                new VersionLineage(VERSION_ONE, 1, "SUBMITTED", List.of(SOURCE_ONE, SOURCE_TWO),
                        Optional.of(accepted)))));
        var versionOneStates = result.states().stream()
                .filter(state -> state.currentVersionId().equals(VERSION_ONE))
                .toList();
        assertThat(result.states().stream().map(state -> state.status()))
                .containsOnly(VersionStatus.PENDING_REVIEW)
                .as("AC-X07: la remisión de v2 sin decisión anula la aceptación de v1");
        assertThat(versionOneStates).isEmpty();
    }

    @Test
    void submittedSuccessorDoesNotResurrectWithdrawnCoverageFromAnOlderVersion() {
        var accepted = decision(VERSION_ONE, ReviewCoverageOutcome.Outcome.ACCEPTED,
                ReviewCoverageOutcome.Outcome.ACCEPTED);
        var result = projection.project(new EvidenceLineage(EVIDENCE, List.of(
                new VersionLineage(VERSION_ONE, 1, "SUBMITTED", List.of(SOURCE_ONE, SOURCE_TWO),
                        Optional.of(accepted)),
                new VersionLineage(VERSION_THREE, 3, "DRAFT", List.of(SOURCE_ONE), Optional.empty()),
                new VersionLineage(VERSION_TWO, 2, "SUBMITTED", List.of(SOURCE_TWO), Optional.empty()))));

        assertThat(result.sources()).containsExactly(SOURCE_TWO);
        assertThat(result.states()).containsExactly(new CoverageReviewProjectionService.SourceReviewState(
                VERSION_TWO, VersionStatus.PENDING_REVIEW));
    }

    @Test
    void draftOnlyLineageDoesNotContributeCoverage() {
        var result = projection.project(new EvidenceLineage(EVIDENCE, List.of(
                new VersionLineage(VERSION_ONE, 1, "DRAFT", List.of(SOURCE_ONE), Optional.empty()))));
        assertThat(result.sources()).isEmpty();
        assertThat(result.states()).isEmpty();
    }

    @Test
    void keepsPerSourceOwnershipAfterRectification() {
        var first = decision(VERSION_ONE, ReviewCoverageOutcome.Outcome.REJECTED,
                ReviewCoverageOutcome.Outcome.ACCEPTED);
        var rectified = first.next(
                "Rectificación documental que corrige el rechazo previo de la segunda cobertura.",
                false, null, REVIEWER, NOW.plusSeconds(60), List.of(
                        new ReviewCoverageOutcome(SOURCE_ONE, ReviewCoverageOutcome.Outcome.ACCEPTED,
                                "Cobertura saneada en la rectificación"),
                        new ReviewCoverageOutcome(SOURCE_TWO, ReviewCoverageOutcome.Outcome.ACCEPTED,
                                "Cobertura reinspeccionada en la rectificación")));
        var result = projection.project(new EvidenceLineage(EVIDENCE, List.of(
                new VersionLineage(VERSION_ONE, 1, "SUBMITTED", List.of(SOURCE_ONE, SOURCE_TWO),
                        Optional.of(rectified)))));
        assertThat(result.states().stream().map(state -> state.status()))
                .containsExactly(VersionStatus.ACCEPTED, VersionStatus.ACCEPTED);
    }

    private DocumentReviewDecision decision(UUID version, ReviewCoverageOutcome.Outcome first,
            ReviewCoverageOutcome.Outcome second) {
        var outcomes = List.of(
                new ReviewCoverageOutcome(SOURCE_ONE, first, "Resultado documental determinado por el revisor."),
                new ReviewCoverageOutcome(SOURCE_TWO, second, "Resultado documental determinado por el revisor."));
        return new DocumentReviewDecision(
                null, version, DocumentReviewDecision.derive(outcomes),
                "Revisión documental justificada para la proyección de cobertura.",
                REVIEWER, NOW, false, null, null, null, 0, outcomes);
    }
}
