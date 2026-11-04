package ec.edu.unl.sec.documentreview.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.unl.sec.documentreview.domain.DocumentReviewDecision;
import ec.edu.unl.sec.documentreview.domain.ReviewCoverageOutcome;
import ec.edu.unl.sec.identityaccess.infrastructure.SyntheticAccessData;
import ec.edu.unl.sec.selfassessment.infrastructure.SyntheticCareerData;
import ec.edu.unl.sec.selfassessment.infrastructure.SyntheticPilotData;
import ec.edu.unl.sec.support.PostgresTestSupport;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Persistence contract for decision tracking (T053/T119, AC-X06/X07):
 * decisions are append-only with per-coverage outcomes, rectifications chain
 * through predecessor ids with a single successor link per decision and the
 * inbox only offers the newest submitted version of each evidence lineage.
 */
class DocumentReviewPersistenceTest {
    private static final UUID REVIEWER = UUID.fromString("20000000-0000-0000-0000-000000000004");
    private static final Instant DECIDED_AT = Instant.parse("2026-10-23T12:00:00Z");

    static Stream<String> databases() {
        return Boolean.parseBoolean(System.getenv().getOrDefault("SEC_RUN_FILE_DATABASE_TESTS", "false"))
                ? Stream.of("sqlite", "postgres") : Stream.of("sqlite");
    }

    @ParameterizedTest
    @MethodSource("databases")
    void keepsAppendOnlyDecisionsWithDerivedOutcomesAndSingleSuccessorLinks(String database) {
        try (var fixture = new Fixture(database)) {
            var evidence = UUID.randomUUID();
            var version = UUID.randomUUID();
            seedSubmittedVersion(fixture, evidence, version, 1, null);
            var repository = new JdbcDocumentReviewRepository(fixture.jdbc());

            var outcomes = List.of(
                    new ReviewCoverageOutcome(fixture.sourceOne, ReviewCoverageOutcome.Outcome.ACCEPTED,
                            "Cobertura completa y verificada."),
                    new ReviewCoverageOutcome(fixture.sourceTwo, ReviewCoverageOutcome.Outcome.OBSERVED,
                            "Observación documental registrada para corregir."));
            var first = new DocumentReviewDecision(
                    null, version, DocumentReviewDecision.derive(outcomes),
                    "Revisión documental observada con fundamento suficiente.",
                    REVIEWER, DECIDED_AT, false, null, null, null, 0, outcomes);
            asTransaction(fixture, () -> repository.save(first));

            assertThat(repository.latest(version)).hasValueSatisfying(stored -> {
                assertThat(stored.id()).isEqualTo(first.id());
                assertThat(stored.versionId()).isEqualTo(version);
                assertThat(stored.decision()).isEqualTo(DocumentReviewDecision.Decision.OBSERVED);
                assertThat(stored.justification()).isEqualTo(first.justification());
                assertThat(stored.reviewedAt()).isEqualTo(DECIDED_AT);
                assertThat(stored.revision()).isZero();
                assertThat(stored.predecessorId()).isNull();
                assertThat(stored.successorVersionId()).isNull();
                assertThat(stored.outcomes())
                        .extracting(ReviewCoverageOutcome::requiredSourceId,
                                ReviewCoverageOutcome::outcome, ReviewCoverageOutcome::note)
                        .containsExactlyInAnyOrder(
                                org.assertj.core.groups.Tuple.tuple(fixture.sourceOne,
                                        ReviewCoverageOutcome.Outcome.ACCEPTED, "Cobertura completa y verificada."),
                                org.assertj.core.groups.Tuple.tuple(fixture.sourceTwo,
                                        ReviewCoverageOutcome.Outcome.OBSERVED,
                                        "Observación documental registrada para corregir."));
            });
            assertThat(repository.decision(first.id())).hasValueSatisfying(stored ->
                    assertThat(stored.outcomes()).hasSize(2));
            assertThat(repository.decision(UUID.randomUUID())).isEmpty();
            assertThat(repository.historyForVersion(version))
                    .extracting(DocumentReviewDecision::id)
                    .containsExactly(first.id());
            assertThat(repository.historyForVersion(UUID.randomUUID())).isEmpty();
            assertThat(repository.historyForProcess(fixture.processId))
                    .extracting(DocumentReviewDecision::id)
                    .containsExactly(first.id());

            assertThatThrownBy(() -> repository.save(new DocumentReviewDecision(
                    null, version, DocumentReviewDecision.derive(outcomes),
                    "Otra revisión con el mismo número de revisión para el guard.",
                    REVIEWER, DECIDED_AT, false, null, null, null, 0, outcomes)))
                    .isInstanceOf(DataAccessException.class);

            var rectified = first.next(
                    "Rectificación válida: la segunda cobertura permanece observada.",
                    false, null, REVIEWER, DECIDED_AT.plusSeconds(60), List.of(
                            new ReviewCoverageOutcome(fixture.sourceOne, ReviewCoverageOutcome.Outcome.ACCEPTED,
                                    "Cobertura completa y verificada."),
                            new ReviewCoverageOutcome(fixture.sourceTwo, ReviewCoverageOutcome.Outcome.OBSERVED,
                                    "Observación documental vigente para corregir.")));
            asTransaction(fixture, () -> repository.save(rectified));

            assertThat(repository.latest(version)).hasValueSatisfying(stored -> {
                assertThat(stored.revision()).isEqualTo(1);
                assertThat(stored.predecessorId()).isEqualTo(first.id());
            });
            assertThat(repository.decision(first.id())).hasValueSatisfying(stored -> {
                assertThat(stored.revision()).isZero();
                assertThat(stored.successorVersionId()).isNull();
            });
            assertThat(repository.historyForVersion(version))
                    .extracting(DocumentReviewDecision::id, DocumentReviewDecision::revision)
                    .containsExactly(org.assertj.core.groups.Tuple.tuple(first.id(), 0L),
                            org.assertj.core.groups.Tuple.tuple(rectified.id(), 1L));
            assertThat(repository.historyForEvidence(evidence))
                    .extracting(DocumentReviewDecision::id)
                    .containsExactly(first.id(), rectified.id());
            assertThat(repository.historyForProcess(fixture.processId))
                    .extracting(DocumentReviewDecision::id)
                    .containsExactly(first.id(), rectified.id());
            for (var decision : repository.historyForVersion(version)) {
                assertThat(decision.outcomes()).hasSize(2)
                        .allSatisfy(outcome -> assertThat(outcome.outcome()).isNotNull());
            }

            var successor = UUID.randomUUID();
            asTransaction(fixture, () -> repository.linkSuccessor(first.id(), successor));
            assertThat(repository.decision(first.id())).hasValueSatisfying(stored ->
                    assertThat(stored.successorVersionId()).isEqualTo(successor));
            assertThatThrownBy(() -> asTransaction(fixture,
                    () -> repository.linkSuccessor(rectified.id(), successor)))
                    .isInstanceOf(DataAccessException.class)
                    .as("cada versión correctiva queda enlazada a una sola decisión desencadenante");
            assertThat(repository.decision(rectified.id())).hasValueSatisfying(stored ->
                    assertThat(stored.successorVersionId()).isNull());
            var replaced = UUID.randomUUID();
            asTransaction(fixture, () -> repository.linkSuccessor(first.id(), replaced));
            assertThat(repository.decision(first.id())).hasValueSatisfying(stored ->
                    assertThat(stored.successorVersionId()).isEqualTo(replaced));
        }
    }

