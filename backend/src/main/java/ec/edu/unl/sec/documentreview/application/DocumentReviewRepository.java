package ec.edu.unl.sec.documentreview.application;

import ec.edu.unl.sec.documentreview.domain.DocumentReviewDecision;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentReviewRepository {
    DocumentReviewDecision save(DocumentReviewDecision decision);

    Optional<DocumentReviewDecision> latest(UUID versionId);

    Optional<DocumentReviewDecision> decision(UUID decisionId);

    /** Marks the corrective successor produced by a rectification/correction flow. */
    void linkSuccessor(UUID decisionId, UUID successorVersionId);

    /** Complete decision chain across every version of one evidence lineage. */
    List<DocumentReviewDecision> historyForEvidence(UUID evidenceId);

    /** Decisions and outcomes projected for the review UI of a specific version. */
    List<DocumentReviewDecision> historyForVersion(UUID versionId);

    List<DocumentReviewDecision> historyForProcess(UUID processId);
}
