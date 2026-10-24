package ec.edu.unl.sec.reporting.application;

import ec.edu.unl.sec.reporting.domain.DocumentarySnapshot;

public interface DocumentarySnapshotCodec {
    DocumentarySnapshot seal(DocumentarySnapshot.Content content);
    DocumentarySnapshot read(String payloadJson, String sha256);
    ec.edu.unl.sec.reporting.domain.DocumentaryClosure sealClosure(ec.edu.unl.sec.reporting.domain.DocumentaryClosure.Content content);
    ec.edu.unl.sec.reporting.domain.DocumentaryClosure readClosure(String payloadJson,String sha256);
}
