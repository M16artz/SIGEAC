package ec.edu.unl.sec.evidence.application;

import ec.edu.unl.sec.evidence.domain.EvidenceVersion;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EvidenceHistoryService {
    private final EvidenceDraftRepository repository;

    public EvidenceHistoryService(EvidenceDraftRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<EvidenceVersion> history(UUID evidenceId) {
        var details = repository.findEvidence(evidenceId).orElseThrow(EvidenceDraftService::notFound);
        List<EvidenceVersion> versions = new ArrayList<>();
        var current = details.latestVersion();
        versions.add(current);
        var cursor = current.predecessorId();
        while (cursor != null) {
            var version = repository.findVersion(cursor).orElseThrow(EvidenceDraftService::notFound);
            versions.add(version);
            cursor = version.predecessorId();
        }
        versions.sort(Comparator.comparingInt(EvidenceVersion::revisionNumber));
        return List.copyOf(versions);
    }
}
