package ec.edu.unl.sec.archive.api;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import ec.edu.unl.sec.archive.application.ArchiveProblemException;
import ec.edu.unl.sec.archive.application.ArchiveService;
import ec.edu.unl.sec.archive.application.IntegralClosureService;
import ec.edu.unl.sec.archive.domain.ArchiveRecord;
import ec.edu.unl.sec.archive.domain.IntegralClosure;
import ec.edu.unl.sec.audit.application.AuditEventCommand;
import ec.edu.unl.sec.audit.application.FailureAuditPort;
import ec.edu.unl.sec.identityaccess.application.CurrentActorPort;
import ec.edu.unl.sec.shared.api.EntityTagSupport;
import ec.edu.unl.sec.shared.infrastructure.CorrelationFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
public class ArchiveController {
    private final IntegralClosureService closures;
    private final ArchiveService archive;
    private final FailureAuditPort failures;
    private final CurrentActorPort actors;

    public ArchiveController(IntegralClosureService closures, ArchiveService archive, FailureAuditPort failures,
            CurrentActorPort actors) {
        this.closures = closures;
        this.archive = archive;
        this.failures = failures;
        this.actors = actors;
    }

    @GetMapping("/processes/{processId}/closure-preview")
    public ResponseEntity<IntegralClosureService.Preview> preview(@PathVariable UUID processId,
            @RequestParam String closureType) {
        var preview = closures.preview(processId, closureType);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .eTag(EntityTagSupport.strong(preview.revision())).body(preview);
    }

    @PostMapping("/processes/{processId}/close")
    public ResponseEntity<IntegralClosureService.ClosureView> close(@PathVariable UUID processId,
            @RequestHeader(name = "If-Match", required = false) String etag,
            @Valid @RequestBody ClosureRequest command, HttpServletRequest request) {
        UUID correlation = correlation(request);
        long expected = EntityTagSupport.require(etag);
        IntegralClosure closure;
        try {
            closure = closures.close(processId, expected, new IntegralClosureService.CloseCommand(
                    command.closurePolicyVersion(), provenance(command.provenance()),
                    command.exceptionDecisionIds()), correlation);
        } catch (ArchiveProblemException rejected) {
            rejection("INTEGRAL_CLOSURE_REJECTED", processId, correlation, rejected.code());
            throw rejected;
        } catch (ConcurrencyFailureException stale) {
            rejection("INTEGRAL_CLOSURE_REJECTED", processId, correlation, "STALE_WORKFLOW");
            throw stale;
        }
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
                .eTag(EntityTagSupport.strong(expected + 2)).body(view(closure));
    }

    @PostMapping("/processes/{processId}/archive")
    public ResponseEntity<ArchiveView> archive(@PathVariable UUID processId,
            @RequestHeader(name = "If-Match", required = false) String etag,
            @Valid @RequestBody ArchiveRequest command, HttpServletRequest request) {
        UUID correlation = correlation(request);
        EntityTagSupport.require(etag);
        ArchiveRecord record;
        try {
            record = archive.archive(processId, new ArchiveService.ArchiveCommand(command.closureSnapshotId(),
                    command.retentionPolicyVersion(), archiveProvenance(command.provenance())), correlation);
        } catch (ArchiveProblemException rejected) {
            rejection("PROCESS_ARCHIVE_REJECTED", processId, correlation, rejected.code());
            throw rejected;
        } catch (ConcurrencyFailureException stale) {
            rejection("PROCESS_ARCHIVE_REJECTED", processId, correlation, "STALE_WORKFLOW");
            throw stale;
        }
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
                .eTag(EntityTagSupport.strong(1))
                .body(new ArchiveView(record.id(), record.processId(), record.closureSnapshotId(),
                        record.archivedAt(), record.archivedBy(), record.retentionPolicyVersion(),
                        record.integrityStatus()));
    }

    @GetMapping("/processes/{processId}/closures")
    public ResponseEntity<IntegralClosureService.ClosurePage> history(@PathVariable UUID processId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        var result = closures.history(processId, page, size);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .eTag(EntityTagSupport.strong(result.totalElements())).body(result);
    }

    private IntegralClosureService.ClosureView view(IntegralClosure closure) {
        return new IntegralClosureService.ClosureView(closure.id(), closure.processId(), closure.type(),
                closure.policyVersion(), closure.sha256(), closure.pendingIds(), closure.exceptionDecisionIds(),
                closure.closedBy(), closure.closedAt());
    }

    private IntegralClosureService.Provenance provenance(Provenance provenance) {
        return new IntegralClosureService.Provenance(provenance.authorityAssignmentId(), provenance.legalBasis(),
                provenance.documentReference(), provenance.reason());
    }

    private ArchiveService.Provenance archiveProvenance(Provenance provenance) {
        return new ArchiveService.Provenance(provenance.authorityAssignmentId(), provenance.legalBasis(),
                provenance.documentReference(), provenance.reason());
    }

    private void rejection(String action, UUID objectId, UUID correlation, String code) {
        actors.currentActor().ifPresent(actor -> failures.recordAfterRollback(new AuditEventCommand(
                Instant.now(), actor.technical() ? null : actor.identityId(),
                actor.technical() ? actor.identityId() : null, correlation, action, "ArchiveRecord", objectId,
                null, null, "REJECTED", code, Map.of())));
    }

    private UUID correlation(HttpServletRequest request) {
        var raw = request.getAttribute(CorrelationFilter.CORRELATION_ATTRIBUTE);
        try {
            return UUID.fromString(String.valueOf(raw));
        } catch (IllegalArgumentException ex) {
            return UUID.randomUUID();
        }
    }

    public record Provenance(@NotNull UUID authorityAssignmentId, @NotBlank @Size(max = 4000) String legalBasis,
            @NotBlank @Size(max = 2000) String documentReference,
            @NotBlank @Size(min = 10, max = 4000) String reason) {}

    public record ClosureRequest(@NotBlank @Size(max = 100) String closurePolicyVersion,
            @NotNull @Valid Provenance provenance,
            @NotNull @Size(max = 500) List<UUID> exceptionDecisionIds) {
        @JsonAnySetter public void unknown(String key, Object value) {
            throw new IllegalArgumentException("Campo no admitido");
        }
    }

    public record ArchiveRequest(@NotNull UUID closureSnapshotId,
            @NotBlank @Size(max = 100) String retentionPolicyVersion,
            @NotNull @Valid Provenance provenance) {
        @JsonAnySetter public void unknown(String key, Object value) {
            throw new IllegalArgumentException("Campo no admitido");
        }
    }

    public record ArchiveView(UUID id, UUID processId, UUID closureSnapshotId, Instant archivedAt, UUID archivedBy,
            String retentionPolicyVersion, String integrityStatus) {}
}
