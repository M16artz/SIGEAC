package ec.edu.unl.sec.evidence.application;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Read-only current-submission projection for callers that already resolved source scope. */
public interface EvidenceProgressPort {
    List<CurrentCoverage> currentCoverages(UUID processId, Set<UUID> sourceIds);
    Applicability applicability(UUID processId);
    List<ApplicabilityReference> currentApplicabilityDecisions(UUID processId);

    record ApplicabilityReference(UUID id, String subjectType, UUID subjectId, String decision, long revision) {}

    record CurrentCoverage(UUID evidenceId, UUID versionId, UUID requiredSourceId) {}
    record Applicability(Set<UUID> excludedIndicatorIds, Set<UUID> excludedSourceIds) {
        public Applicability {
            excludedIndicatorIds = Set.copyOf(excludedIndicatorIds);
            excludedSourceIds = Set.copyOf(excludedSourceIds);
        }
    }
}
