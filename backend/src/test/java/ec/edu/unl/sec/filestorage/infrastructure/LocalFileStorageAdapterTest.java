package ec.edu.unl.sec.filestorage.infrastructure;

import static org.assertj.core.api.Assertions.*;

import ec.edu.unl.sec.filestorage.application.FileStoragePort.PreparedObject;
import ec.edu.unl.sec.filestorage.application.FileStorageProperties;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Assumptions;

class LocalFileStorageAdapterTest {
    @TempDir Path directory;
    private final byte[] content = "synthetic evidence".getBytes(StandardCharsets.UTF_8);

    @Test void stagesAndPromotesOnSameFileStoreWithExactBytesAndHash() throws Exception {
        var adapter = adapter(directory.resolve("files"));
        UUID upload = UUID.randomUUID();
        var prepared = adapter.prepare(upload, new ByteArrayInputStream(content));
        assertThat(prepared.stagingKey()).isEqualTo(upload + ".part");
        assertThat(prepared.sizeBytes()).isEqualTo(content.length);
        assertThat(prepared.sha256()).isEqualTo(hash(content));
        assertThat(Files.getFileStore(directory.resolve("files/staging")))
                .isEqualTo(Files.getFileStore(directory.resolve("files/objects")));
        String key = adapter.promote(prepared);
        assertThat(key).isEqualTo(upload + ".bin");
        assertThat(Files.exists(directory.resolve("files/staging").resolve(prepared.stagingKey()))).isFalse();
        try (var stream = adapter.open(key)) { assertThat(stream.readAllBytes()).isEqualTo(content); }
        assertThat(adapter.verify(key).sha256()).isEqualTo(hash(content));
        assertThat(adapter.verify(key).sizeBytes()).isEqualTo(content.length);
    }

    @Test void retriesAcrossAdapterInstancesWithoutOverwritingExistingContent() throws Exception {
        Path root = directory.resolve("files");
        var first = adapter(root);
        UUID upload = UUID.randomUUID();
        var prepared = first.prepare(upload, new ByteArrayInputStream(content));
        var second = adapter(root);
        assertThat(second.prepare(upload, new ByteArrayInputStream(content))).isEqualTo(prepared);
        String key = second.promote(prepared);
        assertThat(first.promote(prepared)).isEqualTo(key);
        assertThat(first.prepare(upload, new ByteArrayInputStream(content))).isEqualTo(prepared);
        assertThatThrownBy(() -> first.prepare(upload, new ByteArrayInputStream(new byte[]{1})))
                .isInstanceOf(IOException.class).hasMessage("FILE_UPLOAD_CONFLICT");
        assertThatThrownBy(() -> first.promote(new PreparedObject(upload, prepared.stagingKey(), 1, hash(new byte[]{1}))))
                .isInstanceOf(IOException.class).hasMessage("FILE_INTEGRITY_FAILED");
        try (var stream = first.open(key)) { assertThat(stream.readAllBytes()).isEqualTo(content); }
    }

    @Test void rejectsUntrustedKeysAndForgedOrCorruptPreparedObjects() throws Exception {
        var adapter = adapter(directory.resolve("files"));
        for (String key : new String[]{"../outside", "C:\\outside", "/etc/passwd", "not-an-object", UUID.randomUUID() + ".bin/../other"}) {
            assertThatThrownBy(() -> adapter.open(key)).isInstanceOf(IOException.class).hasMessage("FILE_KEY_INVALID");
            assertThatThrownBy(() -> adapter.verify(key)).isInstanceOf(IOException.class).hasMessage("FILE_KEY_INVALID");
        }
        UUID upload = UUID.randomUUID();
        var prepared = adapter.prepare(upload, new ByteArrayInputStream(content));
        assertThatThrownBy(() -> adapter.promote(new PreparedObject(upload, "../outside", prepared.sizeBytes(), prepared.sha256())))
                .isInstanceOf(IOException.class).hasMessage("FILE_KEY_INVALID");
        Files.write(directory.resolve("files/staging").resolve(prepared.stagingKey()), new byte[]{1});
        assertThatThrownBy(() -> adapter.promote(prepared)).hasMessage("FILE_INTEGRITY_FAILED");
        assertThat(Files.exists(directory.resolve("files/objects").resolve(upload + ".bin"))).isFalse();
    }

    @Test void cleansOnlyItsPartialWriteAfterReadFailureOrOversize() throws Exception {
        var properties = properties(directory.resolve("files"));
        properties.setMaxBytes(3);
        var adapter = new LocalFileStorageAdapter(properties);
        adapter.initialize();
        assertThatThrownBy(() -> adapter.prepare(UUID.randomUUID(), new ByteArrayInputStream(content)))
                .hasMessage("FILE_TOO_LARGE");
        InputStream broken = new InputStream() {
            @Override public int read() throws IOException { throw new IOException("synthetic failure"); }
        };
        assertThatThrownBy(() -> adapter.prepare(UUID.randomUUID(), broken)).hasMessage("synthetic failure");
        try (var files = Files.list(directory.resolve("files/staging"))) { assertThat(files).isEmpty(); }
    }

