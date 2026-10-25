package ec.edu.unl.sec.academicevaluation.application;

import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AcademicReconciliationService {
    public void reconcile(UUID indicatorId, String note) {
        // Capture discrepancy/resolution; preserve prior valuations
    }
}
