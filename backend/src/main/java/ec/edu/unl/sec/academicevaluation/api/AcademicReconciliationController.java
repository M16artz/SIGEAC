package ec.edu.unl.sec.academicevaluation.api;

import ec.edu.unl.sec.academicevaluation.application.AcademicReconciliationService;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/academic-evaluations/disagreements")
public class AcademicReconciliationController {
    private final AcademicReconciliationService reconciliation;

    public AcademicReconciliationController(AcademicReconciliationService reconciliation) {
        this.reconciliation = reconciliation;
    }

    @PostMapping("/{indicatorId}")
    public void reconcile(@PathVariable UUID indicatorId, @RequestBody(required = false) Map<String, Object> note) {
        String text = note == null ? null : String.valueOf(note.get("note"));
        reconciliation.reconcile(indicatorId, text);
    }
}
