package ec.edu.unl.sec.reporting.api;

import ec.edu.unl.sec.reporting.application.DocumentaryProgressQueryService;
import ec.edu.unl.sec.reporting.application.ReportingProblemException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DocumentaryProgressController {
    private final DocumentaryProgressQueryService query;

    public DocumentaryProgressController(DocumentaryProgressQueryService query) { this.query = query; }

    @GetMapping("/processes/{processId}/documentary-progress")
    public ResponseEntity<ProgressView> progress(@PathVariable UUID processId,
            @RequestParam(required = false) UUID indicatorId,
            @RequestParam(required = false) UUID requiredSourceId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam Map<String, String> parameters) {
        if (!Set.of("indicatorId", "requiredSourceId", "page", "size").containsAll(parameters.keySet())) {
            throw new ReportingProblemException(ReportingProblemException.Kind.INVALID,
                    "UNSUPPORTED_FILTER", "La consulta admite indicador, fuente y paginación; no cortes históricos.");
        }
        var view = query.query(processId, indicatorId, requiredSourceId, page, size);
        var progress = view.progress();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new ProgressView(
                view.processId(), view.modelVersionId(), view.calculatedAt(), progress.formulaVersion(),
                progress.status().name(), progress.requiredSourceTotal(), progress.registeredSourceTotal(),
                progress.acceptedSourceTotal(), progress.collectionPercent(), progress.documentReviewPercent(),
                view.scopeDescription(), view.indicatorIds(), view.sources(), view.page(), view.size(), view.totalSources()));
    }

    public record ProgressView(UUID processId, UUID modelVersionId, Instant calculatedAt, String formulaVersion,
            String state, int requiredSourceTotal, int registeredSourceTotal, int acceptedSourceTotal,
            BigDecimal collectionPercent, BigDecimal documentReviewPercent, String scopeDescription,
            List<UUID> indicatorIds, List<DocumentaryProgressQueryService.SourceView> sources,
            int page, int size, int totalSources) {}
}
