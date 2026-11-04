package ec.edu.unl.sec.evidence.application;

import ec.edu.unl.sec.evidence.domain.ExternalReference;
import ec.edu.unl.sec.evidence.domain.VersionFile;
import java.util.List;
import java.util.UUID;

public interface EvidenceSourceRepository {
    List<VersionFile> files(UUID versionId);
    List<ExternalReference> references(UUID versionId);
    /** Mutations share a transaction; claim the draft revision before allocating positions or changing children. */
    boolean advanceDraftRevision(UUID versionId, long expectedRevision);
    int nextFilePosition(UUID versionId);
    int nextReferencePosition(UUID versionId);
    void addFile(VersionFile file);
    void removeFile(UUID versionId, UUID fileId);
    void addReference(ExternalReference reference);
    void updateReference(ExternalReference reference);
    void removeReference(UUID versionId, UUID referenceId);
}
