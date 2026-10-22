package ec.edu.unl.sec.support;

import static org.assertj.core.api.Assertions.assertThat;

import ec.edu.unl.sec.identityaccess.infrastructure.JpaIdentityAccessRepository;
import ec.edu.unl.sec.identityaccess.infrastructure.SyntheticAccessData;
import ec.edu.unl.sec.selfassessment.infrastructure.SyntheticPilotData;
import ec.edu.unl.sec.evaluationmodel.infrastructure.JdbcEvaluationModelRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class SqliteSyntheticDataTest {
    @Test
    void loadsSyntheticUsersAndFunctions() {
        try (var dataSource = new SingleConnectionDataSource("jdbc:sqlite::memory:", true)) {
            new ResourceDatabasePopulator(new ClassPathResource("db/sqlite/schema.sql")).execute(dataSource);
            var jdbc = new JdbcTemplate(dataSource);
            var passwords = new BCryptPasswordEncoder(4);
            new ec.edu.unl.sec.selfassessment.infrastructure.SyntheticCareerData(new ec.edu.unl.sec.selfassessment.infrastructure.JdbcCareerCatalogRepository(jdbc,new ObjectMapper().findAndRegisterModules())).run(null);
            new SyntheticAccessData(jdbc, passwords).run(null);
            new SyntheticPilotData(jdbc, new ObjectMapper().findAndRegisterModules()).run(null);

            assertThat(jdbc.queryForObject("select count(*) from access_identity", Integer.class)).isEqualTo(5);
            assertThat(jdbc.queryForList(
                            "select login_normalized from access_identity order by login_normalized", String.class))
                    .containsExactly("admin", "coordinador", "evaluador", "responsable", "revisor");
            assertThat(jdbc.queryForObject("select count(*) from process_function", Integer.class)).isEqualTo(6);
            var responsible = new JpaIdentityAccessRepository(jdbc).findLogin("responsable").orElseThrow();
            assertThat(passwords.matches("Synthetic-User-2026", responsible.bcryptHash())).isTrue();
            assertThat(jdbc.queryForObject("select count(*) from self_assessment_process", Integer.class)).isEqualTo(1);
            assertThat(jdbc.queryForObject("select count(*) from process_indicator", Integer.class)).isEqualTo(2);
            assertThat(jdbc.queryForObject("select count(*) from process_assignment", Integer.class)).isEqualTo(2);
            var models = new JdbcEvaluationModelRepository(jdbc, new ObjectMapper().findAndRegisterModules());
            var version = models.listVersions(models.listModels().getFirst().id()).getFirst();
            assertThat(version.criteria().getFirst().children().getFirst().children()).hasSize(2);
            assertThat(version.approverId()).isNotEqualTo(version.authorId());
            jdbc.update("update model_version set source='fixture://sqlite',snapshot_json='{}' where id=?",version.id());
            new SyntheticPilotData(jdbc,new ObjectMapper().findAndRegisterModules()).run(null);
            new SyntheticAccessData(jdbc,passwords).run(null);
            assertThat(models.findVersion(version.id()).orElseThrow().criteria()).isNotEmpty();
            assertThat(jdbc.queryForObject("select count(*) from self_assessment_process",Integer.class)).isEqualTo(1);
        }
    }
}
