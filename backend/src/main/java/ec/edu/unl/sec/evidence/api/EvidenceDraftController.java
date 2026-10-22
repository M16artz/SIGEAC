package ec.edu.unl.sec.evidence.api;

import ec.edu.unl.sec.evidence.application.EvidenceDraftService;
import ec.edu.unl.sec.shared.api.EntityTagSupport;
import ec.edu.unl.sec.shared.infrastructure.CorrelationFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class EvidenceDraftController {
    private final EvidenceDraftService service;

    public EvidenceDraftController(EvidenceDraftService service) {
        this.service = service;
    }

    @GetMapping("/evidences")
    public EvidenceView.Page<EvidenceView> list(
            @RequestParam UUID processId,
            @RequestParam Optional<UUID> indicatorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(100, Math.max(1, size));
        var visible = service.list(processId, indicatorId).stream().map(EvidenceView::from).toList();
        int from = Math.min(visible.size(), safePage * safeSize);
        int to = Math.min(visible.size(), from + safeSize);
        int pages = visible.isEmpty() ? 0 : (visible.size() + safeSize - 1) / safeSize;
        return new EvidenceView.Page<>(
                visible.subList(from, to),
                new EvidenceView.PageMetadata(safePage, safeSize, visible.size(), pages));
    }

    @PostMapping("/evidences")
    public ResponseEntity<EvidenceView.Version> create(
            @Valid @RequestBody EvidenceDraftRequest.Create request, HttpServletRequest servletRequest) {
        var created = service.create(
                new EvidenceDraftService.CreateCommand(
                        request.processId(),
                        request.title(),
                        request.description(),
                        request.provenance(),
                        request.coveredFrom(),
                        request.coveredTo(),
                        request.indicatorIds()),
                correlation(servletRequest));
        var version = created.latestVersion();
        return ResponseEntity.created(URI.create("/api/v1/evidence-versions/" + version.id()))
                .eTag(EntityTagSupport.strong(version.lockRevision()))
                .body(EvidenceView.version(version));
    }

    @GetMapping("/evidences/{evidenceId}")
    public EvidenceView getEvidence(@PathVariable UUID evidenceId) {
        return EvidenceView.from(service.getEvidence(evidenceId));
    }

    @GetMapping("/evidence-versions/{versionId}")
    public ResponseEntity<EvidenceView.Version> getVersion(@PathVariable UUID versionId) {
        var version = service.getVersion(versionId);
        return ResponseEntity.ok()
                .eTag(EntityTagSupport.strong(version.lockRevision()))
                .body(EvidenceView.version(version));
    }

    @PatchMapping("/evidence-versions/{versionId}")
    public ResponseEntity<EvidenceView.Version> update(
            @PathVariable UUID versionId,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody EvidenceDraftRequest.Update request,
            HttpServletRequest servletRequest) {
        var version = service.update(
                versionId,
                ifMatch,
                new EvidenceDraftService.UpdateCommand(
                        request.title(),
                        request.description(),
                        request.provenance(),
                        request.coveredFrom(),
                        request.coveredTo(),
                        request.indicatorIds()),
                correlation(servletRequest));
        return ResponseEntity.ok()
                .eTag(EntityTagSupport.strong(version.lockRevision()))
                .body(EvidenceView.version(version));
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
