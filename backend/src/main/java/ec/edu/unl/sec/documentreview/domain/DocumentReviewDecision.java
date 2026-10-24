package ec.edu.unl.sec.documentreview.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Append-only documentary decision for one submitted evidence version. The
 * global status is always derived from the per-coverage outcomes (FR-022),
 * never accepted for the whole version at once, and has no academic level
 * attached (FR-023). Rectifications chain a new decision through
 * {@code predecessorId} preserving the previous one (FR-024);
 * {@code successorVersionId} links the corrective draft when the documentary
 * responsible fixes the version (P10), so double successors are impossible.
 */
public record DocumentReviewDecision(UUID id, UUID versionId, Decision decision, String justification,
        UUID reviewerId, Instant reviewedAt, boolean selfReviewException, String exceptionReason,
        UUID predecessorId, UUID successorVersionId, long revision, List<ReviewCoverageOutcome> outcomes) {
    public enum Decision { ACCEPTED, OBSERVED, REJECTED }

    public DocumentReviewDecision {
        id = id == null ? UUID.randomUUID() : id;
        Objects.requireNonNull(versionId, "version required");
        Objects.requireNonNull(decision, "decision required");
        Objects.requireNonNull(reviewerId, "reviewer required");
        justification = text(justification, 10, "justification");
        reviewedAt = reviewedAt == null ? Instant.now() : reviewedAt;
        if (selfReviewException) exceptionReason = text(exceptionReason, 10, "exception reason");
        if (revision < 0) throw new IllegalArgumentException("revision must be non-negative");
        if ((revision == 0) != (predecessorId == null))
            throw new IllegalArgumentException("invalid predecessor");
        outcomes = outcomes == null ? List.of() : List.copyOf(outcomes);
        if (outcomes.isEmpty()) throw new IllegalArgumentException("coverage outcomes required");
        if (outcomes.stream().map(ReviewCoverageOutcome::requiredSourceId).collect(Collectors.toSet()).size()
                != outcomes.size())
            throw new IllegalArgumentException("duplicate outcome source");
        var derived = derive(outcomes);
        if (derived != decision)
            throw new IllegalArgumentException("decision must be derived from coverage outcomes");
    }

    /**
     * Global documentary state derives from all coverage outcomes: any
     * rejection rejects the version, any unresolved observation leaves it
     * observed, and only a complete acceptance is accepted (AC-X06).
     */
    public static Decision derive(List<ReviewCoverageOutcome> outcomes) {
        boolean rejected = outcomes.stream()
                .anyMatch(outcome -> outcome.outcome() == ReviewCoverageOutcome.Outcome.REJECTED);
        if (rejected) return Decision.REJECTED;
        boolean observed = outcomes.stream()
                .anyMatch(outcome -> outcome.outcome() == ReviewCoverageOutcome.Outcome.OBSERVED);
        return observed ? Decision.OBSERVED : Decision.ACCEPTED;
    }

    /** Validates that every outcome refers to a coverage declared on the version. */
    public static List<ReviewCoverageOutcome> coverOutcomes(List<ReviewCoverageOutcome> outcomes,
            Set<UUID> coveredRequiredSourceIds, UUID versionId) {
        for (var outcome : outcomes) {
            if (!coveredRequiredSourceIds.contains(outcome.requiredSourceId())) {
                throw new IllegalArgumentException(
                        "review outcome without declared coverage on version " + versionId);
            }
        }
        return List.copyOf(outcomes);
    }

    /** Next decision in the chain for one version (FR-024). */
    public DocumentReviewDecision next(String justification, boolean selfReviewException,
            String exceptionReason, UUID reviewerId, Instant reviewedAt, List<ReviewCoverageOutcome> outcomes) {
        return new DocumentReviewDecision(
                null,
                versionId,
                derive(outcomes),
                justification,
                reviewerId,
                reviewedAt,
                selfReviewException,
                exceptionReason,
                id,
                null,
                revision + 1,
                outcomes);
    }

    private static String text(String value, int min, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " required");
        String stripped = value.strip();
        if (stripped.length() < min || stripped.length() > 4000)
            throw new IllegalArgumentException(field + " must contain between " + min + " and 4000 characters");
        return stripped;
    }
}