    @ParameterizedTest
    @MethodSource("databases")
    void offersOnlyTheNewestSubmittedVersionPerEvidenceOnTheInbox(String database) {
        try (var fixture = new Fixture(database)) {
            var evidence = UUID.randomUUID();
            var superseded = UUID.randomUUID();
            var latest = UUID.randomUUID();
            seedSubmittedVersion(fixture, evidence, superseded, 1, null);
            seedSubmittedVersion(fixture, evidence, latest, 2, superseded);
            fixture.update("update evidence set latest_revision=2 where id=?", fixture.key(evidence));

            var submissions = new JdbcReviewSubmissionAdapter(fixture.jdbc());

            assertThat(submissions.latestRevisionNumber(evidence)).isEqualTo(2);
            assertThat(submissions.latestRevisionNumber(UUID.randomUUID())).isZero();

            var pending = submissions.pending(fixture.processId);
            assertThat(pending).hasSize(1);
            assertSoftly(softly -> {
                softly.assertThat(pending.getFirst().versionId()).isEqualTo(latest);
                softly.assertThat(pending.getFirst().revisionNumber()).isEqualTo(2);
                softly.assertThat(pending.getFirst().evidenceId()).isEqualTo(evidence);
                softly.assertThat(pending.getFirst().careerId())
                        .isEqualTo(fixture.schema == null ? null : SyntheticAccessData.PILOT_CAREER);
                softly.assertThat(pending.getFirst().coveredRequiredSourceIds())
                        .containsExactlyInAnyOrder(fixture.sourceOne, fixture.sourceTwo);
                softly.assertThat(pending.getFirst().indicatorIds()).hasSize(1);
            });
            assertThat(submissions.submission(superseded)).hasValueSatisfying(found -> {
                assertThat(found.revisionNumber()).isEqualTo(1);
                assertThat(found.evidenceId()).isEqualTo(evidence);
            });
        }
    }

