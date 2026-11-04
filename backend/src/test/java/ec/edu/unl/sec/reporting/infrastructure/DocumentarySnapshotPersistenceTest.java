package ec.edu.unl.sec.reporting.infrastructure;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.unl.sec.identityaccess.infrastructure.SyntheticAccessData;
import ec.edu.unl.sec.reporting.application.ReportingProblemException;
import ec.edu.unl.sec.reporting.domain.DocumentaryProgressCalculator;
import ec.edu.unl.sec.reporting.domain.DocumentarySnapshot;
import ec.edu.unl.sec.selfassessment.infrastructure.JdbcCareerCatalogRepository;
import ec.edu.unl.sec.selfassessment.infrastructure.SyntheticCareerData;
import ec.edu.unl.sec.selfassessment.infrastructure.SyntheticPilotData;
import ec.edu.unl.sec.support.PostgresTestSupport;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class DocumentarySnapshotPersistenceTest {
    @Test
    @org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named="SEC_RUN_FILE_DATABASE_TESTS",matches="true")
    void concurrentReservationsOfTheSameRevisionCommitOnlyOnceOnPostgres() throws Exception {
        String schema="snapshot_race_"+UUID.randomUUID().toString().replace("-","");
        String url=PostgresTestSupport.jdbcUrl()+(PostgresTestSupport.jdbcUrl().contains("?") ? "&" : "?")+"currentSchema="+schema;
        var source=new org.springframework.jdbc.datasource.DriverManagerDataSource(url,PostgresTestSupport.username(),PostgresTestSupport.password());
        var jdbc=new JdbcTemplate(source);
        var workers=java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            Flyway.configure().dataSource(source).schemas(schema).defaultSchema(schema).load().migrate();
            var json=new ObjectMapper().findAndRegisterModules();
            new SyntheticCareerData(new JdbcCareerCatalogRepository(jdbc,json)).run(null);
            new SyntheticAccessData(jdbc,new BCryptPasswordEncoder(4)).run(null);
            new SyntheticPilotData(jdbc,json).run(null);
            var processes=new ec.edu.unl.sec.selfassessment.application.DocumentaryProcessService(
                    new ec.edu.unl.sec.selfassessment.infrastructure.JdbcSelfAssessmentRepository(jdbc,json));
            var before=processes.context(SyntheticPilotData.PROCESS_ID).orElseThrow();
            var ready=new java.util.concurrent.CountDownLatch(2);
            java.util.concurrent.Callable<Boolean> reserve=() -> {
                var tx=new org.springframework.transaction.support.TransactionTemplate(
                        new org.springframework.jdbc.datasource.DataSourceTransactionManager(source));
                tx.setIsolationLevel(org.springframework.transaction.TransactionDefinition.ISOLATION_SERIALIZABLE);
                try {
                    return tx.execute(status -> {
                        assertThat(processes.context(SyntheticPilotData.PROCESS_ID).orElseThrow().revision()).isEqualTo(before.revision());
                        ready.countDown();
                        try { assertThat(ready.await(10,java.util.concurrent.TimeUnit.SECONDS)).isTrue(); }
                        catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new IllegalStateException(interrupted); }
                        return processes.reserveSnapshot(SyntheticPilotData.PROCESS_ID,before.revision());
                    });
                } catch (org.springframework.dao.ConcurrencyFailureException stale) { return false; }
            };
            var first=workers.submit(reserve);
            var second=workers.submit(reserve);
            assertThat(List.of(first.get(20,java.util.concurrent.TimeUnit.SECONDS),second.get(20,java.util.concurrent.TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true,false);
            var after=processes.context(SyntheticPilotData.PROCESS_ID).orElseThrow();
            assertThat(after.revision()).isEqualTo(before.revision()+1);
            assertThat(after.state()).isEqualTo(before.state());
        } finally {
            workers.shutdownNow();
            jdbc.execute("drop schema "+schema+" cascade");
        }
    }

    static Stream<String> databases() {
        return Boolean.parseBoolean(System.getenv().getOrDefault("SEC_RUN_FILE_DATABASE_TESTS","false"))
                ? Stream.of("sqlite","postgres-clean","postgres-upgrade","postgres-upgrade16") : Stream.of("sqlite");
    }

    @ParameterizedTest
    @MethodSource("databases")
    void roundTripsExactPayloadAndEnforcesAppendOnlyHistory(String database) {
        String schema=database.startsWith("postgres") ? "documentary_"+UUID.randomUUID().toString().replace("-","") : null;
        String url=schema==null ? "jdbc:sqlite::memory:" : PostgresTestSupport.jdbcUrl()
                +(PostgresTestSupport.jdbcUrl().contains("?") ? "&" : "?")+"currentSchema="+schema;
        try (var source=schema==null ? new SingleConnectionDataSource(url,true)
                : new SingleConnectionDataSource(url,PostgresTestSupport.username(),PostgresTestSupport.password(),true)) {
            var jdbc=new JdbcTemplate(source);
            try {
                if (schema==null) {
                    new ResourceDatabasePopulator(new ClassPathResource("db/sqlite/schema.sql")).execute(source);
                    new SqliteDocumentarySnapshotGuards(jdbc).run(null);
                } else {
                    if ("postgres-upgrade".equals(database))
                        Flyway.configure().dataSource(source).schemas(schema).defaultSchema(schema).target("15").load().migrate();
                    if ("postgres-upgrade16".equals(database))
                        Flyway.configure().dataSource(source).schemas(schema).defaultSchema(schema).target("16").load().migrate();
                    var flyway=Flyway.configure().dataSource(source).schemas(schema).defaultSchema(schema).load();
                    flyway.migrate();
                    flyway.validate();
                    assertThat(flyway.info().current().getVersion()).isEqualTo(org.flywaydb.core.api.MigrationVersion.fromVersion("23"));
                }
                var json=new ObjectMapper().findAndRegisterModules();
                new SyntheticCareerData(new JdbcCareerCatalogRepository(jdbc,json)).run(null);
                new SyntheticAccessData(jdbc,new BCryptPasswordEncoder(4)).run(null);
                new SyntheticPilotData(jdbc,json).run(null);
                var codec=new JacksonDocumentarySnapshotCodec(json);
                var repository=new JdbcDocumentarySnapshotRepository(jdbc,codec);
                var snapshot=codec.seal(content(1));
                repository.save(snapshot);
                assertThat(repository.find(snapshot.content().id())).contains(snapshot);
                var closures=new JdbcDocumentaryClosureRepository(jdbc,codec);
                var closed=codec.sealClosure(new ec.edu.unl.sec.reporting.domain.DocumentaryClosure.Content(UUID.randomUUID(),
                        snapshot.content().processId(),snapshot.content().careerId(),snapshot.content().id(),snapshot.sha256(),
                        "DOCUMENTARY_CLOSED",2,"DOCUMENTARY_STRICT_V1",snapshot.content().createdBy(),snapshot.content().createdAt(),
                        "Cierre documental sintético según política estricta.",List.of()));
                closures.save(closed);
                assertThat(closures.find(closed.content().id())).contains(closed);
                assertThat(closures.list(SyntheticPilotData.PROCESS_ID,0,20)).containsExactly(closed.header());
                assertThatThrownBy(() -> jdbc.update("update documentary_closure set reason=? where id=?",
                        "No se permite sobrescribir este cierre.",closed.content().id())).isInstanceOf(DataAccessException.class);
                assertThatThrownBy(() -> jdbc.update("delete from documentary_closure where id=?",closed.content().id()))
                        .isInstanceOf(DataAccessException.class);
                assertThat(repository.header(snapshot.content().id())).contains(snapshot.header());
                assertThat(repository.list(SyntheticPilotData.PROCESS_ID,0,20)).containsExactly(snapshot.header());
                assertThat(repository.list(UUID.randomUUID(),0,20)).isEmpty();
                assertThat(repository.find(UUID.randomUUID())).isEmpty();
                assertThatThrownBy(() -> repository.save(codec.seal(content(1)))).isInstanceOf(DataAccessException.class);
                assertThatThrownBy(() -> jdbc.update("update documentary_snapshot set reason=? where id=?",
                        "No debe permitirse sobrescribir el corte.",snapshot.content().id())).isInstanceOf(DataAccessException.class);
                assertThatThrownBy(() -> jdbc.update("delete from documentary_snapshot where id=?",snapshot.content().id()))
                        .isInstanceOf(DataAccessException.class);
                assertThat(repository.find(snapshot.content().id())).contains(snapshot);
                var invalid=codec.seal(new DocumentarySnapshot.Content(UUID.randomUUID(),SyntheticPilotData.PROCESS_ID,
                        snapshot.content().careerId(),snapshot.content().modelVersionId(),"ACTIVE",2,snapshot.content().createdBy(),
                        snapshot.content().createdAt(),"Conteos contradictorios rechazados por la base.",List.of(),
                        new DocumentaryProgressCalculator.Progress(DocumentaryProgressCalculator.Status.CONFIGURED,1,2,0,"SOURCE_COVERAGE_V1"),
                        List.of(),List.of()));
                assertThatThrownBy(() -> repository.save(invalid)).isInstanceOf(DataAccessException.class);
            } finally { if (schema!=null) jdbc.execute("drop schema "+schema+" cascade"); }
        }
    }

    @Test
    void hashingIsRepeatableAndRejectsCorruptPayloads() {
        var codec=new JacksonDocumentarySnapshotCodec(new ObjectMapper().findAndRegisterModules());
        var content=content(1);
        var first=codec.seal(content);
        assertThat(codec.seal(content)).isEqualTo(first);
        assertThat(codec.read(first.payloadJson(),first.sha256())).isEqualTo(first);
        assertThatThrownBy(() -> codec.read(first.payloadJson().replace("Corte documental","Contenido alterado"),first.sha256()))
                .isInstanceOfSatisfying(ReportingProblemException.class,
                        problem -> assertThat(problem.code()).isEqualTo("SNAPSHOT_INTEGRITY_FAILED"));
    }

    private DocumentarySnapshot.Content content(long revision) {
        return new DocumentarySnapshot.Content(UUID.randomUUID(),SyntheticPilotData.PROCESS_ID,
                SyntheticAccessData.PILOT_CAREER,UUID.fromString("60000000-0000-0000-0000-000000000002"),"ACTIVE",revision,
                UUID.nameUUIDFromBytes("sec-unl:coordinador".getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                Instant.parse("2026-10-24T12:00:00.123456789Z"),"Corte documental sintético con conteos exactos.",List.of(),
                new DocumentaryProgressCalculator.Progress(DocumentaryProgressCalculator.Status.CONFIGURED,4,3,2,"SOURCE_COVERAGE_V1"),
                List.of(),List.of());
    }
}
