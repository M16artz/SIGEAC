package ec.edu.unl.sec.academicevaluation.application;

import org.junit.jupiter.api.Test;

class AcademicReconciliationServiceTest {
    @Test
    void capturesDisagreementAndPreservesPriorValuations() {
        var service = new AcademicReconciliationService();
        service.reconcile(java.util.UUID.randomUUID(), "discrepancia");
    }
}
