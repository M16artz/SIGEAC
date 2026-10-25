package ec.edu.unl.sec.academicevaluation.application;

import ec.edu.unl.sec.academicevaluation.domain.ManualAcademicEvaluation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ManualEvaluationRepository {
    Optional<ManualAcademicEvaluation> latest(UUID processId, UUID indicatorId, UUID evaluatorId);
    List<ManualAcademicEvaluation> history(UUID processId, UUID indicatorId, long offset, int limit);
    List<ManualAcademicEvaluation> current(UUID processId, UUID indicatorId);
    void append(ManualAcademicEvaluation evaluation);
}
