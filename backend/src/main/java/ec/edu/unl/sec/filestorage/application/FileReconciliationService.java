package ec.edu.unl.sec.filestorage.application;

import ec.edu.unl.sec.filestorage.domain.FileAsset;
import ec.edu.unl.sec.filestorage.domain.FileRecoveryOperation;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Reconciles interrupted uploads with leases and idempotent retries. Each
 * operation is re-verified against the store before the asset state changes,
 * so a retry never publishes unverifiable bytes or deletes submitted assets.
 */
@Service
public class FileReconciliationService {
    private final FileAssetRepository assets;
    private final FileRecoveryRepository recovery;
    private final FileStoragePort storage;
    private final FileStorageProperties properties;
    private final Clock clock;

    @Autowired
    public FileReconciliationService(FileAssetRepository assets, FileRecoveryRepository recovery,
            FileStoragePort storage, FileStorageProperties properties) {
        this(assets, recovery, storage, properties, Clock.systemUTC());
    }

    FileReconciliationService(FileAssetRepository assets, FileRecoveryRepository recovery,
            FileStoragePort storage, FileStorageProperties properties, Clock clock) {
        this.assets = assets;
        this.recovery = recovery;
        this.storage = storage;
        this.properties = properties;
        this.clock = clock;
    }

    public int reconcileDue(String owner, int limit) {
        Instant now = clock.instant();
        int processed = 0;
        for (var due : recovery.findDue(now, limit)) {
            var lease = recovery.claim(due.id(), owner, now, properties.getReconciliation().getLease())
                    .orElse(null);
            if (lease == null) continue;
            reconcile(lease, now);
            processed++;
        }
        return processed;
    }

    public void scheduleVerification(UUID fileId, UUID correlationId) {
        var now = clock.instant();
        recovery.create(new FileRecoveryOperation(UUID.randomUUID(), fileId,
                FileRecoveryOperation.Operation.VERIFY, FileRecoveryOperation.Status.PENDING,
                0, now, null, null, null, correlationId == null ? UUID.randomUUID() : correlationId, now, now));
    }

    void reconcile(FileRecoveryOperation lease, Instant now) {
        var asset = lease.fileId() == null ? null : assets.findById(lease.fileId()).orElse(null);
        if (asset == null) {
            finish(lease, now, false, "FILE_ASSET_MISSING");
            return;
        }
        switch (lease.operation()) {
            case PROMOTE -> promote(lease, asset, now);
            case VERIFY -> verify(lease, asset, now);
            case QUARANTINE -> {
                changeStatus(asset, FileAsset.Status.QUARANTINED);
                recovery.succeed(lease, now);
            }
            case CLEANUP -> recovery.succeed(lease, now);
        }
    }

    private void promote(FileRecoveryOperation lease, FileAsset asset, Instant now) {
        if (asset.status() == FileAsset.Status.AVAILABLE) {
            verify(lease, asset, now);
            return;
        }
        if (asset.status() != FileAsset.Status.STAGED) {
            finish(lease, now, false, "FILE_ASSET_NOT_STAGED");
            return;
        }
        String stagingKey = asset.stagingKey() == null ? asset.uploadId() + ".part" : asset.stagingKey();
        var prepared = new FileStoragePort.PreparedObject(asset.uploadId(), stagingKey,
                asset.sizeBytes(), asset.sha256());
        try {
            storage.promote(prepared);
            requireMatch(asset);
            if (assets.changeStatus(asset.id(), FileAsset.Status.STAGED, FileAsset.Status.AVAILABLE)) {
                recovery.succeed(lease, now);
            } else {
                finish(lease, now, false, "FILE_STATUS_CONFLICT");
            }
        } catch (IOException failure) {
            String code = reason(failure, "FILE_PROMOTION_FAILED");
            if ("FILE_STORAGE_PATH_UNSAFE".equals(code) || "FILE_KEY_INVALID".equals(code)) {
                changeStatus(asset, FileAsset.Status.MISSING);
            }
            recovery.fail(lease, now, retryAt(now), code);
        }
    }

    private void verify(FileRecoveryOperation lease, FileAsset asset, Instant now) {
        try {
            requireMatch(asset);
            if (asset.status() == FileAsset.Status.STAGED) {
                assets.changeStatus(asset.id(), FileAsset.Status.STAGED, FileAsset.Status.AVAILABLE);
            }
            recovery.succeed(lease, now);
        } catch (IOException missing) {
            if (asset.status() == FileAsset.Status.AVAILABLE) changeStatus(asset, FileAsset.Status.MISSING);
            else if (asset.status() == FileAsset.Status.STAGED) changeStatus(asset, FileAsset.Status.MISSING);
            recovery.fail(lease, now, retryAt(now), reason(missing, "FILE_STORAGE_MISSING"));
        } catch (IllegalStateException corrupt) {
            changeStatus(asset, FileAsset.Status.QUARANTINED);
            recovery.fail(lease, now, retryAt(now), "FILE_INTEGRITY_FAILED");
        }
    }

    private void requireMatch(FileAsset asset) throws IOException {
        var verification = storage.verify(asset.storageKey());
        if (verification.sizeBytes() != asset.sizeBytes() || !verification.sha256().equals(asset.sha256())) {
            throw new IllegalStateException("FILE_INTEGRITY_FAILED");
        }
    }

    private void changeStatus(FileAsset asset, FileAsset.Status next) {
        if (asset.status() == next) return;
        assets.changeStatus(asset.id(), asset.status(), next);
    }

    private void finish(FileRecoveryOperation lease, Instant now, boolean success, String code) {
        if (success) recovery.succeed(lease, now);
        else recovery.fail(lease, now, retryAt(now), code);
    }

    private Instant retryAt(Instant now) {
        Duration grace = properties.getReconciliation().getGrace();
        return now.plus(grace.isNegative() || grace.isZero() ? Duration.ofMinutes(1) : grace);
    }

    private static String reason(IOException failure, String fallback) {
        String message = failure.getMessage();
        if (message != null && message.matches("[A-Z][A-Z0-9_]{0,99}")) return message;
        return fallback;
    }
}
