package ec.edu.unl.sec.evidence.application;

import ec.edu.unl.sec.filestorage.application.FileAssetRepository;
import ec.edu.unl.sec.filestorage.application.FileRecoveryRepository;
import ec.edu.unl.sec.filestorage.application.FileStoragePort;
import ec.edu.unl.sec.filestorage.application.FileValidationPolicyRepository;
import ec.edu.unl.sec.filestorage.domain.FileAsset;
import ec.edu.unl.sec.filestorage.domain.FileRecoveryOperation;
import ec.edu.unl.sec.filestorage.domain.FileValidationException;
import ec.edu.unl.sec.filestorage.application.FileInspectionPort;
import ec.edu.unl.sec.filestorage.application.FileStorageProperties;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Prepares, validates and promotes one uploaded source before attaching its
 * immutable snapshot to a draft. Database work and storage work are separate
 * steps; a failed promotion leaves a leased recovery operation instead of a
 * half-attached file.
 */
@Service
public class EvidenceFileService {
    private final EvidenceDraftService drafts;
    private final EvidenceSourceService sources;
    private final FileAssetRepository assets;
    private final FileRecoveryRepository recovery;
    private final FileValidationPolicyRepository policies;
    private final FileStoragePort storage;
    private final FileInspectionPort inspection;
    private final FileStorageProperties properties;
    private final Clock clock;

    @Autowired
    public EvidenceFileService(EvidenceDraftService drafts, EvidenceSourceService sources,
            FileAssetRepository assets, FileRecoveryRepository recovery,
            FileValidationPolicyRepository policies, FileStoragePort storage,
            FileInspectionPort inspection, FileStorageProperties properties) {
        this(drafts, sources, assets, recovery, policies, storage, inspection, properties, Clock.systemUTC());
    }

    EvidenceFileService(EvidenceDraftService drafts, EvidenceSourceService sources,
            FileAssetRepository assets, FileRecoveryRepository recovery,
            FileValidationPolicyRepository policies, FileStoragePort storage,
            FileInspectionPort inspection, FileStorageProperties properties, Clock clock) {
        this.drafts = drafts;
        this.sources = sources;
        this.assets = assets;
        this.recovery = recovery;
        this.policies = policies;
        this.storage = storage;
        this.inspection = inspection;
        this.properties = properties;
        this.clock = clock;
    }

    public EvidenceSourceService.Changed<EvidenceSourceService.SourceFile> upload(
            UUID versionId, String ifMatch, String originalName, String declaredMediaType,
            InputStream content, UUID correlationId) {
        var version = drafts.requireEditableVersion(versionId, ifMatch);
        var policy = policies.findActiveByCode(properties.getValidationPolicyId()).orElseThrow(() ->
                new EvidenceProblemException(EvidenceProblemException.Kind.STORAGE_UNAVAILABLE,
                        "FILE_POLICY_UNAVAILABLE", "No hay una política de archivos activa."));
        String name = safeName(originalName);
        String declared = declaredMediaType == null || declaredMediaType.isBlank()
                ? "application/octet-stream" : declaredMediaType.strip();

        Path temporary = null;
        try {
            temporary = Files.createTempFile("sec-upload-", ".part");
            FileInspectionPort.Inspection inspected;
            try (var destination = Files.newOutputStream(temporary)) {
                inspected = inspection.inspect(content, destination, declared, policy);
            }
            UUID uploadId = UUID.randomUUID();
            FileStoragePort.PreparedObject prepared;
            try (var staged = Files.newInputStream(temporary)) {
                prepared = storage.prepare(uploadId, staged);
            }
            if (prepared.sizeBytes() != inspected.sizeBytes() || !prepared.sha256().equals(inspected.sha256())) {
                throw failure("FILE_INTEGRITY_FAILED");
            }
            var now = clock.instant();
            var asset = new FileAsset(UUID.randomUUID(), uploadId, uploadId + ".bin", prepared.stagingKey(),
                    name, declared, inspected.detectedMediaType(), inspected.sizeBytes(), inspected.sha256(),
                    policy.id(), FileAsset.Status.STAGED, now);
            assets.create(asset);
            promoteAndVerify(asset, prepared, correlationId);
            return sources.attachFile(versionId, asset.id(), version.lockRevision());
        } catch (FileValidationException invalid) {
            if ("FILE_TOO_LARGE".equals(invalid.getMessage())) {
                throw new EvidenceProblemException(EvidenceProblemException.Kind.PAYLOAD_TOO_LARGE,
                        invalid.getMessage(), "El archivo supera el tamaño máximo permitido.");
            }
            throw new EvidenceProblemException(EvidenceProblemException.Kind.INVALID,
                    invalid.getMessage(), "El archivo no cumple la política de validación.");
        } catch (IOException storageFailure) {
            throw failure(storageFailure.getMessage());
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); }
                catch (IOException ignored) { /* Temp files live outside the published object store. */ }
            }
        }
    }

    private void promoteAndVerify(FileAsset asset, FileStoragePort.PreparedObject prepared, UUID correlationId) {
        try {
            storage.promote(prepared);
            var verification = storage.verify(asset.storageKey());
            if (verification.sizeBytes() != asset.sizeBytes() || !verification.sha256().equals(asset.sha256())) {
                throw new IOException("FILE_INTEGRITY_FAILED");
            }
            if (!assets.changeStatus(asset.id(), FileAsset.Status.STAGED, FileAsset.Status.AVAILABLE)) {
                throw new IOException("FILE_STATUS_CONFLICT");
            }
        } catch (IOException promotionFailure) {
            schedulePromotion(asset, correlationId);
            throw failure(promotionFailure.getMessage());
        }
    }

    private void schedulePromotion(FileAsset asset, UUID correlationId) {
        var now = clock.instant();
        recovery.create(new FileRecoveryOperation(UUID.randomUUID(), asset.id(),
                FileRecoveryOperation.Operation.PROMOTE, FileRecoveryOperation.Status.PENDING,
                0, now, null, null, null, correlationId == null ? UUID.randomUUID() : correlationId, now, now));
    }

    private static EvidenceProblemException failure(String code) {
        if ("FILE_TOO_LARGE".equals(code)) {
            return new EvidenceProblemException(EvidenceProblemException.Kind.PAYLOAD_TOO_LARGE,
                    "FILE_TOO_LARGE", "El archivo supera el tamaño máximo permitido.");
        }
        return new EvidenceProblemException(EvidenceProblemException.Kind.STORAGE_UNAVAILABLE,
                "FILE_STORAGE_UNAVAILABLE", "No fue posible almacenar el archivo; reintente la carga.");
    }

    private static String safeName(String originalName) {
        String candidate = originalName == null ? "" : originalName.replace('\\', '/');
        int slash = candidate.lastIndexOf('/');
        if (slash >= 0) candidate = candidate.substring(slash + 1);
        candidate = candidate.replaceAll("[\\p{Cntrl}]", "").strip();
        if (candidate.isBlank() || candidate.equals(".") || candidate.equals("..")) candidate = "archivo";
        return candidate.length() > 500 ? candidate.substring(0, 500) : candidate;
    }
}
