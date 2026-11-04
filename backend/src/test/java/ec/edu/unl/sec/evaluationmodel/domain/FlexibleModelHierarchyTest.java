package ec.edu.unl.sec.evaluationmodel.domain;

import static org.assertj.core.api.Assertions.assertThat;

import ec.edu.unl.sec.evaluationmodel.support.ModelFixtures;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FlexibleModelHierarchyTest {
    @Test void acceptsDirectAndNestedIndicatorsAndFutureCategories() {
        var direct = node("I-1", "INDICATOR", 1);
        var nested = node("I-2", "INDICATOR", 1);
        var root = node("C-1", "CRITERION", 1, direct,
                node("S-1", "SUBCRITERION", 2, node("G-1", "LOCAL_GROUP", 1, nested)));
        var result = ModelHierarchy.inspect(List.of(root));
        assertThat(result.issues()).isEmpty();
        assertThat(result.indicators()).containsExactly(direct, nested);
        assertThat(result.entries()).filteredOn(e -> e.node().id().equals(direct.id()))
                .extracting(ModelHierarchy.Entry::parentId).containsExactly(root.id());
    }

    @Test void preservesLegacyTypesAndIndicatorIdentities() {
        var roots = ModelFixtures.criteria();
        var result = ModelHierarchy.inspect(roots);
        assertThat(result.issues()).isEmpty();
        assertThat(result.entries()).extracting(ModelHierarchy.Entry::nodeType)
                .containsExactly("CRITERION", "STANDARD", "INDICATOR");
        assertThat(result.indicators()).containsExactly(roots.getFirst().children().getFirst().children().getFirst());
    }

    @Test void rejectsCyclesByIdentityWithoutRecursingForever() {
        UUID id = UUID.randomUUID();
        var repeated = new ModelNode(id, "C-2", "Repetido", "", 1, List.of(), "CRITERION");
        var root = new ModelNode(id, "C-1", "Raíz", "", 1, List.of(repeated), "CRITERION");
        assertThat(ModelHierarchy.inspect(List.of(root)).issues()).contains("NODE_CYCLE:C-2");
    }

    @Test void rejectsDuplicateIdsCodesPositionsAndIndicatorChildren() {
        UUID id = UUID.randomUUID();
        var first = new ModelNode(id, " i-1 ", "Primero", "", 1, List.of(), "INDICATOR");
        var duplicate = new ModelNode(id, "I-1", "Segundo", "", 1, List.of(), "INDICATOR");
        var parent = node("P-1", "INDICATOR", 2, node("I-2", "INDICATOR", 1));
        var issues = ModelHierarchy.inspect(List.of(node("C-1", "CRITERION", 1, first, duplicate, parent))).issues();
        assertThat(issues).contains("NODE_ID_DUPLICATE:" + id, "INDICATOR_CODE_DUPLICATE:I-1",
                "NODE_POSITION_DUPLICATE:I-1", "INDICATOR_CHILDREN_FORBIDDEN:P-1");
    }

    @Test void rejectsEmptyBranchesAndSupportsRecursivePublicationInputs() {
        assertThat(ModelHierarchy.inspect(List.of(node("C-1", "CRITERION", 1))).issues()).contains("INDICATOR_REQUIRED:C-1");
        var old = ModelFixtures.draft();
        var typed = new ModelVersion(old.id(), old.modelId(), old.versionNumber(), old.status(), old.source(),
                old.authorId(), null, null, 0, old.createdAt(), null,
                List.of(node("C-1", "CRITERION", 1, node("G-1", "GROUP", 1, node("S-1", "SUBCRITERION", 1,
                        node("IND-1", "INDICATOR", 1))))), old.scale(), old.formula());
        assertThat(ModelHierarchy.inspect(typed.criteria()).issues()).isEmpty();
        assertThat(PublicationValidator.validate(typed, ModelFixtures.APPROVER, "sec.progress/v1")).contains("MODEL_CONFIGURATION_REQUIRED");
    }

    private static ModelNode node(String code, String type, int position, ModelNode... children) {
        return new ModelNode(null, code, "Nodo sintético", "", position, List.of(children), type);
    }
}
