package ec.edu.unl.sec.migration;
import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.unl.sec.support.PostgresTestSupport;
import ec.edu.unl.sec.identityaccess.infrastructure.SyntheticAccessData;
import ec.edu.unl.sec.selfassessment.infrastructure.*;
import ec.edu.unl.sec.evaluationmodel.infrastructure.JdbcEvaluationModelRepository;
import ec.edu.unl.sec.selfassessment.domain.*;
import java.time.*;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
class ProcessPreparationMigrationTest extends PostgresTestSupport {
    @Test void upgradesDraftWithoutInventingAuthorityAndPersistsAuthorizedActivation() {
        Assumptions.assumeTrue(Boolean.parseBoolean(System.getenv().getOrDefault("SEC_RUN_DATABASE_TESTS","false")));
        var source=new DriverManagerDataSource(jdbcUrl(),username(),password());
        var before=Flyway.configure().dataSource(source).target("6").cleanDisabled(false).load(); before.clean(); before.migrate();
        var jdbc=new JdbcTemplate(source); var json=new ObjectMapper().findAndRegisterModules();
        new SyntheticAccessData(jdbc,new BCryptPasswordEncoder(4)).run(null);
        new SyntheticPilotData(jdbc,json).run(null);
        var models=new JdbcEvaluationModelRepository(jdbc,json);
        var version=models.listVersions(models.listModels().getFirst().id()).getFirst();
        assertThat(version.approverId()).isNotEqualTo(version.authorId());
        var repository=new JdbcSelfAssessmentRepository(jdbc,json);
        var process=new SelfAssessmentProcess(null,UUID.randomUUID(),"UPGRADE-PILOT","Migración sintética",LocalDate.now(),LocalDate.now().plusDays(1),version.id(),1,ProcessState.DRAFT,0,UUID.randomUUID(),Instant.now(),List.of());
        repository.save(process,-1);
        assertThat(Flyway.configure().dataSource(source).target("7").load().migrate().migrationsExecuted).isEqualTo(1);
        var upgraded=repository.find(process.id()).orElseThrow();
        assertThat(upgraded.state()).isEqualTo(ProcessState.PREPARATION);
        assertThat(upgraded.authorization()).isNull();
        var authorized=upgraded.authorize(new ProcessAuthorization(null,UUID.randomUUID(),"Designación sintética","fixture://authorization","Inicio autorizado sintético",process.createdBy(),Instant.now()));
        repository.save(authorized,0);
        var active=authorized.activate(List.of(new SelfAssessmentProcess.IndicatorSnapshot(UUID.randomUUID(),"IND-PILOT","Indicador sintético")));
        repository.save(active,1);
        assertThat(repository.find(process.id()).orElseThrow().authorization()).isEqualTo(authorized.authorization());
        assertThat(jdbc.queryForObject("select count(*) from process_indicator where process_id=?",Integer.class,process.id())).isEqualTo(1);
        repository.save(active.transition(ProcessState.SUSPENDED),2);
        assertThat(repository.find(process.id()).orElseThrow().state()).isEqualTo(ProcessState.SUSPENDED);
    }
}
