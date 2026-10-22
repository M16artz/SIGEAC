package ec.edu.unl.sec.evidence.api;

import ec.edu.unl.sec.evidence.application.StructuredEvidenceService;
import ec.edu.unl.sec.evidence.domain.StructuredEvidence;
import ec.edu.unl.sec.shared.api.EntityTagSupport;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Registers typed snapshots for structured-data and institutional-reference
 * modalities (FR-069 / AC-X16); listing is an additive v1 route.
 */
@RestController
public class StructuredEvidenceController {
    private final StructuredEvidenceService snapshots;

    public StructuredEvidenceController(StructuredEvidenceService snapshots) {
        this.snapshots = snapshots;
    }

    @GetMapping("/evidence-versions/{versionId}/structured-sources")
    public ResponseEntity<List<StructuredSourceView>> list(@PathVariable UUID versionId) {
        return ResponseEntity.ok().body(snapshots.snapshots(versionId).stream()
                .map(StructuredSourceView::from).toList());
    }

    @PostMapping("/evidence-versions/{versionId}/structured-sources")
    public ResponseEntity<StructuredSourceView> register(
            @PathVariable UUID versionId,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody StructuredSourceCommand request) {
        var registered = snapshots.register(versionId, ifMatch, new StructuredEvidenceService.StructuredCommand(
                request.requiredSourceId(), request.kind(), request.value(), request.unit(),
                request.periodStart(), request.periodEnd(), request.reference()));
        return ResponseEntity.created(URI.create("/api/v1/evidence-versions/" + versionId + "/structured-sources"))
                .eTag(EntityTagSupport.strong(registered.versionRevision()))
                .body(StructuredSourceView.from(registered.value()));
    }

    public record StructuredSourceCommand(
            @NotNull UUID requiredSourceId,
            @NotNull StructuredEvidence.Kind kind,
            @NotNull Object value,
            @Size(max = 100) String unit,
            @NotNull LocalDate periodStart,
            @NotNull LocalDate periodEnd,
            @Size(max = 2000) String reference) {}

    public record StructuredSourceView(UUID id, String status, Instant createdAt, UUID requiredSourceId, String kind,
            Object value, String valueType, String unit, LocalDate periodStart, LocalDate periodEnd,
            String reference) {
        static StructuredSourceView from(StructuredEvidence snapshot) {
            return new StructuredSourceView(snapshot.id(), "DRAFT", snapshot.createdAt(), snapshot.requiredSourceId(),
                    snapshot.kind().name(), snapshot.value().value(), snapshot.value().type().name(),
                    snapshot.unit(), snapshot.periodStart(), snapshot.periodEnd(), snapshot.reference());
        }
    }
}
