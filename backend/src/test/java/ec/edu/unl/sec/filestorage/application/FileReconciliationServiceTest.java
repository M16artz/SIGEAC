package ec.edu.unl.sec.filestorage.application;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import ec.edu.unl.sec.filestorage.domain.FileAsset;
import ec.edu.unl.sec.filestorage.domain.FileRecoveryOperation;
import ec.edu.unl.sec.filestorage.application.FileStorageProperties;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FileReconciliationServiceTest {
    private final Instant now = Instant.parse("2026-10-22T00:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private final FileAssetRepository assets = mock(FileAssetRepository.class);
    private final FileRecoveryRepository recovery = mock(FileRecoveryRepository.class);
    private final FileStoragePort storage = mock(FileStoragePort.class);
    private final FileStorageProperties properties = new FileStorageProperties();
    private final FileReconciliationService service =
            new FileReconciliationService(assets, recovery, storage, properties, clock);

    @BeforeEach void setup() {
        properties.getReconciliation().setGrace(Duration.ofMinutes(15));
        properties.getReconciliation().setLease(Duration.ofMinutes(2));
    }

    @Test void promotesAnInterruptedStagedUploadThenMarksItAvailable() throws Exception {
        var asset = asset(FileAsset.Status.STAGED);
        var lease = lease(asset.id(), FileRecoveryOperation.Operation.PROMOTE);
        when(assets.findById(asset.id())).thenReturn(Optional.of(asset));
        when(storage.promote(any())).thenReturn(asset.storageKey());
        when(storage.verify(asset.storageKey())).thenReturn(new FileStoragePort.Verification(asset.sizeBytes(), asset.sha256()));
        when(assets.changeStatus(asset.id(), FileAsset.Status.STAGED, FileAsset.Status.AVAILABLE)).thenReturn(true);
        when(recovery.succeed(lease, now)).thenReturn(true);

        service.reconcile(lease, now);

        verify(storage).promote(new FileStoragePort.PreparedObject(asset.uploadId(), asset.stagingKey(),
                asset.sizeBytes(), asset.sha256()));
        verify(assets).changeStatus(asset.id(), FileAsset.Status.STAGED, FileAsset.Status.AVAILABLE);
        verify(recovery).succeed(lease, now);
        verify(recovery, never()).fail(any(), any(), any(), any());
    }

    @Test void acceptsAnAlreadyPromotedObjectWithoutOverwritingIt() throws Exception {
        var asset = asset(FileAsset.Status.AVAILABLE);
        var lease = lease(asset.id(), FileRecoveryOperation.Operation.PROMOTE);
        when(assets.findById(asset.id())).thenReturn(Optional.of(asset));
        when(storage.verify(asset.storageKey())).thenReturn(new FileStoragePort.Verification(asset.sizeBytes(), asset.sha256()));
        when(recovery.succeed(lease, now)).thenReturn(true);

        service.reconcile(lease, now);

        verify(storage, never()).promote(any());
        verify(recovery).succeed(lease, now);
    }

    @Test void marksAMissingPromotionAsMissingAndKeepsItRetryable() throws Exception {
        var asset = asset(FileAsset.Status.STAGED);
        var lease = lease(asset.id(), FileRecoveryOperation.Operation.PROMOTE);
        when(assets.findById(asset.id())).thenReturn(Optional.of(asset));
        when(storage.promote(any())).thenThrow(new IOException("FILE_STORAGE_PATH_UNSAFE"));
        when(recovery.fail(eq(lease), eq(now), eq(now.plus(Duration.ofMinutes(15))), eq("FILE_STORAGE_PATH_UNSAFE")))
                .thenReturn(true);

        service.reconcile(lease, now);

        verify(assets).changeStatus(asset.id(), FileAsset.Status.STAGED, FileAsset.Status.MISSING);
        verify(recovery).fail(lease, now, now.plus(Duration.ofMinutes(15)), "FILE_STORAGE_PATH_UNSAFE");
        verify(recovery, never()).succeed(any(), any());
    }

    @Test void quarantinesAnAvailableObjectWhoseBytesNoLongerMatch() throws Exception {
        var asset = asset(FileAsset.Status.AVAILABLE);
        var lease = lease(asset.id(), FileRecoveryOperation.Operation.VERIFY);
        when(assets.findById(asset.id())).thenReturn(Optional.of(asset));
        when(storage.verify(asset.storageKey())).thenReturn(new FileStoragePort.Verification(asset.sizeBytes() + 1, asset.sha256()));

        service.reconcile(lease, now);

        verify(assets).changeStatus(asset.id(), FileAsset.Status.AVAILABLE, FileAsset.Status.QUARANTINED);
        verify(recovery).fail(eq(lease), eq(now), any(), eq("FILE_INTEGRITY_FAILED"));
    }

    @Test void marksAnAvailableObjectAsMissingWhenTheStoreCannotFindIt() throws Exception {
        var asset = asset(FileAsset.Status.AVAILABLE);
        var lease = lease(asset.id(), FileRecoveryOperation.Operation.VERIFY);
        when(assets.findById(asset.id())).thenReturn(Optional.of(asset));
        when(storage.verify(asset.storageKey())).thenThrow(new IOException("FILE_STORAGE_PATH_UNSAFE"));

        service.reconcile(lease, now);

        verify(assets).changeStatus(asset.id(), FileAsset.Status.AVAILABLE, FileAsset.Status.MISSING);
        verify(recovery).fail(eq(lease), eq(now), any(), eq("FILE_STORAGE_PATH_UNSAFE"));
    }

    @Test void doesNotTouchStorageForAnOrphanOperationWithoutAnAsset() throws Exception {
        UUID fileId = UUID.randomUUID();
        var lease = lease(fileId, FileRecoveryOperation.Operation.VERIFY);
        when(assets.findById(fileId)).thenReturn(Optional.empty());

        service.reconcile(lease, now);

        verifyNoInteractions(storage);
        verify(recovery).fail(eq(lease), eq(now), any(), eq("FILE_ASSET_MISSING"));
    }

    @Test void schedulesIdempotentVerificationWorkAndProcessesDueOperationsWithLeases() {
        UUID fileId = UUID.randomUUID(), correlation = UUID.randomUUID();
        service.scheduleVerification(fileId, correlation);
        verify(recovery).create(argThat(operation -> operation.fileId().equals(fileId)
                && operation.operation() == FileRecoveryOperation.Operation.VERIFY
                && operation.status() == FileRecoveryOperation.Status.PENDING
                && operation.correlationId().equals(correlation)));
    }

    private FileAsset asset(FileAsset.Status status) {
        UUID upload = UUID.randomUUID();
        return new FileAsset(UUID.randomUUID(), upload, upload + ".bin", upload + ".part", "synthetic.txt",
                "text/plain", "text/plain", 9, "a".repeat(64), UUID.randomUUID(), status, now);
    }

    private FileRecoveryOperation lease(UUID fileId, FileRecoveryOperation.Operation operation) {
        var pending = new FileRecoveryOperation(UUID.randomUUID(), fileId, operation,
                FileRecoveryOperation.Status.PENDING, 0, now, null, null, null, UUID.randomUUID(), now, now);
        return pending.lease("worker", now, Duration.ofMinutes(2));
    }
}
