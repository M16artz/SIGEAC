package ec.edu.unl.sec.evidence.application;

import ec.edu.unl.sec.evidence.domain.ApplicabilityDecision;
import ec.edu.unl.sec.evidence.domain.SourceCoverage;
import ec.edu.unl.sec.evidence.domain.StructuredEvidence;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EvidenceGovernanceRepository {
    List<SourceCoverage> coverages(UUID versionId);

    Optional<SourceCoverage> coverage(UUID coverageId);

    SourceCoverage saveCoverage(SourceCoverage coverage);

    void deleteCoverage(UUID coverageId);

    List<StructuredEvidence> structuredSnapshots(UUID versionId);

    Optional<StructuredEvidence> structuredSnapshot(UUID versionId, UUID requiredSourceId);

    StructuredEvidence saveStructured(StructuredEvidence snapshot);

    boolean markSubmitted(UUID versionId, Instant submittedAt, long expectedLockRevision);

    boolean advanceDraftRevision(UUID versionId, long expectedLockRevision);

    Optional<ApplicabilityDecision> latestApplicability(UUID processId, ApplicabilityDecision.SubjectType subjectType,
            UUID subjectId);

    ApplicabilityDecision saveApplicability(ApplicabilityDecision decision);

    List<ApplicabilityDecision> applicabilityHistory(UUID processId);
}
