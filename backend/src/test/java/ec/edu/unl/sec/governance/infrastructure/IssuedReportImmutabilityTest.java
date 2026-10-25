package ec.edu.unl.sec.governance.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.unl.sec.governance.application.ReportRepository;
import ec.edu.unl.sec.governance.domain.ApprovalDecision;
import ec.edu.unl.sec.governance.domain.ObservationReply;
import ec.edu.unl.sec.governance.domain.ReportObservation;
import ec.edu.unl.sec.governance.domain.ReportSocialization;
import ec.edu.unl.sec.governance.domain.ReportVersion;
import ec.edu.unl.sec.identityaccess.infrastructure.SyntheticAccessData;
import ec.edu.unl.sec.selfassessment.infrastructure.SyntheticCareerData;
import ec.edu.unl.sec.selfassessment.infrastructure.SyntheticPilotData;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Immutability evidence of the emitted content on documental history: observations, replies,
 * socializations and decisions append events, but the emitted bytes never change.
 */
class IssuedReportImmutabilityTest {
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

    @Test void emittedContentRemainsImmutableAcrossWorkflowEvents() {
        try (var source = new SingleConnectionDataSource("jdbc:sqlite::memory:", true)) {
            var jdbc = new JdbcTemplate(source);
            jdbc.execute("pragma foreign_keys=on");
            new ResourceDatabasePopulator(new ClassPathResource("db/sqlite/schema.sql")).execute(source);
            new SqliteGovernanceGuards(jdbc).run(null);
            new SyntheticCareerData(new ec.edu.unl.sec.selfassessment.infrastructure.JdbcCareerCatalogRepository(jdbc, json)).run(null);
            new SyntheticAccessData(jdbc, new BCryptPasswordEncoder(4)).run(null);
            new SyntheticPilotData(jdbc, json).run(null);
            var repository = new JdbcReportRepository(jdbc, json);
            var actor = identity("coordinador");
            var emitted = new ReportVersion(UUID.randomUUID(), SyntheticPilotData.PROCESS_ID, new ReportVersion.Data(
                    ReportVersion.Kind.PRELIMINARY, UUID.randomUUID(), "Plantilla sintética P14 v1", true,
                    "Contenido sustentado de autoevaluación. No constituye acreditación.", null),
                    actor, java.time.Instant.now(), null, 1L, 1L, sha("preliminar"));
            repository.appendVersion(emitted);
            repository.appendObservation(new ReportObservation(UUID.randomUUID(), emitted.id(),
                    "Sección III", "Corrija la cobertura faltante de la fuente B.", actor, java.time.Instant.now()));
            repository.appendReply(new ObservationReply(UUID.randomUUID(),
                    repository.observations(emitted.id()).getFirst().id(),
                    "Respuesta sustentada de la comisión.", null, actor, java.time.Instant.now()));
            repository.appendSocialization(new ReportSocialization(UUID.randomUUID(), emitted.id(),
                    java.time.Instant.now(), List.of(UUID.randomUUID()), "fixture://acta-sintetica",
                    actor, java.time.Instant.now()));
            assertThat(repository.findVersion(emitted.id())).contains(emitted);
            assertThat(repository.observations(emitted.id())).hasSize(1);
            assertThat(repository.repliesForVersion(emitted.id())).hasSize(1);
            assertThat(repository.socializations(emitted.id())).hasSize(1);
            assertThat(repository.versions(SyntheticPilotData.PROCESS_ID, 0, 20)).containsExactly(emitted);
            assertThat(repository.latest(SyntheticPilotData.PROCESS_ID)).contains(emitted);
            assertThatThrownBy(() -> jdbc.update("update governance_report_version set payload_json='{}' where id=?", emitted.id()))
                    .isInstanceOf(DataAccessException.class);
            assertThatThrownBy(() -> jdbc.update(
                    "update governance_report_version set content_sha256=? where id=?",
                    "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", emitted.id()))
                    .isInstanceOf(DataAccessException.class);
            assertThatThrownBy(() -> jdbc.update("delete from governance_report_version where id=?", emitted.id()))
                    .isInstanceOf(DataAccessException.class);
            assertThatThrownBy(() -> jdbc.update("delete from governance_report_observation where version_id=?", emitted.id()))
                    .isInstanceOf(DataAccessException.class);
            assertThatThrownBy(() -> jdbc.update("delete from governance_report_reply", new Object[]{}))
                    .isInstanceOf(DataAccessException.class);
            try {
                var rawRows = jdbc.query("select payload_json, content_sha256 from governance_report_version where id=?",
                        (row, i) -> new String[]{row.getString(1), row.getString(2)}, emitted.id());
                var raw = json.readValue(rawRows.getFirst()[0], ReportVersion.class);
                assertThat(raw.data().content()).isEqualTo(emitted.data().content());
                assertThat(rawRows.getFirst()[1]).isEqualTo(emitted.contentSha256());
            } catch (Exception e) { throw new IllegalStateException(e); }
        }
    }

