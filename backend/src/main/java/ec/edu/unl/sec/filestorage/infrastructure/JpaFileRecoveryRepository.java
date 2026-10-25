package ec.edu.unl.sec.filestorage.infrastructure;

import ec.edu.unl.sec.filestorage.application.FileRecoveryRepository;
import ec.edu.unl.sec.filestorage.domain.FileRecoveryOperation;
import ec.edu.unl.sec.shared.infrastructure.JdbcValueSupport;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JpaFileRecoveryRepository implements FileRecoveryRepository {
    private static final String COLUMNS = "id,file_id,operation,status,attempts,not_before,lease_owner,lease_until,"
            + "safe_reason,correlation_id,created_at,updated_at";
    private static final String DUE = "updated_at<=? and not_before<=? and (status in ('PENDING','FAILED') "
            + "or (status='LEASED' and lease_until<=?))";
    private final JdbcTemplate jdbc;

    public JpaFileRecoveryRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override public void create(FileRecoveryOperation operation) {
        if (operation.status() != FileRecoveryOperation.Status.PENDING || operation.attempts() != 0) {
            throw new IllegalArgumentException("New recovery work must be pending");
        }
        jdbc.update("insert into file_recovery_operation(" + COLUMNS + ") values(?,?,?,?,?,?,?,?,?,?,?,?)",
                operation.id(), operation.fileId(), operation.operation().name(), operation.status().name(),
                operation.attempts(), Timestamp.from(operation.notBefore()), null, null, operation.safeReason(),
                operation.correlationId(), Timestamp.from(operation.createdAt()), Timestamp.from(operation.updatedAt()));
    }

    @Override public Optional<FileRecoveryOperation> findById(UUID id) {
        return jdbc.query("select " + COLUMNS + " from file_recovery_operation where id=?",
                (row, number) -> map(row), id).stream().findFirst();
    }

    @Override public List<FileRecoveryOperation> findDue(Instant now, int limit) {
        if (limit <= 0) throw new IllegalArgumentException("Positive recovery batch size required");
        Timestamp time = Timestamp.from(now);
        return jdbc.query("select " + COLUMNS + " from file_recovery_operation where " + DUE
                        + " order by not_before,created_at,id limit ?", (row, number) -> map(row), time, time, time, limit);
    }

    @Override public Optional<FileRecoveryOperation> claim(UUID id, String owner, Instant now, Duration duration) {
        try { return claimAvailable(id,owner,now,duration); }
        catch(org.springframework.dao.DataAccessException failure) {
            // A shared-cache SQLite writer can lose a claim before the compare-and-set executes.
            for(Throwable cause=failure;cause!=null;cause=cause.getCause())
                if(cause instanceof SQLException sql && sql.getClass().getName().startsWith("org.sqlite.")
                        && (sql.getErrorCode()==5 || sql.getErrorCode()==6)) return Optional.empty();
            throw failure;
        }
    }
    private Optional<FileRecoveryOperation> claimAvailable(UUID id,String owner,Instant now,Duration duration) {
        var current = findById(id);
        if (current.isEmpty() || !current.get().isDue(now)) return Optional.empty();
        var lease = current.get().lease(owner, now, duration);
        Timestamp time = Timestamp.from(now);
        int changed = jdbc.update("update file_recovery_operation set status='LEASED',attempts=attempts+1,"
                        + "lease_owner=?,lease_until=?,safe_reason=null,updated_at=? where id=? and attempts=? and " + DUE,
                owner, Timestamp.from(lease.leaseUntil()), time, id, current.get().attempts(), time, time, time);
        return changed == 1 ? Optional.of(lease) : Optional.empty();
    }

    @Override public boolean succeed(FileRecoveryOperation lease, Instant now) {
        return lease.hasActiveLease(now) && finish(lease, lease.succeed(now), now);
    }

    @Override public boolean fail(FileRecoveryOperation lease, Instant now, Instant retryAt, String safeReason) {
        return lease.hasActiveLease(now) && finish(lease, lease.fail(now, retryAt, safeReason), now);
    }

    private boolean finish(FileRecoveryOperation lease, FileRecoveryOperation result, Instant now) {
        // Attempts fence a previous worker even when the replacement reuses its owner name.
        return jdbc.update("update file_recovery_operation set status=?,not_before=?,lease_owner=null,lease_until=null,"
                        + "safe_reason=?,updated_at=? where id=? and status='LEASED' and attempts=? and lease_owner=? "
                        + "and lease_until>? and updated_at<=?", result.status().name(), Timestamp.from(result.notBefore()),
                result.safeReason(), Timestamp.from(now), lease.id(), lease.attempts(), lease.leaseOwner(),
                Timestamp.from(now), Timestamp.from(now)) == 1;
    }

    private static FileRecoveryOperation map(ResultSet row) throws SQLException {
        return new FileRecoveryOperation(JdbcValueSupport.uuid(row, 1), JdbcValueSupport.uuid(row, 2),
                FileRecoveryOperation.Operation.valueOf(row.getString(3)), FileRecoveryOperation.Status.valueOf(row.getString(4)),
                row.getInt(5), JdbcValueSupport.instant(row, 6), row.getString(7), JdbcValueSupport.instant(row, 8),
                row.getString(9), JdbcValueSupport.uuid(row, 10), JdbcValueSupport.instant(row, 11), JdbcValueSupport.instant(row, 12));
    }
}
