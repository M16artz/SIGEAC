package ec.edu.unl.sec.academicevaluation.application;

import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AcademicEvaluationService {
    public Evaluation evaluate(UUID indicatorId, String method, String limitations, String justification) {
        if (method == null || method.isBlank()) throw new IllegalArgumentException("method required");
        return new Evaluation(indicatorId, method, limitations, justification);
    }

    public record Evaluation(UUID indicatorId, String method, String limitations, String justification) {}
}
