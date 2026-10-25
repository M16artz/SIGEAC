package ec.edu.unl.sec.governance.application;

import ec.edu.unl.sec.academicevaluation.application.AcademicConsolidationPort;
import ec.edu.unl.sec.audit.application.AuditEventCommand;
import ec.edu.unl.sec.audit.application.AuditPort;
import ec.edu.unl.sec.governance.domain.Consolidation;
import ec.edu.unl.sec.governance.domain.Finding;
import ec.edu.unl.sec.identityaccess.application.AuthorizationPort;
import ec.edu.unl.sec.identityaccess.application.CurrentActorPort;
import ec.edu.unl.sec.identityaccess.domain.AuthorizationScope;
import ec.edu.unl.sec.identityaccess.domain.OperationCode;
import ec.edu.unl.sec.selfassessment.application.DocumentaryProcessPort;
import ec.edu.unl.sec.shared.domain.DomainClock;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * P13 finding registry and academic consolidation. Findings stay immutable; the consolidation
 * chain is append-only and aggregation only happens under a validated rule.
 */
@Service
public class FindingService {
    private static final java.util.Set<String> CLOSED_PHASES = java.util.Set.of("PREPARATION", "SUSPENDED", "CLOSING", "CLOSED");
    private static final String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";

    private final CurrentActorPort actors;
    private final AuthorizationPort auth;
    private final DocumentaryProcessPort processes;
    private final AcademicConsolidationPort academic;
    private final FindingRepository findings;
    private final AuditPort audit;
    private final DomainClock clock = DomainClock.systemUtc();

    public FindingService(CurrentActorPort actors, AuthorizationPort auth, DocumentaryProcessPort processes,
            AcademicConsolidationPort academic, FindingRepository findings, AuditPort audit) {
        this.actors = Objects.requireNonNull(actors, "actors");
        this.auth = Objects.requireNonNull(auth, "auth");
        this.processes = Objects.requireNonNull(processes, "processes");
        this.academic = Objects.requireNonNull(academic, "academic");
        this.findings = Objects.requireNonNull(findings, "findings");
        this.audit = Objects.requireNonNull(audit, "audit");
    }

    public record RegisterCommand(String type, String title, String description, List<UUID> indicatorIds,
            List<UUID> evaluationIds, List<UUID> evidenceVersionIds) {}

    public record ConsolidateCommand(List<UUID> findingIds, UUID aggregationRuleId, String reason) {}

    public record FindingPage(List<Finding> items, long totalElements) {}

    @Transactional
    public Finding register(UUID processId, RegisterCommand command, UUID correlationId) {
        var actor = requireWriter(OperationCode.FINDING_WRITE);
        var context = writeScope(actor, OperationCode.FINDING_WRITE, processId);
        if (command.indicatorIds() == null || command.indicatorIds().isEmpty())
            throw problem(GovernanceProblemException.Kind.INVALID, "REPORT_FINDING_SUPPORT_FOREIGN",
                    "El hallazgo requiere al menos un indicador del proceso.");
        var unknown = command.indicatorIds().stream().filter(id -> !context.indicatorIds().contains(id)).count();
        if (unknown > 0)
            throw problem(GovernanceProblemException.Kind.INVALID, "REPORT_FINDING_SUPPORT_FOREIGN",
                    "Los soportes deben pertenecer a los indicadores declarados del proceso.");
        var finding = new Finding(UUID.randomUUID(), processId, new Finding.Data(parseType(command.type()),
                command.title(), command.description(), command.indicatorIds(), command.evaluationIds(),
                command.evidenceVersionIds()), actor, clock.now());
        findings.appendFinding(finding);
        audit.record(new AuditEventCommand(clock.now(), actor, null, correlationId, "FINDING_REGISTERED",
                "Finding", finding.id(), context.careerId(), processId, "SUCCESS", null,
                Map.<String, Object>of("type", finding.data().type().name())));
        return finding;
    }

    public FindingPage registry(UUID processId, long offset, long limit) {
        return findings(processId, offset, limit);
    }

    @Transactional(readOnly = true)
    public FindingPage findings(UUID processId, long offset, long limit) {
        var actor = requireWriter(OperationCode.FINDING_READ);
        var context = readScope(actor, OperationCode.FINDING_READ, processId);
        if (limit < 1 || limit > 100 || offset < 0)
            throw problem(GovernanceProblemException.Kind.INVALID, "REPORT_PAGE_LIMIT_EXCEEDED",
                    "La paginación de hallazgos admite hasta 100 elementos.");
        context.toString();
        return new FindingPage(findings.findings(processId, offset, limit), findings.totalFindings(processId));
    }

