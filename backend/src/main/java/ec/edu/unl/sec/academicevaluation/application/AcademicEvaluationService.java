package ec.edu.unl.sec.academicevaluation.application;

import ec.edu.unl.sec.academicevaluation.domain.ManualAcademicEvaluation;
import ec.edu.unl.sec.audit.application.AuditEventCommand;
import ec.edu.unl.sec.audit.application.AuditPort;
import ec.edu.unl.sec.documentreview.application.DocumentReviewProgressPort;
import ec.edu.unl.sec.evaluationmodel.application.ModelCatalogPort;
import ec.edu.unl.sec.evaluationmodel.domain.IndicatorDefinition;
import ec.edu.unl.sec.evaluationmodel.domain.RatingScale;
import ec.edu.unl.sec.evaluationmodel.domain.ValuationRule;
import ec.edu.unl.sec.evidence.application.EvidenceProgressPort;
import ec.edu.unl.sec.evidence.application.RequiredSourceCatalogPort;
import ec.edu.unl.sec.identityaccess.application.AuthorizationPort;
import ec.edu.unl.sec.identityaccess.application.CurrentActorPort;
import ec.edu.unl.sec.identityaccess.domain.AuthorizationScope;
import ec.edu.unl.sec.identityaccess.domain.OperationCode;
import ec.edu.unl.sec.selfassessment.application.DocumentaryProcessPort;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AcademicEvaluationService {
    private final CurrentActorPort actors;
    private final AuthorizationPort authorization;
    private final DocumentaryProcessPort processes;
    private final ModelCatalogPort models;
    private final RequiredSourceCatalogPort sources;
    private final EvidenceProgressPort evidence;
    private final DocumentReviewProgressPort reviews;
    private final ManualEvaluationRepository repository;
    private final AuditPort audit;

    public AcademicEvaluationService(CurrentActorPort actors, AuthorizationPort authorization,
            DocumentaryProcessPort processes, ModelCatalogPort models, RequiredSourceCatalogPort sources,
            EvidenceProgressPort evidence, DocumentReviewProgressPort reviews,
            ManualEvaluationRepository repository, AuditPort audit) {
        this.actors=actors; this.authorization=authorization; this.processes=processes; this.models=models;
        this.sources=sources; this.evidence=evidence; this.reviews=reviews; this.repository=repository; this.audit=audit;
    }

    @Transactional(readOnly=true)
    public View view(UUID processId, UUID indicatorId, int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw invalid("INVALID_PAGE", "Página o tamaño inválidos.");
        var actor = actor();
        var process = authorized(actor, processId, indicatorId);
        var rules = rules(process, indicatorId);
        var entries = repository.history(processId, indicatorId, (long) page * size, size + 1);
        var own = repository.latest(processId, indicatorId, actor);
        return new View(processId, indicatorId, process.modelVersionId(), process.state(),
                rules.rule().methodDefinition(), rules.scale().options(), accepted(process, indicatorId),
                own.map(ManualAcademicEvaluation::revision).orElse(0L), own.map(ManualAcademicEvaluation::id).orElse(null),
                entries.stream().limit(size).toList(), page, size, entries.size() > size);
    }

    @Transactional
    public ManualAcademicEvaluation evaluate(UUID processId, UUID indicatorId, long expectedRevision,
            Command command, UUID correlation) {
        var actor = actor();
        authorized(actor, processId, indicatorId);
        var process = processes.lock(processId).orElseThrow(AcademicEvaluationService::notFound);
        if (!Set.of("ACTIVE", "DOCUMENTARY_CLOSED", "REOPENED").contains(process.state()))
            throw new AcademicProblemException(AcademicProblemException.Kind.CONFLICT,
                    "ACADEMIC_PHASE_CLOSED", "El estado del proceso no permite valoraciones académicas.");
        var latest = repository.latest(processId, indicatorId, actor);
        if (expectedRevision != latest.map(ManualAcademicEvaluation::revision).orElse(0L)
                || !Objects.equals(command.predecessorId(), latest.map(ManualAcademicEvaluation::id).orElse(null)))
            throw new AcademicProblemException(AcademicProblemException.Kind.PRECONDITION_FAILED,
                    "STALE_EVALUATION", "La valoración cambió; recargue antes de continuar.");
        var rules = rules(process, indicatorId);
        var option = rules.scale().options().stream().filter(o -> o.id().equals(command.ratingOptionId()))
                .findFirst().orElseThrow(() -> invalid("INVALID_RATING", "La opción no pertenece a la escala del indicador."));
        var ids = command.evidenceVersionIds();
        if (ids == null || ids.isEmpty() || ids.size() > 100 || ids.stream().anyMatch(Objects::isNull) || ids.stream().distinct().count() != ids.size())
            throw invalid("INVALID_SUPPORTS", "Seleccione versiones distintas de evidencias aceptadas.");
        var supports = accepted(process, indicatorId).stream().filter(s -> ids.contains(s.evidenceVersionId())).toList();
        if (!supports.stream().map(ManualAcademicEvaluation.Support::evidenceVersionId).collect(Collectors.toSet()).equals(new HashSet<>(ids)))
            throw invalid("INVALID_SUPPORTS", "Una versión no es sustento aceptado y vigente de este indicador.");
        ManualAcademicEvaluation evaluation;
        try {
            evaluation = new ManualAcademicEvaluation(UUID.randomUUID(), processId, indicatorId, process.modelVersionId(),
                    rules.rule().id(), rules.scale().id(), option.id(), option.label(), rules.rule().methodDefinition(),
                    command.limitations(), command.justification(), supports, actor, Instant.now(), expectedRevision + 1, command.predecessorId());
        } catch (IllegalArgumentException ex) {
            throw invalid("INVALID_EVALUATION", "Indique justificación y limitaciones de 10 a 4000 caracteres.");
        }
        repository.append(evaluation);
        audit.record(new AuditEventCommand(evaluation.evaluatedAt(), actor, null, correlation,
                "ACADEMIC_MANUAL_EVALUATION_RECORDED", "ACADEMIC_EVALUATION", evaluation.id(), process.careerId(), processId,
                "SUCCESS", null, Map.of("indicatorId", indicatorId.toString(), "revision", evaluation.revision())));
        return evaluation;
    }

    private UUID actor() {
        var actor=actors.currentActor().orElseThrow(() -> new AcademicProblemException(
                AcademicProblemException.Kind.UNAUTHORIZED,"AUTHENTICATION_REQUIRED","Se requiere una sesión vigente."));
        if (actor.technical() || !authorization.hasOperation(actor.identityId(), OperationCode.INDICATOR_EVALUATE))
            throw new AcademicProblemException(AcademicProblemException.Kind.FORBIDDEN,"EVALUATION_DENIED","No dispone de la función evaluadora.");
        return actor.identityId();
    }
    private DocumentaryProcessPort.Context authorized(UUID actor, UUID processId, UUID indicatorId) {
        var process=processes.context(processId).orElseThrow(AcademicEvaluationService::notFound);
        if (!process.indicatorIds().contains(indicatorId) || !authorization.isAllowed(actor, OperationCode.INDICATOR_EVALUATE,
                new AuthorizationScope(process.careerId(), processId, indicatorId))) throw notFound();
        return process;
    }
    private Rules rules(DocumentaryProcessPort.Context process, UUID indicatorId) {
        var model=models.publishedVersion(process.modelVersionId()).orElseThrow(AcademicEvaluationService::notFound);
        var config=model.configuration();
        if (config == null) throw invalid("MANUAL_RULE_UNAVAILABLE", "El indicador no tiene una regla manual configurada.");
        var definition=config.indicators().stream().filter(d -> d.nodeId().equals(indicatorId)).findFirst().orElseThrow(AcademicEvaluationService::notFound);
        var rule=config.rules().stream().filter(r -> r.id().equals(definition.valuationRuleId()) && r.indicatorNodeId().equals(indicatorId))
                .findFirst().orElseThrow(() -> invalid("MANUAL_RULE_UNAVAILABLE", "El indicador no tiene una regla manual configurada."));
        var scale=config.scales().stream().filter(s -> s.id().equals(definition.ratingScaleId())).findFirst().orElse(null);
        if (definition.nature() != IndicatorDefinition.Nature.QUALITATIVE || rule.ruleType() != ValuationRule.Type.MANUAL_QUALITATIVE
                || rule.status() != ValuationRule.Status.VALIDATED || scale == null || !Objects.equals(rule.scaleId(), scale.id())
                || scale.options().isEmpty() || !rule.validationIssues(scale).isEmpty())
            throw invalid("MANUAL_RULE_UNAVAILABLE", "Se requiere una regla cualitativa validada y su escala publicada.");
        return new Rules(rule, scale);
    }
    Scope scope(UUID processId, UUID indicatorId, boolean write) {
        var actor=actor();
        var process=authorized(actor,processId,indicatorId);
        if (write) {
            process=processes.lock(processId).orElseThrow(AcademicEvaluationService::notFound);
            if (!Set.of("ACTIVE","DOCUMENTARY_CLOSED","REOPENED").contains(process.state()))
                throw new AcademicProblemException(AcademicProblemException.Kind.CONFLICT,"ACADEMIC_PHASE_CLOSED",
                        "El estado del proceso no permite cambios académicos.");
        }
        return new Scope(actor,process);
    }
    record Scope(UUID actorId, DocumentaryProcessPort.Context process) {}
    List<ManualAcademicEvaluation.Support> accepted(DocumentaryProcessPort.Context process, UUID indicatorId) {
        var applicability=evidence.applicability(process.id());
        if (applicability.excludedIndicatorIds().contains(indicatorId)) return List.of();
        var sourceIds=sources.model(process.modelVersionId()).orElseThrow(AcademicEvaluationService::notFound).sources().stream()
                .filter(s -> s.indicatorId().equals(indicatorId) && !applicability.excludedSourceIds().contains(s.id()))
                .map(RequiredSourceCatalogPort.Entry::id).collect(Collectors.toSet());
        if (sourceIds.isEmpty()) return List.of();
        var current=evidence.currentCoverages(process.id(), sourceIds).stream().filter(c -> sourceIds.contains(c.requiredSourceId())).toList();
        var versionIds=current.stream().map(EvidenceProgressPort.CurrentCoverage::versionId).collect(Collectors.toSet());
        if (versionIds.isEmpty()) return List.of();
        var outcomes=reviews.latestOutcomes(versionIds);
        return current.stream().flatMap(c -> outcomes.stream()
                .filter(o -> o.versionId().equals(c.versionId()) && o.requiredSourceId().equals(c.requiredSourceId()) && "ACCEPTED".equals(o.outcome()))
                .map(o -> new ManualAcademicEvaluation.Support(c.versionId(), c.requiredSourceId(), o.decisionId(), c.title())))
                .distinct().sorted(Comparator.comparing(ManualAcademicEvaluation.Support::evidenceVersionId)
                        .thenComparing(ManualAcademicEvaluation.Support::requiredSourceId)).toList();
    }
    private static AcademicProblemException invalid(String code, String message) { return new AcademicProblemException(AcademicProblemException.Kind.INVALID, code, message); }
    private static AcademicProblemException notFound() { return new AcademicProblemException(AcademicProblemException.Kind.NOT_FOUND,"RESOURCE_NOT_FOUND","No se encontró el indicador en el ámbito autorizado."); }
    private record Rules(ValuationRule rule, RatingScale scale) {}
    public record Command(UUID ratingOptionId, List<UUID> evidenceVersionIds, String justification, String limitations, UUID predecessorId) {}
    public record View(UUID processId, UUID indicatorId, UUID modelVersionId, String processState, String method,
            List<RatingScale.Option> ratingOptions, List<ManualAcademicEvaluation.Support> acceptedSupports, long ownRevision,
            UUID ownLatestId, List<ManualAcademicEvaluation> items, int page, int size, boolean hasNext) {}
}
