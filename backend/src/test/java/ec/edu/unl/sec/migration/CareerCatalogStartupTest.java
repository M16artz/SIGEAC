package ec.edu.unl.sec.migration;
import static org.assertj.core.api.Assertions.*;
import ec.edu.unl.sec.SecApplication;
import ec.edu.unl.sec.support.PostgresTestSupport;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
class CareerCatalogStartupTest extends PostgresTestSupport {
    @Test void bootMigratesBeforeJpaValidationAndSeedsTheCatalogOnPostgres() {
        Assumptions.assumeTrue(Boolean.parseBoolean(System.getenv().getOrDefault("SEC_RUN_DATABASE_TESTS","false")));
        var before=Flyway.configure().dataSource(jdbcUrl(),username(),password()).target("7").cleanDisabled(false).load();before.clean();before.migrate();
        try(var context=new SpringApplicationBuilder(SecApplication.class).run(
                "--spring.profiles.active=postgres-migration-test","--server.address=127.0.0.1","--server.port=0",
                "--spring.datasource.url="+jdbcUrl(),"--spring.datasource.username="+username(),"--spring.datasource.password="+password(),
                "--spring.flyway.enabled=true","--spring.jpa.hibernate.ddl-auto=validate","--spring.sql.init.mode=never",
                "--sec.demo-data=true","--sec.identity-access.bcrypt-strength=10")) {
            assertThat(context.getBean(Flyway.class).info().current().getVersion().getVersion()).isEqualTo("008");
            var jdbc=context.getBean(JdbcTemplate.class);
            assertThat(jdbc.queryForObject("select count(*) from career where code='CAREER-SYN' and status='ACTIVE'",Integer.class)).isEqualTo(1);
            assertThat(jdbc.queryForObject("select count(*) from access_identity",Integer.class)).isEqualTo(5);
        }
    }
}
