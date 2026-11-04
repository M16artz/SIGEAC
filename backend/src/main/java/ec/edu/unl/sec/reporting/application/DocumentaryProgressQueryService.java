package ec.edu.unl.sec.reporting.application;

import ec.edu.unl.sec.documentreview.application.DocumentReviewProgressPort;
import ec.edu.unl.sec.evidence.application.EvidenceContextPort;
import ec.edu.unl.sec.evidence.application.EvidenceProgressPort;
import ec.edu.unl.sec.evidence.application.RequiredSourceCatalogPort;
import ec.edu.unl.sec.identityaccess.application.AuthorizationPort;
import ec.edu.unl.sec.identityaccess.application.CurrentActorPort;
import ec.edu.unl.sec.identityaccess.domain.AuthorizationScope;
import ec.edu.unl.sec.identityaccess.domain.OperationCode;
import ec.edu.unl.sec.reporting.domain.DocumentaryProgressCalculator;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentaryProgressQueryService {
    private final CurrentActorPort actors;
    private final AuthorizationPort authorization;
    private final EvidenceContextPort context;
    private final RequiredSourceCatalogPort catalog;
    private final EvidenceProgressPort evidence;
    private final DocumentReviewProgressPort reviews;
    private final DocumentaryProgressCalculator calculator = new DocumentaryProgressCalculator();

    public DocumentaryProgressQueryService(CurrentActorPort actors, AuthorizationPort authorization,
            EvidenceContextPort context, RequiredSourceCatalogPort catalog, EvidenceProgressPort evidence,
            DocumentReviewProgressPort reviews) {
        this.actors = actors;
        this.authorization = authorization;
        this.context = context;
        this.catalog = catalog;
        this.evidence = evidence;
        this.reviews = reviews;
    }

    @Transactional(readOnly = true)
    public View query(UUID processId, UUID indicatorId, UUID requiredSourceId, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ReportingProblemException(ReportingProblemException.Kind.INVALID,
                    "INVALID_PAGE", "La página debe ser positiva o cero y el tamaño entre 1 y 100.");
        }
        return calculate(processId, indicatorId, requiredSourceId, page, size);
    }

    @Transactional(readOnly = true)
    public View snapshotView(UUID processId) {
        return calculate(processId, null, null, 0, Integer.MAX_VALUE);
    }

    private View calculate(UUID processId, UUID indicatorId, UUID requiredSourceId, int page, int size) {
        var actor = actors.currentActor().orElseThrow(() -> new ReportingProblemException(
                ReportingProblemException.Kind.UNAUTHORIZED, "UNAUTHORIZED", "Se requiere una sesión vigente."));
        if (actor.technical() || !authorization.hasOperation(actor.identityId(), OperationCode.PROGRESS_READ)) {
            throw new ReportingProblemException(ReportingProblemException.Kind.FORBIDDEN,
                    "FORBIDDEN", "No dispone de la capacidad para consultar seguimiento.");
        }
        var process = context.process(processId).orElseThrow(DocumentaryProgressQueryService::notFound);
        var indicators = process.indicators().stream()
                .filter(indicator -> indicatorId == null || indicator.id().equals(indicatorId))
                .filter(indicator -> authorization.isAllowed(actor.identityId(), OperationCode.PROGRESS_READ,
                        new AuthorizationScope(process.careerId(), processId, indicator.id())))
                .map(EvidenceContextPort.Indicator::id).toList();
        if (indicators.isEmpty() && (indicatorId != null || !authorization.isAllowed(actor.identityId(),
                OperationCode.PROGRESS_READ, new AuthorizationScope(process.careerId(), processId, null)))) {
            throw notFound();
        }
        var model = catalog.model(process.fixedModelVersionId()).orElseThrow(DocumentaryProgressQueryService::notFound);
        var selected = model.sources().stream()
                .filter(source -> indicators.contains(source.indicatorId()))
                .filter(source -> requiredSourceId == null || source.id().equals(requiredSourceId))
                .sorted(Comparator.comparing(RequiredSourceCatalogPort.Entry::indicatorCode)
                        .thenComparing(RequiredSourceCatalogPort.Entry::code)
                        .thenComparing(RequiredSourceCatalogPort.Entry::id)).toList();
        if (requiredSourceId != null && selected.isEmpty()) throw notFound();
        var sourceIds = selected.stream().map(RequiredSourceCatalogPort.Entry::id).collect(Collectors.toSet());
        var applicability = evidence.applicability(processId);
        var current = sourceIds.isEmpty() ? List.<EvidenceProgressPort.CurrentCoverage>of()
                : evidence.currentCoverages(processId, sourceIds);
        // Restrict projections again so a multi-source version cannot expose another indicator's coverage.
        var visibleCoverage = current.stream().filter(coverage -> sourceIds.contains(coverage.requiredSourceId())).toList();
        var versions = visibleCoverage.stream().map(EvidenceProgressPort.CurrentCoverage::versionId).collect(Collectors.toSet());
        var outcomes = (versions.isEmpty() ? List.<DocumentReviewProgressPort.Outcome>of() : reviews.latestOutcomes(versions))
                .stream().collect(Collectors.toMap(outcome -> new ReviewKey(outcome.versionId(), outcome.requiredSourceId()),
                        Function.identity()));
        var grouped = visibleCoverage.stream().collect(Collectors.groupingBy(EvidenceProgressPort.CurrentCoverage::requiredSourceId));
        var sources = new ArrayList<SourceView>();
        var inputs = new ArrayList<DocumentaryProgressCalculator.Source>();
        var coverages = new ArrayList<DocumentaryProgressCalculator.Coverage>();
        for (var source : selected) {
            boolean applicable = !applicability.excludedIndicatorIds().contains(source.indicatorId())
                    && !applicability.excludedSourceIds().contains(source.id());
            inputs.add(new DocumentaryProgressCalculator.Source(source.id(), source.required(), applicable, source.cardinalityMin()));
            var detail = new ArrayList<CoverageView>();
            for (var coverage : grouped.getOrDefault(source.id(), List.of())) {
                var outcome = outcomes.get(new ReviewKey(coverage.versionId(), source.id()));
                var state = outcome == null ? "PENDING_REVIEW" : outcome.outcome();
                coverages.add(new DocumentaryProgressCalculator.Coverage(source.id(), coverage.evidenceId(), "ACCEPTED".equals(state)));
                detail.add(new CoverageView(coverage.evidenceId(), coverage.versionId(),
                        outcome == null ? null : outcome.decisionId(), state));
            }
            sources.add(new SourceView(source.id(), source.indicatorId(), source.code(), source.name(),
                    source.required(), applicable, source.cardinalityMin(), List.copyOf(detail)));
        }
        var progress = calculator.calculate(inputs, coverages);
        return new View(processId, process.fixedModelVersionId(), Instant.now(),
                "Indicadores autorizados del proceso; filtros aplicados antes del cálculo.", indicators, progress,
                sources.stream().skip((long) page * size).limit(size).toList(), page, size, sources.size());
    }

    private static ReportingProblemException notFound() {
        return new ReportingProblemException(ReportingProblemException.Kind.NOT_FOUND,
                "RESOURCE_NOT_FOUND", "No se encontró el recurso en el ámbito autorizado.");
    }

    private record ReviewKey(UUID versionId, UUID sourceId) {}
    public record CoverageView(UUID evidenceId, UUID versionId, UUID decisionId, String outcome) {}
    public record SourceView(UUID id, UUID indicatorId, String code, String name, boolean required,
            boolean applicable, int cardinalityMin, List<CoverageView> coverages) {}
    public record View(UUID processId, UUID modelVersionId, Instant calculatedAt, String scopeDescription,
            List<UUID> indicatorIds, DocumentaryProgressCalculator.Progress progress,
            List<SourceView> sources, int page, int size, int totalSources) {}
}
