package ec.edu.unl.sec.evidence.api;

import ec.edu.unl.sec.evidence.application.EvidenceSourceService;
import ec.edu.unl.sec.shared.api.EntityTagSupport;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class EvidenceSourceController {
    private final EvidenceSourceService service;

    public EvidenceSourceController(EvidenceSourceService service) { this.service = service; }

    @PostMapping("/evidence-versions/{versionId}/external-references")
    public ResponseEntity<ExternalReferenceView> addReference(@PathVariable UUID versionId,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody ExternalReferenceRequest request) {
        var changed = service.addReference(versionId, ifMatch, request.uri(), request.description());
        return ResponseEntity.created(URI.create("/api/v1/evidence-versions/" + versionId + "/external-references/" + changed.value().id()))
                .eTag(EntityTagSupport.strong(changed.revision())).body(ExternalReferenceView.from(changed.value()));
    }

    @PatchMapping("/evidence-versions/{versionId}/external-references/{referenceId}")
    public ResponseEntity<ExternalReferenceView> updateReference(@PathVariable UUID versionId, @PathVariable UUID referenceId,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody ExternalReferenceRequest request) {
        var changed = service.updateReference(versionId, referenceId, ifMatch, request.uri(), request.description());
        return ResponseEntity.ok().eTag(EntityTagSupport.strong(changed.revision())).body(ExternalReferenceView.from(changed.value()));
    }

    @DeleteMapping("/evidence-versions/{versionId}/external-references/{referenceId}")
    public ResponseEntity<Void> removeReference(@PathVariable UUID versionId, @PathVariable UUID referenceId,
            @RequestHeader(name = "If-Match", required = false) String ifMatch) {
        return ResponseEntity.noContent().eTag(EntityTagSupport.strong(service.removeReference(versionId, referenceId, ifMatch))).build();
    }

    @DeleteMapping("/evidence-versions/{versionId}/files/{fileId}")
    public ResponseEntity<Void> removeFile(@PathVariable UUID versionId, @PathVariable UUID fileId,
            @RequestHeader(name = "If-Match", required = false) String ifMatch) {
        return ResponseEntity.noContent().eTag(EntityTagSupport.strong(service.removeFile(versionId, fileId, ifMatch))).build();
    }
}
