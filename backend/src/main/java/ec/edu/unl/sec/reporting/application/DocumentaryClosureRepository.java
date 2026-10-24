package ec.edu.unl.sec.reporting.application;

import ec.edu.unl.sec.reporting.domain.DocumentaryClosure;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentaryClosureRepository {
    void save(DocumentaryClosure closure);
    Optional<DocumentaryClosure.Header> header(UUID id);
    Optional<DocumentaryClosure> find(UUID id);
    List<DocumentaryClosure.Header> list(UUID processId,long offset,int limit);
}
