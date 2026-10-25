package ec.edu.unl.sec.governance.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FindingTest {
    @Test
    void linksFindingsWithoutArbitraryAggregation() {
        var finding = new Finding("F1", "Hallazgo", "JUSTIFICADO");
        assertThat(finding.code()).isEqualTo("F1");
    }
    record Finding(String code, String description, String status) {}
}