    private static void seedSubmittedVersion(Fixture fixture, UUID evidence, UUID version,
            int revision, UUID predecessor) {
        if (revision == 1) {
            fixture.update("insert into evidence(id,process_id,owner_id,created_at,correlation_id,latest_revision)"
                            + " values(?,?,?,?,?,1)",
                    fixture.key(evidence), fixture.key(fixture.processId), fixture.key(UUID.randomUUID()),
                    Timestamp.from(DECIDED_AT), fixture.key(UUID.randomUUID()));
        }
        fixture.update("insert into evidence_version(id,evidence_id,revision_number,predecessor_id,status,title,"
                        + "description,provenance,covered_from,covered_to,author_id,submitted_at,created_at)"
                        + " values(?,?,?,?,?,?,?,?,?,?,?,?,?)",
                fixture.key(version), fixture.key(evidence), revision,
                predecessor == null ? null : fixture.key(predecessor), "SUBMITTED",
                "Informe institucional de la carrera sintética.",
                "Descripción del informe institucional para la revisión documental.",
                "Procedencia institucional del informe.",
                "2026-01-01", "2026-06-30", fixture.key(UUID.randomUUID()),
                Timestamp.from(DECIDED_AT), Timestamp.from(DECIDED_AT));
        fixture.update("insert into evidence_version_indicator(version_id,indicator_id,indicator_code,"
                        + "indicator_title,position) values(?,?,?,?,1)",
                fixture.key(version), fixture.key(UUID.randomUUID()), "IND-1", "Indicador sintético");
        fixture.update("insert into source_coverage(id,version_id,required_source_id,coverage_note,"
                        + "declared_by,declared_at) values(?,?,?,?,?,?)",
                fixture.key(UUID.randomUUID()), fixture.key(version), fixture.key(fixture.sourceOne),
                "Cobertura completa y verificada.", fixture.key(REVIEWER), DECIDED_AT.toString());
        if (revision == 2) {
            fixture.update("insert into source_coverage(id,version_id,required_source_id,coverage_note,"
                            + "declared_by,declared_at) values(?,?,?,?,?,?)",
                    fixture.key(UUID.randomUUID()), fixture.key(version), fixture.key(fixture.sourceTwo),
                    "Cobertura completa y verificada.", fixture.key(REVIEWER), DECIDED_AT.toString());
        }
    }

    private static void asTransaction(Fixture fixture, Runnable work) {
        new TransactionTemplate(new DataSourceTransactionManager(fixture.source))
                .executeWithoutResult(status -> work.run());
    }

    private static byte[] binary(UUID value) {
        return ByteBuffer.allocate(16).putLong(value.getMostSignificantBits())
                .putLong(value.getLeastSignificantBits()).array();
    }

    private static final class Fixture implements AutoCloseable {
        final String schema;
        final SingleConnectionDataSource source;
        final JdbcTemplate jdbc;
        final UUID sourceOne;
        final UUID sourceTwo;
        final UUID processId;

        Fixture(String database) {
            if (database.equals("postgres")) {
                schema = "reviews_" + UUID.randomUUID().toString().replace("-", "");
                String url = PostgresTestSupport.jdbcUrl()
                        + (PostgresTestSupport.jdbcUrl().contains("?") ? "&" : "?") + "currentSchema=" + schema;
                source = new SingleConnectionDataSource(url, PostgresTestSupport.username(),
                        PostgresTestSupport.password(), true);
                Flyway.configure().dataSource(source).schemas(schema).defaultSchema(schema).load().migrate();
                var json = new ObjectMapper().findAndRegisterModules();
                new SyntheticCareerData(
                        new ec.edu.unl.sec.selfassessment.infrastructure.JdbcCareerCatalogRepository(jdbc(), json))
                        .run(null);
                new SyntheticAccessData(jdbc(), new BCryptPasswordEncoder(4)).run(null);
                new SyntheticPilotData(jdbc(), json).run(null);
                sourceOne = SyntheticPilotData.INDICATOR_A_ID;
                sourceTwo = SyntheticPilotData.INDICATOR_B_ID;
                processId = SyntheticPilotData.PROCESS_ID;
            } else {
                schema = null;
                source = new SingleConnectionDataSource("jdbc:sqlite::memory:", true);
                new ResourceDatabasePopulator(new ClassPathResource("db/sqlite/schema.sql")).execute(source);
                jdbc().execute("PRAGMA foreign_keys=ON");
                sourceOne = UUID.randomUUID();
                sourceTwo = UUID.randomUUID();
                processId = UUID.randomUUID();
            }
            jdbc = jdbc();
        }

        JdbcTemplate jdbc() {
            return new JdbcTemplate(source);
        }

        Object key(UUID value) {
            return schema == null ? binary(value) : value;
        }

        void update(String sql, Object... args) {
            jdbc.update(sql, args);
        }

        @Override
        public void close() {
            try {
                if (schema != null) jdbc.execute("drop schema " + schema + " cascade");
            } finally {
                source.close();
            }
        }
    }
}
