package ec.edu.unl.sec.filestorage.infrastructure;

import static ec.edu.unl.sec.filestorage.domain.FileValidationPolicyTest.policy;
import static org.assertj.core.api.Assertions.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class ContentInspectionServiceTest {
    private final ContentInspectionService inspector = new ContentInspectionService();

    @Test void detectsBytesWithoutFilenameOrDeclaredTypeHintsAndCopiesExactContent() throws Exception {
        byte[] bytes = "%PDF-1.7\nsynthetic fixture\n".getBytes(StandardCharsets.US_ASCII);
        var destination = new ByteArrayOutputStream();
        var result = inspector.inspect(new ByteArrayInputStream(bytes), destination, "application/pdf", policy(false));
        assertThat(result.detectedMediaType()).isEqualTo("application/pdf");
        assertThat(result.sha256()).isEqualTo(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
        assertThat(result.sha256()).matches("[a-f0-9]{64}");
        assertThat(result.sizeBytes()).isEqualTo(bytes.length);
        assertThat(destination.toByteArray()).isEqualTo(bytes);
        assertThatThrownBy(() -> inspector.inspect(new ByteArrayInputStream(bytes), OutputStream.nullOutputStream(),
                "text/plain", policy(false))).hasMessage("FILE_TYPE_MISMATCH");
    }

    @Test void acceptsExact25MiBAndReadsAtMostOneExtraByteToRejectOversize() throws Exception {
        var exact = new RepeatedStream(26_214_400);
        assertThat(inspector.inspect(exact, OutputStream.nullOutputStream(), "text/plain", policy(false)).sizeBytes())
                .isEqualTo(26_214_400);
        var over = new RepeatedStream(26_214_500);
        assertThatThrownBy(() -> inspector.inspect(over, OutputStream.nullOutputStream(), "text/plain", policy(false)))
                .hasMessage("FILE_TOO_LARGE");
        assertThat(over.readBytes).isEqualTo(26_214_401);
    }

    @Test void emptyFileAcceptanceIsDecidedByPolicy() throws Exception {
        assertThatThrownBy(() -> inspector.inspect(InputStream.nullInputStream(), OutputStream.nullOutputStream(),
                "application/octet-stream", policy(false))).hasMessage("FILE_EMPTY");
        assertThat(inspector.inspect(InputStream.nullInputStream(), OutputStream.nullOutputStream(),
                "application/octet-stream", policy(true)).sizeBytes()).isZero();
    }

    @Test void propagatesReadAndWriteFailures() {
        InputStream broken = new InputStream() {
            @Override public int read() throws IOException { throw new IOException("synthetic read failure"); }
        };
        assertThatThrownBy(() -> inspector.inspect(broken, OutputStream.nullOutputStream(), "text/plain", policy(false)))
                .isInstanceOf(IOException.class);
        OutputStream full = new OutputStream() {
            @Override public void write(int value) throws IOException { throw new IOException("synthetic write failure"); }
        };
        assertThatThrownBy(() -> inspector.inspect(new ByteArrayInputStream(new byte[]{65}), full, "text/plain", policy(false)))
                .isInstanceOf(IOException.class);
    }

    private static final class RepeatedStream extends InputStream {
        private final long length;
        private long readBytes;
        private RepeatedStream(long length) { this.length = length; }
        @Override public int read() { return readBytes++ < length ? 'a' : -1; }
        @Override public int read(byte[] buffer, int offset, int count) {
            if (readBytes == length) return -1;
            int size = (int) Math.min(count, length - readBytes);
            java.util.Arrays.fill(buffer, offset, offset + size, (byte) 'a');
            readBytes += size;
            return size;
        }
    }
}
