package ec.edu.unl.sec.evidence.application;

import ec.edu.unl.sec.evidence.domain.Evidence;
import ec.edu.unl.sec.evidence.domain.EvidenceVersion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EvidenceDraftRepository {
    void create(Evidence evidence, EvidenceVersion version);

    Optional<EvidenceDraftService.EvidenceDetails> findEvidence(UUID evidenceId);

    Optional<EvidenceVersion> findVersion(UUID versionId);

    List<EvidenceDraftService.EvidenceDetails> listByProcess(UUID processId);

    void update(EvidenceVersion version, long expectedRevision);

    /** Inserts the correction draft and advances the evidence lineage pointer. */
    void createSuccessor(EvidenceVersion successor, UUID correctedVersionId);
}
