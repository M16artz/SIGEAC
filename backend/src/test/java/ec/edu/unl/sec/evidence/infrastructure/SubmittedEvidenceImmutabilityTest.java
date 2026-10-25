package ec.edu.unl.sec.evidence.infrastructure;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.unl.sec.evidence.domain.ExternalReference;
import ec.edu.unl.sec.evidence.domain.VersionFile;
import ec.edu.unl.sec.filestorage.domain.FileAsset;
import ec.edu.unl.sec.filestorage.infrastructure.JpaFileAssetRepository;
import ec.edu.unl.sec.support.PostgresTestSupport;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.support.TransactionTemplate;

@EnabledIfEnvironmentVariable(named = "SEC_RUN_FILE_DATABASE_TESTS", matches = "true")
class SubmittedEvidenceImmutabilityTest {
    @Test
    void submittedVersionsAndChildrenCannotBeMutated() {
        String schema = "imm_" + UUID.randomUUID().toString().replace("-", "");
        String url = PostgresTestSupport.jdbcUrl() + (PostgresTestSupport.jdbcUrl().contains("?") ? "&" : "?") + "currentSchema=" + schema;
        SingleConnectionDataSource source = new SingleConnectionDataSource(url, PostgresTestSupport.username(), PostgresTestSupport.password(), true);
        try {
            Flyway.configure().dataSource(source).schemas(schema).defaultSchema(schema).load().migrate();
            var jdbc = new JdbcTemplate(source);
            var json = new ObjectMapper().findAndRegisterModules();
            new ec.edu.unl.sec.selfassessment.infrastructure.SyntheticCareerData(
                    new ec.edu.unl.sec.selfassessment.infrastructure.JdbcCareerCatalogRepository(jdbc, json)).run(null);
            new ec.edu.unl.sec.identityaccess.infrastructure.SyntheticAccessData(jdbc,
                    new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(4)).run(null);
            new ec.edu.unl.sec.selfassessment.infrastructure.SyntheticPilotData(jdbc, json).run(null);

            UUID root = UUID.randomUUID();
            UUID version = UUID.randomUUID();
            UUID indicator = UUID.randomUUID();
            UUID policy = UUID.randomUUID();
            UUID upload = UUID.randomUUID();
            UUID fileId = UUID.randomUUID();
            UUID refId = UUID.randomUUID();

            jdbc.update("insert into evidence(id,process_id,owner_id,created_at,correlation_id,latest_revision) values(?,?,?,?,?,1)",
                    root, ec.edu.unl.sec.selfassessment.infrastructure.SyntheticPilotData.PROCESS_ID,
                    UUID.randomUUID(), java.sql.Timestamp.from(Instant.parse("2026-10-22T00:00:00Z")), UUID.randomUUID());
            jdbc.update("insert into evidence_version(id,evidence_id,revision_number,status,title,description,provenance,"
                            + "covered_from,covered_to,author_id,created_at) values(?,?,1,'DRAFT','T','D','P','2026-01-01','2026-12-31',?,?)",
                    version, root, UUID.randomUUID(), java.sql.Timestamp.from(Instant.parse("2026-10-22T00:00:00Z")));
            jdbc.update("insert into evidence_version_indicator(version_id,indicator_id,indicator_code,indicator_title,position) values(?,?,?,?,1)",
                    version, indicator, "IND-1", "Indicador", 1);
            jdbc.update("insert into file_validation_policy values(?,?,?,?,'[\"text/plain\"]','{\"text/plain\":[\"text/plain\"]}',?,?)",
                    policy, "synthetic-v1", 26_214_400, true, true, java.sql.Timestamp.from(Instant.parse("2026-10-22T00:00:00Z")));
            var asset = new FileAsset(fileId, upload, upload + ".bin", upload + ".part",
                    "synthetic.txt", "application/octet-stream", "text/plain", 9, "a".repeat(64), policy,
                    FileAsset.Status.AVAILABLE, Instant.parse("2026-10-22T00:00:00Z"));
            new JpaFileAssetRepository(jdbc).create(asset);
            jdbc.update("insert into evidence_version_file(version_id,file_id,original_name,declared_media_type,detected_media_type,size_bytes,sha256,position) values(?,?,?,?,?,?,?,1)",
                    version, fileId, "synthetic.txt", "text/plain", "text/plain", 9, "a".repeat(64));
            jdbc.update("insert into evidence_external_reference(id,version_id,uri,description,position) values(?,?,?,?,1)",
                    refId, version, "urn:synthetic:x", "desc", 1);

            jdbc.update("update evidence_version set status='SUBMITTED',submitted_at=? where id=?",
                    java.sql.Timestamp.from(Instant.parse("2026-10-22T01:00:00Z")), version);

            assertThatThrownBy(() -> jdbc.update("update evidence_version set title='X' where id=?", version))
                    .isInstanceOf(DataAccessException.class);
            assertThatThrownBy(() -> jdbc.update("delete from evidence_version where id=?", version))
                    .isInstanceOf(DataAccessException.class);
            assertThatThrownBy(() -> jdbc.update("update evidence_version_indicator set indicator_title='X' where version_id=?", version))
                    .isInstanceOf(DataAccessException.class);
            assertThatThrownBy(() -> jdbc.update("delete from evidence_version_indicator where version_id=?", version))
                    .isInstanceOf(DataAccessException.class);
            assertThatThrownBy(() -> jdbc.update("update evidence_version_file set position=2 where version_id=?", version))
                    .isInstanceOf(DataAccessException.class);
            assertThatThrownBy(() -> jdbc.update("delete from evidence_version_file where version_id=?", version))
                    .isInstanceOf(DataAccessException.class);
            assertThatThrownBy(() -> jdbc.update("update evidence_external_reference set uri='urn:synthetic:y' where id=?", refId))
                    .isInstanceOf(DataAccessException.class);
            assertThatThrownBy(() -> jdbc.update("delete from evidence_external_reference where id=?", refId))
                    .isInstanceOf(DataAccessException.class);

            // draft still mutable
            UUID draftRoot = UUID.randomUUID();
            UUID draftVersion = UUID.randomUUID();
            jdbc.update("insert into evidence(id,process_id,owner_id,created_at,correlation_id,latest_revision) values(?,?,?,?,?,1)",
                    draftRoot, ec.edu.unl.sec.selfassessment.infrastructure.SyntheticPilotData.PROCESS_ID,
                    UUID.randomUUID(), java.sql.Timestamp.from(Instant.parse("2026-10-22T00:00:00Z")), UUID.randomUUID());
            jdbc.update("insert into evidence_version(id,evidence_id,revision_number,status,title,description,provenance,"
                            + "covered_from,covered_to,author_id,created_at) values(?,?,1,'DRAFT','T','D','P','2026-01-01','2026-12-31',?,?)",
                    draftVersion, draftRoot, UUID.randomUUID(), java.sql.Timestamp.from(Instant.parse("2026-10-22T00:00:00Z")));
            jdbc.update("update evidence_version set title='T2' where id=?", draftVersion);
        } finally {
            try {
                new JdbcTemplate(source).execute("drop schema " + schema + " cascade");
            } catch (Exception ignored) {
            }
            source.close();
        }
    }
}
