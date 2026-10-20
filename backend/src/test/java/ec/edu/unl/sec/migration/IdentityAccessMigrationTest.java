package ec.edu.unl.sec.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.edu.unl.sec.support.PostgresTestSupport;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class IdentityAccessMigrationTest extends PostgresTestSupport {
    @Test
    void createsIdentitySchemaWithChecksAndAuditAppendOnlyGuard() {
        Assumptions.assumeTrue(
                Boolean.parseBoolean(System.getenv().getOrDefault("SEC_RUN_DATABASE_TESTS", "false")),
                "Set SEC_RUN_DATABASE_TESTS=true only for a disposable test database");
        Flyway flyway = Flyway.configure()
                .dataSource(jdbcUrl(), username(), password())
                .cleanDisabled(false)
                .load();
        flyway.clean();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(3);

        JdbcTemplate jdbc =
                new JdbcTemplate(new DriverManagerDataSource(jdbcUrl(), username(), password()));
        Integer tables = jdbc.queryForObject(
                "select count(*) from information_schema.tables where table_name in "
                        + "('access_identity','access_credential','access_session','authentication_attempt',"
                        + "'process_function','function_operation','functional_assignment','conflict_policy',"
                        + "'conflict_exception')",
                Integer.class);
        assertThat(tables).isEqualTo(9);

        UUID identity = UUID.randomUUID();
        jdbc.update(
                "insert into access_identity(id,login_normalized,display_name,identity_type,active,revision,created_at) values(?,?,?,?,true,0,now())",
                identity,
                "synthetic.user",
                "Synthetic User",
                "HUMAN");
        assertThatThrownBy(() -> jdbc.update(
                        "insert into access_identity(id,login_normalized,display_name,identity_type,active,revision,created_at) values(?,?,?,?,true,0,now())",
                        UUID.randomUUID(),
                        "synthetic.user",
                        "Other",
                        "HUMAN"))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);

        UUID event = UUID.randomUUID();
        jdbc.update(
                "insert into audit_event(id,occurred_at,actor_user_id,correlation_id,action,object_type,object_id,outcome) values(?,now(),?,?,?,?,?,?)",
                event,
                identity,
                UUID.randomUUID(),
                "SESSION_LOGIN",
                "ACCESS_SESSION",
                identity,
                "SUCCESS");
        assertThatThrownBy(() -> jdbc.update("update audit_event set outcome='FAILED' where id=?", event))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
}
