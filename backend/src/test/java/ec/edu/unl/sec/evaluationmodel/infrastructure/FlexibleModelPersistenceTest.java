package ec.edu.unl.sec.evaluationmodel.infrastructure;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.unl.sec.evaluationmodel.domain.*;
import ec.edu.unl.sec.evaluationmodel.application.ModelProblemException;
import ec.edu.unl.sec.evaluationmodel.support.ModelFixtures;
import ec.edu.unl.sec.evaluationmodel.support.ConfiguredModelFixtures;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

class FlexibleModelPersistenceTest {
    @Test void roundTripsTypedNodesAndReadsUnmodifiedLegacySnapshots() throws Exception {
        try (var dataSource = new SingleConnectionDataSource("jdbc:sqlite::memory:", true)) {
            new ResourceDatabasePopulator(new ClassPathResource("db/sqlite/schema.sql")).execute(dataSource);
            var jdbc = new JdbcTemplate(dataSource);
            var json = new ObjectMapper().findAndRegisterModules();
            var repository = new JdbcEvaluationModelRepository(jdbc, json);
            repository.saveModel(new EvaluationModel(ModelFixtures.MODEL, "Sintético", "", ModelFixtures.AUTHOR, Instant.now()));
            var old = ModelFixtures.draft();
            repository.saveVersion(old, -1);
            String legacy = json.writeValueAsString(old).replace(",\"nodeType\":null", "").replace(",\"configuration\":null", "");
            jdbc.update("update model_version set snapshot_json=? where id=?", legacy, old.id());
            assertThat(repository.findVersion(old.id())).contains(old);
            var fixture = ConfiguredModelFixtures.first();
            var configured = new ModelVersion(null, old.modelId(), 3, ModelVersionStatus.DRAFT, fixture.source(),
                    old.authorId(), null, old.id(), 0, old.createdAt(), null, fixture.nodes(), null, null, fixture.configuration());
            repository.saveVersion(configured, -1);
            assertThat(repository.findVersion(configured.id())).contains(configured);
            var complete = configured.publish(ModelFixtures.APPROVER, Instant.now());
            repository.saveVersion(complete, 0);
            assertThatThrownBy(() -> repository.saveVersion(configured, 0)).isInstanceOf(ModelProblemException.class)
                    .extracting("code").isEqualTo("STALE_MODEL_VERSION");
            assertThat(repository.findVersion(complete.id())).contains(complete);
            assertThat(new ModelCatalogAdapter(repository).publishedVersion(complete.id()).orElseThrow().configuration()).isEqualTo(fixture.configuration());
            assertThat(repository.findVersion(old.id())).contains(old);
            assertThat(jdbc.queryForObject("select snapshot_json from model_version where id=?", String.class, old.id())).isEqualTo(legacy);

            var direct = new ModelNode(null, "IND-1", "Directo", "", 1, List.of(), "INDICATOR");
            var nested = new ModelNode(null, "IND-2", "Anidado", "", 1, List.of(), "INDICATOR");
            var root = new ModelNode(null, "C-1", "Criterio", "", 1, List.of(direct,
                    new ModelNode(null, "SC-1", "Subcriterio", "", 2, List.of(nested), "SUBCRITERION")), "CRITERION");
            var typed = new ModelVersion(null, old.modelId(), 2, ModelVersionStatus.DRAFT, old.source(),
                    old.authorId(), null, old.id(), 0, old.createdAt(), null, List.of(root), old.scale(), old.formula());
            repository.saveVersion(typed, -1);
            assertThat(repository.findVersion(typed.id())).contains(typed);
            var published = typed.publish(ModelFixtures.APPROVER, Instant.now());
            repository.saveVersion(published, 0);
            var catalog = new ModelCatalogAdapter(repository).publishedVersion(typed.id()).orElseThrow();
            assertThat(catalog.indicators()).extracting(i -> i.id()).containsExactly(direct.id(), nested.id());
            assertThat(repository.findVersion(old.id())).contains(old);
        }
    }
}
