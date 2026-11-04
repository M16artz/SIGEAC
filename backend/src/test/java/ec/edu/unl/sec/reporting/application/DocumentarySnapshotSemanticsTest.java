package ec.edu.unl.sec.reporting.application;

import static org.assertj.core.api.Assertions.assertThat;

import ec.edu.unl.sec.documentreview.application.CoverageReviewProjectionService;
import ec.edu.unl.sec.documentreview.application.CoverageReviewProjectionService.EvidenceLineage;
import ec.edu.unl.sec.documentreview.application.CoverageReviewProjectionService.VersionLineage;
import ec.edu.unl.sec.documentreview.application.CoverageReviewProjectionService.VersionStatus;
import ec.edu.unl.sec.documentreview.domain.DocumentReviewDecision;
import ec.edu.unl.sec.documentreview.domain.ReviewCoverageOutcome;
import ec.edu.unl.sec.reporting.domain.DocumentaryProgressCalculator;
import ec.edu.unl.sec.reporting.domain.DocumentaryProgressCalculator.Coverage;
import ec.edu.unl.sec.reporting.domain.DocumentaryProgressCalculator.Source;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Current-candidate semantics only; persistence and documentary closure remain separate tasks. */
class DocumentarySnapshotSemanticsTest {
    private final CoverageReviewProjectionService projection = new CoverageReviewProjectionService();
    private final DocumentaryProgressCalculator calculator = new DocumentaryProgressCalculator();
    private final UUID source = UUID.randomUUID();
    private final UUID evidence = UUID.randomUUID();
    private final UUID versionOne = UUID.randomUUID();
    private final UUID versionTwo = UUID.randomUUID();

    @Test
    void draftKeepsAcceptanceButSubmissionRequiresANewDecision() {
        var accepted = acceptedVersion(versionOne, 1);
        var draft = new VersionLineage(versionTwo, 2, "DRAFT", List.of(source), Optional.empty());
        var before = progress(List.of(new EvidenceLineage(evidence, List.of(accepted, draft))), 1);
        assertThat(before.acceptedSourceTotal()).isEqualTo(1);

        var submitted = new VersionLineage(versionTwo, 2, "SUBMITTED", List.of(source), Optional.empty());
        var after = progress(List.of(new EvidenceLineage(evidence, List.of(accepted, submitted))), 1);
        assertThat(after.registeredSourceTotal()).isEqualTo(1);
        assertThat(after.acceptedSourceTotal()).isZero();
        assertThat(before.acceptedSourceTotal()).isEqualTo(1);
    }

    @Test
    void withdrawalRemovesCoverageButDoesNotRemoveAnotherEvidencesAcceptance() {
        var original = acceptedVersion(versionOne, 1);
        var withdrawn = new VersionLineage(versionTwo, 2, "SUBMITTED", List.of(), Optional.empty());
        var lineage = new EvidenceLineage(evidence, List.of(original, withdrawn));
        var result = progress(List.of(lineage), 1);
        assertThat(result.registeredSourceTotal()).isZero();
        assertThat(result.acceptedSourceTotal()).isZero();

        var independent = new EvidenceLineage(UUID.randomUUID(), List.of(acceptedVersion(UUID.randomUUID(), 1)));
        result = progress(List.of(lineage, independent), 1);
        assertThat(result.registeredSourceTotal()).isEqualTo(1);
        assertThat(result.acceptedSourceTotal()).isEqualTo(1);
    }

    @Test
    void twoAcceptedRevisionsOfOneEvidenceDoNotSatisfyCardinalityOfTwo() {
        var lineage = new EvidenceLineage(evidence,
                List.of(acceptedVersion(versionOne, 1), acceptedVersion(versionTwo, 2)));
        var result = progress(List.of(lineage), 2);
        assertThat(result.registeredSourceTotal()).isEqualTo(1);
        assertThat(result.acceptedSourceTotal()).isZero();

        var independent = new EvidenceLineage(UUID.randomUUID(), List.of(acceptedVersion(UUID.randomUUID(), 1)));
        result = progress(List.of(lineage, independent), 2);
        assertThat(result.acceptedSourceTotal()).isEqualTo(1);
    }

    private DocumentaryProgressCalculator.Progress progress(List<EvidenceLineage> lineages, int minimum) {
        var coverages = new ArrayList<Coverage>();
        for (var lineage : lineages) {
            var projected = projection.project(lineage);
            for (int index = 0; index < projected.sources().size(); index++) {
                coverages.add(new Coverage(projected.sources().get(index), lineage.evidenceId(),
                        projected.states().get(index).status() == VersionStatus.ACCEPTED));
            }
        }
        return calculator.calculate(List.of(new Source(source, true, true, minimum)), coverages);
    }

    private VersionLineage acceptedVersion(UUID versionId, int revision) {
        var outcome = new ReviewCoverageOutcome(source, ReviewCoverageOutcome.Outcome.ACCEPTED, null);
        var decision = new DocumentReviewDecision(null, versionId, DocumentReviewDecision.Decision.ACCEPTED,
                "Aceptación documental sintética para verificar el cálculo de seguimiento.",
                UUID.randomUUID(), Instant.parse("2026-10-24T12:00:00Z"), false, null, null, null, 0,
                List.of(outcome));
        return new VersionLineage(versionId, revision, "SUBMITTED", List.of(source), Optional.of(decision));
    }
}
