package ec.edu.unl.sec.evidence.api;

import ec.edu.unl.sec.evidence.application.CreateEvidenceSuccessorService;
import ec.edu.unl.sec.evidence.application.EvidenceDraftService;
import ec.edu.unl.sec.evidence.application.EvidenceSourceService;
import ec.edu.unl.sec.shared.api.EntityTagSupport;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EvidenceHistoryController {
    private final CreateEvidenceSuccessorService successors;
    private final EvidenceDraftService drafts;
    private final EvidenceSourceService sources;

    public EvidenceHistoryController(CreateEvidenceSuccessorService successors, EvidenceDraftService drafts,
            EvidenceSourceService sources) {
        this.successors = successors;
        this.drafts = drafts;
        this.sources = sources;
    }

    @PostMapping("/evidence-versions/{versionId}/successor")
    public ResponseEntity<EvidenceView.Version> createSuccessor(@PathVariable UUID versionId) {
        var created = successors.createSuccessor(versionId);
        var version = drafts.getVersion(created.versionId());
        return ResponseEntity.created(URI.create("/api/v1/evidence-versions/" + version.id()))
                .eTag(EntityTagSupport.strong(version.lockRevision()))
                .body(EvidenceView.version(version, sources.snapshot(version.id())));
    }
}
