package ec.edu.unl.sec.archive.application;

import ec.edu.unl.sec.archive.domain.ArchiveRecord;
import ec.edu.unl.sec.archive.domain.IntegralClosure;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IntegralClosureRepository {
    void append(IntegralClosure closure);

    Optional<IntegralClosure> find(UUID closureId);

    Optional<IntegralClosure> findForProcess(UUID processId, UUID closureId);

    List<IntegralClosure> closures(UUID processId, long offset, int limit);

    long closureTotal(UUID processId);

    void appendArchive(ArchiveRecord record);

    Optional<ArchiveRecord> archiveForClosure(UUID closureId);
}
