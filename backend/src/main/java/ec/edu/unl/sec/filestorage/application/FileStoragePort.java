package ec.edu.unl.sec.filestorage.application;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public interface FileStoragePort {
    PreparedObject prepare(UUID uploadId, InputStream content) throws IOException;
    String promote(PreparedObject prepared) throws IOException;
    InputStream open(String storageKey) throws IOException;
    Verification verify(String storageKey) throws IOException;
    record PreparedObject(UUID uploadId,String stagingKey,long sizeBytes,String sha256){}
    record Verification(long sizeBytes,String sha256){}
}
