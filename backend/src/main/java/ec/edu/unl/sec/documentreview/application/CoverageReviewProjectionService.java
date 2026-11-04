package ec.edu.unl.sec.documentreview.application;

import ec.edu.unl.sec.documentreview.domain.DocumentReviewDecision;
import ec.edu.unl.sec.documentreview.domain.ReviewCoverageOutcome;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Documentary projection of review outcomes across one evidence lineage
 * (AC-X06, AC-X07): per-source review state derives only from the newest submitted
 * version of the evidence; draft successors do not void an earlier
 * acceptance, submission of the successor does, and a documentary projection
 * never carries an academic valuation (FR-023).
 */
@Service
public class CoverageReviewProjectionService {

    public Projection project(EvidenceLineage lineage) {
        var states = new LinkedHashMap<UUID, SourceReviewState>();
        var currentVersion = lineage.versions().stream()
                .filter(version -> "SUBMITTED".equals(version.status()))
                .max(Comparator.comparingInt(VersionLineage::revisionNumber));
        // A submitted successor replaces the whole coverage set, including withdrawals.
        for (var version : currentVersion.stream().toList()) {
            for (var sourceId : version.coveredSourceIds()) {
                if (states.containsKey(sourceId)) continue;
                var decision = version.decision();
                var state = decision
                        .map(documentReview -> reviewState(sourceId, version, documentReview))
                        .orElseGet(() -> new SourceReviewState(version.versionId(), VersionStatus.PENDING_REVIEW));
                states.put(sourceId, state);
            }
        }
        var current = states.values().stream()
                .collect(java.util.stream.Collectors.toList());
        return new Projection(new ArrayList<>(states.keySet()), current);
    }

    /**
     * Per-source state from the triggering decision: a full acceptance is
     * accepted, observations leave the coverage observed (AC-X06).
     */
    private SourceReviewState reviewState(UUID sourceId, VersionLineage version, DocumentReviewDecision decision) {
        var outcome = decision.outcomes().stream()
                .filter(candidate -> candidate.requiredSourceId().equals(sourceId))
                .findFirst();
        if (outcome.isEmpty()) {
            return new SourceReviewState(version.versionId(), VersionStatus.PENDING_REVIEW);
        }
        return switch (outcome.get().outcome()) {
            case ACCEPTED -> new SourceReviewState(version.versionId(), VersionStatus.ACCEPTED);
            case OBSERVED -> new SourceReviewState(version.versionId(), VersionStatus.OBSERVED);
            case REJECTED -> new SourceReviewState(version.versionId(), VersionStatus.REJECTED);
        };
    }

    /** Global derivation exposed for dashboards without any academic inference. */
    public DocumentReviewDecision.Decision documentReviewStatus(List<ReviewCoverageOutcome> outcomes) {
        return DocumentReviewDecision.derive(outcomes);
    }

    public enum VersionStatus {
        ACCEPTED, OBSERVED, REJECTED, PENDING_REVIEW
    }

    public record VersionLineage(UUID versionId, int revisionNumber, String status, List<UUID> coveredSourceIds,
            Optional<DocumentReviewDecision> decision) {}

    public record EvidenceLineage(UUID evidenceId, List<VersionLineage> versions) {}

    public record SourceReviewState(UUID currentVersionId, VersionStatus status) {}

    public record Projection(List<UUID> sources, List<SourceReviewState> states) {}
}
