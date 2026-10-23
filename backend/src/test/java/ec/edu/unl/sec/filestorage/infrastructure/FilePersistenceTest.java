package ec.edu.unl.sec.filestorage.infrastructure;

import static org.assertj.core.api.Assertions.*;

import ec.edu.unl.sec.filestorage.domain.FileAsset;
import ec.edu.unl.sec.filestorage.domain.FileRecoveryOperation;
import ec.edu.unl.sec.support.PostgresTestSupport;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

class FilePersistenceTest {
    private final Instant now = Instant.parse("2026-10-22T00:00:00Z");

    static Stream<String> databases() {
        return Boolean.parseBoolean(System.getenv().getOrDefault("SEC_RUN_FILE_DATABASE_TESTS", "false"))
                ? Stream.of("sqlite", "postgres") : Stream.of("sqlite");
    }

    @ParameterizedTest @MethodSource("databases")
    void keepsUniqueUploadAndStorageIdentityButAllowsRepeatedHashes(String database) {
        try (var fixture = new Fixture(database)) {
            var source = fixture.source;
            var jdbc = new JdbcTemplate(source);
            UUID policy = policy(jdbc);
            var repository = new JpaFileAssetRepository(jdbc);
            var asset = asset(policy);
            repository.create(asset);
            repository.create(asset(policy));
            assertThat(repository.findById(asset.id())).contains(asset);
            assertThat(repository.findByUploadId(asset.uploadId())).contains(asset);
            assertThat(repository.findById(UUID.randomUUID())).isEmpty();
            var other = asset(policy);
            assertThatThrownBy(() -> repository.create(new FileAsset(other.id(), asset.uploadId(),
                    other.storageKey(), other.stagingKey(), other.originalName(), other.declaredMediaType(),
                    other.detectedMediaType(), other.sizeBytes(), other.sha256(), policy, other.status(), now)))
                    .isInstanceOf(DataAccessException.class);
            assertThatThrownBy(() -> repository.create(new FileAsset(other.id(), other.uploadId(),
                    asset.storageKey(), other.stagingKey(), other.originalName(), other.declaredMediaType(),
                    other.detectedMediaType(), other.sizeBytes(), other.sha256(), policy, other.status(), now)))
                    .isInstanceOf(DataAccessException.class);
            assertThat(repository.changeStatus(asset.id(), FileAsset.Status.STAGED, FileAsset.Status.AVAILABLE)).isTrue();
            assertThat(repository.changeStatus(asset.id(), FileAsset.Status.STAGED, FileAsset.Status.MISSING)).isFalse();
            assertThat(repository.findById(asset.id())).contains(asset.withStatus(FileAsset.Status.AVAILABLE));
            assertThatThrownBy(() -> repository.changeStatus(asset.id(), FileAsset.Status.AVAILABLE, FileAsset.Status.STAGED))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @ParameterizedTest @MethodSource("databases")
    void persistsRetryScheduleAndRejectsCompletionsFromExpiredOrSupersededLeases(String database) {
        try (var fixture = new Fixture(database)) {
            var source = fixture.source;
            var repository = new JpaFileRecoveryRepository(new JdbcTemplate(source));
            var operation = new FileRecoveryOperation(UUID.randomUUID(), null, FileRecoveryOperation.Operation.VERIFY,
                    FileRecoveryOperation.Status.PENDING, 0, now, null, null, null, UUID.randomUUID(), now, now);
            repository.create(operation);
            assertThat(repository.findById(operation.id())).contains(operation);
            assertThat(repository.findDue(now.minusSeconds(1), 10)).isEmpty();
            assertThat(repository.findDue(now, 10)).containsExactly(operation);
            var lease = repository.claim(operation.id(), "worker", now, Duration.ofSeconds(10)).orElseThrow();
            assertThat(repository.claim(operation.id(), "competitor", now, Duration.ofSeconds(10))).isEmpty();
            assertThat(repository.succeed(lease, now.plusSeconds(10))).isFalse();
            assertThat(repository.findDue(now.plusSeconds(10), 10)).containsExactly(lease);
            var reclaimed = repository.claim(operation.id(), "worker", now.plusSeconds(10), Duration.ofSeconds(10)).orElseThrow();
            assertThat(reclaimed.attempts()).isEqualTo(2);
            assertThat(repository.succeed(lease, now.plusSeconds(11))).isFalse();
            assertThat(repository.fail(reclaimed, now.plusSeconds(11), now.plusSeconds(30), "FILE_STORAGE_FAILED")).isTrue();
            assertThat(repository.findDue(now.plusSeconds(29), 10)).isEmpty();
            var retry = repository.claim(operation.id(), "worker", now.plusSeconds(30), Duration.ofSeconds(10)).orElseThrow();
            assertThat(retry.attempts()).isEqualTo(3);
            assertThat(repository.succeed(retry, now.plusSeconds(31))).isTrue();
            assertThat(repository.succeed(retry, now.plusSeconds(32))).isFalse();
            assertThat(repository.findDue(now.plusSeconds(100), 10)).isEmpty();
            assertThat(repository.findById(operation.id()).orElseThrow().status()).isEqualTo(FileRecoveryOperation.Status.SUCCEEDED);
        }
    }

    @ParameterizedTest @MethodSource("databases")
    void onlyOneIndependentWorkerCanClaimTheSameOperation(String database) throws Exception {
        try (var fixture = new Fixture(database); var other = fixture.connection();
                var workers = Executors.newFixedThreadPool(2)) {
            var first = new JpaFileRecoveryRepository(new JdbcTemplate(fixture.source));
            var second = new JpaFileRecoveryRepository(new JdbcTemplate(other));
            UUID id = UUID.randomUUID();
            first.create(new FileRecoveryOperation(id, null, FileRecoveryOperation.Operation.VERIFY,
                    FileRecoveryOperation.Status.PENDING, 0, now, null, null, null, UUID.randomUUID(), now, now));
            var ready = new CountDownLatch(2);
            var start = new CountDownLatch(1);
            var a = workers.submit(() -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) throw new AssertionError("Workers did not start");
                return first.claim(id, "worker-a", now, Duration.ofMinutes(2));
            });
            var b = workers.submit(() -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) throw new AssertionError("Workers did not start");
                return second.claim(id, "worker-b", now, Duration.ofMinutes(2));
            });
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(Stream.of(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS))
                    .filter(java.util.Optional::isPresent).count()).isEqualTo(1);
            assertThat(first.findById(id).orElseThrow().attempts()).isEqualTo(1);
        }
    }

    @ParameterizedTest @MethodSource("databases")
    void linksRecoveryWorkToAnExistingAssetAndRejectsMissingAssets(String database) {
        try (var fixture = new Fixture(database)) {
            var jdbc = new JdbcTemplate(fixture.source);
            var asset = asset(policy(jdbc));
            new JpaFileAssetRepository(jdbc).create(asset);
            var repository = new JpaFileRecoveryRepository(jdbc);
            var operation = new FileRecoveryOperation(UUID.randomUUID(), asset.id(), FileRecoveryOperation.Operation.PROMOTE,
                    FileRecoveryOperation.Status.PENDING, 0, now, null, null, null, UUID.randomUUID(), now, now);
            repository.create(operation);
            assertThat(repository.findById(operation.id())).contains(operation);
            assertThatThrownBy(() -> repository.create(new FileRecoveryOperation(UUID.randomUUID(), UUID.randomUUID(),
                    operation.operation(), operation.status(), 0, now, null, null, null, operation.correlationId(), now, now)))
                    .isInstanceOf(DataAccessException.class);
        }
    }
    private UUID policy(JdbcTemplate jdbc) {
        UUID id = UUID.randomUUID();
        jdbc.update("insert into file_validation_policy values(?,?,?,?,'[\"text/plain\"]',"
                        + "'{\"text/plain\":[\"text/plain\"]}',?,?)", id, "synthetic-v1", 26_214_400, false, true, Timestamp.from(now));
        return id;
    }
    private FileAsset asset(UUID policy) {
        UUID upload = UUID.randomUUID();
        return new FileAsset(UUID.randomUUID(), upload, upload + ".bin", upload + ".part", "synthetic.txt",
                "application/octet-stream", "text/plain", 10, "a".repeat(64), policy, FileAsset.Status.STAGED, now);
    }

    private static final class Fixture implements AutoCloseable {
        private final String schema;
        private final String url;
        private final SingleConnectionDataSource source;

        Fixture(String database) {
            schema = database.equals("postgres") ? "file_" + UUID.randomUUID().toString().replace("-", "") : null;
            url = schema == null ? "jdbc:sqlite:file:" + UUID.randomUUID() + "?mode=memory&cache=shared"
                    : PostgresTestSupport.jdbcUrl() + (PostgresTestSupport.jdbcUrl().contains("?") ? "&" : "?") + "currentSchema=" + schema;
            source = connection();
            if (schema == null) new ResourceDatabasePopulator(new ClassPathResource("db/sqlite/schema.sql")).execute(source);
            else Flyway.configure().dataSource(source).schemas(schema).defaultSchema(schema).load().migrate();
        }

        SingleConnectionDataSource connection() {
            var connection = schema == null ? new SingleConnectionDataSource(url, true)
                    : new SingleConnectionDataSource(url, PostgresTestSupport.username(), PostgresTestSupport.password(), true);
            if (schema == null) {
                var jdbc = new JdbcTemplate(connection);
                jdbc.execute("PRAGMA foreign_keys=ON");
                jdbc.execute("PRAGMA busy_timeout=10000");
            }
            return connection;
        }

        @Override public void close() {
            try { if (schema != null) new JdbcTemplate(source).execute("drop schema " + schema + " cascade"); }
            finally { source.close(); }
        }
    }
}
