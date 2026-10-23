package ec.edu.unl.sec.filestorage.application;

import ec.edu.unl.sec.filestorage.domain.FileValidationPolicy;
import java.util.Optional;
import java.util.UUID;

public interface FileValidationPolicyRepository {
    Optional<FileValidationPolicy> findActiveByCode(String code);
    Optional<FileValidationPolicy> findById(UUID id);
}
