package ec.edu.unl.sec.evidence.api;

import ec.edu.unl.sec.evidence.application.EvidenceFileService;
import ec.edu.unl.sec.evidence.application.EvidenceSourceService;
import ec.edu.unl.sec.shared.api.EntityTagSupport;
import ec.edu.unl.sec.shared.infrastructure.CorrelationFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class EvidenceSourceController {
    private final EvidenceSourceService service;
    private final EvidenceFileService files;

    public EvidenceSourceController(EvidenceSourceService service, EvidenceFileService files) {
        this.service = service;
        this.files = files;
    }

    @PostMapping(path = "/evidence-versions/{versionId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileMetadataView> upload(@PathVariable UUID versionId,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            @RequestPart("file") MultipartFile file, HttpServletRequest request) throws IOException {
        var changed = files.upload(versionId, ifMatch, file.getOriginalFilename(), file.getContentType(),
                file.getInputStream(), correlation(request));
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create("/api/v1/evidence-versions/" + versionId + "/files/"
                        + changed.value().snapshot().fileId()))
                .eTag(EntityTagSupport.strong(changed.revision()))
                .body(FileMetadataView.from(changed.value()));
    }

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

    private static UUID correlation(HttpServletRequest request) {
        Object value = request.getAttribute(CorrelationFilter.CORRELATION_ATTRIBUTE);
        if (value == null) return UUID.randomUUID();
        try {
            return UUID.fromString(value.toString());
        } catch (IllegalArgumentException ignored) {
            return UUID.nameUUIDFromBytes(value.toString().getBytes(StandardCharsets.UTF_8));
        }
    }
}
