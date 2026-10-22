package ec.edu.unl.sec.selfassessment.application;

import ec.edu.unl.sec.evaluationmodel.application.ModelCatalogPort;
import ec.edu.unl.sec.identityaccess.application.AuthorizationPort;
import ec.edu.unl.sec.identityaccess.application.CurrentActorPort;
import ec.edu.unl.sec.identityaccess.application.FunctionalAssignmentPolicyPort;
import ec.edu.unl.sec.identityaccess.application.InstitutionalAuthorityPort;
import ec.edu.unl.sec.identityaccess.domain.AuthorizationScope;
import ec.edu.unl.sec.identityaccess.domain.OperationCode;
import ec.edu.unl.sec.selfassessment.domain.ProcessActivity;
import ec.edu.unl.sec.selfassessment.domain.ProcessAssignment;
import ec.edu.unl.sec.selfassessment.domain.ProcessState;
import ec.edu.unl.sec.selfassessment.domain.ProcessTeam;
import ec.edu.unl.sec.selfassessment.domain.SelfAssessmentProcess;
import ec.edu.unl.sec.selfassessment.domain.TeamMembership;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessPlanningService {
    private final SelfAssessmentRepository repository;
    private final ModelCatalogPort models;
    private final CurrentActorPort actors;
    private final AuthorizationPort auth;
    private final InstitutionalAuthorityPort authority;
    private final FunctionalAssignmentPolicyPort policy;
    private final SelfAssessmentAuditService audit;

    public ProcessPlanningService(SelfAssessmentRepository repository, ModelCatalogPort models,
            CurrentActorPort actors, AuthorizationPort auth, InstitutionalAuthorityPort authority,
            FunctionalAssignmentPolicyPort policy, SelfAssessmentAuditService audit) {
        this.repository = repository; this.models = models; this.actors = actors; this.auth = auth;
        this.authority = authority; this.policy = policy; this.audit = audit;
    }

    @Transactional
    public Planned<ProcessTeam> createTeam(UUID processId, long expected, String name, String role,
            Instant from, Instant until, UUID correlation) {
        var actor = actor();
        var process = process(processId);
        requirePlanner(actor.identityId(), process);
        SelfAssessmentService.check(process, expected);
        requirePlanningState(process);
        requireProcessPeriod(process, from, until);
        boolean overlap = repository.teams(processId).stream()
                .anyMatch(team -> team.name().equalsIgnoreCase(name.strip()) && team.overlaps(from, until));
        if (overlap) throw error(ProcessProblemException.Kind.CONFLICT, "TEAM_OVERLAP",
                "Ya existe un equipo con ese nombre y vigencia superpuesta.");
        var team = new ProcessTeam(null, processId, name, role, from, until, actor.identityId(), Instant.now(), 0);
        var changed = process.advanceRevision();
        repository.save(changed, expected);
        repository.saveTeam(team);
        audit.record(actor.identityId(), correlation, "PROCESS_TEAM_CREATED", changed, "SUCCESS", null);
        return new Planned<>(team, changed.revision());
    }

    @Transactional
    public Planned<TeamMembership> addMember(UUID teamId, long expected, UUID userId, UUID roleAssignmentId,
            Instant from, Instant until, UUID correlation) {
        var actor = actor();
        var team = repository.findTeam(teamId).orElseThrow(ProcessPlanningService::hidden);
        var process = process(team.processId());
        requirePlanner(actor.identityId(), process);
        SelfAssessmentService.check(process, expected);
        requirePlanningState(process);
        requireProcessPeriod(process, from, until);
        if (from.isBefore(team.validFrom()) || until.isAfter(team.validUntil()))
            throw error(ProcessProblemException.Kind.INVALID, "MEMBERSHIP_OUTSIDE_TEAM",
                    "La vigencia de la membresia debe estar contenida en la vigencia del equipo.");
        boolean overlap = repository.memberships(teamId).stream()
                .anyMatch(member -> member.userId().equals(userId) && member.validFrom().isBefore(until)
                        && member.validUntil().isAfter(from));
        if (overlap) throw error(ProcessProblemException.Kind.CONFLICT, "MEMBERSHIP_OVERLAP",
                "La persona ya tiene una membresia superpuesta en este equipo.");
        var membership = new TeamMembership(null, teamId, userId, roleAssignmentId, from, until,
                actor.identityId(), Instant.now());
        var changed = process.advanceRevision();
        repository.save(changed, expected);
        repository.saveMembership(membership);
        audit.record(actor.identityId(), correlation, "PROCESS_TEAM_MEMBER_ADDED", changed, "SUCCESS", null);
        return new Planned<>(membership, changed.revision());
    }

    @Transactional
    public Planned<ProcessAssignment> assignParticipant(UUID processId, long expected, UUID indicatorId,
            UUID userId, UUID teamId, ProcessAssignment.Function function, Instant from, Instant until,
            String reason, UUID correlation) {
        var actor = actor();
        var process = process(processId);
        requirePlanner(actor.identityId(), process);
        SelfAssessmentService.check(process, expected);
        if (process.state() != ProcessState.ACTIVE)
            throw error(ProcessProblemException.Kind.CONFLICT, "PROCESS_NOT_ACTIVE",
                    "Las asignaciones de responsabilidad requieren un proceso activo.");
        if ((userId == null) == (teamId == null))
            throw error(ProcessProblemException.Kind.INVALID, "ASSIGNMENT_SUBJECT_REQUIRED",
                    "La asignacion requiere exactamente una persona o un equipo.");
        requireIndicator(process, indicatorId);
        requireProcessPeriod(process, from, until);
        if (reason == null || reason.isBlank() || reason.length() > 2000)
            throw error(ProcessProblemException.Kind.INVALID, "CRITICAL_CHANGE_REASON_REQUIRED",
                    "El cambio critico requiere motivo.");
        var scope = new AuthorizationScope(process.careerId(), process.id(), indicatorId);
        if (teamId != null) {
            var team = repository.findTeam(teamId).orElseThrow(ProcessPlanningService::hidden);
            if (!team.processId().equals(processId))
                throw error(ProcessProblemException.Kind.INVALID, "TEAM_OUTSIDE_PROCESS",
                        "El equipo no pertenece al proceso.");
            if (from.isBefore(team.validFrom()) || until.isAfter(team.validUntil()))
                throw error(ProcessProblemException.Kind.INVALID, "ASSIGNMENT_OUTSIDE_TEAM",
                        "La asignacion debe estar contenida en la vigencia del equipo.");
        } else {
            var decision = policy.evaluate(userId, function.name(), scope, from, until);
            if (!decision.allowed())
                throw error(ProcessProblemException.Kind.CONFLICT, decision.reasonCode(),
                        "La asignacion entra en conflicto con la politica vigente.");
        }
        boolean overlap = repository.assignments(processId).stream()
                .anyMatch(existing -> existing.revokedAt() == null && existing.indicatorId().equals(indicatorId)
                        && existing.function() == function && java.util.Objects.equals(existing.identityId(), userId)
                        && java.util.Objects.equals(existing.teamId(), teamId)
                        && from.isBefore(existing.validTo()) && until.isAfter(existing.validFrom()));
        if (overlap) throw error(ProcessProblemException.Kind.CONFLICT, "ASSIGNMENT_OVERLAP",
                "Ya existe una asignacion identica con vigencia superpuesta.");
        var assignment = new ProcessAssignment(null, processId, indicatorId, userId, teamId, function, from, until,
                actor.identityId(), reason.strip(), null, 0);
        var changed = process.advanceRevision();
        repository.save(changed, expected);
        repository.saveAssignment(assignment);
        audit.record(actor.identityId(), correlation, "PROCESS_ASSIGNMENT_CREATED", changed, "SUCCESS", null);
        return new Planned<>(assignment, changed.revision());
    }

    @Transactional
    public Planned<ProcessActivity> createActivity(UUID processId, long expected, ActivityInput input,
            UUID correlation) {
        var actor = actor();
        var process = process(processId);
        requirePlanner(actor.identityId(), process);
        SelfAssessmentService.check(process, expected);
        requirePlanningState(process);
        requireIndicator(process, input.indicatorId());
        requireAssignment(process, input.assignmentId());
        Deadline deadline = deadline(process, input);
        var dependencyIds = dependencies(process, input.dependencyIds());
        requireAcyclic(processId, null, dependencyIds);
        var activity = new ProcessActivity(null, processId, input.indicatorId(), input.requiredSourceId(),
                input.assignmentId(), input.title(), input.dueAt(), ProcessActivity.Status.PLANNED, dependencyIds,
                deadline.reason(), deadline.authorityAssignmentId(), deadline.legalBasis(), actor.identityId(),
                Instant.now(), 0);
        var changed = process.advanceRevision();
        repository.save(changed, expected);
        repository.saveActivity(activity, -1);
        audit.record(actor.identityId(), correlation, "PROCESS_ACTIVITY_CREATED", changed, "SUCCESS", null);
        return new Planned<>(activity, changed.revision());
    }

    @Transactional
    public Planned<ProcessActivity> updateActivity(UUID activityId, long expected, ActivityInput input,
            UUID correlation) {
        var actor = actor();
        var current = repository.findActivity(activityId).orElseThrow(ProcessPlanningService::hidden);
        var process = process(current.processId());
        requirePlanner(actor.identityId(), process);
        SelfAssessmentService.check(process, expected);
        requirePlanningState(process);
        requireIndicator(process, input.indicatorId());
        requireAssignment(process, input.assignmentId());
        Deadline deadline = deadline(process, input);
        var dependencyIds = dependencies(process, input.dependencyIds());
        requireAcyclic(process.id(), activityId, dependencyIds);
        var changedActivity = new ProcessActivity(current.id(), current.processId(), input.indicatorId(),
                input.requiredSourceId(), input.assignmentId(), input.title(), input.dueAt(), current.status(),
                dependencyIds, deadline.reason(), deadline.authorityAssignmentId(), deadline.legalBasis(),
                current.createdBy(), current.createdAt(), current.revision() + 1);
        var changed = process.advanceRevision();
        repository.save(changed, expected);
        repository.saveActivity(changedActivity, current.revision());
        audit.record(actor.identityId(), correlation, "PROCESS_ACTIVITY_UPDATED", changed, "SUCCESS", null);
        return new Planned<>(changedActivity, changed.revision());
    }

    @Transactional(readOnly = true)
    public ProcessTeam team(UUID teamId) {
        var actor = actor();
        var team = repository.findTeam(teamId).orElseThrow(ProcessPlanningService::hidden);
        requirePlanner(actor.identityId(), process(team.processId()));
        return team;
    }

    @Transactional(readOnly = true)
    public List<ProcessTeam> teams(UUID processId) {
        var actor = actor();
        requirePlanner(actor.identityId(), process(processId));
        return repository.teams(processId);
    }

    @Transactional(readOnly = true)
    public List<TeamMembership> memberships(UUID teamId) {
        var actor = actor();
        var team = repository.findTeam(teamId).orElseThrow(ProcessPlanningService::hidden);
        requirePlanner(actor.identityId(), process(team.processId()));
        return repository.memberships(teamId);
    }

    @Transactional(readOnly = true)
    public List<ProcessActivity> activities(UUID processId) {
        var actor = actor();
        requirePlanner(actor.identityId(), process(processId));
        return repository.activities(processId);
    }

    @Transactional(readOnly = true)
    public ProcessActivity activity(UUID activityId) {
        var actor = actor();
        var activity = repository.findActivity(activityId).orElseThrow(ProcessPlanningService::hidden);
        requirePlanner(actor.identityId(), process(activity.processId()));
        return activity;
    }

    private Deadline deadline(SelfAssessmentProcess process, ActivityInput input) {
        if (input.dueAt() == null)
            throw error(ProcessProblemException.Kind.INVALID, "ACTIVITY_DUE_DATE_REQUIRED",
                    "La actividad requiere fecha de vencimiento.");
        Instant start = process.periodStart().atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = process.periodEnd().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        if (!input.dueAt().isBefore(start) && input.dueAt().isBefore(end)) return Deadline.none();
        if (input.exceptionReason() == null || input.exceptionReason().isBlank()
                || input.exceptionAuthorityAssignmentId() == null || input.exceptionLegalBasis() == null
                || input.exceptionLegalBasis().isBlank())
            throw error(ProcessProblemException.Kind.INVALID, "DEADLINE_EXCEPTION_REQUIRED",
                    "Un plazo fuera del periodo requiere excepcion, fundamento y autoridad.");
        var scope = new AuthorizationScope(process.careerId(), process.id(), input.indicatorId());
        if (authority == null || !authority.isAuthorizedAssignment(input.exceptionAuthorityAssignmentId(),
                actor().identityId(), OperationCode.PROCESS_COORDINATE, scope))
            throw error(ProcessProblemException.Kind.FORBIDDEN, "DEADLINE_AUTHORITY_DENIED",
                    "La asignacion indicada no autoriza la excepcion de plazo.");
        return new Deadline(input.exceptionReason().strip(), input.exceptionAuthorityAssignmentId(),
                input.exceptionLegalBasis().strip());
    }

    private List<UUID> dependencies(SelfAssessmentProcess process, List<UUID> requested) {
        if (requested == null || requested.isEmpty()) return List.of();
        var unique = new LinkedHashSet<>(requested);
        if (unique.size() != requested.size())
            throw error(ProcessProblemException.Kind.INVALID, "DEPENDENCY_DUPLICATED",
                    "Las dependencias no pueden repetirse.");
        var known = new HashSet<UUID>();
        repository.activities(process.id()).forEach(activity -> known.add(activity.id()));
        if (!known.containsAll(unique))
            throw error(ProcessProblemException.Kind.INVALID, "DEPENDENCY_OUTSIDE_PROCESS",
                    "Toda dependencia debe ser una actividad del mismo proceso.");
        return List.copyOf(unique);
    }

    private void requireAcyclic(UUID processId, UUID currentId, List<UUID> dependencyIds) {
        Map<UUID, List<UUID>> graph = new HashMap<>();
        for (var activity : repository.activities(processId)) {
            graph.put(activity.id(), activity.dependencyIds());
        }
        UUID start = currentId == null ? UUID.randomUUID() : currentId;
        graph.put(start, dependencyIds);
        Set<UUID> visiting = new HashSet<>();
        Set<UUID> visited = new HashSet<>();
        visit(start, graph, visiting, visited);
    }

    private static void visit(UUID node, Map<UUID, List<UUID>> graph, Set<UUID> visiting, Set<UUID> visited) {
        if (visited.contains(node)) return;
        if (!visiting.add(node))
            throw error(ProcessProblemException.Kind.CONFLICT, "DEPENDENCY_CYCLE",
                    "Las dependencias de actividades no pueden formar ciclos.");
        for (UUID dependency : graph.getOrDefault(node, List.of())) visit(dependency, graph, visiting, visited);
        visiting.remove(node);
        visited.add(node);
    }

    private void requirePlanningState(SelfAssessmentProcess process) {
        if (process.state() != ProcessState.PREPARATION && process.state() != ProcessState.ACTIVE)
            throw error(ProcessProblemException.Kind.CONFLICT, "PROCESS_NOT_PLANNABLE",
                    "La planificacion solo procede en preparacion o ejecucion activa.");
    }

    private void requireProcessPeriod(SelfAssessmentProcess process, Instant from, Instant until) {
        if (from == null || until == null || !from.isBefore(until))
            throw error(ProcessProblemException.Kind.INVALID, "INVALID_VIGENCY",
                    "La vigencia requiere un intervalo inequivoco.");
        Instant start = process.periodStart().atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = process.periodEnd().plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        if (from.isBefore(start) || until.isAfter(end))
            throw error(ProcessProblemException.Kind.INVALID, "VIGENCY_OUTSIDE_PERIOD",
                    "La vigencia debe estar contenida en el periodo del proceso.");
    }

    private void requireIndicator(SelfAssessmentProcess process, UUID indicatorId) {
        if (indicatorId == null) throw error(ProcessProblemException.Kind.INVALID, "INDICATOR_REQUIRED",
                "El indicador es obligatorio.");
        if (!process.indicators().isEmpty()) {
            if (process.indicators().stream().noneMatch(indicator -> indicator.id().equals(indicatorId)))
                throw error(ProcessProblemException.Kind.INVALID, "INDICATOR_OUTSIDE_FIXED_MODEL",
                        "El indicador no pertenece al modelo fijado.");
            return;
        }
        var model = models.publishedVersion(process.modelVersionId()).orElseThrow(() -> error(
                ProcessProblemException.Kind.CONFLICT, "MODEL_VERSION_UNAVAILABLE",
                "La version fijada no esta disponible."));
        if (model.indicators().stream().noneMatch(indicator -> indicator.id().equals(indicatorId)))
            throw error(ProcessProblemException.Kind.INVALID, "INDICATOR_OUTSIDE_FIXED_MODEL",
                    "El indicador no pertenece al modelo fijado.");
    }

    private void requireAssignment(SelfAssessmentProcess process, UUID assignmentId) {
        if (assignmentId == null || repository.assignments(process.id()).stream()
                .noneMatch(assignment -> assignment.id().equals(assignmentId) && assignment.revokedAt() == null))
            throw error(ProcessProblemException.Kind.INVALID, "ASSIGNMENT_NOT_FOUND",
                    "La actividad requiere una asignacion vigente del proceso.");
    }

    private void requirePlanner(UUID actorId, SelfAssessmentProcess process) {
        var scope = new AuthorizationScope(process.careerId(), process.id(), null);
        if (!auth.isAllowed(actorId, OperationCode.PROCESS_COORDINATE, scope)
                && !auth.isAllowed(actorId, OperationCode.PROCESS_MANAGE, scope))
            throw hidden();
    }

    private CurrentActorPort.Actor actor() {
        return actors.currentActor().orElseThrow(() -> error(ProcessProblemException.Kind.UNAUTHORIZED,
                "AUTHENTICATION_REQUIRED", "Se requiere autenticacion."));
    }

    private SelfAssessmentProcess process(UUID id) {
        return repository.find(id).orElseThrow(ProcessPlanningService::hidden);
    }

    private static ProcessProblemException hidden() {
        return error(ProcessProblemException.Kind.NOT_FOUND, "PROCESS_NOT_FOUND",
                "El proceso no existe o no esta visible.");
    }

    private static ProcessProblemException error(ProcessProblemException.Kind kind, String code, String message) {
        return SelfAssessmentService.problem(kind, code, message);
    }

    public record Planned<T>(T value, long processRevision) {}

    public record ActivityInput(UUID indicatorId, UUID requiredSourceId, UUID assignmentId, String title,
            Instant dueAt, List<UUID> dependencyIds, String exceptionReason,
            UUID exceptionAuthorityAssignmentId, String exceptionLegalBasis) {
        public ActivityInput {
            dependencyIds = dependencyIds == null ? List.of() : List.copyOf(dependencyIds);
        }
    }

    private record Deadline(String reason, UUID authorityAssignmentId, String legalBasis) {
        static Deadline none() { return new Deadline(null, null, null); }
    }
}
