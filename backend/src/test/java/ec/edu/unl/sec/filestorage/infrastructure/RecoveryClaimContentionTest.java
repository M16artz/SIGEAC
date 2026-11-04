package ec.edu.unl.sec.filestorage.infrastructure;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import ec.edu.unl.sec.filestorage.domain.FileRecoveryOperation;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.UncategorizedSQLException;

class RecoveryClaimContentionTest {
    @Test void sqliteContentionDoesNotAcquireOrCompleteWorkButOtherDatabaseErrorsPropagate() {
        var jdbc=mock(JdbcTemplate.class);var now=Instant.now();var id=UUID.randomUUID();
        var pending=new FileRecoveryOperation(id,null,FileRecoveryOperation.Operation.VERIFY,FileRecoveryOperation.Status.PENDING,
                0,now,null,null,null,UUID.randomUUID(),now,now);
        var repository=new JpaFileRecoveryRepository(jdbc) {
            @Override public Optional<FileRecoveryOperation> findById(UUID ignored) {return Optional.of(pending);}
        };
        when(jdbc.update(anyString(),any(Object[].class))).thenThrow(new UncategorizedSQLException("claim","update",
                new org.sqlite.SQLiteException("synthetic contention",org.sqlite.SQLiteErrorCode.SQLITE_LOCKED_SHAREDCACHE)));
        assertThat(repository.claim(id,"worker",now,Duration.ofMinutes(1))).isEmpty();
        var failure=new UncategorizedSQLException("claim","update",new java.sql.SQLException("synthetic other error",null,6));
        when(jdbc.update(anyString(),any(Object[].class))).thenThrow(failure);
        assertThatThrownBy(()->repository.claim(id,"worker",now,Duration.ofMinutes(1))).isSameAs(failure);
    }
}
