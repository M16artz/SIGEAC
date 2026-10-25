package ec.edu.unl.sec.academicevaluation.api;

import ec.edu.unl.sec.academicevaluation.application.AcademicCalculationService;
import ec.edu.unl.sec.academicevaluation.application.AcademicEvaluationService;
import ec.edu.unl.sec.academicevaluation.application.VariableValueService;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/academic-evaluations")
public class AcademicEvaluationController {
    private final VariableValueService variables;
    private final AcademicCalculationService calculations;
    private final AcademicEvaluationService evaluation;

    public AcademicEvaluationController(VariableValueService variables, AcademicCalculationService calculations,
            AcademicEvaluationService evaluation) {
        this.variables = variables;
        this.calculations = calculations;
        this.evaluation = evaluation;
    }

    @PostMapping("/indicators/{indicatorId}/variables")
    public void register(@PathVariable UUID indicatorId, @RequestBody Map<String, String> body) {
        body.forEach((k, v) -> variables.register(indicatorId, k, v));
    }

    @PostMapping("/indicators/{indicatorId}/calculate")
    public AcademicCalculationService.CalculationResult calculate(@PathVariable UUID indicatorId,
            @RequestParam String strategy) {
        return calculations.calculate(indicatorId, strategy);
    }

    @PostMapping("/indicators/{indicatorId}/evaluate")
    public AcademicEvaluationService.Evaluation evaluate(@PathVariable UUID indicatorId,
            @RequestBody EvaluateRequest request) {
        return evaluation.evaluate(indicatorId, request.method(), request.limitations(), request.justification());
    }

    public record EvaluateRequest(String method, String limitations, String justification) {}
}
