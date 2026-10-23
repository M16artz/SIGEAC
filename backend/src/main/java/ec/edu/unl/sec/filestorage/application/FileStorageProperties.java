package ec.edu.unl.sec.filestorage.application;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.nio.file.Path;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "sec.files")
public class FileStorageProperties {
    @NotNull
    private Path root = Path.of("./var/files");

    @Positive
    private long maxBytes = 26_214_400L;

    @NotBlank
    private String validationPolicyId = "pilot-synthetic-v1";

    @Valid
    @NotNull
    private Reconciliation reconciliation = new Reconciliation();

    public Path getRoot() { return root; }
    public void setRoot(Path root) { this.root = root; }
    public long getMaxBytes() { return maxBytes; }
    public void setMaxBytes(long maxBytes) { this.maxBytes = maxBytes; }
    public String getValidationPolicyId() { return validationPolicyId; }
    public void setValidationPolicyId(String validationPolicyId) { this.validationPolicyId = validationPolicyId; }
    public Reconciliation getReconciliation() { return reconciliation; }
    public void setReconciliation(Reconciliation reconciliation) { this.reconciliation = reconciliation; }

    public static final class Reconciliation {
        @NotNull
        private Duration grace = Duration.ofMinutes(15);

        @NotNull
        private Duration lease = Duration.ofMinutes(2);

        public Duration getGrace() { return grace; }
        public void setGrace(Duration grace) { this.grace = grace; }
        public Duration getLease() { return lease; }
        public void setLease(Duration lease) { this.lease = lease; }
    }
}
