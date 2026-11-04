package ec.edu.unl.sec.evidence.domain;

import ec.edu.unl.sec.filestorage.domain.FileAsset;
import ec.edu.unl.sec.filestorage.domain.FileValidationPolicy;
import java.util.Objects;
import java.util.UUID;

public record VersionFile(UUID versionId, UUID fileId, String originalName, String declaredMediaType,
        String detectedMediaType, long sizeBytes, String sha256, int position) {
    public VersionFile {
        Objects.requireNonNull(versionId);
        Objects.requireNonNull(fileId);
        if (originalName == null || originalName.isBlank() || originalName.length() > 500
                || declaredMediaType == null || declaredMediaType.isBlank() || declaredMediaType.length() > 255
                || detectedMediaType == null || detectedMediaType.isBlank() || detectedMediaType.length() > 255
                || sizeBytes < 0 || sizeBytes > FileValidationPolicy.MAX_BYTES || sha256 == null
                || !sha256.matches("[a-f0-9]{64}") || position < 1) {
            throw new IllegalArgumentException("Invalid version file snapshot");
        }
    }

    public static VersionFile from(UUID versionId, FileAsset asset, int position) {
        if (asset.status() != FileAsset.Status.AVAILABLE) throw new IllegalStateException("File must be available");
        return new VersionFile(versionId, asset.id(), asset.originalName(), asset.declaredMediaType(),
                asset.detectedMediaType(), asset.sizeBytes(), asset.sha256(), position);
    }
}
