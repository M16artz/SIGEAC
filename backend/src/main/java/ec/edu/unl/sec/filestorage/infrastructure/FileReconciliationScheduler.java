package ec.edu.unl.sec.filestorage.infrastructure;

import ec.edu.unl.sec.filestorage.application.FileReconciliationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "sec.files.reconciliation", name = "enabled", havingValue = "true", matchIfMissing = true)
public class FileReconciliationScheduler {
    private final FileReconciliationService reconciliation;

    public FileReconciliationScheduler(FileReconciliationService reconciliation) {
        this.reconciliation = reconciliation;
    }

    @Scheduled(initialDelayString = "${sec.files.reconciliation.initial-delay-ms:30000}",
            fixedDelayString = "${sec.files.reconciliation.interval-ms:60000}")
    public void reconcile() {
        reconciliation.reconcileDue("scheduler", 50);
    }
}
