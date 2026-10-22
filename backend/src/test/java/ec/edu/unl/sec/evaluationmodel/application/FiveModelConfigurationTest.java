package ec.edu.unl.sec.evaluationmodel.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.unl.sec.evaluationmodel.domain.*;
import ec.edu.unl.sec.evaluationmodel.infrastructure.ModelCatalogAdapter;
import ec.edu.unl.sec.evaluationmodel.support.*;
import ec.edu.unl.sec.identityaccess.application.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class FiveModelConfigurationTest {
    @Test void publishesAllFiveSyntheticConfigurationsUsingTheSameEngine() throws Exception {
        var manifest = ConfiguredModelFixtures.manifest();
        assertThat(manifest.synthetic()).isTrue();
        assertThat(manifest.models()).extracting(ConfiguredModelFixtures.Fixture::key)
                .containsExactly("general-2024", "medicine-2018", "nursing-2016", "dentistry-2018", "law-matrix");
        var repository = new InMemoryEvaluationModelRepository();
        var actor = new AtomicReference<>(new CurrentActorPort.Actor(ModelFixtures.AUTHOR, false));
        var authorization = mock(AuthorizationPort.class);
        when(authorization.hasOperation(any(), any())).thenReturn(true);
        var service = new EvaluationModelService(repository, () -> Optional.of(actor.get()), authorization,
                new EvaluationModelProperties(null, null), mock(EvaluationModelAuditService.class));
        var catalog = new ModelCatalogAdapter(repository);
        Set<Integer> depths = new HashSet<>();
        Set<RequiredSource.SourceType> sourceTypes = new HashSet<>();
        Set<String> units = new HashSet<>();
        var json = new ObjectMapper().findAndRegisterModules();
        for (var fixture : manifest.models()) {
            assertThat(fixture.synthetic()).isTrue();
            assertThat(fixture.source()).contains("sin validez institucional");
            actor.set(new CurrentActorPort.Actor(ModelFixtures.AUTHOR, false));
            var model = service.createModel(fixture.name(), "Solo prueba de software", UUID.randomUUID());
            var draft = service.createDraft(model.id(), fixture.source(), fixture.nodes(), fixture.configuration(), UUID.randomUUID());
            actor.set(new CurrentActorPort.Actor(ModelFixtures.APPROVER, false));
            var published = service.publishConfigured(draft.id(), 0, ModelFixtures.APPROVER, UUID.randomUUID());
            var view = catalog.publishedVersion(published.id()).orElseThrow();
            assertThat(view.indicators()).hasSize(2);
            assertThat(view.configuration()).isEqualTo(fixture.configuration());
            assertThat(json.readValue(json.writeValueAsString(published), ModelVersion.class)).isEqualTo(published);
            depths.add(depth(fixture.nodes()));
            for (var indicator : view.configuration().indicators()) {
                assertThat(indicator.sources()).isNotEmpty();
                indicator.sources().forEach(source -> sourceTypes.add(source.sourceType()));
            }
            for (var rule : view.configuration().rules()) {
                var scale = view.configuration().scales().stream().filter(s -> s.id().equals(rule.scaleId())).findFirst().orElse(null);
                assertThat(rule.automaticCalculationAllowed(scale, Set.of())).isFalse();
                rule.variables().forEach(variable -> units.add(variable.unit()));
            }
        }
        assertThat(depths).contains(2, 3, 4, 5);
        assertThat(sourceTypes).containsExactlyInAnyOrder(RequiredSource.SourceType.values());
        assertThat(units).hasSizeGreaterThan(1);
    }

    @Test void normativeManifestExplicitlyLeavesSourcesAndInstitutionalValidityUnverified() throws Exception {
        var root = new ObjectMapper().readTree(ConfiguredModelFixtures.path("normative-manifest.json").toFile());
        assertThat(root.path("synthetic").asBoolean()).isTrue();
        assertThat(root.path("institutionalValidity").asBoolean()).isFalse();
        Set<String> keys = new HashSet<>();
        for (var model : root.path("models")) {
            keys.add(model.path("key").asText());
            assertThat(model.path("verificationStatus").asText()).isEqualTo("PENDING_VALIDATION");
            assertThat(model.path("verifiedPages").isNull()).isTrue();
            assertThat(model.path("documentSha256").isNull()).isTrue();
            assertThat(model.path("unresolvedDecisions").toString()).contains("CL-011", "CL-013", "CL-014");
        }
        assertThat(keys).containsExactlyInAnyOrderElementsOf(ConfiguredModelFixtures.manifest().models().stream()
                .map(ConfiguredModelFixtures.Fixture::key).toList());
    }

    private static int depth(List<ModelNode> nodes) {
        return nodes.stream().mapToInt(node -> 1 + depth(node.children())).max().orElse(0);
    }
}
