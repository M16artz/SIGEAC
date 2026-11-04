package ec.edu.unl.sec.evaluationmodel.infrastructure;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("sqlite-test")
@Order(-1)
public class SqliteModelGuards implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    public SqliteModelGuards(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override public void run(ApplicationArguments args) {
        // Spring's SQL script splitter cannot parse semicolons inside SQLite trigger bodies.
        jdbc.execute("""
                CREATE TRIGGER IF NOT EXISTS indicator_normative_snapshot_guard BEFORE INSERT ON indicator_normative_reference
                WHEN NOT EXISTS (
                    SELECT 1 FROM model_version v, json_each(v.snapshot_json, '$.configuration.indicators') indicator
                    WHERE v.id = NEW.model_version_id AND json_extract(indicator.value, '$.nodeId') = NEW.indicator_node_id
                )
                BEGIN
                    SELECT RAISE(ABORT, 'normative indicator must belong to the configured model version snapshot');
                END
                """);
    }
}
