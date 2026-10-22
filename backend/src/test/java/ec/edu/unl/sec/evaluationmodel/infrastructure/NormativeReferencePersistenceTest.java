package ec.edu.unl.sec.evaluationmodel.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.unl.sec.evaluationmodel.application.ModelProblemException;
import ec.edu.unl.sec.evaluationmodel.domain.EvaluationModel;
import ec.edu.unl.sec.evaluationmodel.domain.ModelCareerApplicability;
import ec.edu.unl.sec.evaluationmodel.domain.ModelVersion;
import ec.edu.unl.sec.evaluationmodel.domain.ModelVersionStatus;
import ec.edu.unl.sec.evaluationmodel.domain.NormativeReference;
import ec.edu.unl.sec.evaluationmodel.support.ModelFixtures;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

class NormativeReferencePersistenceTest {
    @Test void persistsReferencesAndAppendsCareerDecisions() {
        try (var dataSource = new SingleConnectionDataSource("jdbc:sqlite::memory:", true)) {
            new ResourceDatabasePopulator(new ClassPathResource("db/sqlite/schema.sql")).execute(dataSource);
            var jdbc = new JdbcTemplate(dataSource);
            var json = new ObjectMapper().findAndRegisterModules();
            var models = new JdbcEvaluationModelRepository(jdbc, json);
            var references = new JpaNormativeReferenceRepository(jdbc);
            insertIdentity(jdbc, ModelFixtures.APPROVER);
            UUID careerId = UUID.randomUUID();
            jdbc.update("insert into career(id,academic_unit_id,code,name,status,valid_from,classification_pending,revision,created_by,created_at) "
                            + "values(?,?,?,?,'ACTIVE','2026-01-01',0,1,?,?)",
                    careerId, UUID.fromString("00000000-0000-0000-0000-000000000008"), "T-" + careerId,
                    "Carrera sintetica", ModelFixtures.AUTHOR, "2026-01-01T00:00:00Z");
            models.saveModel(new EvaluationModel(ModelFixtures.MODEL, "Sintetico", "", ModelFixtures.AUTHOR, Instant.now()));
            ModelVersion version = new ModelVersion(null, ModelFixtures.MODEL, 1, ModelVersionStatus.DRAFT, "Sintetica",
                    ModelFixtures.AUTHOR, null, null, 0, Instant.now(), null, ModelFixtures.criteria(), ModelFixtures.scale(),
                    ModelFixtures.formula());
            models.saveVersion(version, -1);
            assertThat(references.careerExists(careerId)).isTrue();

            NormativeReference reference = new NormativeReference(null, "Referencia sintetica", "Emisor", "2026",
                    "fixture://normative/persistence", "b".repeat(64), "3-4", "Disposicion",
                    NormativeReference.VerificationStatus.VERIFIED, ModelFixtures.APPROVER,
                    Instant.parse("2026-10-01T00:00:00Z"));
            references.saveReference(reference);
            assertThat(references.findReference(reference.id())).contains(reference);

            var decision = new ModelCareerApplicability(null, version.id(), careerId,
                    ModelCareerApplicability.Status.APPLICABLE, LocalDate.parse("2026-10-01"), null,
                    new ModelCareerApplicability.Decision("Direccion", "CL-011", "fixture://decision", "Prueba"),
                    null, 1, ModelFixtures.APPROVER, Instant.parse("2026-10-22T00:00:00Z"));
            references.appendApplicability(decision);
            assertThat(references.latestApplicability(version.id(), careerId)).contains(decision);
            assertThat(references.listApplicability(version.id())).containsExactly(decision);
            assertThatThrownBy(() -> references.appendApplicability(decision))
                    .isInstanceOf(ModelProblemException.class)
                    .extracting("code").isEqualTo("STALE_MODEL_APPLICABILITY");
        }
    }

    private static void insertIdentity(JdbcTemplate jdbc, UUID id) {
        jdbc.update("insert into access_identity(id,login_normalized,display_name,identity_type,active,revision,created_at) "
                + "values(?,?,?,?,1,0,?)", id, "user-" + id, "Usuario sintetico", "INSTITUTIONAL", "2026-01-01T00:00:00Z");
    }
}
