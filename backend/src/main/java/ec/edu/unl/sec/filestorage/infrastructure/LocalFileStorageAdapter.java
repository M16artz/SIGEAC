package ec.edu.unl.sec.filestorage.infrastructure;

import ec.edu.unl.sec.filestorage.application.FileStoragePort;
import ec.edu.unl.sec.filestorage.application.FileStorageProperties;
import ec.edu.unl.sec.filestorage.domain.FileValidationPolicy;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.stereotype.Component;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardOpenOption.*;

@Component
public class LocalFileStorageAdapter implements FileStoragePort {
    private final Path root;
    private final long maxBytes;

    public LocalFileStorageAdapter(FileStorageProperties properties) {
        root = properties.getRoot().toAbsolutePath().normalize();
        maxBytes = properties.getMaxBytes();
    }

    @PostConstruct
    public void initialize() throws IOException {
        if (maxBytes <= 0 || maxBytes > FileValidationPolicy.MAX_BYTES) {
            throw new IOException("FILE_SIZE_CONFIGURATION_INVALID");
        }
        createDirectories(root);
        for (String name : new String[]{"staging", "objects", "locks"}) createDirectories(root.resolve(name));
        if (!Files.getFileStore(root.resolve("staging")).equals(Files.getFileStore(root.resolve("objects")))) {
            throw new IOException("FILE_STORAGE_DIFFERENT_FILE_STORES");
        }
        Path probe = Files.createTempFile(root.resolve("staging"), ".probe-", ".tmp");
        Path destination = root.resolve("objects").resolve(probe.getFileName());
        try { atomicMove(probe, destination); }
        finally {
            Files.deleteIfExists(probe);
            Files.deleteIfExists(destination);
        }
    }

    @Override
    public synchronized PreparedObject prepare(UUID uploadId, InputStream content) throws IOException {
        if (uploadId == null) throw new IOException("FILE_KEY_INVALID");
        checkDirectories();
        Path temporary = Files.createTempFile(root.resolve("staging"), ".upload-", ".tmp");
        try {
            Verification incoming;
            try (var channel = FileChannel.open(temporary, WRITE, NOFOLLOW_LINKS)) {
                var output = Channels.newOutputStream(channel);
                incoming = digest(content, output);
                channel.force(true);
            }
            try (var channel = lockChannel(uploadId); var lock = channel.lock()) {
                if (!lock.isValid()) throw new IOException("FILE_UPLOAD_BUSY");
                checkDirectories();
                Path staging = root.resolve("staging").resolve(uploadId + ".part");
                Path object = root.resolve("objects").resolve(uploadId + ".bin");
                if (Files.exists(object, NOFOLLOW_LINKS)) requireSame(verifyPath(object), incoming, "FILE_UPLOAD_CONFLICT");
                else if (Files.exists(staging, NOFOLLOW_LINKS)) requireSame(verifyPath(staging), incoming, "FILE_UPLOAD_CONFLICT");
                else atomicMove(temporary, staging);
                return new PreparedObject(uploadId, uploadId + ".part", incoming.sizeBytes(), incoming.sha256());
            } catch (OverlappingFileLockException busy) { throw new IOException("FILE_UPLOAD_BUSY", busy); }
        } finally { Files.deleteIfExists(temporary); }
    }

