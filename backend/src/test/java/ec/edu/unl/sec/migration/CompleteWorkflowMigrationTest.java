package ec.edu.unl.sec.migration;

import static org.assertj.core.api.Assertions.assertThat;

import ec.edu.unl.sec.support.PostgresTestSupport;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

@EnabledIfEnvironmentVariable(named = "SEC_RUN_FILE_DATABASE_TESTS", matches = "true")
class CompleteWorkflowMigrationTest {
    @Test
    void migratesFromCleanPostgresToV026() {
        verifyMigration(null);
    }

    @ParameterizedTest(name = "preserves history and catalog from V{0} to V026")
    @ValueSource(strings = {"8", "9", "10", "11", "12", "13", "14", "15", "16", "16.1",
            "17", "18", "19", "20", "21", "22", "23", "24", "25"})
    void migratesFromEachPreviousDelivery(String previousVersion) {
        verifyMigration(previousVersion);
    }

    private void verifyMigration(String previousVersion) {
        String schema = "mig_" + UUID.randomUUID().toString().replace("-", "");
        String url = PostgresTestSupport.jdbcUrl() + (PostgresTestSupport.jdbcUrl().contains("?") ? "&" : "?") + "currentSchema=" + schema;
        SingleConnectionDataSource source = new SingleConnectionDataSource(url, PostgresTestSupport.username(), PostgresTestSupport.password(), true);
        try {
            var jdbc = new JdbcTemplate(source);
            var unitId = UUID.randomUUID();
            java.util.List<java.util.Map<String, Object>> historical = java.util.List.of();
            if (previousVersion != null) {
                Flyway.configure().dataSource(source).schemas(schema).defaultSchema(schema)
                        .target(previousVersion).load().migrate();
                jdbc.update("insert into academic_unit(id,code,name,status,revision,created_by,created_at)"
                                + " values(?,?,?,'ACTIVE',1,?,now())",
                        unitId, "SYNTHETIC-MIGRATION", "Unidad sintetica conservada", UUID.randomUUID());
                historical = jdbc.queryForList("select version,checksum,installed_rank from flyway_schema_history"
                        + " where success=true order by installed_rank");
            }
            var flyway = Flyway.configure().dataSource(source).schemas(schema).defaultSchema(schema).load();
            flyway.migrate();
            flyway.validate();
            assertThat(flyway.info().current().getVersion()).isEqualTo(MigrationVersion.fromVersion("26"));
            Integer count = jdbc.queryForObject("select count(*) from flyway_schema_history"
                    + " where success=true and version is not null", Integer.class);
            assertThat(count).isEqualTo(27); // V001–V026 plus V016.1.
            assertThat(jdbc.queryForList("select table_name from information_schema.tables where table_schema=?",
                    String.class, schema)).contains("academic_unit", "source_coverage", "document_review_decision",
                            "governance_report_version", "improvement_action", "integral_closure", "archive_record");
            if (previousVersion != null) {
                assertThat(jdbc.queryForObject("select name from academic_unit where id=?", String.class, unitId))
                        .isEqualTo("Unidad sintetica conservada");
                assertThat(jdbc.queryForList("select version,checksum,installed_rank from flyway_schema_history"
                        + " where success=true order by installed_rank").subList(0, historical.size())).isEqualTo(historical);
            }
        } finally {
            try {
                new JdbcTemplate(source).execute("drop schema " + schema + " cascade");
            } catch (Exception ignored) {
            }
            source.close();
        }
    }
}
