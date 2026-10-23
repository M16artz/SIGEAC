package ec.edu.unl.sec.filestorage.application;

import ec.edu.unl.sec.filestorage.domain.FileRecoveryOperation;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FileRecoveryRepository {
    void create(FileRecoveryOperation operation);
    Optional<FileRecoveryOperation> findById(UUID id);
    List<FileRecoveryOperation> findDue(Instant now, int limit);
    Optional<FileRecoveryOperation> claim(UUID id, String owner, Instant now, Duration duration);
    boolean succeed(FileRecoveryOperation lease, Instant now);
    boolean fail(FileRecoveryOperation lease, Instant now, Instant retryAt, String safeReason);
}
