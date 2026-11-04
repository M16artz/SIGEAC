package ec.edu.unl.sec.filestorage.application;

import ec.edu.unl.sec.filestorage.domain.FileAsset;
import java.util.Optional;
import java.util.UUID;

public interface FileAssetRepository {
    void create(FileAsset asset);
    Optional<FileAsset> findById(UUID id);
    Optional<FileAsset> findByUploadId(UUID uploadId);
    boolean changeStatus(UUID id, FileAsset.Status expected, FileAsset.Status next);
}
