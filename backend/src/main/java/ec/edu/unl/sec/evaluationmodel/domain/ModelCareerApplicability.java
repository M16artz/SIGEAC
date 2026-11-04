package ec.edu.unl.sec.evaluationmodel.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ModelCareerApplicability(UUID id, UUID modelVersionId, UUID careerId, Status status, LocalDate validFrom,
        LocalDate validUntil, Decision provenance, UUID predecessorId, int revision, UUID decidedBy, Instant decidedAt) {
    public ModelCareerApplicability {
        id = id == null ? UUID.randomUUID() : id;
        if (modelVersionId == null || careerId == null || status == null || validFrom == null)
            throw new IllegalArgumentException("version, career, decision and validFrom required");
        if (validUntil != null && validUntil.isBefore(validFrom))
            throw new IllegalArgumentException("validUntil cannot precede validFrom");
        if (provenance == null) throw new IllegalArgumentException("applicability provenance required");
        if (revision < 1) throw new IllegalArgumentException("applicability revision must be positive");
        if (decidedBy == null || decidedAt == null) throw new IllegalArgumentException("applicability author required");
    }

    public boolean effectiveOn(LocalDate date) {
        return status == Status.APPLICABLE && !date.isBefore(validFrom)
                && (validUntil == null || !date.isAfter(validUntil));
    }

    public enum Status { PENDING_VALIDATION, APPLICABLE, NOT_APPLICABLE, EXPIRED }

    public record Decision(String authorityFunction, String legalBasis, String documentReference, String reason) {
        public Decision {
            authorityFunction = text(authorityFunction, 300, "authority function");
            legalBasis = text(legalBasis, 2000, "legal basis");
            documentReference = text(documentReference, 2000, "document reference");
            reason = text(reason, 4000, "reason");
        }

        private static String text(String value, int max, String field) {
            if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " required");
            String trimmed = value.trim();
            if (trimmed.length() > max) throw new IllegalArgumentException(field + " too long");
            return trimmed;
        }
    }
}
