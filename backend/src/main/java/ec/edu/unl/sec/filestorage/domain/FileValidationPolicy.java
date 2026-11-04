package ec.edu.unl.sec.filestorage.domain;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record FileValidationPolicy(UUID id, String code, long maxBytes, boolean allowEmpty,
        Set<String> allowedDetectedTypes, Map<String, Set<String>> compatibility) {
    public static final long MAX_BYTES = 26_214_400;

    public FileValidationPolicy {
        Objects.requireNonNull(id);
        if (code == null || code.isBlank() || code.length() > 100 || maxBytes < 0 || maxBytes > MAX_BYTES) {
            throw new IllegalArgumentException("Invalid file validation policy");
        }
        allowedDetectedTypes = Set.copyOf(allowedDetectedTypes);
        compatibility = compatibility.entrySet().stream().collect(Collectors.toUnmodifiableMap(
                Map.Entry::getKey, entry -> Set.copyOf(entry.getValue())));
    }

    public void validate(long sizeBytes, String declaredMediaType, String detectedMediaType) {
        if (sizeBytes < 0) throw new IllegalArgumentException("Invalid file size");
        if (sizeBytes > maxBytes) throw new FileValidationException("FILE_TOO_LARGE");
        if (sizeBytes == 0 && !allowEmpty) throw new FileValidationException("FILE_EMPTY");
        if (!allowedDetectedTypes.contains(detectedMediaType)) {
            throw new FileValidationException("FILE_TYPE_NOT_ALLOWED");
        }
        String declared = declaredMediaType == null ? ""
                : declaredMediaType.split(";", 2)[0].strip().toLowerCase(Locale.ROOT);
        if (!compatibility.getOrDefault(declared, Set.of()).contains(detectedMediaType)) {
            throw new FileValidationException("FILE_TYPE_MISMATCH");
        }
    }
}
