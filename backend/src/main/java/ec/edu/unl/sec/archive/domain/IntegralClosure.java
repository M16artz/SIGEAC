package ec.edu.unl.sec.archive.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Immutable integral closure snapshot; the payload is sealed with its SHA-256 digest. */
public record IntegralClosure(UUID id, UUID processId, UUID documentaryClosureId, String policyVersion,
        String payloadJson, String sha256, List<UUID> pendingIds, List<UUID> exceptionDecisionIds,
        UUID closedBy, Instant closedAt) {
    public IntegralClosure {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(processId, "processId");
        Objects.requireNonNull(documentaryClosureId, "documentaryClosureId");
        Objects.requireNonNull(policyVersion, "policyVersion");
        Objects.requireNonNull(payloadJson, "payloadJson");
        Objects.requireNonNull(closedBy, "closedBy");
        Objects.requireNonNull(closedAt, "closedAt");
        if (policyVersion.isBlank()) throw new IllegalArgumentException("policy version is required");
        if (payloadJson.isBlank()) throw new IllegalArgumentException("payload is required");
        if (sha256 == null || !sha256.matches("[a-f0-9]{64}"))
            throw new IllegalArgumentException("invalid closure digest");
        pendingIds = pendingIds == null ? List.of() : List.copyOf(pendingIds);
        exceptionDecisionIds = exceptionDecisionIds == null ? List.of() : List.copyOf(exceptionDecisionIds);
        closedAt = closedAt.truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
    }

    public String type() {
        return "INTEGRAL_CLOSURE";
    }
}
