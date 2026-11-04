package ec.edu.unl.sec.evidence.application;

import ec.edu.unl.sec.evidence.domain.VersionFile;
import ec.edu.unl.sec.filestorage.application.FileAssetRepository;
import ec.edu.unl.sec.filestorage.application.FileStoragePort;
import ec.edu.unl.sec.filestorage.domain.FileAsset;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authorized, integrity-verified download. Bytes are spooled and compared with
 * the registered size/digest before any response is produced; the verified
 * temporary copy is always removed.
 */
@Service
public class EvidenceDownloadService {
    private final EvidenceDraftService drafts;
    private final EvidenceSourceRepository sources;
    private final FileAssetRepository assets;
    private final FileStoragePort storage;

    public EvidenceDownloadService(EvidenceDraftService drafts, EvidenceSourceRepository sources,
            FileAssetRepository assets, FileStoragePort storage) {
        this.drafts = drafts;
        this.sources = sources;
        this.assets = assets;
        this.storage = storage;
    }

    @Transactional(readOnly = true)
    public Download prepare(UUID versionId, UUID fileId) {
        drafts.getVersion(versionId);
        var snapshot = sources.files(versionId).stream()
                .filter(file -> file.fileId().equals(fileId))
                .findFirst()
                .orElseThrow(EvidenceDraftService::notFound);
        var asset = assets.findById(fileId).orElseThrow(EvidenceDraftService::notFound);
        if (asset.status() != FileAsset.Status.AVAILABLE) throw integrityFailure();
        return verifyAndRead(snapshot, asset);
    }

    private Download verifyAndRead(VersionFile snapshot, FileAsset asset) {
        Path spool = null;
        try {
            spool = Files.createTempFile("sec-download-", ".part");
            var digest = sha256();
            long size;
            try (InputStream input = storage.open(asset.storageKey());
                    OutputStream output = Files.newOutputStream(spool)) {
                size = copy(input, output, digest);
            }
            var actual = HexFormat.of().formatHex(digest.digest());
            if (size != snapshot.sizeBytes() || size != asset.sizeBytes()
                    || !actual.equals(snapshot.sha256()) || !actual.equals(asset.sha256())) {
                throw integrityFailure();
            }
            var bytes = Files.readAllBytes(spool);
            return new Download(safeName(snapshot.originalName()), snapshot.detectedMediaType(),
                    snapshot.sizeBytes(), snapshot.sha256(), bytes);
        } catch (IOException failure) {
            throw integrityFailure();
        } finally {
            if (spool != null) {
                try {
                    Files.deleteIfExists(spool);
                } catch (IOException ignored) {
                    // Best-effort cleanup; the verified response is unaffected.
                }
            }
        }
    }

    private static long copy(InputStream input, OutputStream output, MessageDigest digest) throws IOException {
        var buffer = new byte[8192];
        long total = 0;
        int read;
        while ((read = input.read(buffer)) != -1) {
            digest.update(buffer, 0, read);
            output.write(buffer, 0, read);
            total += read;
            if (total > FileAssetLimits.MAX_BYTES) break;
        }
        return total;
    }

    private static String safeName(String originalName) {
        var cleaned = originalName.replaceAll("[\\r\\n\"\\\\/]", "_").strip();
        return cleaned.isEmpty() ? "evidencia" : cleaned;
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static EvidenceProblemException integrityFailure() {
        return new EvidenceProblemException(EvidenceProblemException.Kind.CONFLICT,
                "FILE_INTEGRITY_FAILED",
                "El archivo no está disponible o no supera la verificación de integridad.");
    }

    public record Download(String filename, String mediaType, long sizeBytes, String sha256, byte[] content) {
        public Download {
            content = content.clone();
        }

        @Override
        public byte[] content() {
            return content.clone();
        }
    }

    private static final class FileAssetLimits {
        private static final long MAX_BYTES = 26_214_400L;

        private FileAssetLimits() {}
    }
}
