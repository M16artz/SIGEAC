package ec.edu.unl.sec.migration;

import static org.assertj.core.api.Assertions.assertThat;
import ec.edu.unl.sec.support.PostgresTestSupport;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

class EvaluationModelMigrationTest extends PostgresTestSupport {
    @Test void createsEvaluationModelSchemaFromCleanDatabase(){Assumptions.assumeTrue(Boolean.parseBoolean(System.getenv().getOrDefault("SEC_RUN_DATABASE_TESTS","false")),"requires disposable PostgreSQL");Flyway f=Flyway.configure().dataSource(jdbcUrl(),username(),password()).target("4").cleanDisabled(false).load();f.clean();assertThat(f.migrate().migrationsExecuted).isEqualTo(4);}
}