    @Override
    public synchronized String promote(PreparedObject prepared) throws IOException {
        if (prepared == null || prepared.uploadId() == null
                || !(prepared.uploadId() + ".part").equals(prepared.stagingKey())
                || prepared.sizeBytes() < 0 || prepared.sizeBytes() > maxBytes
                || prepared.sha256() == null || !prepared.sha256().matches("[a-f0-9]{64}")) {
            throw new IOException("FILE_KEY_INVALID");
        }
        checkDirectories();
        try (var channel = lockChannel(prepared.uploadId()); var lock = channel.lock()) {
            if (!lock.isValid()) throw new IOException("FILE_UPLOAD_BUSY");
            checkDirectories();
            String key = prepared.uploadId() + ".bin";
            Path object = root.resolve("objects").resolve(key);
            Verification expected = new Verification(prepared.sizeBytes(), prepared.sha256());
            if (Files.exists(object, NOFOLLOW_LINKS)) {
                requireSame(verifyPath(object), expected, "FILE_INTEGRITY_FAILED");
                return key;
            }
            Path staging = root.resolve("staging").resolve(prepared.stagingKey());
            requireSame(verifyPath(staging), expected, "FILE_INTEGRITY_FAILED");
            atomicMove(staging, object);
            requireSame(verifyPath(object), expected, "FILE_INTEGRITY_FAILED");
            return key;
        } catch (OverlappingFileLockException busy) { throw new IOException("FILE_UPLOAD_BUSY", busy); }
    }

    @Override public InputStream open(String storageKey) throws IOException {
        Path object = objectPath(storageKey);
        requireRegularFile(object);
        return Files.newInputStream(object, NOFOLLOW_LINKS);
    }

    @Override public Verification verify(String storageKey) throws IOException {
        return verifyPath(objectPath(storageKey));
    }

    protected void atomicMove(Path source, Path target) throws IOException {
        Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
    }

    private Path objectPath(String key) throws IOException {
        if (key == null || !key.matches("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}\\.bin")) {
            throw new IOException("FILE_KEY_INVALID");
        }
        checkDirectories();
        return root.resolve("objects").resolve(key);
    }

    private FileChannel lockChannel(UUID uploadId) throws IOException {
        return FileChannel.open(root.resolve("locks").resolve(uploadId + ".lock"), CREATE, WRITE, NOFOLLOW_LINKS);
    }

    private Verification verifyPath(Path path) throws IOException {
        requireRegularFile(path);
        try (var stream = Files.newInputStream(path, NOFOLLOW_LINKS)) {
            return digest(stream, java.io.OutputStream.nullOutputStream());
        }
    }

    private Verification digest(InputStream source, java.io.OutputStream destination) throws IOException {
        MessageDigest hash;
        try { hash = MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
        byte[] buffer = new byte[8192];
        long size = 0;
        int count;
        while ((count = source.read(buffer, 0, (int) Math.min(buffer.length, maxBytes - size + 1))) != -1) {
            if (count == 0) continue;
            size += count;
            if (size > maxBytes) throw new IOException("FILE_TOO_LARGE");
            hash.update(buffer, 0, count);
            destination.write(buffer, 0, count);
        }
        return new Verification(size, HexFormat.of().formatHex(hash.digest()));
    }

    private static void requireSame(Verification actual, Verification expected, String code) throws IOException {
        if (!actual.equals(expected)) throw new IOException(code);
    }

    private static void requireRegularFile(Path path) throws IOException {
        if (!Files.isRegularFile(path, NOFOLLOW_LINKS) || !path.toRealPath().equals(path)) {
            throw new IOException("FILE_STORAGE_PATH_UNSAFE");
        }
    }

    private void checkDirectories() throws IOException {
        requireDirectory(root);
        for (String name : new String[]{"staging", "objects", "locks"}) requireDirectory(root.resolve(name));
    }

    private static void createDirectories(Path directory) throws IOException {
        if (directory.getParent() != null) createDirectories(directory.getParent());
        if (!Files.exists(directory, NOFOLLOW_LINKS)) {
            try { Files.createDirectory(directory); }
            catch (FileAlreadyExistsException concurrentCreation) { /* Validate the winning directory below. */ }
        }
        requireDirectory(directory);
    }

    private static void requireDirectory(Path directory) throws IOException {
        if (!Files.isDirectory(directory, NOFOLLOW_LINKS) || !directory.toRealPath().equals(directory)) {
            throw new IOException("FILE_STORAGE_PATH_UNSAFE");
        }
    }
}
