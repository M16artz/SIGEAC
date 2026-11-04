package ec.edu.unl.sec.archive.application;

import ec.edu.unl.sec.archive.domain.IntegralClosurePolicy;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Read-only evidence needed to evaluate and seal an integral closure. */
public interface ClosureEvidencePort {
    record DocumentaryClosureEvidence(UUID id, UUID snapshotId, String policyVersion, String sha256,
            Instant closedAt, UUID closedBy) {}

    record ReportEvidence(UUID versionId, int versionNumber, String state) {}

    List<DocumentaryClosureEvidence> documentaryClosures(UUID processId);

    Optional<ReportEvidence> finalReport(UUID processId);

    List<IntegralClosurePolicy.ActionSnapshot> actions(UUID processId);

    boolean authorizedDecision(UUID processId, UUID decisionId);
}