    @Transactional
    public Consolidation consolidate(UUID processId, long expectedRevision, ConsolidateCommand command, UUID correlationId) {
        var actor = requireWriter(OperationCode.ACADEMIC_CONSOLIDATE);
        var context = writeScope(actor, OperationCode.ACADEMIC_CONSOLIDATE, processId);
        if (command.aggregationRuleId() != null
                && !academic.allowsAggregation(context.modelVersionId(), command.aggregationRuleId()))
            throw problem(GovernanceProblemException.Kind.INVALID, "ACADEMIC_AGGREGATION_RULE_UNVALIDATED",
                    "La agregación solo procede con una regla metodológica validada.");
        var consolidationIds = command.findingIds() == null ? List.<UUID>of() : command.findingIds();
        var distinct = consolidationIds.stream().distinct().toList();
        if (distinct.size() != consolidationIds.size() || distinct.isEmpty())
            throw problem(GovernanceProblemException.Kind.INVALID, "REPORT_FINDING_SUPPORT_FOREIGN",
                    "La consolidación requiere hallazgos registrados y distintos.");
        var present = new java.util.HashSet<>(findings.byIds(processId, distinct)
                .stream().map(Finding::id).toList());
        if (!present.containsAll(distinct))
            throw problem(GovernanceProblemException.Kind.NOT_FOUND, "REPORT_FINDING_NOT_FOUND",
                    "Uno de los hallazgos indicados no existe en este proceso.");
        var alreadyConsolidated = findings.findingsPresentInConsolidations(processId);
        if (distinct.stream().anyMatch(alreadyConsolidated::contains))
            throw problem(GovernanceProblemException.Kind.CONFLICT, "REPORT_FINDING_ALREADY_CONSOLIDATED",
                    "Un hallazgo consolidado no puede volver a consolidarse.");
        var last = findings.latestConsolidation(processId);
        var requiredRevision = last.map(Consolidation::revision).orElse(0L);
        if (requiredRevision != expectedRevision)
            throw new ConcurrencyFailureException("stale consolidation: revision mismatch");
        var consolidation = new Consolidation(UUID.randomUUID(), processId, new Consolidation.Data(distinct,
                command.aggregationRuleId(), command.reason()), actor, clock.now(),
                last.map(Consolidation::id).orElse(null), requiredRevision + 1);
        findings.appendConsolidation(consolidation);
        audit.record(new AuditEventCommand(clock.now(), actor, null, correlationId, "REPORT_CONSOLIDATION_RECORDED",
                "Consolidation", consolidation.id(), context.careerId(), processId, "SUCCESS", null,
                Map.<String, Object>of("revision", consolidation.revision(),
                        "findings", consolidation.data().findingIds().size())));
        return consolidation;
    }

    private Finding.Type parseType(String value) {
        try {
            return Finding.Type.valueOf(value);
        } catch (RuntimeException e) {
            throw problem(GovernanceProblemException.Kind.INVALID, "REPORT_FINDING_TYPE_UNSUPPORTED",
                    "La clase de hallazgo indicada no es admitida.");
        }
    }

    private UUID requireWriter(OperationCode operation) {
        return actors.currentActor()
                .filter(a -> !a.technical())
                .orElseThrow(() -> problem(GovernanceProblemException.Kind.FORBIDDEN, "REPORT_ACTOR_REQUIRED",
                        "Solo sesiones de usuario activas pueden operar el registro de hallazgos."))
                .identityId();
    }

    private DocumentaryProcessPort.Context writeScope(UUID actor, OperationCode operation, UUID processId) {
        if (!auth.hasOperation(actor, operation)) throw hiddenAccess();
        var context = processes.lock(processId)
                .orElseThrow(() -> problem(GovernanceProblemException.Kind.NOT_FOUND, RESOURCE_NOT_FOUND,
                        "El proceso indicado no existe."));
        if (!auth.isAllowed(actor, operation, new AuthorizationScope(context.careerId(), context.id(), null)))
            throw hiddenAccess();
        if (CLOSED_PHASES.contains(context.state()))
            throw problem(GovernanceProblemException.Kind.CONFLICT, "REPORT_PHASE_CLOSED",
                    "La fase del proceso no admite modificaciones de hallazgos.");
        return context;
    }

    private DocumentaryProcessPort.Context readScope(UUID actor, OperationCode operation, UUID processId) {
        if (!auth.hasOperation(actor, operation)) throw hiddenAccess();
        var context = processes.context(processId)
                .orElseThrow(() -> problem(GovernanceProblemException.Kind.NOT_FOUND, RESOURCE_NOT_FOUND,
                        "El proceso indicado no existe."));
        if (!auth.isAllowed(actor, operation, new AuthorizationScope(context.careerId(), context.id(), null)))
            throw hiddenAccess();
        return context;
    }

    private GovernanceProblemException hiddenAccess() {
        return problem(GovernanceProblemException.Kind.NOT_FOUND, RESOURCE_NOT_FOUND,
                "El recurso solicitado no existe.");
    }

    private GovernanceProblemException problem(GovernanceProblemException.Kind kind, String code, String message) {
        return new GovernanceProblemException(kind, code, message);
    }
}
