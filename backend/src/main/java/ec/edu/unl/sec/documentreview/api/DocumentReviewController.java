package ec.edu.unl.sec.documentreview.api;

import ec.edu.unl.sec.documentreview.application.DocumentReviewProblemException;
import ec.edu.unl.sec.documentreview.application.DocumentReviewService;
import ec.edu.unl.sec.documentreview.application.ReviewSubmissionPort;
import ec.edu.unl.sec.documentreview.domain.DocumentReviewDecision;
import ec.edu.unl.sec.documentreview.domain.ReviewCoverageOutcome;
import ec.edu.unl.sec.shared.api.EntityTagSupport;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Documentary review endpoints (D5, P09-P10): reviewer inbox, justified
 * per-coverage decisions, append-only rectifications and corrective
 * successors linked to the triggering decision. Routes are additive v1.
 */
@RestController
public class DocumentReviewController {
    private final DocumentReviewService reviews;

    public DocumentReviewController(DocumentReviewService reviews) {
        this.reviews = reviews;
    }

    @GetMapping("/processes/{processId}/document-review/inbox")
    public ResponseEntity<List<InboxView>> inbox(@PathVariable UUID processId) {
        var entries = reviews.inbox(processId);
        return ResponseEntity.ok()
                .eTag(EntityTagSupport.strong(entries.size()))
                .body(entries.stream().map(InboxView::from).toList());
    }

    @GetMapping("/processes/{processId}/document-review-decisions")
    public ResponseEntity<List<DecisionView>> history(@PathVariable UUID processId) {
        return ResponseEntity.ok()
                .body(reviews.history(processId).stream().map(DecisionView::from).toList());
    }

    @GetMapping("/evidence-versions/{versionId}/document-review")
    public ResponseEntity<VersionReviewView> versionReview(@PathVariable UUID versionId) {
        return ResponseEntity.ok().body(VersionReviewView.from(reviews.versionReview(versionId)));
    }

    @PostMapping("/evidence-versions/{versionId}/document-review")
    public ResponseEntity<DecisionView> decide(
            @PathVariable UUID versionId,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody DecisionCommand request) {
        var decided = reviews.decide(versionId, new DocumentReviewService.ReviewInput(
                revision(ifMatch),
                request.selfReviewException(),
                request.exceptionReason(),
                request.justification(),
                outcomes(request.outcomes())));
        return ResponseEntity.created(URI.create("/api/v1/evidence-versions/" + versionId + "/document-review"))
                .eTag(EntityTagSupport.strong(decided.revision()))
                .body(DecisionView.from(decided));
    }

    @PostMapping("/document-review-decisions/{decisionId}/correction")
    public ResponseEntity<CorrectionView> correct(
            @PathVariable UUID decisionId,
            @RequestHeader(name = "If-Match", required = false) String ifMatch) {
        var correction = reviews.correct(decisionId, revision(ifMatch));
        return ResponseEntity.created(
                        URI.create("/api/v1/evidence-versions/" + correction.successorVersionId()))
                .body(new CorrectionView(
                        correction.correctedVersionId(),
                        correction.successorVersionId(),
                        correction.successorRevision()));
    }

    private static long revision(String ifMatch) {
        if (ifMatch == null || ifMatch.isBlank()) {
            throw new DocumentReviewProblemException(
                    DocumentReviewProblemException.Kind.PRECONDITION_REQUIRED,
                    "PRECONDITION_REQUIRED", "Se requiere If-Match con la revisión vigente.");
        }
        if (!ifMatch.matches("\"[0-9]+\"")) {
            throw new DocumentReviewProblemException(DocumentReviewProblemException.Kind.INVALID,
                    "INVALID_ETAG", "If-Match no contiene una revisión válida.");
        }
        return Long.parseLong(ifMatch.substring(1, ifMatch.length() - 1));
    }

    private static List<ReviewCoverageOutcome> outcomes(List<OutcomeCommand> raw) {
        return raw.stream().map(outcome -> {
            ReviewCoverageOutcome.Outcome value;
            try {
                value = ReviewCoverageOutcome.Outcome.valueOf(outcome.outcome());
            } catch (IllegalArgumentException exception) {
                throw new DocumentReviewProblemException(DocumentReviewProblemException.Kind.INVALID,
                        "INVALID_OUTCOME", "El resultado de cobertura no es válido.");
            }
            return new ReviewCoverageOutcome(outcome.requiredSourceId(), value, outcome.note());
        }).toList();
    }

    public record OutcomeCommand(@NotNull UUID requiredSourceId, @NotBlank @Size(max = 20) String outcome,
            @Size(max = 4000) String note) {}

    public record DecisionCommand(@NotNull Boolean selfReviewException,
            @Size(max = 4000) String exceptionReason,
            @NotBlank @Size(max = 4000) String justification,
            @NotNull List<OutcomeCommand> outcomes) {}

    public record InboxView(UUID versionId, UUID evidenceId, UUID processId, UUID careerId, int revisionNumber,
            String title, UUID submitterId, Instant submittedAt, List<UUID> indicatorIds,
            List<UUID> coveredRequiredSourceIds) {
        static InboxView from(ReviewSubmissionPort.PendingSubmission submission) {
            return new InboxView(submission.versionId(), submission.evidenceId(), submission.processId(),
                    submission.careerId(), submission.revisionNumber(), submission.title(),
                    submission.submitterId(), submission.submittedAt(), submission.indicatorIds(),
                    submission.coveredRequiredSourceIds());
        }
    }

    public record DecisionView(UUID id, UUID versionId, String decision, String justification, UUID reviewerId,
            Instant reviewedAt, boolean selfReviewException, String exceptionReason, UUID predecessorId,
            UUID successorVersionId, long revision, List<OutcomeView> outcomes) {
        static DecisionView from(DocumentReviewDecision decision) {
            return new DecisionView(decision.id(), decision.versionId(), decision.decision().name(),
                    decision.justification(), decision.reviewerId(), decision.reviewedAt(),
                    decision.selfReviewException(), decision.exceptionReason(), decision.predecessorId(),
                    decision.successorVersionId(), decision.revision(), decision.outcomes().stream()
                            .map(OutcomeView::from).toList());
        }
    }

    public record OutcomeView(UUID requiredSourceId, String outcome, String note) {
        static OutcomeView from(ReviewCoverageOutcome outcome) {
            return new OutcomeView(outcome.requiredSourceId(), outcome.outcome().name(), outcome.note());
        }
    }

    public record VersionReviewView(UUID decisionId, String decision, long revision, UUID successorVersionId,
            List<OutcomeView> outcomes) {
        static VersionReviewView from(Optional<DocumentReviewDecision> decision) {
            return decision.map(value -> new VersionReviewView(value.id(), value.decision().name(),
                            value.revision(), value.successorVersionId(), value.outcomes().stream()
                                    .map(OutcomeView::from).toList()))
                    .orElseGet(() -> new VersionReviewView(null, "PENDING_REVIEW", 0, null, List.of()));
        }
    }

    public record CorrectionView(UUID correctedVersionId, UUID successorVersionId, int successorRevision) {}
}
