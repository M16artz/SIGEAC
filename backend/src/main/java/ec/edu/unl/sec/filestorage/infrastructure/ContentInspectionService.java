package ec.edu.unl.sec.filestorage.infrastructure;

import ec.edu.unl.sec.filestorage.application.FileInspectionPort;
import ec.edu.unl.sec.filestorage.domain.FileValidationException;
import ec.edu.unl.sec.filestorage.domain.FileValidationPolicy;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

@Component
public class ContentInspectionService implements FileInspectionPort {
    private static final int DETECTION_BYTES = 65_536;
    private final Tika tika = new Tika();

    /** Destination must be private staging; the caller removes it if inspection fails. Streams remain caller-owned. */
    public Inspection inspect(InputStream source, OutputStream destination, String declaredMediaType,
            FileValidationPolicy policy) throws IOException {
        MessageDigest digest = sha256();
        var sample = new ByteArrayOutputStream(DETECTION_BYTES);
        byte[] buffer = new byte[8192];
        long size = 0;
        int count;
        while ((count = source.read(buffer, 0, (int) Math.min(buffer.length, policy.maxBytes() - size + 1))) != -1) {
            if (count == 0) continue;
            size += count;
            if (size > policy.maxBytes()) throw new FileValidationException("FILE_TOO_LARGE");
            digest.update(buffer, 0, count);
            sample.write(buffer, 0, Math.min(count, DETECTION_BYTES - sample.size()));
            destination.write(buffer, 0, count);
        }
        // Content-only detection deliberately receives no client filename or Content-Type hints.
        String detected = tika.detect(sample.toByteArray());
        policy.validate(size, declaredMediaType, detected);
        return new Inspection(size, HexFormat.of().formatHex(digest.digest()), detected);
    }

    private static MessageDigest sha256() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}
