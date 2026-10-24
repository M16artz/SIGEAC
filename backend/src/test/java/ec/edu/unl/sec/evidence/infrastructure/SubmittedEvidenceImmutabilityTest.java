package ec.edu.unl.sec.evidence.infrastructure;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.unl.sec.evidence.application.EvidenceProblemException;
import ec.edu.unl.sec.identityaccess.infrastructure.SyntheticAccessData;
import ec.edu.unl.sec.selfassessment.infrastructure.*;
import ec.edu.unl.sec.support.PostgresTestSupport;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class SubmittedEvidenceImmutabilityTest {
    private static final Instant NOW = Instant.parse("2026-10-24T12:00:00Z");
    private static final UUID ACTOR = UUID.nameUUIDFromBytes("sec-unl:responsable".getBytes(StandardCharsets.UTF_8));
    private static final Map<String, String> CHILDREN = Map.of(
            "evidence_version_indicator", "indicator_title", "evidence_version_file", "original_name",
            "evidence_external_reference", "description", "source_coverage", "coverage_note",
            "evidence_structured_value", "value_text");

    static Stream<String> databases() {
        return Boolean.parseBoolean(System.getenv().getOrDefault("SEC_RUN_EVIDENCE_GUARD_DATABASE_TESTS", "false"))
                ? Stream.of("sqlite", "postgres") : Stream.of("sqlite");
    }

    @ParameterizedTest @MethodSource("databases")
    void freezesVersionAndEveryChildWithoutBlockingDraftsOrAssetRecovery(String database) {
        try (var f = new Fixture(database, false)) {
            UUID submitted = f.version(), draft = f.version();
            f.children(submitted);
            f.submit(submitted);
            Object root = f.jdbc.queryForObject("select evidence_id from evidence_version where id=?", Object.class, f.key(submitted));
            assertThatThrownBy(() -> f.jdbc.update("update evidence set owner_id=? where id=?", f.key(UUID.randomUUID()), root))
                    .isInstanceOf(DataAccessException.class).hasMessageContaining("immutable");
            f.jdbc.update("update evidence set latest_revision=latest_revision+1 where id=?", root);
            assertThatThrownBy(() -> f.jdbc.update("update evidence_version set title='changed' where id=?", f.key(submitted)))
                    .isInstanceOf(DataAccessException.class).hasMessageContaining("immutable");
            assertThatThrownBy(() -> f.jdbc.update("delete from evidence_version where id=?", f.key(submitted)))
                    .isInstanceOf(DataAccessException.class).hasMessageContaining("immutable");
            for (var child : CHILDREN.entrySet()) {
                String table = child.getKey();
                assertThatThrownBy(() -> f.jdbc.update("update " + table + " set " + child.getValue() + "='changed' where version_id=?", f.key(submitted)))
                        .isInstanceOf(DataAccessException.class).hasMessageContaining("immutable");
                assertThatThrownBy(() -> f.jdbc.update("delete from " + table + " where version_id=?", f.key(submitted)))
                        .isInstanceOf(DataAccessException.class).hasMessageContaining("immutable");
                assertThatThrownBy(() -> f.jdbc.update("insert into " + table + " select * from " + table + " where version_id=?", f.key(submitted)))
                        .isInstanceOf(DataAccessException.class).hasMessageContaining("immutable");
                assertThatThrownBy(() -> f.jdbc.update("update " + table + " set version_id=? where version_id=?", f.key(draft), f.key(submitted)))
                        .isInstanceOf(DataAccessException.class).hasMessageContaining("immutable");
                assertThat(f.jdbc.queryForObject("select count(*) from " + table + " where version_id=?", Integer.class, f.key(submitted))).isEqualTo(1);
            }
            f.jdbc.update("update evidence_version set title='editable' where id=?", f.key(draft));
            f.children(draft);
            f.jdbc.update("update file_asset set status='MISSING' where id=?", f.asset);
            assertThat(f.jdbc.queryForObject("select title from evidence_version where id=?", String.class, f.key(submitted))).isEqualTo("T");
            assertThat(f.jdbc.queryForObject("select title from evidence_version where id=?", String.class, f.key(draft))).isEqualTo("editable");
        }
    }

    @ParameterizedTest @MethodSource("databases")
    void onlyTheSubmissionTransitionCanSetItsDateAndCannotChangeContent(String database) {
        try (var f = new Fixture(database, false)) {
            UUID version = f.version();
            assertThatThrownBy(() -> f.jdbc.update("update evidence_version set status='SUBMITTED',submitted_at=?,lock_revision=1,title='hijacked' where id=?",
                    Timestamp.from(NOW), f.key(version))).isInstanceOf(DataAccessException.class);
            assertThatThrownBy(() -> f.jdbc.update("update evidence_version set status='SUBMITTED' where id=?", f.key(version)))
                    .isInstanceOf(DataAccessException.class);
            f.submit(version);
            assertThatThrownBy(() -> f.jdbc.update("update evidence_version set status='DRAFT' where id=?", f.key(version)))
                    .isInstanceOf(DataAccessException.class);
            assertThat(f.jdbc.queryForObject("select title from evidence_version where id=?", String.class, f.key(version))).isEqualTo("T");
        }
    }

    @ParameterizedTest @MethodSource("databases")
    void childRevisionInvalidatesSubmissionAndLosingTheRaceDoesNotWrite(String database) {
        try (var f = new Fixture(database, false)) {
            UUID version = f.version();
            var governance = new JdbcEvidenceGovernanceRepository(f.jdbc);
            assertThat(governance.advanceDraftRevision(version, 0)).isTrue();
            assertThat(governance.advanceDraftRevision(version, 0)).isFalse();
            assertThat(governance.markSubmitted(version, NOW, 0)).isFalse();
            assertThat(governance.markSubmitted(version, NOW, 1)).isTrue();
            assertThat(governance.advanceDraftRevision(version, 2)).isFalse();
        }
    }

    @Test @EnabledIfEnvironmentVariable(named="SEC_RUN_EVIDENCE_GUARD_DATABASE_TESTS", matches="true")
    void upgradesV014WithoutChangingAlreadySubmittedContent() {
        try (var f = new Fixture("postgres", true)) {
            UUID version = f.version();
            f.children(version);
            f.submit(version);
            String before = f.jdbc.queryForObject("select to_jsonb(v)::text from evidence_version v where id=?", String.class, version);
            var flyway = f.flyway(null);
            assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
            flyway.validate();
            assertThat(f.jdbc.queryForObject("select to_jsonb(v)::text from evidence_version v where id=?", String.class, version)).isEqualTo(before);
            assertThatThrownBy(() -> f.jdbc.update("update evidence_version set description='overwrite' where id=?", version))
                    .isInstanceOf(DataAccessException.class).hasMessageContaining("immutable");
        }
    }

    @Test @EnabledIfEnvironmentVariable(named="SEC_RUN_EVIDENCE_GUARD_DATABASE_TESTS", matches="true")
    void childWriterWaitsForSubmissionAndRechecksTheCommittedState() throws Exception {
        try (var f = new Fixture("postgres", false);
             var submitter = new SingleConnectionDataSource(f.url, PostgresTestSupport.username(), PostgresTestSupport.password(), true);
             var executor = Executors.newSingleThreadExecutor()) {
            UUID version = f.version();
            f.children(version);
            var connection = submitter.getConnection();
            connection.setAutoCommit(false);
            new JdbcTemplate(submitter).update("update evidence_version set status='SUBMITTED',submitted_at=?,lock_revision=1 where id=?", Timestamp.from(NOW), version);
            var started = new CountDownLatch(1);
            var writer = executor.submit(() -> {
                try (var other = new SingleConnectionDataSource(f.url, PostgresTestSupport.username(), PostgresTestSupport.password(), true)) {
                    started.countDown();
                    return new JdbcTemplate(other).update("update source_coverage set coverage_note='racing write' where version_id=?", version);
                }
            });
            try {
                assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
                assertThatThrownBy(() -> writer.get(200, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
                connection.commit();
                assertThatThrownBy(() -> writer.get(5, TimeUnit.SECONDS)).isInstanceOf(ExecutionException.class)
                        .satisfies(error -> assertThat(error.getCause()).hasMessageContaining("immutable"));
            } finally {
                connection.rollback();
            }
            assertThat(f.jdbc.queryForObject("select coverage_note from source_coverage where version_id=?", String.class, version)).isEqualTo("Note");
        }
    }

    private static final class Fixture implements AutoCloseable {
        final String schema, url;
        final SingleConnectionDataSource dataSource;
        final JdbcTemplate jdbc;
        final UUID policy = UUID.randomUUID(), asset = UUID.randomUUID();
        Fixture(String database, boolean legacy) {
            schema = database.equals("postgres") ? "evidence_guard_" + UUID.randomUUID().toString().replace("-", "") : null;
            url = schema == null ? "jdbc:sqlite::memory:" : PostgresTestSupport.jdbcUrl()
                    + (PostgresTestSupport.jdbcUrl().contains("?") ? "&" : "?") + "currentSchema=" + schema;
            dataSource = schema == null ? new SingleConnectionDataSource(url, true)
                    : new SingleConnectionDataSource(url, PostgresTestSupport.username(), PostgresTestSupport.password(), true);
            jdbc = new JdbcTemplate(dataSource);
            if (schema == null) {
                new ResourceDatabasePopulator(new ClassPathResource("db/sqlite/schema.sql")).execute(dataSource);
                new SqliteEvidenceGuards(jdbc).run(null);
            } else flyway(legacy ? "14" : null).migrate();
            var json = new ObjectMapper().findAndRegisterModules();
            new SyntheticCareerData(new JdbcCareerCatalogRepository(jdbc, json)).run(null);
            new SyntheticAccessData(jdbc, new BCryptPasswordEncoder(4)).run(null);
            new SyntheticPilotData(jdbc, json).run(null);
            jdbc.update("insert into file_validation_policy values(?,?,26214400,?,'[\"text/plain\"]','{}',?,?)",
                    policy, "guard-" + policy, true, true, Timestamp.from(NOW));
            jdbc.update("insert into file_asset(id,upload_id,storage_key,original_name,declared_media_type,detected_media_type,size_bytes,sha256,validation_policy_id,status,created_at) values(?,?,?,'a.txt','text/plain','text/plain',1,?,?,'AVAILABLE',?)",
                    asset, UUID.randomUUID(), asset + ".bin", "a".repeat(64), policy, Timestamp.from(NOW));
        }
        Flyway flyway(String target) {
            var configuration = Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema);
            if (target != null) configuration.target(target);
            return configuration.load();
        }
        UUID version() {
            UUID root = UUID.randomUUID(), version = UUID.randomUUID();
            jdbc.update("insert into evidence(id,process_id,owner_id,created_at,correlation_id,latest_revision) values(?,?,?,?,?,1)",
                    key(root), key(SyntheticPilotData.PROCESS_ID), key(ACTOR), Timestamp.from(NOW), key(UUID.randomUUID()));
            jdbc.update("insert into evidence_version(id,evidence_id,revision_number,status,title,description,provenance,covered_from,covered_to,author_id,created_at) values(?,?,1,'DRAFT','T','D','P','2026-01-01','2026-12-31',?,?)",
                    key(version), key(root), key(ACTOR), Timestamp.from(NOW));
            return version;
        }
        void children(UUID version) {
            jdbc.update("insert into evidence_version_indicator values(?,?,'I','Indicator',1)", key(version), key(UUID.randomUUID()));
            jdbc.update("insert into evidence_version_file values(?,?,'a.txt','text/plain','text/plain',1,?,1)", key(version), asset, "a".repeat(64));
            jdbc.update("insert into evidence_external_reference values(?,?,'urn:synthetic:test','Reference',1)", UUID.randomUUID(), key(version));
            jdbc.update("insert into source_coverage values(?,?,?,'Note',?,?)", UUID.randomUUID(), key(version), UUID.randomUUID(), key(ACTOR), Timestamp.from(NOW));
            jdbc.update("insert into evidence_structured_value values(?,?,?,'STRUCTURED_DATA','NUMBER','1','unit','2026-01-01','2026-12-31',null,?,?)",
                    UUID.randomUUID(), key(version), UUID.randomUUID(), key(ACTOR), Timestamp.from(NOW));
        }
        void submit(UUID version) {
            jdbc.update("update evidence_version set status='SUBMITTED',submitted_at=?,lock_revision=lock_revision+1 where id=?", Timestamp.from(NOW), key(version));
        }
        Object key(UUID value) {
            return schema == null ? ByteBuffer.allocate(16).putLong(value.getMostSignificantBits()).putLong(value.getLeastSignificantBits()).array() : value;
        }
        @Override public void close() {
            try { if (schema != null) jdbc.execute("drop schema " + schema + " cascade"); }
            finally { dataSource.close(); }
        }
    }
}
