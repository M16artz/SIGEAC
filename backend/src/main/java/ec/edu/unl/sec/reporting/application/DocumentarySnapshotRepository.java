package ec.edu.unl.sec.reporting.application;

import ec.edu.unl.sec.reporting.domain.DocumentarySnapshot;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentarySnapshotRepository {
    void save(DocumentarySnapshot snapshot);
    Optional<DocumentarySnapshot.Header> header(UUID id);
    Optional<DocumentarySnapshot> find(UUID id);
    List<DocumentarySnapshot.Header> list(UUID processId, long offset, int limit);
}
