package ec.edu.unl.sec.reporting.infrastructure;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("sqlite-test")
@Order(-1)
public class SqliteDocumentarySnapshotGuards implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    public SqliteDocumentarySnapshotGuards(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    @Override
    public void run(ApplicationArguments args) {
        // Spring's SQL splitter cannot parse semicolons inside SQLite trigger bodies.
        for (String table:new String[]{"documentary_snapshot","documentary_closure"})
            for (String operation : new String[]{"update","delete"})
                jdbc.execute("create trigger if not exists "+table+"_no_"+operation+" before "+operation
                        +" on "+table+" begin select raise(abort,'DOCUMENTARY_SNAPSHOT_IMMUTABLE'); end");
    }
}
