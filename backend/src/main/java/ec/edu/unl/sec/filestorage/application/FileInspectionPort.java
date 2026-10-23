package ec.edu.unl.sec.filestorage.application;

import ec.edu.unl.sec.filestorage.domain.FileValidationPolicy;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/** Bounded content inspection port; adapters decide how bytes are detected. */
public interface FileInspectionPort {
    Inspection inspect(InputStream source, OutputStream destination, String declaredMediaType,
            FileValidationPolicy policy) throws IOException;

    record Inspection(long sizeBytes, String sha256, String detectedMediaType) {}
}
