package ec.edu.unl.sec.migration;
import static org.assertj.core.api.Assertions.*;
import ec.edu.unl.sec.support.PostgresTestSupport;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
class SelfAssessmentMigrationTest extends PostgresTestSupport {
    @Test void migratesCleanDatabaseThroughI2() {
        Assumptions.assumeTrue(Boolean.parseBoolean(System.getenv().getOrDefault("SEC_RUN_DATABASE_TESTS","false")));
        var flyway=Flyway.configure().dataSource(jdbcUrl(),username(),password()).target("5").cleanDisabled(false).load();
        flyway.clean(); assertThat(flyway.migrate().migrationsExecuted).isEqualTo(5);
    }
}
