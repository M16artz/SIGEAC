package ec.edu.unl.sec.reporting.api;

import ec.edu.unl.sec.reporting.application.DocumentarySnapshotService;
import ec.edu.unl.sec.reporting.application.ReportingProblemException;
import ec.edu.unl.sec.reporting.domain.DocumentarySnapshot;
import ec.edu.unl.sec.shared.infrastructure.CorrelationFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class DocumentarySnapshotController {
    private final DocumentarySnapshotService snapshots;
    public DocumentarySnapshotController(DocumentarySnapshotService snapshots) { this.snapshots=snapshots; }

    @PostMapping("/processes/{processId}/documentary-snapshots")
    public ResponseEntity<SnapshotView> create(@PathVariable UUID processId,
            @RequestHeader(name="If-Match",required=false) String ifMatch,
            @Valid @RequestBody SnapshotCommand command,HttpServletRequest request) {
        var snapshot=snapshots.create(processId,revision(ifMatch),command.reason(),correlation(request));
        return ResponseEntity.created(URI.create("/api/v1/documentary-snapshots/"+snapshot.content().id()))
                .cacheControl(CacheControl.noStore()).eTag("\""+snapshot.sha256()+"\"")
                .body(new SnapshotView(snapshot.header(),snapshot.content()));
    }

    @GetMapping("/processes/{processId}/documentary-snapshots")
    public ResponseEntity<DocumentarySnapshotService.Page> history(@PathVariable UUID processId,
            @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,
            @RequestParam Map<String,String> parameters) {
        if (!Set.of("page","size").containsAll(parameters.keySet()))
            throw new ReportingProblemException(ReportingProblemException.Kind.INVALID,"UNSUPPORTED_FILTER","El historial admite únicamente página y tamaño.");
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(snapshots.history(processId,page,size));
    }

    @GetMapping("/documentary-snapshots/{snapshotId}")
    public ResponseEntity<SnapshotView> get(@PathVariable UUID snapshotId) {
        var snapshot=snapshots.get(snapshotId);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).eTag("\""+snapshot.sha256()+"\"")
                .body(new SnapshotView(snapshot.header(),snapshot.content()));
    }

    static long revision(String value) {
        if (value==null || value.isBlank()) throw new ReportingProblemException(
                ReportingProblemException.Kind.PRECONDITION_REQUIRED,"PRECONDITION_REQUIRED","Se requiere If-Match del proceso.");
        if (value.matches("\"[0-9]+\"")) {
            try { return Long.parseLong(value.substring(1,value.length()-1)); }
            catch (NumberFormatException invalid) { /* Out-of-range revisions are invalid too. */ }
        }
        throw new ReportingProblemException(ReportingProblemException.Kind.INVALID,"INVALID_ETAG","La revisión del proceso no es válida.");
    }

    static UUID correlation(HttpServletRequest request) {
        Object raw=request.getAttribute(CorrelationFilter.CORRELATION_ATTRIBUTE);
        try { return raw==null ? UUID.randomUUID() : UUID.fromString(raw.toString()); }
        catch (IllegalArgumentException safeText) {
            return UUID.nameUUIDFromBytes(raw.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    public record SnapshotCommand(@NotBlank @Size(min=10,max=4000) String reason) {
        @com.fasterxml.jackson.annotation.JsonAnySetter
        public void unsupported(String key,Object value) {
            throw new IllegalArgumentException("El corte actual admite únicamente el motivo; no cortes retroactivos.");
        }
    }
    public record SnapshotView(DocumentarySnapshot.Header header,DocumentarySnapshot.Content data) {}
}
