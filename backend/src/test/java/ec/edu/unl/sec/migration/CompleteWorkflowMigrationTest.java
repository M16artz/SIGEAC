package ec.edu.unl.sec.migration;

import static org.assertj.core.api.Assertions.assertThat;

import ec.edu.unl.sec.support.PostgresTestSupport;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

@EnabledIfEnvironmentVariable(named = "SEC_RUN_FILE_DATABASE_TESTS", matches = "true")
class CompleteWorkflowMigrationTest {
    @Test
    void migratesFromCleanPostgresToV020() {
        String schema = "mig_" + UUID.randomUUID().toString().replace("-", "");
        String url = PostgresTestSupport.jdbcUrl() + (PostgresTestSupport.jdbcUrl().contains("?") ? "&" : "?") + "currentSchema=" + schema;
        SingleConnectionDataSource source = new SingleConnectionDataSource(url, PostgresTestSupport.username(), PostgresTestSupport.password(), true);
        try {
            var flyway = Flyway.configure().dataSource(source).schemas(schema).defaultSchema(schema).load();
            flyway.migrate();
            var jdbc = new JdbcTemplate(source);
            Integer count = jdbc.queryForObject("select count(*) from flyway_schema_history where success=true", Integer.class);
            assertThat(count).isNotNull();
            assertThat(count).isGreaterThanOrEqualTo(16);
        } finally {
            try {
                new JdbcTemplate(source).execute("drop schema " + schema + " cascade");
            } catch (Exception ignored) {
            }
            source.close();
        }
    }
}
