package ec.edu.unl.sec.identityaccess.application;

import ec.edu.unl.sec.identityaccess.domain.*;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdentityAdministrationService {
    private static final Instant OPEN_ENDED = Instant.parse("9999-12-31T00:00:00Z");

    private final IdentityAccessRepository repository;
    private final AuthorizationPort authorization;
    private final IdentityAccessAuditService audit;
    private final SessionService sessions;
    private final Clock clock;

    @Autowired
    public IdentityAdministrationService(
            IdentityAccessRepository repository,
            AuthorizationPort authorization,
            IdentityAccessAuditService audit,
            SessionService sessions) {
        this(repository, authorization, audit, sessions, Clock.systemUTC());
    }

    IdentityAdministrationService(
            IdentityAccessRepository repository,
            AuthorizationPort authorization,
            IdentityAccessAuditService audit,
            SessionService sessions,
            Clock clock) {
        this.repository = repository;
        this.authorization = authorization;
        this.audit = audit;
        this.sessions = sessions;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Page<UserView> listUsers(Actor actor, int page, int size) {
        require(actor, OperationCode.ACCESS_MANAGE_USERS, "ACCESS_MANAGE_USERS");
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        List<UserView> items = repository.listUsers(safePage * safeSize, safeSize).stream()
                .map(IdentityAdministrationService::view)
                .toList();
        return new Page<>(items, safePage, safeSize, repository.countUsers());
    }

    @Transactional
    public UUID createUser(
            Actor actor,
            String username,
            String displayName,
            String initialPassword,
            boolean technical,
            UUID correlationId) {
        require(actor, OperationCode.ACCESS_MANAGE_USERS, "ACCESS_MANAGE_USERS");
        String normalized = AccessIdentity.normalize(username);
        if (normalized.length() < 3 || normalized.length() > 100) {
            throw new IllegalArgumentException("El usuario debe tener entre 3 y 100 caracteres.");
        }
        if (displayName == null || displayName.isBlank() || displayName.length() > 200) {
            throw new IllegalArgumentException("El nombre visible es obligatorio y admite hasta 200 caracteres.");
        }
        if (initialPassword == null || initialPassword.length() < 12 || initialPassword.length() > 256) {
            throw new IllegalArgumentException("La contrasena inicial debe tener entre 12 y 256 caracteres.");
        }
        if (repository.findLogin(normalized).isPresent()) {
            reject(actor, correlationId, "IDENTITY_CREATED", normalized, "IDENTITY_ALREADY_EXISTS");
            throw new IdentityConflictException(
                    "IDENTITY_ALREADY_EXISTS", "Ya existe una identidad con ese usuario.");
        }
        Instant now = clock.instant();
        AccessIdentity identity = new AccessIdentity(
                UUID.randomUUID(),
                normalized,
                displayName.strip(),
                technical ? AccessIdentity.Type.TECHNICAL : AccessIdentity.Type.HUMAN,
                true,
                0,
                now);
        repository.createIdentity(identity, sessions.encodePassword(initialPassword));
        audit.record(
                actor.identityId(),
                correlationId,
                "IDENTITY_CREATED",
                "ACCESS_IDENTITY",
                identity.id(),
                null,
                null,
                "SUCCESS",
                "CREATED",
                Map.of("technical", technical));
        return identity.id();
    }

    @Transactional
    public long changeState(
            Actor actor, UUID userId, boolean active, long expectedRevision, UUID correlationId) {
        require(actor, OperationCode.ACCESS_MANAGE_USERS, "ACCESS_MANAGE_USERS");
        IdentityAccessRepository.UserRecord user = repository.findUser(userId)
                .orElseThrow(IdentityNotFoundException::new);
        if (user.revision() != expectedRevision) throw new StaleIdentityRevisionException();
        try {
            if (active) {
                repository.activateIdentity(userId, expectedRevision);
            } else {
                repository.deactivateIdentity(userId, expectedRevision, clock.instant());
            }
        } catch (IllegalStateException stale) {
            throw new StaleIdentityRevisionException();
        }
        String action = active ? "IDENTITY_ENABLED" : "IDENTITY_DISABLED";
        audit.record(
                actor.identityId(),
                correlationId,
                action,
                "ACCESS_IDENTITY",
                userId,
                null,
                null,
                "SUCCESS",
                active ? "ENABLED" : "DISABLED",
                Map.of());
        return expectedRevision + 1;
    }

    @Transactional(readOnly = true)
    public List<AssignmentView> listAssignments(Actor actor, UUID userId) {
        require(actor, OperationCode.ACCESS_MANAGE_USERS, "ACCESS_MANAGE_USERS");
        if (repository.findUser(userId).isEmpty()) throw new IdentityNotFoundException();
        return repository.assignmentRecords(userId, clock.instant()).stream()
                .map(IdentityAdministrationService::view)
                .toList();
    }

    @Transactional
    public UUID assign(
            Actor actor,
            UUID userId,
            String roleCode,
            UUID careerId,
            UUID processId,
            UUID indicatorId,
            Instant validFrom,
            Instant validUntil,
            String exceptionReason,
            UUID correlationId) {
        require(actor, OperationCode.ACCESS_ASSIGN_SCOPE, "ACCESS_ASSIGN_SCOPE");
        IdentityAccessRepository.UserRecord user = repository.findUser(userId)
                .orElseThrow(IdentityNotFoundException::new);
        if (!user.active()) {
            reject(actor, correlationId, "ASSIGNMENT_REJECTED", userId.toString(), "IDENTITY_INACTIVE");
            throw new IdentityConflictException("IDENTITY_INACTIVE", "La identidad no esta activa.");
        }
        if (user.technical()) {
            reject(actor, correlationId, "ASSIGNMENT_REJECTED", userId.toString(), "TECHNICAL_CANNOT_HOLD_FUNCTION");
            throw new IdentityConflictException(
                    "TECHNICAL_CANNOT_HOLD_FUNCTION",
                    "Una cuenta tecnica no puede recibir funciones de proceso.");
        }
        AuthorizationScope scope;
        try {
            scope = new AuthorizationScope(careerId, processId, indicatorId);
        } catch (IllegalArgumentException invalidScope) {
            reject(actor, correlationId, "ASSIGNMENT_REJECTED", userId.toString(), "INVALID_SCOPE");
            throw invalidScope;
        }
        UUID functionId = repository.findFunctionIdByCode(roleCode)
                .orElseThrow(() -> new IllegalArgumentException("La funcion de proceso no existe o no esta activa."));
        Instant from = validFrom == null ? clock.instant() : validFrom.truncatedTo(ChronoUnit.SECONDS);
        Instant until = validUntil == null ? OPEN_ENDED : validUntil.truncatedTo(ChronoUnit.SECONDS);
        if (!from.isBefore(until)) {
            reject(actor, correlationId, "ASSIGNMENT_REJECTED", userId.toString(), "INVALID_VIGENCY");
            throw new IllegalArgumentException("La vigencia debe iniciar antes de finalizar.");
        }
        if (repository.assignmentOverlaps(userId, functionId, scope, from, until)) {
            reject(actor, correlationId, "ASSIGNMENT_REJECTED", userId.toString(), "ASSIGNMENT_OVERLAP");
            throw new IdentityConflictException(
                    "ASSIGNMENT_OVERLAP", "Ya existe una asignacion identica vigente en ese periodo.");
        }

        Instant now = clock.instant();
        UUID policyId = repository
                .findConflictingPolicy(userId, functionId, scope, from, until, now)
                .orElse(null);
        if (policyId != null) {
            String justification = exceptionReason == null ? "" : exceptionReason.strip();
            if (justification.length() < 10 || justification.length() > 1000) {
                reject(actor, correlationId, "ASSIGNMENT_REJECTED", userId.toString(), "FUNCTION_CONFLICT");
                throw new IdentityConflictException(
                        "FUNCTION_CONFLICT",
                        "La combinacion de funciones esta prohibida; se requiere una excepcion autorizada.",
                        policyId);
            }
            repository.createConflictException(new ConflictException(
                    UUID.randomUUID(),
                    policyId,
                    userId,
                    scope,
                    justification,
                    from,
                    until,
                    actor.identityId(),
                    null));
            audit.record(
                    actor.identityId(),
                    correlationId,
                    "CONFLICT_EXCEPTION_CREATED",
                    "CONFLICT_EXCEPTION",
                    userId,
                    careerId,
                    processId,
                    "SUCCESS",
                    "EXCEPTION_GRANTED",
                    Map.of("policyId", policyId.toString()));
        }

        FunctionalAssignment assignment = new FunctionalAssignment(
                UUID.randomUUID(),
                userId,
                functionId,
                scope,
                from,
                until,
                actor.identityId(),
                null,
                0);
        repository.createAssignment(assignment);
        audit.record(
                actor.identityId(),
                correlationId,
                "ASSIGNMENT_CREATED",
                "FUNCTIONAL_ASSIGNMENT",
                assignment.id(),
                careerId,
                processId,
                "SUCCESS",
                "CREATED",
                Map.of("roleCode", roleCode));
        return assignment.id();
    }

    @Transactional
    public long revokeAssignment(Actor actor, UUID userId, UUID assignmentId, long expectedRevision,
            String reason, UUID correlationId) {
        require(actor, OperationCode.ACCESS_ASSIGN_SCOPE, "ACCESS_ASSIGN_SCOPE");
        var assignment = repository.findAssignment(assignmentId)
                .filter(a -> a.identityId().equals(userId)).orElseThrow(IdentityNotFoundException::new);
        if (!authorization.isAllowed(actor.identityId(), OperationCode.ACCESS_ASSIGN_SCOPE, assignment.scope()))
            throw new IdentityNotFoundException();
        if (reason == null || reason.strip().length() < 10 || reason.length() > 1000)
            throw new IllegalArgumentException("La revocacion requiere un motivo entre 10 y 1000 caracteres.");
        if (assignment.revision() != expectedRevision) throw new StaleIdentityRevisionException();
        if (assignment.revokedAt() != null)
            throw new IdentityConflictException("ASSIGNMENT_ALREADY_REVOKED", "La asignacion ya fue revocada.");
        try {
            repository.revokeAssignment(assignmentId, expectedRevision, clock.instant());
        } catch (IllegalStateException stale) {
            throw new StaleIdentityRevisionException();
        }
        audit.record(actor.identityId(), correlationId, "ASSIGNMENT_REVOKED", "FUNCTIONAL_ASSIGNMENT", assignmentId,
                assignment.scope().careerId(), assignment.scope().processId(), "SUCCESS", "REVOKED",
                Map.of("reason", reason.strip(), "identityId", userId.toString(), "functionId", assignment.functionId().toString()));
        return expectedRevision + 1;
    }

    @Transactional
    public UUID createException(
            Actor actor,
            UUID policyId,
            UUID targetIdentityId,
            UUID careerId,
            UUID processId,
            UUID indicatorId,
            String justification,
            Instant validFrom,
            Instant validTo,
            UUID correlationId) {
        require(actor, OperationCode.ACCESS_MANAGE_EXCEPTIONS, "ACCESS_MANAGE_EXCEPTIONS");
        IdentityAccessRepository.PolicyRecord policy = repository.findPolicy(policyId)
                .orElseThrow(IdentityNotFoundException::new);
        Instant now = clock.instant();
        if (!policy.active()
                || now.isBefore(policy.validFrom())
                || !now.isBefore(policy.validTo())) {
            reject(actor, correlationId, "CONFLICT_REJECTED", policyId.toString(), "POLICY_NOT_ACTIVE");
            throw new IdentityConflictException("POLICY_NOT_ACTIVE", "La politica de conflicto no esta vigente.");
        }
        if (repository.findUser(targetIdentityId).isEmpty()) throw new IdentityNotFoundException();
        AuthorizationScope scope;
        try {
            scope = new AuthorizationScope(careerId, processId, indicatorId);
        } catch (IllegalArgumentException invalidScope) {
            reject(actor, correlationId, "CONFLICT_REJECTED", policyId.toString(), "INVALID_SCOPE");
            throw invalidScope;
        }
        String safeJustification = justification == null ? "" : justification.strip();
        if (safeJustification.length() < 10 || safeJustification.length() > 1000) {
            throw new IllegalArgumentException("La justificacion debe tener entre 10 y 1000 caracteres.");
        }
        Instant from = validFrom == null ? now : validFrom.truncatedTo(ChronoUnit.SECONDS);
        Instant until = validTo == null ? OPEN_ENDED : validTo.truncatedTo(ChronoUnit.SECONDS);
        if (!from.isBefore(until)) throw new IllegalArgumentException("La vigencia de la excepcion es invalida.");
        boolean applies = repository.activeAssignments(targetIdentityId, now).stream()
                .anyMatch(assignment -> assignment.functionId().equals(policy.leftFunctionId())
                        || assignment.functionId().equals(policy.rightFunctionId()));
        if (!applies) {
            reject(actor, correlationId, "CONFLICT_REJECTED", policyId.toString(), "CONFLICT_NOT_APPLICABLE");
            throw new IdentityConflictException(
                    "CONFLICT_NOT_APPLICABLE", "La identidad no presenta el conflicto indicado.");
        }
        UUID id = UUID.randomUUID();
        repository.createConflictException(new ConflictException(
                id,
                policyId,
                targetIdentityId,
                scope,
                safeJustification,
                from,
                until,
                actor.identityId(),
                null));
        audit.record(
                actor.identityId(),
                correlationId,
                "CONFLICT_EXCEPTION_CREATED",
                "CONFLICT_EXCEPTION",
                id,
                careerId,
                processId,
                "SUCCESS",
                "EXCEPTION_GRANTED",
                Map.of("policyId", policyId.toString(), "identityId", targetIdentityId.toString()));
        return id;
    }

    @Transactional(readOnly = true)
    public List<UUID> auditCareerIds(Actor actor) {
        require(actor, OperationCode.ACCESS_READ_AUDIT, "ACCESS_READ_AUDIT");
        return repository.authorizedCareerIds(actor.identityId(), OperationCode.ACCESS_READ_AUDIT, clock.instant());
    }

    @Transactional(readOnly = true)
    public CurrentIdentity currentIdentity(UUID identityId) {
        IdentityAccessRepository.UserRecord user = repository.findUser(identityId)
                .orElseThrow(IdentityNotFoundException::new);
        List<AssignmentView> assignments = repository.assignmentRecords(identityId, clock.instant()).stream()
                .map(IdentityAdministrationService::view)
                .toList();
        return new CurrentIdentity(user.id(), user.displayName(), user.technical(), assignments);
    }

    private void require(Actor actor, OperationCode operation, String reason) {
        if (actor == null || !authorization.hasOperation(actor.identityId(), operation)) {
            if (actor != null) {
                reject(actor, actor.correlationId(), "ACCESS_DENIED", operation.value(), reason);
            }
            throw new IdentityAccessDeniedException(reason);
        }
    }

    private void reject(Actor actor, UUID correlationId, String action, String objectId, String reason) {
        try {
            audit.rejection(
                    actor.identityId(),
                    correlationId,
                    action,
                    "ACCESS_IDENTITY",
                    safeUuid(objectId),
                    null,
                    null,
                    reason,
                    Map.of());
        } catch (RuntimeException ignored) {
            // Rejection auditing must never mask the domain response.
        }
    }

    private static UUID safeUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException invalid) {
            return null;
        }
    }

    private static UserView view(IdentityAccessRepository.UserRecord r) {
        return new UserView(r.id(), r.login(), r.displayName(), r.technical(), r.active(), r.revision());
    }

    private static AssignmentView view(IdentityAccessRepository.AssignmentRecord r) {
        return new AssignmentView(
                r.id(),
                r.functionCode(),
                r.functionName(),
                r.scope().careerId(),
                r.scope().processId(),
                r.scope().indicatorId(),
                r.validFrom(),
                r.validTo(), r.revision());
    }

    public record Actor(UUID identityId, UUID correlationId) {}

    public record UserView(
            UUID id, String username, String displayName, boolean technical, boolean active, long revision) {}

    public record AssignmentView(
            UUID id,
            String roleCode,
            String roleName,
            UUID careerId,
            UUID processId,
            UUID indicatorId,
            Instant validFrom,
            Instant validTo, long revision) {}

    public record Page<T>(List<T> items, int page, int size, long total) {}

    public record CurrentIdentity(
            UUID id, String displayName, boolean technical, List<AssignmentView> assignments) {}
}
