package ec.edu.unl.sec.identityaccess.application;

import ec.edu.unl.sec.identityaccess.domain.*;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IdentityAccessRepository {
    Optional<LoginRecord> findLogin(String normalizedLogin);

    long countFailures(String subjectKey, Instant since);

    void recordAttempt(String subjectKey, String sourceKey, Instant at, boolean success, UUID correlationId);

    void saveSession(AccessSession session);

    Optional<SessionIdentity> findSession(String tokenHash);

    void touchSession(UUID sessionId, Instant at, Instant expiresAt);

    void revokeSession(String tokenHash, Instant at);

    List<FunctionalAssignment> activeAssignments(UUID identityId, Instant at);

    List<AssignmentRecord> assignmentRecords(UUID identityId, Instant at);

    boolean functionAllows(UUID functionId, OperationCode operation);

    boolean hasOperation(UUID identityId, OperationCode operation, Instant at);

    boolean hasUnexcusedConflict(UUID identityId, UUID functionId, AuthorizationScope scope, Instant at);

    UUID createIdentity(AccessIdentity identity, String bcryptHash);

    void deactivateIdentity(UUID identityId, long revision, Instant at);

    void activateIdentity(UUID identityId, long revision);

    UUID createAssignment(FunctionalAssignment assignment);

    Optional<UserRecord> findUser(UUID identityId);

    List<UserRecord> listUsers(int offset, int limit);

    long countUsers();

    Optional<UUID> findFunctionIdByCode(String code);

    boolean assignmentOverlaps(UUID identityId, UUID functionId, AuthorizationScope scope, Instant from, Instant to);

    Optional<UUID> findConflictingPolicy(
            UUID identityId, UUID newFunctionId, AuthorizationScope scope, Instant from, Instant to, Instant at);

    Optional<PolicyRecord> findPolicy(UUID policyId);

    Optional<UUID> findPolicyForFunctions(UUID leftFunctionId, UUID rightFunctionId, Instant at);

    UUID createConflictException(ConflictException exception);

    List<UUID> authorizedCareerIds(UUID identityId, OperationCode operation, Instant at);

    record LoginRecord(UUID identityId, String displayName, boolean technical, boolean active, String bcryptHash) {}

    record SessionIdentity(
            UUID sessionId,
            UUID identityId,
            String displayName,
            boolean technical,
            boolean identityActive,
            Instant lastActivityAt,
            Instant expiresAt,
            Instant revokedAt,
            String csrfHash) {}

    record UserRecord(UUID id, String login, String displayName, boolean technical, boolean active, long revision) {}

    record AssignmentRecord(
            UUID id,
            UUID functionId,
            String functionCode,
            String functionName,
            AuthorizationScope scope,
            Instant validFrom,
            Instant validTo,
            Instant revokedAt,
            long revision) {}

    record PolicyRecord(
            UUID id,
            UUID leftFunctionId,
            UUID rightFunctionId,
            boolean active,
            Instant validFrom,
            Instant validTo) {}
}
