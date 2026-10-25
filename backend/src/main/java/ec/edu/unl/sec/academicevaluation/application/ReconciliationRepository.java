package ec.edu.unl.sec.academicevaluation.application;
import ec.edu.unl.sec.academicevaluation.domain.AcademicReconciliation.*;
import java.util.*;

public interface ReconciliationRepository {
    Optional<Policy> policy(UUID processId,UUID indicatorId);
    Optional<Disagreement> latest(UUID processId,UUID indicatorId);
    List<Disagreement> history(UUID processId,UUID indicatorId,long offset,int limit);
    Optional<Resolution> resolution(UUID disagreementId);
    void append(Policy policy);
    void append(Disagreement disagreement);
    void append(Resolution resolution);
}
