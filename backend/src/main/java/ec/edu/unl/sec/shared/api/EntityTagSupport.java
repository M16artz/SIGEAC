package ec.edu.unl.sec.shared.api;

import org.springframework.http.HttpStatus;

public final class EntityTagSupport {
    private EntityTagSupport() {}

    public static String strong(long revision) {
        if (revision < 0) throw new IllegalArgumentException("revision must be non-negative");
        return "\"" + revision + "\"";
    }

    public static long require(String value) {
        if (value == null || value.isBlank()) throw new DomainProblemException(HttpStatus.PRECONDITION_REQUIRED,
                "PRECONDITION_REQUIRED", "Se requiere If-Match.");
        if (value.startsWith("W/")) throw new DomainProblemException(HttpStatus.BAD_REQUEST,
                "WEAK_ETAG_NOT_ALLOWED", "Se requiere un ETag fuerte.");
        if (!value.matches("\"[0-9]+\"")) throw invalid();
        try { return Long.parseLong(value.substring(1, value.length() - 1)); }
        catch (NumberFormatException exception) { throw invalid(); }
    }

    public static void verify(String value, long current) {
        if (require(value) != current) throw new DomainProblemException(HttpStatus.PRECONDITION_FAILED,
                "STALE_DRAFT", "El borrador cambió; recargue antes de continuar.");
    }

    private static DomainProblemException invalid() {
        return new DomainProblemException(HttpStatus.BAD_REQUEST, "INVALID_ETAG", "If-Match no contiene una revisión válida.");
    }
}
