package ec.edu.unl.sec.academicevaluation.application;

import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AcademicCalculationService {
    private final VariableValueService variables;

    public AcademicCalculationService(VariableValueService variables) {
        this.variables = variables;
    }

    public CalculationResult calculate(UUID indicatorId, String strategy) {
        boolean validated = "VALIDATED".equals(strategy);
        return new CalculationResult(indicatorId, validated, variables.get(indicatorId));
    }

    public record CalculationResult(UUID indicatorId, boolean validated, Map<String, String> variables) {}
}
