package ec.edu.unl.sec.evidence.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.edu.unl.sec.evidence.domain.VersionFile;
import ec.edu.unl.sec.filestorage.application.FileAssetRepository;
import ec.edu.unl.sec.filestorage.application.FileStoragePort;
import ec.edu.unl.sec.filestorage.domain.FileAsset;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EvidenceDownloadServiceTest {
    private static final UUID VERSION = UUID.fromString("00000000-0000-0000-0000-000000000701");
    private static final UUID FILE = UUID.fromString("00000000-0000-0000-0000-000000000702");
    private static final byte[] CONTENT = "synthetic evidence".getBytes(StandardCharsets.UTF_8);
    private static final String SHA = sha256(CONTENT);

    private EvidenceDraftService drafts;
    private EvidenceSourceRepository sources;
    private FileAssetRepository assets;
    private FileStoragePort storage;
    private EvidenceDownloadService service;

    @BeforeEach
    void setUp() {
        drafts = mock(EvidenceDraftService.class);
        sources = mock(EvidenceSourceRepository.class);
        assets = mock(FileAssetRepository.class);
        storage = mock(FileStoragePort.class);
        service = new EvidenceDownloadService(drafts, sources, assets, storage);
        when(sources.files(VERSION)).thenReturn(List.of(new VersionFile(
                VERSION, FILE, "synthetic.txt", "text/plain", "text/plain", CONTENT.length, SHA, 1)));
        when(assets.findById(FILE)).thenReturn(Optional.of(asset(FileAsset.Status.AVAILABLE, SHA)));
    }

    @Test
    void returnsExactVerifiedBytesWithASafeFilename() throws IOException {
        when(storage.open("opaque-key")).thenReturn(new ByteArrayInputStream(CONTENT));

        var download = service.prepare(VERSION, FILE);

        assertThat(download.content()).isEqualTo(CONTENT);
        assertThat(download.sizeBytes()).isEqualTo(CONTENT.length);
        assertThat(download.sha256()).isEqualTo(SHA);
        assertThat(download.filename()).isEqualTo("synthetic.txt");
        verify(drafts).getVersion(VERSION);
    }

    @Test
    void rejectsForeignOrUnknownFilesBeforeOpeningStorage() throws IOException {
        when(drafts.getVersion(VERSION)).thenThrow(EvidenceDraftService.notFound());

        assertThatThrownBy(() -> service.prepare(VERSION, FILE))
                .isInstanceOf(EvidenceProblemException.class)
                .satisfies(error -> assertThat(((EvidenceProblemException) error).code()).isEqualTo("RESOURCE_NOT_FOUND"));
        verify(storage, never()).open(any());
    }

    @Test
    void rejectsMismatchedBytesWithoutReturningContent() throws IOException {
        when(storage.open("opaque-key")).thenReturn(
                new ByteArrayInputStream("tampered evidence".getBytes(StandardCharsets.UTF_8)));

        assertThatThrownBy(() -> service.prepare(VERSION, FILE))
                .isInstanceOf(EvidenceProblemException.class)
                .satisfies(error -> assertThat(((EvidenceProblemException) error).code())
                        .isEqualTo("FILE_INTEGRITY_FAILED"));
    }

    @Test
    void rejectsFilesThatAreNotAvailable() {
        when(assets.findById(FILE)).thenReturn(Optional.of(asset(FileAsset.Status.QUARANTINED, SHA)));

        assertThatThrownBy(() -> service.prepare(VERSION, FILE))
                .isInstanceOf(EvidenceProblemException.class)
                .satisfies(error -> assertThat(((EvidenceProblemException) error).code())
                        .isEqualTo("FILE_INTEGRITY_FAILED"));
    }

    private static FileAsset asset(FileAsset.Status status, String sha) {
        return new FileAsset(FILE, UUID.randomUUID(), "opaque-key", null, "synthetic.txt",
                "text/plain", "text/plain", CONTENT.length, sha,
                UUID.fromString("70000000-0000-0000-0000-000000000001"), status, java.time.Instant.EPOCH);
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
