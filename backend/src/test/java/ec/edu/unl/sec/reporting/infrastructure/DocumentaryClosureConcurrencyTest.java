package ec.edu.unl.sec.reporting.infrastructure;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.unl.sec.identityaccess.application.AuthorizationPort;
import ec.edu.unl.sec.identityaccess.application.CurrentActorPort;
import ec.edu.unl.sec.identityaccess.infrastructure.SyntheticAccessData;
import ec.edu.unl.sec.selfassessment.application.DocumentaryProcessService;
import ec.edu.unl.sec.selfassessment.infrastructure.JdbcSelfAssessmentRepository;
import ec.edu.unl.sec.selfassessment.infrastructure.SyntheticPilotData;
import ec.edu.unl.sec.support.PostgresTestSupport;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

@EnabledIfEnvironmentVariable(named="SEC_RUN_FILE_DATABASE_TESTS",matches="true")
class DocumentaryClosureConcurrencyTest {
    @ParameterizedTest
    @ValueSource(strings={"REVIEW","EVIDENCE"})
    void closureLockSerializesAndRejectsLateDocumentaryWrites(String operation) throws Exception {
        String schema="closure_race_"+UUID.randomUUID().toString().replace("-","");
        String url=PostgresTestSupport.jdbcUrl()+(PostgresTestSupport.jdbcUrl().contains("?")?"&":"?")+"currentSchema="+schema;
        var source=new DriverManagerDataSource(url,PostgresTestSupport.username(),PostgresTestSupport.password());
        var jdbc=new JdbcTemplate(source);
        var workers=Executors.newFixedThreadPool(2);
        var release=new CountDownLatch(1);
        try {
            Flyway.configure().dataSource(source).schemas(schema).defaultSchema(schema).load().migrate();
            var json=new ObjectMapper().findAndRegisterModules();
            new ec.edu.unl.sec.selfassessment.infrastructure.SyntheticCareerData(
                    new ec.edu.unl.sec.selfassessment.infrastructure.JdbcCareerCatalogRepository(jdbc,json)).run(null);
            new SyntheticAccessData(jdbc,new BCryptPasswordEncoder(4)).run(null);
            new SyntheticPilotData(jdbc,json).run(null);
            var repository=new JdbcSelfAssessmentRepository(jdbc,json);
            var processes=new DocumentaryProcessService(repository);
            var processId=SyntheticPilotData.PROCESS_ID;
            var before=processes.context(processId).orElseThrow();
            var responsible=UUID.nameUUIDFromBytes("sec-unl:responsable".getBytes(StandardCharsets.UTF_8));
            var reviewer=UUID.nameUUIDFromBytes("sec-unl:revisor".getBytes(StandardCharsets.UTF_8));
            var evidenceId=UUID.randomUUID();var versionId=UUID.randomUUID();
            jdbc.update("insert into evidence(id,process_id,owner_id,created_at,correlation_id,latest_revision) values(?,?,?,?,?,1)",
                    evidenceId,processId,responsible,Timestamp.from(Instant.now()),UUID.randomUUID());
            String status="REVIEW".equals(operation)?"SUBMITTED":"DRAFT";
            jdbc.update("insert into evidence_version(id,evidence_id,revision_number,status,title,description,provenance,covered_from,covered_to,author_id,submitted_at,created_at)"
                            +" values(?,?,1,?,?,?,?,?,?,?,?,?)",versionId,evidenceId,status,"Evidencia sintética", "Descripción documental sintética.",
                    "Fixture de concurrencia",java.time.LocalDate.of(2026,1,1),java.time.LocalDate.of(2026,6,30),responsible,Timestamp.from(Instant.now()),Timestamp.from(Instant.now()));
            var required=UUID.fromString("80000000-0000-0000-0000-000000000001");
            new ec.edu.unl.sec.evidence.infrastructure.JdbcEvidenceGovernanceRepository(jdbc).saveCoverage(
                    new ec.edu.unl.sec.evidence.domain.SourceCoverage(null,versionId,required,"Cobertura sintética explícita.",responsible,Instant.now()));
            var auth=mock(AuthorizationPort.class);when(auth.isAllowed(any(),any(),any())).thenReturn(true);
            CurrentActorPort actors=() -> Optional.of(new CurrentActorPort.Actor(reviewer,false));
            var guard=new ec.edu.unl.sec.documentreview.infrastructure.DocumentReviewBoundaryAdapter(actors,auth,processes);
            var reviews=new ec.edu.unl.sec.documentreview.application.DocumentReviewService(
                    new ec.edu.unl.sec.documentreview.infrastructure.JdbcReviewSubmissionAdapter(jdbc),
                    new ec.edu.unl.sec.documentreview.infrastructure.JdbcDocumentReviewRepository(jdbc),guard,
                    mock(ec.edu.unl.sec.documentreview.application.SubmissionCorrectionPort.class),mock(ec.edu.unl.sec.audit.application.AuditPort.class));
            var evidenceGuard=new ec.edu.unl.sec.evidence.infrastructure.EvidenceBoundaryAdapter(actors,auth,
                    new ec.edu.unl.sec.selfassessment.infrastructure.ProcessCatalogAdapter(repository),
                    mock(ec.edu.unl.sec.selfassessment.application.ProcessAssignmentCatalogPort.class),processes);
            var locked=new CountDownLatch(1);var writing=new CountDownLatch(1);
            var close=workers.submit(() -> new TransactionTemplate(new DataSourceTransactionManager(source)).execute(tx -> {
                processes.lock(processId).orElseThrow();locked.countDown();
                try { assertThat(release.await(10,TimeUnit.SECONDS)).isTrue(); }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt();throw new IllegalStateException(interrupted); }
                assertThat(processes.reserveSnapshot(processId,before.revision())).isTrue();
                return processes.completeClosure(processId,before.revision()+1,reviewer,"Cierre sintético bajo bloqueo común.",UUID.randomUUID()).orElseThrow();
            }));
            assertThat(locked.await(10,TimeUnit.SECONDS)).isTrue();
            var write=workers.submit(() -> {
                try {
                    new TransactionTemplate(new DataSourceTransactionManager(source)).execute(tx -> {
                        writing.countDown();
                        if ("REVIEW".equals(operation)) reviews.decide(versionId,
                                new ec.edu.unl.sec.documentreview.application.DocumentReviewService.ReviewInput(0,false,null,
                                        "Revisión concurrente que debe esperar al cierre.",List.of(new ec.edu.unl.sec.documentreview.domain.ReviewCoverageOutcome(
                                                required,ec.edu.unl.sec.documentreview.domain.ReviewCoverageOutcome.Outcome.ACCEPTED,null))));
                        else {
                            evidenceGuard.processForWrite(processId).orElseThrow();
                            jdbc.update("update evidence_version set status='SUBMITTED' where id=?",versionId);
                        }
                        return null;
                    });
                    return "UNEXPECTED_WRITE";
                } catch (ec.edu.unl.sec.documentreview.application.DocumentReviewProblemException rejected) { return rejected.code(); }
                catch (ec.edu.unl.sec.evidence.application.EvidenceProblemException rejected) { return rejected.code(); }
            });
            assertThat(writing.await(10,TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> write.get(200,TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
            release.countDown();
            assertThat(close.get(20,TimeUnit.SECONDS).state()).isEqualTo("DOCUMENTARY_CLOSED");
            assertThat(write.get(20,TimeUnit.SECONDS)).isEqualTo("PROCESS_NOT_ACTIVE");
            assertThat(jdbc.queryForObject("select count(*) from document_review_decision where version_id=?",Long.class,versionId)).isZero();
            if ("EVIDENCE".equals(operation)) assertThat(jdbc.queryForObject("select status from evidence_version where id=?",String.class,versionId)).isEqualTo("DRAFT");
        } finally {
            release.countDown();workers.shutdownNow();workers.awaitTermination(10,TimeUnit.SECONDS);
            jdbc.execute("drop schema "+schema+" cascade");
        }
    }
}
