package ec.edu.unl.sec.filestorage.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record FileRecoveryOperation(UUID id, UUID fileId, Operation operation, Status status,
        int attempts, Instant notBefore, String leaseOwner, Instant leaseUntil, String safeReason,
        UUID correlationId, Instant createdAt, Instant updatedAt) {
    public enum Operation { PROMOTE, VERIFY, QUARANTINE, CLEANUP }
    public enum Status { PENDING, LEASED, SUCCEEDED, FAILED }

    public FileRecoveryOperation {
        Objects.requireNonNull(id);
        Objects.requireNonNull(operation);
        Objects.requireNonNull(status);
        Objects.requireNonNull(notBefore);
        Objects.requireNonNull(correlationId);
        Objects.requireNonNull(createdAt);
        Objects.requireNonNull(updatedAt);
        if (attempts < 0 || updatedAt.isBefore(createdAt)) throw new IllegalArgumentException("Invalid recovery metadata");
        if (safeReason != null && !safeReason.matches("[A-Z][A-Z0-9_]{0,99}")) {
            throw new IllegalArgumentException("Safe recovery reason code required");
        }
        if (status == Status.FAILED && safeReason == null) throw new IllegalArgumentException("Failure reason required");
        if (status == Status.LEASED) {
            requireOwner(leaseOwner);
            if (attempts == 0 || leaseUntil == null || !leaseUntil.isAfter(updatedAt)) {
                throw new IllegalArgumentException("Invalid recovery lease");
            }
        } else if (leaseOwner != null || leaseUntil != null) throw new IllegalArgumentException("Unexpected recovery lease");
    }

    public boolean isDue(Instant now) {
        return !now.isBefore(updatedAt) && !now.isBefore(notBefore)
                && (status == Status.PENDING || status == Status.FAILED
                || status == Status.LEASED && !now.isBefore(leaseUntil));
    }

    public FileRecoveryOperation lease(String owner, Instant now, Duration duration) {
        requireOwner(owner);
        if (duration == null || duration.isNegative() || duration.isZero()) throw new IllegalArgumentException("Positive lease required");
        if (!isDue(now)) throw new IllegalStateException("Recovery operation is not due");
        return new FileRecoveryOperation(id, fileId, operation, Status.LEASED, Math.incrementExact(attempts),
                notBefore, owner, now.plus(duration), null, correlationId, createdAt, now);
    }

    public FileRecoveryOperation succeed(Instant now) {
        requireActiveLease(now);
        return new FileRecoveryOperation(id, fileId, operation, Status.SUCCEEDED, attempts, notBefore,
                null, null, null, correlationId, createdAt, now);
    }

    public FileRecoveryOperation fail(Instant now, Instant retryAt, String reason) {
        requireActiveLease(now);
        if (retryAt == null || retryAt.isBefore(now)) throw new IllegalArgumentException("Invalid retry time");
        return new FileRecoveryOperation(id, fileId, operation, Status.FAILED, attempts, retryAt,
                null, null, reason, correlationId, createdAt, now);
    }

    public boolean hasActiveLease(Instant now) {
        return status == Status.LEASED && !now.isBefore(updatedAt) && now.isBefore(leaseUntil);
    }

    private void requireActiveLease(Instant now) {
        if (!hasActiveLease(now)) throw new IllegalStateException("Recovery lease is not active");
    }

    private static void requireOwner(String owner) {
        if (owner == null || owner.isBlank() || owner.length() > 200 || owner.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Invalid lease owner");
        }
    }
}
