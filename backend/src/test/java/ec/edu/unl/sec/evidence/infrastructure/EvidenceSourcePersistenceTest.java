package ec.edu.unl.sec.evidence.infrastructure;

import static org.assertj.core.api.Assertions.*;

import ec.edu.unl.sec.evidence.domain.ExternalReference;
import ec.edu.unl.sec.support.PostgresTestSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.stream.Stream;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.support.TransactionTemplate;

class EvidenceSourcePersistenceTest {
    static Stream<String> databases() {
        return Boolean.parseBoolean(System.getenv().getOrDefault("SEC_RUN_FILE_DATABASE_TESTS", "false"))
                ? Stream.of("sqlite", "postgres") : Stream.of("sqlite");
    }

    @ParameterizedTest @MethodSource("databases")
    void keepsOrderedReferencesAndRollsBackRevisionWhenAChildWriteFails(String database) {
        try (var fixture = new Fixture(database)) {
            var source = fixture.source;
            var jdbc = new JdbcTemplate(source);
            UUID root = UUID.randomUUID(), version = UUID.randomUUID();
            jdbc.update("insert into evidence(id,process_id,owner_id,created_at,correlation_id,latest_revision) values(?,?,?,?,?,1)",
                    fixture.key(root), fixture.key(ec.edu.unl.sec.selfassessment.infrastructure.SyntheticPilotData.PROCESS_ID),
                    fixture.key(UUID.randomUUID()), java.sql.Timestamp.from(java.time.Instant.parse("2026-10-22T00:00:00Z")), fixture.key(UUID.randomUUID()));
            jdbc.update("insert into evidence_version(id,evidence_id,revision_number,status,title,description,provenance,"
                            + "covered_from,covered_to,author_id,created_at) values(?,?,1,'DRAFT','T','D','P','2026-01-01','2026-12-31',?,?)",
                    fixture.key(version), fixture.key(root), fixture.key(UUID.randomUUID()), java.sql.Timestamp.from(java.time.Instant.parse("2026-10-22T00:00:00Z")));
            var repository = new JpaEvidenceSourceRepository(jdbc);
            var transaction = new TransactionTemplate(new DataSourceTransactionManager(source));
            var first = new ExternalReference(UUID.randomUUID(), version, "urn:synthetic:first", "Primera", 1);
            transaction.executeWithoutResult(status -> {
                assertThat(repository.advanceDraftRevision(version, 0)).isTrue();
                repository.addReference(first);
            });
            assertThat(repository.references(version)).containsExactly(first);
            assertThat(repository.nextReferencePosition(version)).isEqualTo(2);
            assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
                assertThat(repository.advanceDraftRevision(version, 1)).isTrue();
                repository.addReference(first);
            })).isInstanceOf(org.springframework.dao.DataAccessException.class);
            assertThat(jdbc.queryForObject("select lock_revision from evidence_version where id=?", Long.class, fixture.key(version))).isEqualTo(1);
            transaction.executeWithoutResult(status -> {
                assertThat(repository.advanceDraftRevision(version, 0)).isFalse();
                assertThat(repository.advanceDraftRevision(version, 1)).isTrue();
                repository.updateReference(new ExternalReference(first.id(), version, "urn:synthetic:updated", "Actualizada", 1));
            });
            assertThat(repository.references(version).getFirst().uri()).isEqualTo("urn:synthetic:updated");
            UUID policy = UUID.randomUUID(), upload = UUID.randomUUID();
            jdbc.update("insert into file_validation_policy values(?,?,?,?,'[\"text/plain\"]','{\"text/plain\":[\"text/plain\"]}',?,?)",
                    policy, "synthetic-v1", 26_214_400, true, true, java.sql.Timestamp.from(java.time.Instant.parse("2026-10-22T00:00:00Z")));
            var asset = new ec.edu.unl.sec.filestorage.domain.FileAsset(UUID.randomUUID(), upload, upload + ".bin", upload + ".part",
                    "synthetic.txt", "application/octet-stream", "text/plain", 0, "a".repeat(64), policy,
                    ec.edu.unl.sec.filestorage.domain.FileAsset.Status.AVAILABLE, java.time.Instant.parse("2026-10-22T00:00:00Z"));
            var assets = new ec.edu.unl.sec.filestorage.infrastructure.JpaFileAssetRepository(jdbc);
            assets.create(asset);
            var snapshot = ec.edu.unl.sec.evidence.domain.VersionFile.from(version, asset, 1);
            transaction.executeWithoutResult(status -> {
                assertThat(repository.advanceDraftRevision(version, 2)).isTrue();
                repository.addFile(snapshot);
            });
            assertThat(repository.files(version)).containsExactly(snapshot);
            transaction.executeWithoutResult(status -> repository.removeFile(version, asset.id()));
            assertThat(repository.files(version)).isEmpty();
            assertThat(assets.findById(asset.id())).contains(asset);
            transaction.executeWithoutResult(status -> repository.addFile(snapshot));
            jdbc.update("update evidence_version set status='SUBMITTED',submitted_at=? where id=?",
                    java.sql.Timestamp.from(java.time.Instant.parse("2026-10-22T00:00:00Z")), fixture.key(version));
            transaction.executeWithoutResult(status -> {
                assertThat(repository.advanceDraftRevision(version, 3)).isFalse();
                assertThatThrownBy(() -> repository.removeReference(version, first.id()))
                        .isInstanceOf(ec.edu.unl.sec.evidence.application.EvidenceProblemException.class);
                assertThatThrownBy(() -> repository.removeFile(version, asset.id()))
                        .isInstanceOf(ec.edu.unl.sec.evidence.application.EvidenceProblemException.class);
            });
            assertThat(repository.references(version)).hasSize(1);
        }
    }

    private static byte[] binary(UUID value) {
        return ByteBuffer.allocate(16).putLong(value.getMostSignificantBits()).putLong(value.getLeastSignificantBits()).array();
    }

    private static final class Fixture implements AutoCloseable {
        final String schema;
        final SingleConnectionDataSource source;
        Fixture(String database) {
            schema = database.equals("postgres") ? "sources_" + UUID.randomUUID().toString().replace("-", "") : null;
            if (schema == null) {
                source = new SingleConnectionDataSource("jdbc:sqlite::memory:", true);
                new ResourceDatabasePopulator(new ClassPathResource("db/sqlite/schema.sql")).execute(source);
                new JdbcTemplate(source).execute("PRAGMA foreign_keys=ON");
            } else {
                String url = PostgresTestSupport.jdbcUrl() + (PostgresTestSupport.jdbcUrl().contains("?") ? "&" : "?") + "currentSchema=" + schema;
                source = new SingleConnectionDataSource(url, PostgresTestSupport.username(), PostgresTestSupport.password(), true);
                Flyway.configure().dataSource(source).schemas(schema).defaultSchema(schema).load().migrate();
                var jdbc = new JdbcTemplate(source);
                var json = new ObjectMapper().findAndRegisterModules();
                new ec.edu.unl.sec.selfassessment.infrastructure.SyntheticCareerData(
                        new ec.edu.unl.sec.selfassessment.infrastructure.JdbcCareerCatalogRepository(jdbc, json)).run(null);
                new ec.edu.unl.sec.identityaccess.infrastructure.SyntheticAccessData(jdbc,
                        new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(4)).run(null);
                new ec.edu.unl.sec.selfassessment.infrastructure.SyntheticPilotData(jdbc, json).run(null);
            }
        }
        Object key(UUID value) { return schema == null ? binary(value) : value; }
        @Override public void close() {
            try { if (schema != null) new JdbcTemplate(source).execute("drop schema " + schema + " cascade"); }
            finally { source.close(); }
        }
    }
}
