package ec.edu.unl.sec.selfassessment.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** A recorded decision; the assignment's authority is verified by the application boundary. */
public record ProcessAuthorization(UUID id, UUID authorityAssignmentId, String legalBasis,
        String documentReference, String reason, UUID authorizedBy, Instant authorizedAt) {
    public ProcessAuthorization {
        id = id == null ? UUID.randomUUID() : id;
        Objects.requireNonNull(authorityAssignmentId, "authority assignment required");
        Objects.requireNonNull(authorizedBy, "authorizer required");
        Objects.requireNonNull(authorizedAt, "decision date required");
        legalBasis = text(legalBasis, 1, 4000);
        documentReference = text(documentReference, 1, 2000);
        reason = text(reason, 10, 4000);
    }

    private static String text(String value, int min, int max) {
        if (value == null || value.strip().length() < min || value.strip().length() > max) {
            throw new IllegalArgumentException("invalid authorization provenance");
        }
        return value.strip();
    }
}
