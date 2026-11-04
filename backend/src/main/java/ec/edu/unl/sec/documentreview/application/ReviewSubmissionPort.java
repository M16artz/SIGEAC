package ec.edu.unl.sec.documentreview.application;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Read side of submitted evidence versions for documentary review. Responses
 * project the fixed-model coverage declared on each version without exposing
 * draft state, and reveal whether the evidence already has a newer version.
 */
public interface ReviewSubmissionPort {
    Optional<SubmittedVersion> submission(UUID versionId);

    List<PendingSubmission> pending(UUID processId);

    /** Latest version of the evidence chain, used to block decisions on older versions. */
    long latestRevisionNumber(UUID evidenceId);

    /** Career owning the process, used for scoped authorization of read views. */
    Optional<UUID> career(UUID processId);

    record SubmittedVersion(
            UUID versionId,
            UUID evidenceId,
            UUID processId,
            UUID careerId,
            int revisionNumber,
            UUID submitterId,
            List<UUID> indicatorIds,
            List<UUID> coveredRequiredSourceIds) {}

    record PendingSubmission(
            UUID versionId,
            UUID evidenceId,
            UUID processId,
            UUID careerId,
            int revisionNumber,
            String title,
            UUID submitterId,
            java.time.Instant submittedAt,
            List<UUID> indicatorIds,
            List<UUID> coveredRequiredSourceIds) {}
}
