package ec.edu.unl.sec.filestorage.domain;

import java.util.Objects;

public record StoredFile(
        String storageKey,
        String originalName,
        String declaredMediaType,
        String detectedMediaType,
        long sizeBytes,
        String sha256,
        Status status) {
    public StoredFile {
        Objects.requireNonNull(storageKey);
        Objects.requireNonNull(originalName);
        Objects.requireNonNull(declaredMediaType);
        Objects.requireNonNull(detectedMediaType);
        Objects.requireNonNull(sha256);
        Objects.requireNonNull(status);
        if (sizeBytes < 0 || sizeBytes > 26_214_400) throw new IllegalArgumentException("Invalid file size");
        if (!sha256.matches("[a-f0-9]{64}")) throw new IllegalArgumentException("Invalid SHA-256");
    }

    public enum Status { STAGED, AVAILABLE, QUARANTINED, MISSING }
}
