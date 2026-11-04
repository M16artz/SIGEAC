package ec.edu.unl.sec.evidence.application;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Verifies only file references of versions selected by an authorized caller. */
public interface EvidenceIntegrityPort {
    Report verify(Set<UUID> versionIds);
    record FileReference(UUID versionId,UUID fileId,long sizeBytes,String sha256) {}
    record Issue(UUID versionId,UUID fileId,String code) {}
    record Report(List<FileReference> files,List<Issue> issues) {
        public Report { files=List.copyOf(files); issues=List.copyOf(issues); }
    }
}
