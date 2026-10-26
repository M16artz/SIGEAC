package ec.edu.unl.sec.identityaccess.infrastructure;

import ec.edu.unl.sec.identityaccess.application.IdentityAccessRepository;
import ec.edu.unl.sec.identityaccess.domain.*;
import ec.edu.unl.sec.shared.infrastructure.JdbcValueSupport;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JpaIdentityAccessRepository implements IdentityAccessRepository {
    private final JdbcTemplate jdbc;

    public JpaIdentityAccessRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<LoginRecord> findLogin(String login) {
        return jdbc.query(
                        "select i.id,i.display_name,i.identity_type,i.active,c.bcrypt_hash "
                                + "from access_identity i join access_credential c on c.identity_id=i.id and c.active "
                                + "where i.login_normalized=?",
                        (r, n) -> new LoginRecord(
                                uuid(r, 1),
                                r.getString(2),
                                "TECHNICAL".equals(r.getString(3)),
                                r.getBoolean(4),
                                r.getString(5)),
                        login)
                .stream()
                .findFirst();
    }

    @Override
    public long countFailures(String key, Instant since) {
        Long n = jdbc.queryForObject(
                "select count(*) from authentication_attempt where subject_key=? and occurred_at>=? and not success",
                Long.class,
                key,
                Timestamp.from(since));
        return n == null ? 0 : n;
    }

    @Override
    public void recordAttempt(String subject, String source, Instant at, boolean ok, UUID correlation) {
        jdbc.update(
                "insert into authentication_attempt(id,subject_key,source_key,occurred_at,success,correlation_id) values(?,?,?,?,?,?)",
                UUID.randomUUID(),
                subject,
                source,
                Timestamp.from(at),
                ok,
                correlation);
    }

    @Override
    public void saveSession(AccessSession s) {
        jdbc.update(
                "insert into access_session(id,identity_id,token_hash,csrf_token_hash,created_at,last_activity_at,expires_at,revoked_at) values(?,?,?,?,?,?,?,?)",
                s.id(),
                s.identityId(),
                s.tokenHash(),
                s.csrfHash(),
                Timestamp.from(s.createdAt()),
                Timestamp.from(s.lastActivityAt()),
                Timestamp.from(s.expiresAt()),
                s.revokedAt());
    }

    @Override
    public Optional<SessionIdentity> findSession(String hash) {
        return jdbc.query(
                        "select s.id,i.id,i.display_name,i.identity_type,i.active,s.last_activity_at,s.expires_at,s.revoked_at,s.csrf_token_hash "
                                + "from access_session s join access_identity i on i.id=s.identity_id where s.token_hash=?",
                        (r, n) -> new SessionIdentity(
                                uuid(r, 1),
                                uuid(r, 2),
                                r.getString(3),
                                "TECHNICAL".equals(r.getString(4)),
                                r.getBoolean(5),
                                JdbcValueSupport.instant(r, 6),
                                JdbcValueSupport.instant(r, 7),
                                JdbcValueSupport.instant(r, 8),
                                r.getString(9)),
                        hash)
                .stream()
                .findFirst();
    }

    @Override
    public void touchSession(UUID id, Instant at, Instant expires) {
        jdbc.update(
                "update access_session set last_activity_at=?,expires_at=? where id=? and revoked_at is null",
                Timestamp.from(at),
                Timestamp.from(expires),
                id);
    }

    @Override
    public void revokeSession(String hash, Instant at) {
        jdbc.update(
                "update access_session set revoked_at=? where token_hash=? and revoked_at is null",
                Timestamp.from(at),
                hash);
    }

    @Override
    public List<FunctionalAssignment> activeAssignments(UUID identityId, Instant at) {
        return jdbc.query(
                "select id,identity_id,function_id,career_id,process_id,indicator_id,valid_from,valid_to,granted_by,revoked_at,revision "
                        + "from functional_assignment where identity_id=? and revoked_at is null and valid_from<=? and valid_to>?",
                (r, n) -> assignment(r),
                identityId,
                Timestamp.from(at),
                Timestamp.from(at));
    }

    @Override
    public List<AssignmentRecord> assignmentRecords(UUID identityId, Instant at) {
        return jdbc.query(
                "select a.id,a.function_id,f.code,f.name,a.career_id,a.process_id,a.indicator_id,a.valid_from,a.valid_to,a.revoked_at,a.revision "
                        + "from functional_assignment a join process_function f on f.id=a.function_id "
                        + "where a.identity_id=? and a.revoked_at is null and a.valid_from<=? and a.valid_to>? order by f.code",
                (r, n) -> new AssignmentRecord(
                        uuid(r, 1),
                        uuid(r, 2),
                        r.getString(3),
                        r.getString(4),
                        new AuthorizationScope(
                                uuid(r, 5), uuid(r, 6), uuid(r, 7)),
                        JdbcValueSupport.instant(r, 8),
                        JdbcValueSupport.instant(r, 9),
                        JdbcValueSupport.instant(r, 10),
                        r.getLong(11)),
                identityId,
                Timestamp.from(at),
                Timestamp.from(at));
    }

    @Override
    public boolean functionAllows(UUID functionId, OperationCode op) {
        Integer n = jdbc.queryForObject(
                "select count(*) from process_function f join function_operation o on o.function_id=f.id "
                        + "where f.id=? and f.active and o.operation_code=?",
                Integer.class,
                functionId,
                op.value());
        return n != null && n > 0;
    }

    @Override
    public boolean hasOperation(UUID identityId, OperationCode op, Instant at) {
        Integer n = jdbc.queryForObject(
                "select count(*) from functional_assignment a "
                        + "join process_function f on f.id=a.function_id and f.active "
                        + "join function_operation o on o.function_id=f.id and o.operation_code=? "
                        + "where a.identity_id=? and a.revoked_at is null and a.valid_from<=? and a.valid_to>?",
                Integer.class,
                op.value(),
                identityId,
                Timestamp.from(at),
                Timestamp.from(at));
        return n != null && n > 0;
    }

    @Override
    public boolean hasUnexcusedConflict(UUID identityId, UUID functionId, AuthorizationScope scope, Instant at) {
        Integer n = jdbc.queryForObject(
                "select count(*) from conflict_policy p "
                        + "join functional_assignment a on a.identity_id=? and a.revoked_at is null "
                        + "and a.valid_from<=? and a.valid_to>? and a.career_id=? "
                        + "where p.active and p.valid_from<=? and p.valid_to>? "
                        + "and ((?=p.left_function_id and a.function_id=p.right_function_id) "
                        + "or (?=p.right_function_id and a.function_id=p.left_function_id)) "
                        + "and not exists (select 1 from conflict_exception e where e.policy_id=p.id and e.identity_id=? "
                        + "and e.revoked_at is null and e.valid_from<=? and e.valid_to>? and e.career_id=? "
                        + "and (e.process_id is null or e.process_id is not distinct from ?) "
                        + "and (e.indicator_id is null or e.indicator_id is not distinct from ?))",
                Integer.class,
                identityId,
                Timestamp.from(at),
                Timestamp.from(at),
                scope.careerId(),
                Timestamp.from(at),
                Timestamp.from(at),
                functionId,
                functionId,
                identityId,
                Timestamp.from(at),
                Timestamp.from(at),
                scope.careerId(),
                scope.processId(),
                scope.indicatorId());
        return n != null && n > 0;
    }

    @Override
    public UUID createIdentity(AccessIdentity i, String hash) {
        jdbc.update(
                "insert into access_identity(id,login_normalized,display_name,identity_type,active,revision,created_at) values(?,?,?,?,?,?,?)",
                i.id(),
                i.login(),
                i.displayName(),
                i.type().name(),
                i.active(),
                i.revision(),
                Timestamp.from(i.createdAt()));
        jdbc.update(
                "insert into access_credential(id,identity_id,bcrypt_hash,active,created_at) values(?,?,?,?,?)",
                UUID.randomUUID(),
                i.id(),
                hash,
                true,
                Timestamp.from(i.createdAt()));
        return i.id();
    }

    @Override
    public void deactivateIdentity(UUID id, long revision, Instant at) {
        int n = jdbc.update(
                "update access_identity set active=false,deactivated_at=?,revision=revision+1 where id=? and revision=?",
                Timestamp.from(at),
                id,
                revision);
        if (n != 1) throw new IllegalStateException("STALE_IDENTITY");
        jdbc.update(
                "update access_session set revoked_at=? where identity_id=? and revoked_at is null",
                Timestamp.from(at),
                id);
    }

    @Override
    public void activateIdentity(UUID id, long revision) {
        int n = jdbc.update(
                "update access_identity set active=true,deactivated_at=null,revision=revision+1 where id=? and revision=?",
                id,
                revision);
        if (n != 1) throw new IllegalStateException("STALE_IDENTITY");
    }

    @Override
    public UUID createAssignment(FunctionalAssignment a) {
        jdbc.update(
                "insert into functional_assignment(id,identity_id,function_id,career_id,process_id,indicator_id,valid_from,valid_to,granted_by,granted_at,revision) values(?,?,?,?,?,?,?,?,?,?,?)",
                a.id(),
                a.identityId(),
                a.functionId(),
                a.scope().careerId(),
                a.scope().processId(),
                a.scope().indicatorId(),
                Timestamp.from(a.validFrom()),
                Timestamp.from(a.validTo()),
                a.grantedBy(),
                Timestamp.from(Instant.now()),
                a.revision());
        return a.id();
    }

    @Override
    public Optional<FunctionalAssignment> findAssignment(UUID assignmentId) {
        return jdbc.query("select id,identity_id,function_id,career_id,process_id,indicator_id,valid_from,valid_to,"
                        + "granted_by,revoked_at,revision from functional_assignment where id=?",
                (row, index) -> assignment(row), assignmentId).stream().findFirst();
    }

    @Override
    public void revokeAssignment(UUID assignmentId, long expectedRevision, Instant at) {
        int changed = jdbc.update("update functional_assignment set revoked_at=?,revision=revision+1"
                        + " where id=? and revision=? and revoked_at is null",
                Timestamp.from(at), assignmentId, expectedRevision);
        if (changed != 1) throw new IllegalStateException("STALE_ASSIGNMENT");
    }

    @Override
    public Optional<UserRecord> findUser(UUID identityId) {
        return jdbc.query(
                        "select id,login_normalized,display_name,identity_type,active,revision from access_identity where id=?",
                        (r, n) -> user(r),
                        identityId)
                .stream()
                .findFirst();
    }

    @Override
    public List<UserRecord> listUsers(int offset, int limit) {
        return jdbc.query(
                "select id,login_normalized,display_name,identity_type,active,revision from access_identity "
                        + "order by created_at,id limit ? offset ?",
                (r, n) -> user(r),
                limit,
                offset);
    }

    @Override
    public long countUsers() {
        Long n = jdbc.queryForObject("select count(*) from access_identity", Long.class);
        return n == null ? 0 : n;
    }

    @Override
    public Optional<UUID> findFunctionIdByCode(String code) {
        return jdbc.query(
                        "select id from process_function where code=? and active",
                        (r, n) -> uuid(r, 1),
                        code)
                .stream()
                .findFirst();
    }

    @Override
    public boolean assignmentOverlaps(
            UUID identityId, UUID functionId, AuthorizationScope scope, Instant from, Instant to) {
        Integer n = jdbc.queryForObject(
                "select count(*) from functional_assignment where identity_id=? and function_id=? and revoked_at is null "
                        + "and career_id=? and process_id is not distinct from ? and indicator_id is not distinct from ? "
                        + "and valid_from < ? and valid_to > ?",
                Integer.class,
                identityId,
                functionId,
                scope.careerId(),
                scope.processId(),
                scope.indicatorId(),
                Timestamp.from(to),
                Timestamp.from(from));
        return n != null && n > 0;
    }

    @Override
    public Optional<UUID> findConflictingPolicy(
            UUID identityId,
            UUID newFunctionId,
            AuthorizationScope scope,
            Instant from,
            Instant to,
            Instant at) {
        return jdbc.query(
                        "select p.id from conflict_policy p "
                                + "join functional_assignment a on a.identity_id=? and a.revoked_at is null and a.career_id=? "
                                + "and a.valid_from<? and a.valid_to>? "
                                + "where p.active and p.valid_from<=? and p.valid_to>? "
                                + "and ((p.left_function_id=? and a.function_id=p.right_function_id) "
                                + "or (p.right_function_id=? and a.function_id=p.left_function_id)) "
                                + "and not exists (select 1 from conflict_exception e where e.policy_id=p.id and e.identity_id=? "
                                + "and e.revoked_at is null and e.valid_from<? and e.valid_to>? and e.career_id=? "
                                + "and (e.process_id is null or e.process_id is not distinct from ?) "
                                + "and (e.indicator_id is null or e.indicator_id is not distinct from ?)) limit 1",
                        (r, n) -> uuid(r, 1),
                        identityId,
                        scope.careerId(),
                        Timestamp.from(to),
                        Timestamp.from(from),
                        Timestamp.from(at),
                        Timestamp.from(at),
                        newFunctionId,
                        newFunctionId,
                        identityId,
                        Timestamp.from(to),
                        Timestamp.from(from),
                        scope.careerId(),
                        scope.processId(),
                        scope.indicatorId())
                .stream()
                .findFirst();
    }

    @Override
    public Optional<PolicyRecord> findPolicy(UUID policyId) {
        return jdbc.query(
                        "select id,left_function_id,right_function_id,active,valid_from,valid_to from conflict_policy where id=?",
                        (r, n) -> new PolicyRecord(
                                uuid(r, 1),
                                uuid(r, 2),
                                uuid(r, 3),
                                r.getBoolean(4),
                                JdbcValueSupport.instant(r, 5),
                                JdbcValueSupport.instant(r, 6)),
                        policyId)
                .stream()
                .findFirst();
    }

    @Override
    public Optional<UUID> findPolicyForFunctions(UUID leftFunctionId, UUID rightFunctionId, Instant at) {
        return jdbc.query(
                        "select id from conflict_policy where active and valid_from<=? and valid_to>? "
                                + "and ((left_function_id=? and right_function_id=?) or (left_function_id=? and right_function_id=?)) limit 1",
                        (r, n) -> uuid(r, 1),
                        Timestamp.from(at),
                        Timestamp.from(at),
                        leftFunctionId,
                        rightFunctionId,
                        rightFunctionId,
                        leftFunctionId)
                .stream()
                .findFirst();
    }

    @Override
    public UUID createConflictException(ConflictException e) {
        jdbc.update(
                "insert into conflict_exception(id,policy_id,identity_id,career_id,process_id,indicator_id,justification,valid_from,valid_to,approved_by,revoked_at) values(?,?,?,?,?,?,?,?,?,?,?)",
                e.id(),
                e.policyId(),
                e.identityId(),
                e.scope().careerId(),
                e.scope().processId(),
                e.scope().indicatorId(),
                e.justification().strip(),
                Timestamp.from(e.validFrom()),
                Timestamp.from(e.validTo()),
                e.approvedBy(),
                e.revokedAt());
        return e.id();
    }

    @Override
    public List<UUID> authorizedCareerIds(UUID identityId, OperationCode op, Instant at) {
        return jdbc.query(
                "select distinct a.career_id from functional_assignment a "
                        + "join process_function f on f.id=a.function_id and f.active "
                        + "join function_operation o on o.function_id=f.id and o.operation_code=? "
                        + "where a.identity_id=? and a.revoked_at is null and a.valid_from<=? and a.valid_to>?",
                (r, n) -> uuid(r, 1),
                op.value(),
                identityId,
                Timestamp.from(at),
                Timestamp.from(at));
    }

    private static UUID uuid(ResultSet r, int index) throws SQLException {
        return JdbcValueSupport.uuid(r, index);
    }

    private static UserRecord user(ResultSet r) throws SQLException {
        return new UserRecord(
                uuid(r, 1),
                r.getString(2),
                r.getString(3),
                "TECHNICAL".equals(r.getString(4)),
                r.getBoolean(5),
                r.getLong(6));
    }

    private static FunctionalAssignment assignment(ResultSet r) throws SQLException {
        return new FunctionalAssignment(
                uuid(r, 1),
                uuid(r, 2),
                uuid(r, 3),
                new AuthorizationScope(
                        uuid(r, 4), uuid(r, 5), uuid(r, 6)),
                JdbcValueSupport.instant(r, 7),
                JdbcValueSupport.instant(r, 8),
                uuid(r, 9),
                JdbcValueSupport.instant(r, 10),
                r.getLong(11));
    }
}
