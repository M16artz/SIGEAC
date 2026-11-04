package ec.edu.unl.sec.migration;
import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.unl.sec.support.PostgresTestSupport;
import ec.edu.unl.sec.selfassessment.domain.*;
import ec.edu.unl.sec.selfassessment.infrastructure.*;
import ec.edu.unl.sec.identityaccess.infrastructure.SyntheticAccessData;
import java.time.*;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
class CareerCatalogMigrationTest extends PostgresTestSupport {
    @Test void upgradesV007PreservingUnknownReferencesAndImmutableHistory() {
        Assumptions.assumeTrue(Boolean.parseBoolean(System.getenv().getOrDefault("SEC_RUN_DATABASE_TESTS","false")));
        var source=new DriverManagerDataSource(jdbcUrl(),username(),password());
        var old=Flyway.configure().dataSource(source).target("7").cleanDisabled(false).load();old.clean();old.migrate();
        var jdbc=new JdbcTemplate(source);var json=new ObjectMapper().findAndRegisterModules();
        new SyntheticAccessData(jdbc,new BCryptPasswordEncoder(4)).run(null);new SyntheticPilotData(jdbc,json).run(null);
        assertThat(Flyway.configure().dataSource(source).load().migrate().migrationsExecuted).isEqualTo(1);
        var repository=new JdbcCareerCatalogRepository(jdbc,json);
        var migrated=repository.findCareer(SyntheticCareerData.CAREER).orElseThrow();
        assertThat(migrated.classificationPending()).isTrue();assertThat(migrated.status()).isEqualTo(Career.Status.DISABLED);
        assertThat(jdbc.queryForObject("select career_id from self_assessment_process where id=?",UUID.class,SyntheticPilotData.PROCESS_ID)).isEqualTo(migrated.id());
        assertThat(repository.careerHistory(migrated.id(),0,20).items()).hasSize(1);
        new SyntheticCareerData(repository).run(null);
        var classified=repository.findCareer(migrated.id()).orElseThrow();
        assertThat(classified.status()).isEqualTo(Career.Status.ACTIVE);
        assertThat(repository.careerHistory(migrated.id(),0,20).items()).extracting(Career::status).containsExactly(Career.Status.DISABLED,Career.Status.ACTIVE);
        assertThatThrownBy(()->jdbc.update("delete from career where id=?",classified.id())).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(()->jdbc.update("update career_history set reason='overwrite' where career_id=?",classified.id())).isInstanceOf(DataAccessException.class);
        var unit=new AcademicUnit(UUID.randomUUID(),new AcademicUnit.Data("U-MIG","Unidad"),Career.Status.ACTIVE,1,UUID.randomUUID(),Instant.now());
        repository.saveUnit(unit,-1,unit.createdBy(),"SYNTHETIC_TEST");
        var career=new Career(UUID.randomUUID(),new Career.Data("C-MIG","Nueva carrera",unit.id(),LocalDate.of(2020,1,1),null),Career.Status.ACTIVE,1,unit.createdBy(),Instant.now(),false);
        repository.saveCareer(career,-1,unit.createdBy(),"SYNTHETIC_TEST");
        assertThatThrownBy(()->repository.saveCareer(new Career(UUID.randomUUID(),career.data(),career.status(),1,career.createdBy(),career.createdAt(),false),-1,career.createdBy(),"DUPLICATE"))
                .isInstanceOf(ec.edu.unl.sec.selfassessment.application.CareerCatalogException.class).extracting("code").isEqualTo("DUPLICATE_CODE");
        assertThat(repository.careers(Set.of(career.id()),0,20,"",null,null).items()).containsExactly(career);
    }
}
