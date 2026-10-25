package ec.edu.unl.sec.evidence.application;

import java.util.Map;
import java.util.Set;

public final class EvidenceAuditPolicy {
    private EvidenceAuditPolicy() {
    }

    public static final Set<String> ALLOWED = Set.of(
            "EVIDENCE_CREATED",
            "EVIDENCE_UPDATED",
            "EVIDENCE_SUBMITTED",
            "EVIDENCE_CORRECTION",
            "EVIDENCE_FILE_ATTACHED",
            "EVIDENCE_FILE_REMOVED",
            "EVIDENCE_REFERENCE_ADDED",
            "EVIDENCE_REFERENCE_UPDATED",
            "EVIDENCE_REFERENCE_REMOVED",
            "EVIDENCE_DOWNLOAD",
            "EVIDENCE_SUBMISSION_REJECTED",
            "EVIDENCE_FILE_VALIDATION_FAILED",
            "EVIDENCE_PROMOTION_FAILED",
            "EVIDENCE_CONCURRENCY_CONFLICT",
            "EVIDENCE_DOWNLOAD_DENIED",
            "EVIDENCE_INTEGRITY_FAILED"
    );

    public static Map<String, String> sanitize(Map<String, String> details) {
        if (details == null) {
            return Map.of();
        }
        return details.entrySet().stream()
                .filter(e -> e.getKey() != null && e.getValue() != null)
                .filter(e -> !e.getKey().toLowerCase().contains("key"))
                .filter(e -> !e.getKey().toLowerCase().contains("secret"))
                .filter(e -> !e.getKey().toLowerCase().contains("token"))
                .filter(e -> !e.getValue().startsWith("file:/"))
                .limit(20)
                .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
