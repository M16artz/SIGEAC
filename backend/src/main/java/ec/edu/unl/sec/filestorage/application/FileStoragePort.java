package ec.edu.unl.sec.filestorage.application;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public interface FileStoragePort {
    /** Caller owns the input stream; retrying an upload ID must preserve identical content. */
    PreparedObject prepare(UUID uploadId, InputStream content) throws IOException;
    /** Verify the prepared size/hash and publish atomically; a retry never replaces a final object. */
    String promote(PreparedObject prepared) throws IOException;
    /** Caller closes the returned stream; authorization and pre-response integrity checks belong to the application. */
    InputStream open(String storageKey) throws IOException;
    Verification verify(String storageKey) throws IOException;
    record PreparedObject(UUID uploadId,String stagingKey,long sizeBytes,String sha256){}
    record Verification(long sizeBytes,String sha256){}
}
