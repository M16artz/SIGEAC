package ec.edu.unl.sec.filestorage.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record FileAsset(UUID id, UUID uploadId, String storageKey, String stagingKey,
        String originalName, String declaredMediaType, String detectedMediaType, long sizeBytes,
        String sha256, UUID validationPolicyId, Status status, Instant createdAt) {
    public enum Status { STAGED, AVAILABLE, QUARANTINED, MISSING }

    public FileAsset {
        Objects.requireNonNull(id);
        Objects.requireNonNull(uploadId);
        Objects.requireNonNull(validationPolicyId);
        Objects.requireNonNull(status);
        Objects.requireNonNull(createdAt);
        requireKey(storageKey);
        if (stagingKey != null) requireKey(stagingKey);
        if (status == Status.STAGED && stagingKey == null) throw new IllegalArgumentException("Staging key required");
        originalName = requiredText(originalName, 500);
        if (originalName.contains("/") || originalName.contains("\\") || originalName.equals(".") || originalName.equals("..")) {
            throw new IllegalArgumentException("Invalid original filename");
        }
        declaredMediaType = requiredText(declaredMediaType, 255);
        detectedMediaType = requiredText(detectedMediaType, 255);
        if (sizeBytes < 0 || sizeBytes > FileValidationPolicy.MAX_BYTES) throw new IllegalArgumentException("Invalid file size");
        if (sha256 == null || !sha256.matches("[a-f0-9]{64}")) throw new IllegalArgumentException("Invalid SHA-256");
    }

    public FileAsset withStatus(Status next) {
        requireTransition(status, next);
        return new FileAsset(id, uploadId, storageKey, stagingKey, originalName, declaredMediaType,
                detectedMediaType, sizeBytes, sha256, validationPolicyId, next, createdAt);
    }

    public static void requireTransition(Status previous, Status next) {
        Objects.requireNonNull(previous);
        Objects.requireNonNull(next);
        if (previous != Status.STAGED && next == Status.STAGED) throw new IllegalStateException("Cannot return to staging");
    }

    private static void requireKey(String key) {
        if (key == null || !key.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,499}")) {
            throw new IllegalArgumentException("Invalid opaque key");
        }
    }

    private static String requiredText(String value, int maximum) {
        if (value == null || value.isBlank() || value.length() > maximum || value.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Invalid file metadata");
        }
        return value.strip();
    }
}
