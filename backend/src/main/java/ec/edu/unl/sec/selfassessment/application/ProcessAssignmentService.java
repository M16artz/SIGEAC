package ec.edu.unl.sec.selfassessment.application;

import ec.edu.unl.sec.identityaccess.application.AuthorizationPort;
import ec.edu.unl.sec.identityaccess.application.CurrentActorPort;
import ec.edu.unl.sec.identityaccess.application.FunctionalAssignmentPolicyPort;
import ec.edu.unl.sec.identityaccess.domain.AuthorizationScope;
import ec.edu.unl.sec.identityaccess.domain.OperationCode;
import ec.edu.unl.sec.selfassessment.domain.ProcessAssignment;
import ec.edu.unl.sec.selfassessment.domain.ProcessState;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessAssignmentService {
    private final SelfAssessmentRepository repository;
    private final CurrentActorPort actors;
    private final AuthorizationPort auth;
    private final FunctionalAssignmentPolicyPort policy;
    private final SelfAssessmentAuditService audit;

    public ProcessAssignmentService(SelfAssessmentRepository repository, CurrentActorPort actors,
            AuthorizationPort auth, FunctionalAssignmentPolicyPort policy, SelfAssessmentAuditService audit) {
        this.repository = repository; this.actors = actors; this.auth = auth; this.policy = policy; this.audit = audit;
    }

    @Transactional
    public ProcessAssignment assign(UUID processId, long expected, UUID indicator, UUID identity,
            ProcessAssignment.Function function, Instant from, Instant to, String reason, UUID correlation) {
        var actor = actor();
        var process = repository.find(processId).orElseThrow(ProcessAssignmentService::hidden);
        var scope = new AuthorizationScope(process.careerId(), process.id(), indicator);
        if (actor.technical() || !auth.isAllowed(actor.identityId(), OperationCode.PROCESS_COORDINATE, scope)) throw hidden();
        SelfAssessmentService.check(process, expected);
        if (process.state() == ProcessState.CLOSED) throw error(ProcessProblemException.Kind.CONFLICT,
                "PROCESS_CLOSED", "El proceso cerrado no admite cambios.");
        if (process.state() != ProcessState.ACTIVE) throw error(ProcessProblemException.Kind.CONFLICT,
                "PROCESS_NOT_ACTIVE", "Las asignaciones requieren un proceso activo.");
        if (process.indicators().stream().noneMatch(i -> i.id().equals(indicator))) throw error(
                ProcessProblemException.Kind.INVALID, "INDICATOR_OUTSIDE_FIXED_MODEL", "El indicador no pertenece al modelo fijado.");
        if (identity == null || function == null || from == null || to == null || !from.isBefore(to)) throw error(
                ProcessProblemException.Kind.INVALID, "INVALID_ASSIGNMENT", "La asignación requiere identidad, función y vigencia válida.");
        Instant start = process.periodStart().atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = process.periodEnd().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        if (!from.isBefore(end) || !to.isAfter(start)) throw error(ProcessProblemException.Kind.INVALID,
                "ASSIGNMENT_OUTSIDE_PERIOD", "La vigencia no se superpone al período.");
        if (reason == null || reason.isBlank() || reason.length() > 2000) throw error(
                ProcessProblemException.Kind.INVALID, "CRITICAL_CHANGE_REASON_REQUIRED", "El cambio crítico requiere motivo.");
        boolean duplicate = repository.assignments(processId).stream().anyMatch(a ->
                a.revokedAt() == null && a.identityId().equals(identity) && a.indicatorId().equals(indicator)
                        && a.function() == function && from.isBefore(a.validTo()) && to.isAfter(a.validFrom()));
        if (duplicate) throw error(ProcessProblemException.Kind.CONFLICT, "ASSIGNMENT_OVERLAP", "Ya existe una asignación idéntica con vigencia superpuesta.");
        var decision = policy.evaluate(identity, function.name(), scope, from, to);
        if (!decision.allowed()) throw error(ProcessProblemException.Kind.CONFLICT, decision.reasonCode(), "La asignación entra en conflicto con la política vigente.");
        var assignment = new ProcessAssignment(null, processId, indicator, identity, function, from, to,
                actor.identityId(), reason.strip(), null, 0);
        // Serialize assignment changes through the process revision so concurrent plans cannot overwrite each other.
        repository.save(process.advanceRevision(), expected);
        repository.saveAssignment(assignment);
        audit.record(actor.identityId(), correlation, "PROCESS_ASSIGNMENT_CREATED", process, "SUCCESS", null);
        return assignment;
    }

    @Transactional(readOnly = true)
    public List<ProcessAssignment> list(UUID processId) {
        var actor = actor();
        var process = repository.find(processId).orElseThrow(ProcessAssignmentService::hidden);
        if (actor.technical()) throw hidden();
        var scope = new AuthorizationScope(process.careerId(), process.id(), null);
        if (auth.isAllowed(actor.identityId(), OperationCode.PROCESS_COORDINATE, scope)
                || auth.isAllowed(actor.identityId(), OperationCode.PROCESS_MANAGE, scope)) return repository.assignments(processId);
        return repository.assignments(processId).stream()
                .filter(a -> a.identityId().equals(actor.identityId()) && a.revokedAt() == null
                        && !Instant.now().isBefore(a.validFrom()) && Instant.now().isBefore(a.validTo()))
                .filter(a -> auth.isAllowed(actor.identityId(), operation(a.function()),
                        new AuthorizationScope(process.careerId(), process.id(), a.indicatorId())))
                .toList();
    }

    private CurrentActorPort.Actor actor() {
        return actors.currentActor().orElseThrow(() -> error(ProcessProblemException.Kind.UNAUTHORIZED,
                "AUTHENTICATION_REQUIRED", "Se requiere autenticación."));
    }
    private static OperationCode operation(ProcessAssignment.Function function) {
        return switch (function) {
            case RESPONSIBLE -> OperationCode.EVIDENCE_COLLECT;
            case REVIEWER -> OperationCode.EVIDENCE_REVIEW;
            case EVALUATOR -> OperationCode.INDICATOR_EVALUATE;
        };
    }
    private static ProcessProblemException hidden() { return error(ProcessProblemException.Kind.NOT_FOUND, "PROCESS_NOT_FOUND", "El proceso no existe o no está visible."); }
    private static ProcessProblemException error(ProcessProblemException.Kind kind, String code, String message) {
        return SelfAssessmentService.problem(kind, code, message);
    }
}
