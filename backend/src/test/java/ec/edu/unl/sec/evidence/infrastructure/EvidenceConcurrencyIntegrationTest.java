package ec.edu.unl.sec.evidence.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.unl.sec.evidence.application.CreateEvidenceSuccessorService;
import ec.edu.unl.sec.evidence.application.EvidenceDraftRepository;
import ec.edu.unl.sec.evidence.application.EvidenceDraftService;
import ec.edu.unl.sec.evidence.application.EvidenceGovernanceRepository;
import ec.edu.unl.sec.evidence.application.EvidenceSourceRepository;
import ec.edu.unl.sec.support.PostgresTestSupport;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@EnabledIfEnvironmentVariable(named = "SEC_RUN_FILE_DATABASE_TESTS", matches = "true")
class EvidenceConcurrencyIntegrationTest {
    @Test
    void preventsDuplicateSuccessorUnderRace() throws Exception {
        String schema = "conc_" + UUID.randomUUID().toString().replace("-", "");
        String url = PostgresTestSupport.jdbcUrl() + (PostgresTestSupport.jdbcUrl().contains("?") ? "&" : "?") + "currentSchema=" + schema;
        SingleConnectionDataSource source = new SingleConnectionDataSource(url, PostgresTestSupport.username(), PostgresTestSupport.password(), true);
        try {
            Flyway.configure().dataSource(source).schemas(schema).defaultSchema(schema).load().migrate();
            var jdbc = new JdbcTemplate(source);
            var json = new ObjectMapper().findAndRegisterModules();
            new ec.edu.unl.sec.selfassessment.infrastructure.SyntheticCareerData(
                    new ec.edu.unl.sec.selfassessment.infrastructure.JdbcCareerCatalogRepository(jdbc, json)).run(null);
            new ec.edu.unl.sec.identityaccess.infrastructure.SyntheticAccessData(jdbc, new BCryptPasswordEncoder(4)).run(null);
            new ec.edu.unl.sec.selfassessment.infrastructure.SyntheticPilotData(jdbc, json).run(null);

            UUID root = UUID.randomUUID();
            UUID version = UUID.randomUUID();
            UUID actor = UUID.randomUUID();
            UUID indicator = UUID.randomUUID();
            jdbc.update("insert into evidence(id,process_id,owner_id,created_at,correlation_id,latest_revision) values(?,?,?,?,?,1)",
                    root, ec.edu.unl.sec.selfassessment.infrastructure.SyntheticPilotData.PROCESS_ID,
                    actor, java.sql.Timestamp.from(Instant.parse("2026-10-22T00:00:00Z")), UUID.randomUUID());
            jdbc.update("insert into evidence_version(id,evidence_id,revision_number,status,title,description,provenance,"
                            + "covered_from,covered_to,author_id,created_at,submitted_at,lock_revision) values(?,?,1,'SUBMITTED','T','D','P','2026-01-01','2026-12-31',?,?,?,1)",
                    version, root, actor, java.sql.Timestamp.from(Instant.parse("2026-10-22T00:00:00Z")),
                    java.sql.Timestamp.from(Instant.parse("2026-10-22T01:00:00Z")));
            jdbc.update("insert into evidence_version_indicator(version_id,indicator_id,indicator_code,indicator_title,position) values(?,?,?,?,1)",
                    version, indicator, "IND-1", "Indicador", 1);
            jdbc.update("update evidence set latest_revision=1 where id=?", root);

            var drafts = org.mockito.Mockito.mock(EvidenceDraftService.class);
            var draftRepo = org.mockito.Mockito.mock(EvidenceDraftRepository.class);
            var sources = org.mockito.Mockito.mock(EvidenceSourceRepository.class);
            var governance = org.mockito.Mockito.mock(EvidenceGovernanceRepository.class);
            var audit = org.mockito.Mockito.mock(ec.edu.unl.sec.audit.application.AuditPort.class);
            var service = new CreateEvidenceSuccessorService(drafts, draftRepo, sources, governance, audit,
                    java.time.Clock.fixed(Instant.parse("2026-10-23T12:00:00Z"), java.time.ZoneOffset.UTC));
            org.mockito.Mockito.when(sources.nextFilePosition(org.mockito.ArgumentMatchers.any())).thenReturn(1);
            org.mockito.Mockito.when(sources.nextReferencePosition(org.mockito.ArgumentMatchers.any())).thenReturn(1);
            org.mockito.Mockito.when(governance.coverages(version)).thenReturn(java.util.List.of());
            org.mockito.Mockito.when(governance.structuredSnapshots(version)).thenReturn(java.util.List.of());
            org.mockito.Mockito.when(sources.references(version)).thenReturn(java.util.List.of());
            org.mockito.Mockito.when(sources.files(version)).thenReturn(java.util.List.of());

            var latch = new CountDownLatch(1);
            var successes = new AtomicInteger();
            var failures = new AtomicInteger();
            var v1 = version;
            org.mockito.Mockito.when(drafts.createSuccessorDraft(v1)).thenAnswer(inv -> {
                latch.countDown();
                latch.await();
                UUID sid = UUID.randomUUID();
                return new ec.edu.unl.sec.evidence.domain.EvidenceVersion(
                        sid, root, 2, v1,
                        ec.edu.unl.sec.evidence.domain.EvidenceVersionStatus.DRAFT,
                        "T", "D", "P",
                        java.time.LocalDate.of(2026, 1, 1), java.time.LocalDate.of(2026, 6, 30),
                        actor, Instant.parse("2026-10-23T12:00:00Z"), null, 0,
                        java.util.List.of(new ec.edu.unl.sec.evidence.domain.VersionIndicator(sid, indicator, "IND-1", "Indicador", 1)));
            });
            org.mockito.Mockito.doAnswer(inv -> {
                var s = inv.getArgument(0, ec.edu.unl.sec.evidence.domain.EvidenceVersion.class);
                try {
                    jdbc.update("insert into evidence_version(id,evidence_id,revision_number,predecessor_id,status,title,description,provenance,covered_from,covered_to,author_id,created_at,lock_revision) values(?,?,?,?,?,?,?,?,?,?,?,?,0)",
                            s.id(), root, 2, v1, "DRAFT", "T", "D", "P", java.sql.Date.valueOf("2026-01-01"), java.sql.Date.valueOf("2026-12-31"), actor, java.sql.Timestamp.from(Instant.parse("2026-10-23T12:00:00Z")));
                    jdbc.update("insert into evidence_version_indicator(version_id,indicator_id,indicator_code,indicator_title,position) values(?,?,?,?,1)",
                            s.id(), indicator, "IND-1", "Indicador", 1);
                    jdbc.update("update evidence set latest_revision=2 where id=?", root);
                    successes.incrementAndGet();
                    return null;
                } catch (Exception e) {
                    failures.incrementAndGet();
                    throw e;
                }
            }).when(draftRepo).createSuccessor(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(v1));

            Runnable r = () -> {
                try {
                    service.createSuccessor(v1);
                } catch (Exception ignored) {
                    failures.incrementAndGet();
                }
            };
            var t1 = new Thread(r);
            var t2 = new Thread(r);
            t1.start(); t2.start();
            t1.join(); t2.join();
            assertThat(successes.get()).isEqualTo(1);
        } finally {
            try {
                new JdbcTemplate(source).execute("drop schema " + schema + " cascade");
            } catch (Exception ignored) {
            }
            source.close();
        }
    }
}
