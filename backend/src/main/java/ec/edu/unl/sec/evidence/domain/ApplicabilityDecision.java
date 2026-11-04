package ec.edu.unl.sec.evidence.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Append-only applicability decision for an indicator or required source of
 * the fixed model within a process. A newer decision supersedes the previous
 * one through {@code predecessorId}; never two live decisions per subject.
 */
public record ApplicabilityDecision(UUID id, UUID processId, SubjectType subjectType, UUID subjectId,
        Decision decision, String ruleReference, String justification, String legalBasis, String documentReference,
        UUID authorizedBy, Instant decidedAt, UUID predecessorId, long revision) {
    public enum SubjectType { INDICATOR, REQUIRED_SOURCE }
    public enum Decision { APPLICABLE, NOT_APPLICABLE }

    public ApplicabilityDecision {
        id = id == null ? UUID.randomUUID() : id;
        Objects.requireNonNull(processId, "process required");
        Objects.requireNonNull(subjectType, "subject type required");
        Objects.requireNonNull(subjectId, "subject required");
        Objects.requireNonNull(decision, "decision required");
        Objects.requireNonNull(authorizedBy, "authority required");
        ruleReference = text(ruleReference, 2000, "rule reference");
        justification = text(justification, 4000, "justification");
        if (justification.length() < 10)
            throw new IllegalArgumentException("justification must contain at least 10 characters");
        legalBasis = text(legalBasis, 4000, "legal basis");
        documentReference = text(documentReference, 2000, "document reference");
        decidedAt = decidedAt == null ? Instant.now() : decidedAt;
        if (revision < 0) throw new IllegalArgumentException("revision must be non-negative");
    }

    public boolean supersedes(UUID previousId) {
        if (predecessorId == null) return previousId == null || revision == 0;
        return predecessorId.equals(previousId) && revision > 0;
    }

    private static String text(String value, int max, String field) {
        if (value == null || value.isBlank() || value.strip().length() > max)
            throw new IllegalArgumentException(field + " required");
        return value.strip();
    }
}
