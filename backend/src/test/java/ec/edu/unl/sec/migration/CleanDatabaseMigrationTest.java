package ec.edu.unl.sec.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ec.edu.unl.sec.support.PostgresTestSupport;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

class CleanDatabaseMigrationTest extends PostgresTestSupport {
    @Test
    void appliesEveryMigrationToDedicatedCleanPostgresDatabase() {
        Assumptions.assumeTrue(Boolean.parseBoolean(System.getenv().getOrDefault("SEC_RUN_DATABASE_TESTS", "false")),
                "Set SEC_RUN_DATABASE_TESTS=true only for a disposable test database");
        Flyway flyway = Flyway.configure()
                .dataSource(jdbcUrl(), username(), password())
                .cleanDisabled(false)
                .load();
        flyway.clean();
        var result = flyway.migrate();
        assertEquals(8, result.migrationsExecuted);
    }
}
