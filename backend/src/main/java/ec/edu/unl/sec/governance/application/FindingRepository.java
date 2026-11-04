package ec.edu.unl.sec.governance.application;

import ec.edu.unl.sec.governance.domain.Consolidation;
import ec.edu.unl.sec.governance.domain.Finding;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Persistence contract for registered findings and their consolidation chain. */
public interface FindingRepository {
    void appendFinding(Finding finding);

    Optional<Finding> findFinding(UUID processId, UUID findingId);

    List<Finding> findings(UUID processId, long offset, long limit);

    List<Finding> byIds(UUID processId, List<UUID> ids);

    long totalFindings(UUID processId);

    void appendConsolidation(Consolidation consolidation);

    Optional<Consolidation> latestConsolidation(UUID processId);

    long totalConsolidations(UUID processId);

    Set<UUID> findingsPresentInConsolidations(UUID processId);
}
