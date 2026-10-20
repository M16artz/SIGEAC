package ec.edu.unl.sec.identityaccess.support;

import ec.edu.unl.sec.identityaccess.application.IdentityAccessRepository;
import ec.edu.unl.sec.identityaccess.domain.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class InMemoryIdentityAccessRepository implements IdentityAccessRepository {
    public record Function(UUID id, String code, String name, Set<String> operations, boolean active) {}
    private record Attempt(String subject, Instant at, boolean success) {}
    private record StoredSession(AccessSession session) {}
    public record StoredException(ConflictException exception) {}

    private final Map<UUID, UserRecord> users = new LinkedHashMap<>();
    private final Map<UUID, String> passwords = new LinkedHashMap<>();
    private final List<Attempt> attempts = new ArrayList<>();
    private final List<StoredSession> sessions = new ArrayList<>();
    private final List<FunctionalAssignment> assignments = new ArrayList<>();
    private final Map<UUID, Function> functions = new LinkedHashMap<>();
    private final Map<UUID, PolicyRecord> policies = new LinkedHashMap<>();
    private final List<StoredException> exceptions = new ArrayList<>();

    public void addFunction(Function function) {
        functions.put(function.id(), function);
    }

    public void addPolicy(PolicyRecord policy) {
        policies.put(policy.id(), policy);
    }

    public void addUser(UUID id, String login, String displayName, boolean technical, boolean active, String bcryptHash) {
        users.put(id, new UserRecord(id, login, displayName, technical, active, 0));
        passwords.put(id, bcryptHash);
    }

    public void addAssignment(FunctionalAssignment assignment) {
        assignments.add(assignment);
    }

    public List<FunctionalAssignment> assignments() {
        return List.copyOf(assignments);
    }

    public List<StoredException> exceptions() {
        return List.copyOf(exceptions);
    }

    @Override
    public Optional<LoginRecord> findLogin(String normalizedLogin) {
        return users.values().stream()
                .filter(user -> user.login().equals(normalizedLogin))
                .findFirst()
                .map(user -> new LoginRecord(
                        user.id(), user.displayName(), user.technical(), user.active(), passwords.get(user.id())));
    }

    @Override
    public long countFailures(String subjectKey, Instant since) {
        return attempts.stream()
                .filter(attempt -> attempt.subject().equals(subjectKey))
                .filter(attempt -> !attempt.at().isBefore(since))
                .filter(attempt -> !attempt.success())
                .count();
    }

    @Override
    public void recordAttempt(String subjectKey, String sourceKey, Instant at, boolean success, UUID correlationId) {
        attempts.add(new Attempt(subjectKey, at, success));
    }

    @Override
    public void saveSession(AccessSession session) {
        sessions.add(new StoredSession(session));
    }

    @Override
    public Optional<SessionIdentity> findSession(String tokenHash) {
        return sessions.stream()
                .map(StoredSession::session)
                .filter(session -> session.tokenHash().equals(tokenHash))
                .findFirst()
                .flatMap(session -> users.values().stream()
                        .filter(user -> user.id().equals(session.identityId()))
                        .findFirst()
                        .map(user -> new SessionIdentity(
                                session.id(),
                                user.id(),
                                user.displayName(),
                                user.technical(),
                                user.active(),
                                session.lastActivityAt(),
                                session.expiresAt(),
                                session.revokedAt(),
                                session.csrfHash())));
    }

    @Override
    public void touchSession(UUID sessionId, Instant at, Instant expiresAt) {
        for (int i = 0; i < sessions.size(); i++) {
            AccessSession session = sessions.get(i).session();
            if (session.id().equals(sessionId)) {
                sessions.set(i, new StoredSession(new AccessSession(
                        session.id(),
                        session.identityId(),
                        session.tokenHash(),
                        session.csrfHash(),
                        session.createdAt(),
                        at,
                        expiresAt,
                        session.revokedAt())));
            }
        }
    }

    @Override
    public void revokeSession(String tokenHash, Instant at) {
        for (int i = 0; i < sessions.size(); i++) {
            AccessSession session = sessions.get(i).session();
            if (session.tokenHash().equals(tokenHash)) {
                sessions.set(i, new StoredSession(new AccessSession(
                        session.id(),
                        session.identityId(),
                        session.tokenHash(),
                        session.csrfHash(),
                        session.createdAt(),
                        session.lastActivityAt(),
                        session.expiresAt(),
                        at)));
            }
        }
    }

    @Override
    public List<FunctionalAssignment> activeAssignments(UUID identityId, Instant at) {
        return assignments.stream()
                .filter(assignment -> assignment.identityId().equals(identityId))
                .filter(assignment -> assignment.activeAt(at))
                .toList();
    }

    @Override
    public List<AssignmentRecord> assignmentRecords(UUID identityId, Instant at) {
        return activeAssignments(identityId, at).stream()
                .map(assignment -> {
                    Function function = functions.get(assignment.functionId());
                    return new AssignmentRecord(
                            assignment.id(),
                            assignment.functionId(),
                            function == null ? "UNKNOWN" : function.code(),
                            function == null ? "Unknown" : function.name(),
                            assignment.scope(),
                            assignment.validFrom(),
                            assignment.validTo(),
                            assignment.revokedAt(),
                            assignment.revision());
                })
                .toList();
    }

    @Override
    public boolean functionAllows(UUID functionId, OperationCode operation) {
        Function function = functions.get(functionId);
        return function != null && function.active() && function.operations().contains(operation.value());
    }

    @Override
    public boolean hasOperation(UUID identityId, OperationCode operation, Instant at) {
        return activeAssignments(identityId, at).stream()
                .anyMatch(assignment -> functionAllows(assignment.functionId(), operation));
    }

    @Override
    public boolean hasUnexcusedConflict(UUID identityId, UUID functionId, AuthorizationScope scope, Instant at) {
        return policies.values().stream()
                .filter(policy -> policy.active()
                        && !at.isBefore(policy.validFrom())
                        && at.isBefore(policy.validTo()))
                .filter(policy -> pair(policy, functionId))
                .anyMatch(policy -> hasConflictingAssignment(identityId, policy, scope, at)
                        && !hasException(identityId, policy.id(), scope, at));
    }

    @Override
    public UUID createIdentity(AccessIdentity identity, String bcryptHash) {
        users.put(
                identity.id(),
                new UserRecord(
                        identity.id(),
                        identity.login(),
                        identity.displayName(),
                        identity.type() == AccessIdentity.Type.TECHNICAL,
                        identity.active(),
                        identity.revision()));
        passwords.put(identity.id(), bcryptHash);
        return identity.id();
    }

    @Override
    public void deactivateIdentity(UUID identityId, long revision, Instant at) {
        UserRecord user = users.get(identityId);
        if (user == null || user.revision() != revision) throw new IllegalStateException("STALE_IDENTITY");
        users.put(
                identityId,
                new UserRecord(user.id(), user.login(), user.displayName(), user.technical(), false, revision + 1));
        for (int i = 0; i < sessions.size(); i++) {
            AccessSession session = sessions.get(i).session();
            if (session.identityId().equals(identityId) && session.revokedAt() == null) {
                sessions.set(i, new StoredSession(new AccessSession(
                        session.id(),
                        session.identityId(),
                        session.tokenHash(),
                        session.csrfHash(),
                        session.createdAt(),
                        session.lastActivityAt(),
                        session.expiresAt(),
                        at)));
            }
        }
    }

    @Override
    public void activateIdentity(UUID identityId, long revision) {
        UserRecord user = users.get(identityId);
        if (user == null || user.revision() != revision) throw new IllegalStateException("STALE_IDENTITY");
        users.put(
                identityId,
                new UserRecord(user.id(), user.login(), user.displayName(), user.technical(), true, revision + 1));
    }

    @Override
    public UUID createAssignment(FunctionalAssignment assignment) {
        assignments.add(assignment);
        return assignment.id();
    }

    @Override
    public Optional<UserRecord> findUser(UUID identityId) {
        return Optional.ofNullable(users.get(identityId));
    }

    @Override
    public List<UserRecord> listUsers(int offset, int limit) {
        return users.values().stream()
                .sorted(Comparator.comparing(UserRecord::login))
                .skip(offset)
                .limit(limit)
                .toList();
    }

    @Override
    public long countUsers() {
        return users.size();
    }

    @Override
    public Optional<UUID> findFunctionIdByCode(String code) {
        return functions.values().stream()
                .filter(Function::active)
                .filter(function -> function.code().equals(code))
                .map(Function::id)
                .findFirst();
    }

    @Override
    public boolean assignmentOverlaps(
            UUID identityId, UUID functionId, AuthorizationScope scope, Instant from, Instant to) {
        return assignments.stream()
                .filter(assignment -> assignment.identityId().equals(identityId))
                .filter(assignment -> assignment.functionId().equals(functionId))
                .filter(assignment -> assignment.revokedAt() == null)
                .filter(assignment -> assignment.scope().equals(scope))
                .anyMatch(assignment -> assignment.validFrom().isBefore(to) && assignment.validTo().isAfter(from));
    }

    @Override
    public Optional<UUID> findConflictingPolicy(
            UUID identityId,
            UUID newFunctionId,
            AuthorizationScope scope,
            Instant from,
            Instant to,
            Instant at) {
        return policies.values().stream()
                .filter(policy -> policy.active()
                        && !at.isBefore(policy.validFrom())
                        && at.isBefore(policy.validTo()))
                .filter(policy -> pair(policy, newFunctionId))
                .filter(policy -> assignments.stream()
                        .filter(assignment -> assignment.identityId().equals(identityId))
                        .filter(assignment -> assignment.revokedAt() == null)
                        .filter(assignment -> otherFunction(policy, newFunctionId).equals(assignment.functionId()))
                        .filter(assignment -> assignment.scope().careerId().equals(scope.careerId()))
                        .anyMatch(assignment -> assignment.validFrom().isBefore(to) && assignment.validTo().isAfter(from)))
                .filter(policy -> !hasException(identityId, policy.id(), scope, at))
                .map(PolicyRecord::id)
                .findFirst();
    }

    @Override
    public Optional<PolicyRecord> findPolicy(UUID policyId) {
        return Optional.ofNullable(policies.get(policyId));
    }

    @Override
    public Optional<UUID> findPolicyForFunctions(UUID leftFunctionId, UUID rightFunctionId, Instant at) {
        return policies.values().stream()
                .filter(policy -> policy.active()
                        && !at.isBefore(policy.validFrom())
                        && at.isBefore(policy.validTo()))
                .filter(policy -> (policy.leftFunctionId().equals(leftFunctionId)
                                && policy.rightFunctionId().equals(rightFunctionId))
                        || (policy.leftFunctionId().equals(rightFunctionId)
                                && policy.rightFunctionId().equals(leftFunctionId)))
                .map(PolicyRecord::id)
                .findFirst();
    }

    @Override
    public UUID createConflictException(ConflictException exception) {
        exceptions.add(new StoredException(exception));
        return exception.id();
    }

    @Override
    public List<UUID> authorizedCareerIds(UUID identityId, OperationCode operation, Instant at) {
        return activeAssignments(identityId, at).stream()
                .filter(assignment -> functionAllows(assignment.functionId(), operation))
                .map(assignment -> assignment.scope().careerId())
                .distinct()
                .toList();
    }

    private boolean hasConflictingAssignment(
            UUID identityId, PolicyRecord policy, AuthorizationScope scope, Instant at) {
        return assignments.stream()
                .filter(assignment -> assignment.identityId().equals(identityId))
                .filter(assignment -> assignment.activeAt(at))
                .filter(assignment -> pair(policy, assignment.functionId()))
                .anyMatch(assignment -> assignment.scope().careerId().equals(scope.careerId()));
    }

    private boolean hasException(UUID identityId, UUID policyId, AuthorizationScope scope, Instant at) {
        return exceptions.stream()
                .map(StoredException::exception)
                .filter(exception -> exception.policyId().equals(policyId))
                .filter(exception -> exception.identityId().equals(identityId))
                .filter(exception -> exception.activeAt(at))
                .anyMatch(exception -> exception.scope().overlaps(scope));
    }

    private static boolean pair(PolicyRecord policy, UUID functionId) {
        return policy.leftFunctionId().equals(functionId) || policy.rightFunctionId().equals(functionId);
    }

    private static UUID otherFunction(PolicyRecord policy, UUID functionId) {
        return policy.leftFunctionId().equals(functionId) ? policy.rightFunctionId() : policy.leftFunctionId();
    }
}
