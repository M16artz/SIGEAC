package ec.edu.unl.sec.reporting.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import ec.edu.unl.sec.reporting.domain.DocumentaryProgressCalculator.Coverage;
import ec.edu.unl.sec.reporting.domain.DocumentaryProgressCalculator.Source;
import ec.edu.unl.sec.reporting.domain.DocumentaryProgressCalculator.Status;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DocumentaryProgressCalculatorTest {
    private final DocumentaryProgressCalculator calculator = new DocumentaryProgressCalculator();
    private final UUID first = UUID.randomUUID();
    private final UUID second = UUID.randomUUID();
    private final UUID third = UUID.randomUUID();
    private final UUID fourth = UUID.randomUUID();
    private final UUID evidence = UUID.randomUUID();

    @Test
    void countsDistinctSourcesAndKeepsDocumentaryMeasuresSeparate() {
        var sources = List.of(required(first), required(second), required(third), required(fourth));
        var result = calculator.calculate(sources, List.of(
                coverage(first, true), coverage(first, true),
                new Coverage(first, UUID.randomUUID(), true),
                coverage(second, true), coverage(third, false)));

        assertThat(result.status()).isEqualTo(Status.CONFIGURED);
        assertThat(result.requiredSourceTotal()).isEqualTo(4);
        assertThat(result.registeredSourceTotal()).isEqualTo(3);
        assertThat(result.acceptedSourceTotal()).isEqualTo(2);
        assertThat(result.collectionPercent()).isEqualByComparingTo("75.00");
        assertThat(result.documentReviewPercent()).isEqualByComparingTo("50.00");
        assertThat(result.formulaVersion()).isEqualTo("SOURCE_COVERAGE_V1");
    }

    @Test
    void collectionNeedsOneSubmissionButAcceptanceNeedsDistinctEvidenceCardinality() {
        var sources = List.of(new Source(first, true, true, 2));
        var duplicate = coverage(first, true);
        var result = calculator.calculate(sources, List.of(duplicate, duplicate));

        assertThat(result.collectionPercent()).isEqualByComparingTo("100.00");
        assertThat(result.documentReviewPercent()).isEqualByComparingTo("0.00");

        result = calculator.calculate(sources, List.of(duplicate, new Coverage(first, UUID.randomUUID(), true)));
        assertThat(result.acceptedSourceTotal()).isEqualTo(1);
        assertThat(result.documentReviewPercent()).isEqualByComparingTo("100.00");
    }

    @Test
    void observedOrRejectedEvidenceDoesNotSatisfyAcceptanceCardinality() {
        var result = calculator.calculate(List.of(new Source(first, true, true, 2)), List.of(
                coverage(first, true), new Coverage(first, UUID.randomUUID(), false)));
        assertThat(result.registeredSourceTotal()).isEqualTo(1);
        assertThat(result.acceptedSourceTotal()).isZero();
    }

    @Test
    void optionalAndExcludedSourcesDoNotChangeEitherNumeratorOrDenominator() {
        var result = calculator.calculate(List.of(required(first),
                new Source(second, false, true, 1), new Source(third, true, false, 1)),
                List.of(coverage(second, true), coverage(third, true)));
        assertThat(result.requiredSourceTotal()).isEqualTo(1);
        assertThat(result.registeredSourceTotal()).isZero();
        assertThat(result.acceptedSourceTotal()).isZero();
        assertThat(result.collectionPercent()).isEqualByComparingTo("0.00");
    }

    @Test
    void noConfiguredRequiredSourcesHaveNullPercentages() {
        for (var sources : List.of(List.<Source>of(), List.of(new Source(first, false, true, 1)))) {
            var result = calculator.calculate(sources, List.of());
            assertThat(result.status()).isEqualTo(Status.NOT_CONFIGURED);
            assertThat(result.collectionPercent()).isNull();
            assertThat(result.documentReviewPercent()).isNull();
            assertThat(result.requiredSourceTotal()).isZero();
        }
    }

    @Test
    void allRequiredSourcesExcludedHaveADifferentReasonForNullPercentages() {
        var result = calculator.calculate(List.of(new Source(first, true, false, 1)),
                List.of(coverage(first, true)));
        assertThat(result.status()).isEqualTo(Status.NOT_APPLICABLE);
        assertThat(result.collectionPercent()).isNull();
        assertThat(result.documentReviewPercent()).isNull();
        assertThat(result.registeredSourceTotal()).isZero();
        assertThat(result.acceptedSourceTotal()).isZero();
    }

    @Test
    void configuredSourcesWithoutSubmissionsHaveZeroPercentages() {
        var result = calculator.calculate(List.of(required(first)), List.of());
        assertThat(result.status()).isEqualTo(Status.CONFIGURED);
        assertThat(result.collectionPercent()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.documentReviewPercent()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void roundsOnlyDerivedPercentagesAndKeepsExactCounts() {
        var result = calculator.calculate(List.of(required(first), required(second), required(third)),
                List.of(coverage(first, true), coverage(second, false)));
        assertThat(result.requiredSourceTotal()).isEqualTo(3);
        assertThat(result.registeredSourceTotal()).isEqualTo(2);
        assertThat(result.acceptedSourceTotal()).isEqualTo(1);
        assertThat(result.collectionPercent()).isEqualByComparingTo("66.67");
        assertThat(result.documentReviewPercent()).isEqualByComparingTo("33.33");
    }

    @Test
    void repeatedIdenticalSourceDefinitionsCountOnce() {
        var result = calculator.calculate(List.of(required(first), required(first)), List.of(coverage(first, true)));
        assertThat(result.requiredSourceTotal()).isEqualTo(1);
        assertThat(result.acceptedSourceTotal()).isEqualTo(1);
    }

    @Test
    void rejectsContradictoryDefinitionsAndCurrentOutcomes() {
        assertThatIllegalArgumentException().isThrownBy(() -> calculator.calculate(
                List.of(required(first), new Source(first, true, true, 2)), List.of()));
        assertThatIllegalArgumentException().isThrownBy(() -> calculator.calculate(
                List.of(required(first)), List.of(coverage(first, true), coverage(first, false))));
    }

    @Test
    void validatesCardinalityAndIdentifiers() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Source(first, true, true, 0));
        assertThatIllegalArgumentException().isThrownBy(() -> new Source(null, true, true, 1));
        assertThatIllegalArgumentException().isThrownBy(() -> new Coverage(first, null, true));
        assertThatIllegalArgumentException().isThrownBy(() -> new Coverage(null, evidence, true));
    }

    @Test
    void rejectsCoverageOutsideTheSuppliedSourceScope() {
        assertThatIllegalArgumentException().isThrownBy(() -> calculator.calculate(
                List.of(required(first)), List.of(coverage(second, true))));
    }

    private Source required(UUID id) {
        return new Source(id, true, true, 1);
    }

    private Coverage coverage(UUID source, boolean accepted) {
        return new Coverage(source, evidence, accepted);
    }
}
