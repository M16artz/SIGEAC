package ec.edu.unl.sec.evaluationmodel.domain;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RequiredSourceTest {
    private final UUID indicator = UUID.randomUUID();

    @Test void supportsAllSourceTypesOptionalSourcesAndUnboundedCardinality() {
        for (var type : RequiredSource.SourceType.values()) {
            var source = new RequiredSource(null, indicator, " f-1 ", type, "Fuente", "Descripción", false, 1, null, "2026", 1);
            assertThat(source.code()).isEqualTo("F-1");
            assertThat(source.required()).isFalse();
            assertThat(source.cardinalityMax()).isNull();
        }
    }

    @Test void rejectsInvalidCardinalityAndMissingMetadata() {
        assertThatIllegalArgumentException().isThrownBy(() -> source(indicator, "F-1", 0, null, 1));
        assertThatIllegalArgumentException().isThrownBy(() -> source(indicator, "F-1", 2, 1, 1));
        assertThatIllegalArgumentException().isThrownBy(() -> source(indicator, "F-1", 1, 1, 0));
        assertThatIllegalArgumentException().isThrownBy(() -> source(null, "F-1", 1, 1, 1));
        assertThatIllegalArgumentException().isThrownBy(() -> source(indicator, " ", 1, 1, 1));
        assertThatIllegalArgumentException().isThrownBy(() -> new RequiredSource(null, indicator, "F-1",
                RequiredSource.SourceType.FILE, "Fuente", "Descripción", true, 1, null, " ", 1));
        assertThat(source(indicator, "F-1", 2, 2, 1).cardinalityMax()).isEqualTo(2);
    }

    @Test void preservesIndicatorSpecificPeriodScaleRuleAndImmutableSources() {
        UUID scale = UUID.randomUUID(), rule = UUID.randomUUID();
        var sources = new ArrayList<>(List.of(source(indicator, "F-1", 1, null, 1)));
        var definition = new IndicatorDefinition(indicator, IndicatorDefinition.Nature.QUANTITATIVE,
                "Estándar sintético", List.of("Elemento"), "Último año", scale, rule, sources);
        sources.clear();
        assertThat(definition.sources()).hasSize(1);
        assertThat(definition.ratingScaleId()).isEqualTo(scale);
        assertThat(definition.valuationRuleId()).isEqualTo(rule);
        assertThat(definition.evaluationPeriodDefinition()).isEqualTo("Último año");
        assertThatThrownBy(() -> definition.sources().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test void rejectsDuplicateNormalizedCodesAndSourcesFromOtherIndicators() {
        var first = source(indicator, "F-1", 1, null, 1);
        assertThatIllegalArgumentException().isThrownBy(() -> definition(List.of(first, source(indicator, " f-1 ", 1, 1, 2))));
        assertThatIllegalArgumentException().isThrownBy(() -> definition(List.of(first, source(UUID.randomUUID(), "F-2", 1, 1, 2))));
        assertThatIllegalArgumentException().isThrownBy(() -> definition(List.of(first, source(indicator, "F-2", 1, 1, 1))));
        var reusedId = new RequiredSource(first.id(), indicator, "F-2", first.sourceType(), first.name(), first.description(),
                true, 1, null, first.periodRule(), 2);
        assertThatIllegalArgumentException().isThrownBy(() -> definition(List.of(first, reusedId)));
        assertThatIllegalArgumentException().isThrownBy(() -> new IndicatorDefinition(indicator,
                IndicatorDefinition.Nature.QUALITATIVE, " ", List.of(), "2026", null, null, List.of()));
    }

    private IndicatorDefinition definition(List<RequiredSource> sources) {
        return new IndicatorDefinition(indicator, IndicatorDefinition.Nature.QUALITATIVE, "Estándar", List.of(), "2026", null, null, sources);
    }

    private static RequiredSource source(UUID indicator, String code, int min, Integer max, int ordinal) {
        return new RequiredSource(null, indicator, code, RequiredSource.SourceType.FILE, "Fuente sintética",
                "Descripción", true, min, max, "2026", ordinal);
    }
}
