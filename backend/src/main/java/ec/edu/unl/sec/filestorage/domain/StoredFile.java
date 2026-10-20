package ec.edu.unl.sec.filestorage.domain;

import java.util.Objects;

public record StoredFile(
        String storageKey,
        String originalName,
        String mediaType,
        long sizeBytes,
        String sha256,
        Status status) {
    public StoredFile {
        Objects.requireNonNull(storageKey);
        Objects.requireNonNull(originalName);
        Objects.requireNonNull(mediaType);
        Objects.requireNonNull(sha256);
        Objects.requireNonNull(status);
        if (sizeBytes < 1) throw new IllegalArgumentException("File cannot be empty");
        if (!sha256.matches("[a-fA-F0-9]{64}")) throw new IllegalArgumentException("Invalid SHA-256");
    }

    public enum Status { STAGED, AVAILABLE, QUARANTINED, MISSING }
}