    @Test void processingTransitionsGuardRevisionsAndBlockDisplacement() {
        try (var source = new SingleConnectionDataSource("jdbc:sqlite::memory:", true)) {
            var jdbc = new JdbcTemplate(source);
            jdbc.execute("pragma foreign_keys=on");
            new ResourceDatabasePopulator(new ClassPathResource("db/sqlite/schema.sql")).execute(source);
            new SqliteGovernanceGuards(jdbc).run(null);
            new SyntheticCareerData(new ec.edu.unl.sec.selfassessment.infrastructure.JdbcCareerCatalogRepository(jdbc, json)).run(null);
            new SyntheticAccessData(jdbc, new BCryptPasswordEncoder(4)).run(null);
            new SyntheticPilotData(jdbc, json).run(null);
            var repository = new JdbcReportRepository(jdbc, json);
            var preliminary = new ReportVersion(UUID.randomUUID(), SyntheticPilotData.PROCESS_ID, new ReportVersion.Data(
                    ReportVersion.Kind.PRELIMINARY, UUID.randomUUID(), "Plantilla sintética P14 v1", true,
                    "Contenido sustentado de autoevaluación. No constituye acreditación.", null),
                    identity("coordinador"), java.time.Instant.now(), null, 1L, 1L, sha("preliminar"));
            repository.appendVersion(preliminary);
            var version = new ReportVersion(UUID.randomUUID(), SyntheticPilotData.PROCESS_ID, new ReportVersion.Data(
                    ReportVersion.Kind.FINAL, UUID.randomUUID(), "Plantilla sintética P14 v1", true,
                    "Contenido sustentado del cierre académico. No constituye acreditación.", preliminary.id()),
                    identity("coordinador"), java.time.Instant.now(), preliminary.id(), 2L, 1L, sha("final"));
            repository.appendVersion(version);
            assertThat(repository.processing(version.id())).contains(new ReportRepository.Processing("FINAL", 0L));
            assertThat(repository.advanceProcessing(version.id(), 0L, "FINAL")).isTrue();
            assertThat(repository.advanceProcessing(version.id(), 0L, "APPROVED")).isFalse();
            assertThat(repository.processing(version.id())).contains(new ReportRepository.Processing("FINAL", 1L));
            assertThat(repository.advanceProcessing(version.id(), 1L, "APPROVED")).isTrue();
            assertThat(repository.processing(version.id())).contains(new ReportRepository.Processing("APPROVED", 2L));
            assertThatThrownBy(() -> jdbc.update("update governance_report_processing set version_id=? where version_id=?",
                    UUID.randomUUID(), version.id())).isInstanceOf(DataAccessException.class);
            assertThatThrownBy(() -> jdbc.update("delete from governance_report_processing where version_id=?", version.id()))
                    .isInstanceOf(DataAccessException.class);
        }
    }

    private UUID identity(String login) {
        return UUID.nameUUIDFromBytes(("sec-unl:" + login).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private String sha(String seed) {
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of().formatHex(digest.digest(("Informe sintético: " + seed)
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
}
