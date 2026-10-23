package ec.edu.unl.sec.filestorage.domain;

import static org.assertj.core.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FileRecoveryOperationTest {
    private final Instant now = Instant.parse("2026-10-22T00:00:00Z");

    @Test void leasesDueWorkAndFencesExpiredWorkersWithAnAttemptNumber() {
        var pending = pending();
        var lease = pending.lease("worker-a", now, Duration.ofMinutes(2));
        assertThat(lease.status()).isEqualTo(FileRecoveryOperation.Status.LEASED);
        assertThat(lease.attempts()).isEqualTo(1);
        assertThatThrownBy(() -> lease.lease("worker-b", now.plusSeconds(1), Duration.ofMinutes(2)))
                .isInstanceOf(IllegalStateException.class);
        var recovered = lease.lease("worker-b", now.plusSeconds(120), Duration.ofMinutes(2));
        assertThat(recovered.attempts()).isEqualTo(2);
        assertThat(recovered.leaseOwner()).isEqualTo("worker-b");
        assertThatThrownBy(() -> lease.succeed(now.plusSeconds(120))).isInstanceOf(IllegalStateException.class);
        var success = recovered.succeed(now.plusSeconds(121));
        assertThat(success.status()).isEqualTo(FileRecoveryOperation.Status.SUCCEEDED);
        assertThat(success.leaseOwner()).isNull();
        assertThat(success.leaseUntil()).isNull();
        assertThatThrownBy(() -> success.lease("worker-c", now.plusSeconds(300), Duration.ofMinutes(2)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test void schedulesRetryAndAcceptsOnlySafeReasonCodes() {
        var lease = pending().lease("worker", now, Duration.ofMinutes(2));
        var failed = lease.fail(now.plusSeconds(1), now.plusSeconds(60), "FILE_STORAGE_FAILED");
        assertThat(failed.status()).isEqualTo(FileRecoveryOperation.Status.FAILED);
        assertThat(failed.safeReason()).isEqualTo("FILE_STORAGE_FAILED");
        assertThatThrownBy(() -> failed.lease("worker", now.plusSeconds(59), Duration.ofMinutes(2)))
                .isInstanceOf(IllegalStateException.class);
        assertThat(failed.lease("worker", now.plusSeconds(60), Duration.ofMinutes(2)).attempts()).isEqualTo(2);
        assertThatThrownBy(() -> lease.fail(now.plusSeconds(1), now.plusSeconds(60), "C:\\private\\file"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> lease.fail(now.plusSeconds(1), now, "FILE_STORAGE_FAILED"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void validatesLeaseDurationAndAllowsOrphanOperationsWithoutAnAsset() {
        assertThat(pending().fileId()).isNull();
        assertThatThrownBy(() -> pending().lease("worker", now, Duration.ZERO)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> pending().lease("", now, Duration.ofMinutes(2))).isInstanceOf(IllegalArgumentException.class);
    }

    private FileRecoveryOperation pending() {
        return new FileRecoveryOperation(UUID.randomUUID(), null, FileRecoveryOperation.Operation.VERIFY,
                FileRecoveryOperation.Status.PENDING, 0, now, null, null, null, UUID.randomUUID(), now, now);
    }
}
