package ec.edu.unl.sec.evaluationmodel.domain;

import java.time.Instant;
import java.util.UUID;

public record NormativeReference(UUID id, String title, String issuer, String edition, String documentReference,
        String documentSha256, String pages, String disposition, VerificationStatus verificationStatus,
        UUID verifiedBy, Instant verifiedAt) {
    public NormativeReference {
        id = id == null ? UUID.randomUUID() : id;
        title = text(title, 500, "title");
        documentReference = text(documentReference, 2000, "document reference");
        issuer = optionalText(issuer, 300, "issuer");
        edition = optionalText(edition, 200, "edition");
        pages = optionalText(pages, 200, "pages");
        disposition = optionalText(disposition, 2000, "disposition");
        documentSha256 = optionalText(documentSha256, 64, "document sha256");
        verificationStatus = verificationStatus == null ? VerificationStatus.PENDING_VALIDATION : verificationStatus;
        if (verificationStatus == VerificationStatus.VERIFIED) {
            if (issuer == null || edition == null) throw new IllegalArgumentException("verified reference requires issuer and edition");
            if (disposition == null) throw new IllegalArgumentException("verified reference requires disposition");
            if (verifiedBy == null || verifiedAt == null) throw new IllegalArgumentException("verified reference requires verifier and date");
            if (documentSha256 == null || !documentSha256.matches("[0-9a-fA-F]{64}"))
                throw new IllegalArgumentException("verified reference requires sha256");
        } else if (verifiedBy != null || verifiedAt != null || disposition != null) {
            throw new IllegalArgumentException("pending reference cannot claim verification");
        }
    }

    public boolean verified() {
        return verificationStatus == VerificationStatus.VERIFIED;
    }

    private static String text(String value, int max, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " required");
        String trimmed = value.trim();
        if (trimmed.length() > max) throw new IllegalArgumentException(field + " too long");
        return trimmed;
    }

    private static String optionalText(String value, int max, String field) {
        if (value == null || value.isBlank()) return null;
        String trimmed = value.trim();
        if (trimmed.length() > max) throw new IllegalArgumentException(field + " too long");
        return trimmed;
    }

    public enum VerificationStatus { PENDING_VALIDATION, VERIFIED }
}