    @Test void refusesStartupWhenAtomicMoveIsUnavailable() {
        var adapter = new LocalFileStorageAdapter(properties(directory.resolve("files"))) {
            @Override protected void atomicMove(Path source, Path target) throws IOException {
                throw new AtomicMoveNotSupportedException("synthetic source", "synthetic target", "unsupported");
            }
        };
        assertThatThrownBy(adapter::initialize).isInstanceOf(AtomicMoveNotSupportedException.class);
    }

    @Test void handlesAnEmptyObjectAndMissingObjectsWithoutCreatingContent() throws Exception {
        var adapter = adapter(directory.resolve("files"));
        var prepared = adapter.prepare(UUID.randomUUID(), InputStream.nullInputStream());
        assertThat(prepared.sizeBytes()).isZero();
        String key = adapter.promote(prepared);
        assertThat(adapter.verify(key).sha256()).isEqualTo(hash(new byte[0]));
        assertThatThrownBy(() -> adapter.open(UUID.randomUUID() + ".bin")).isInstanceOf(IOException.class);
        assertThatThrownBy(() -> adapter.verify(UUID.randomUUID() + ".bin")).isInstanceOf(IOException.class);
    }

    @Test void doesNotOverwriteContentWhenAnotherWriterHoldsTheUploadLock() throws Exception {
        Path root = directory.resolve("files");
        var adapter = adapter(root);
        UUID upload = UUID.randomUUID();
        var prepared = adapter.prepare(upload, new ByteArrayInputStream(content));
        try (var channel = java.nio.channels.FileChannel.open(root.resolve("locks").resolve(upload + ".lock"),
                StandardOpenOption.WRITE); var lock = channel.lock()) {
            assertThat(lock.isValid()).isTrue();
            assertThatThrownBy(() -> adapter.prepare(upload, new ByteArrayInputStream(new byte[]{1})))
                    .isInstanceOf(IOException.class).hasMessage("FILE_UPLOAD_BUSY");
            assertThatThrownBy(() -> adapter.promote(prepared)).isInstanceOf(IOException.class).hasMessage("FILE_UPLOAD_BUSY");
        }
        assertThat(adapter.verify(adapter.promote(prepared)).sha256()).isEqualTo(hash(content));
    }

    @Test void retainsStagingWhenPromotionFailsWithoutCopyFallback() throws Exception {
        var adapter = new LocalFileStorageAdapter(properties(directory.resolve("files"))) {
            @Override protected void atomicMove(Path source, Path target) throws IOException {
                if (source.toString().endsWith(".part")) throw new IOException("synthetic promotion failure");
                super.atomicMove(source, target);
            }
        };
        adapter.initialize();
        var prepared = adapter.prepare(UUID.randomUUID(), new ByteArrayInputStream(content));
        assertThatThrownBy(() -> adapter.promote(prepared)).hasMessage("synthetic promotion failure");
        assertThat(Files.readAllBytes(directory.resolve("files/staging").resolve(prepared.stagingKey()))).isEqualTo(content);
        try (var files = Files.list(directory.resolve("files/objects"))) { assertThat(files).isEmpty(); }
    }

    @Test void rejectsSymbolicLinksAtRootAndAtObjectPaths() throws Exception {
        Path outside = Files.createDirectory(directory.resolve("outside"));
        Path link = directory.resolve("linked-root");
        try { Files.createSymbolicLink(link, outside); }
        catch (UnsupportedOperationException | FileSystemException unavailable) {
            Assumptions.abort("Symbolic links unavailable: " + unavailable.getClass().getSimpleName());
        }
        assertThatThrownBy(() -> adapter(link)).isInstanceOf(IOException.class);
        Path root = directory.resolve("files");
        var adapter = adapter(root);
        String key = UUID.randomUUID() + ".bin";
        Path external = Files.write(outside.resolve("external"), content);
        Files.createSymbolicLink(root.resolve("objects").resolve(key), external);
        assertThatThrownBy(() -> adapter.open(key)).isInstanceOf(IOException.class);
        assertThatThrownBy(() -> adapter.verify(key)).isInstanceOf(IOException.class);
    }

    private LocalFileStorageAdapter adapter(Path root) throws IOException {
        var adapter = new LocalFileStorageAdapter(properties(root));
        adapter.initialize();
        return adapter;
    }
    private static FileStorageProperties properties(Path root) {
        var properties = new FileStorageProperties();
        properties.setRoot(root);
        return properties;
    }
    private static String hash(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
}
