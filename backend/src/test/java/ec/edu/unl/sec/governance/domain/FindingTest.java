package ec.edu.unl.sec.governance.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FindingTest {
    final UUID process = UUID.randomUUID(), indicatorOne = UUID.randomUUID(), indicatorTwo = UUID.randomUUID(),
            evaluation = UUID.randomUUID(), version = UUID.randomUUID(), author = UUID.randomUUID();
    Finding.Data data(String description, List<UUID> indicators, List<UUID> evaluations, List<UUID> evidences) {
        return new Finding.Data(Finding.Type.WEAKNESS, "Hallazgo sintético", description, indicators, evaluations, evidences);
    }
    Finding finding(Finding.Data data) {
        return new Finding(UUID.randomUUID(), process, data, author, Instant.parse("2026-10-25T10:00:00Z"));
    }

    @Test void linksIndicatorsEvaluationsAndEvidencesWithoutAggregatingAnything() {
        var value = finding(data("Descripción justificada con suficientes caracteres.",
                List.of(indicatorOne, indicatorTwo), List.of(evaluation), List.of(version)));
        assertThat(value.supports()).containsExactlyInAnyOrder(
                new Finding.Support(Finding.Kind.INDICATOR, indicatorOne),
                new Finding.Support(Finding.Kind.INDICATOR, indicatorTwo),
                new Finding.Support(Finding.Kind.EVALUATION, evaluation),
                new Finding.Support(Finding.Kind.EVIDENCE_VERSION, version));
        assertThat(value.status()).isEqualTo(Finding.Status.REGISTERED);
        assertThat(value.version()).isEqualTo(1L);
        assertThat(value.data().title()).isEqualTo("Hallazgo sintético");
        assertThat(value.data().type()).isEqualTo(Finding.Type.WEAKNESS);
    }

    @Test void requiresClassifiedTypeJustificationAndAtLeastOneIndicator() {
        assertThatThrownBy(() -> data("Descripción suficientemente larga.", List.of(), List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("indicator");
        assertThatThrownBy(() -> new Finding.Data(null, "Hallazgo sintético",
                "Descripción suficientemente larga.", List.of(indicatorOne), List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("type");
        assertThatThrownBy(() -> data("corto", List.of(indicatorOne), List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("text");
    }

    @Test void rejectsBlankTitleAndDuplicateSupports() {
        assertThatThrownBy(() -> new Finding.Data(Finding.Type.OBSERVATION, "  ", "Descripción justificada con suficientes caracteres.",
                List.of(indicatorOne), List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("justified");
        assertThatThrownBy(() -> finding(data("Descripción justificada con suficientes caracteres.",
                List.of(indicatorOne, indicatorOne), List.of(), List.of())))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("distinct");
        assertThatThrownBy(() -> finding(data("Descripción justificada con suficientes caracteres.",
                List.of(indicatorOne), List.of(), List.of(version, version))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("distinct");
    }

    @Test void preservesJustifiedTextsExactlyWithoutRatings() {
        var value = finding(data("   Descripción justificada con suficientes caracteres.   ",
                List.of(indicatorOne), List.of(), List.of()));
        assertThat(value.data().description()).startsWith("Descripción").endsWith("caracteres.");
        assertThat(value.supports()).hasSize(1);
    }
}
