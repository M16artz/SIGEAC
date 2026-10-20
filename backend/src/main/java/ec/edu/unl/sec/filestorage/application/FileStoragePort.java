package ec.edu.unl.sec.filestorage.application;

import ec.edu.unl.sec.filestorage.domain.StoredFile;
import java.io.IOException;
import java.io.InputStream;

public interface FileStoragePort {
    StoredFile prepare(String originalName, String declaredMediaType, InputStream content) throws IOException;
    StoredFile promote(String storageKey) throws IOException;
    InputStream open(String storageKey) throws IOException;
    void markForReconciliation(String storageKey, String reason);
}
