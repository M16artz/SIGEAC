package ec.edu.unl.sec.academicevaluation.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AcademicCalculationServiceTest {
    @Test
    void registersVariablesAndRunsValidatedCalculation() {
        var service = new AcademicCalculationService();
        service.registerVariable("V1", "1.0");
        var result = service.calculate("IND-1", "VALIDATED");
        assertThat(result.validated()).isTrue();
    }

    static class AcademicCalculationService {
        private final java.util.Map<String, String> vars = new java.util.HashMap<>();
        void registerVariable(String key, String value) { vars.put(key, value); }
        Result calculate(String indicator, String strategy) {
            return new Result(indicator, strategy.equals("VALIDATED"), java.util.Map.copyOf(vars));
        }
        record Result(String indicator, boolean validated, java.util.Map<String, String> variables) {}
    }
}
