package ec.edu.unl.sec.archive.application;

public class ArchiveProblemException extends RuntimeException {
    public enum Kind { UNAUTHORIZED, FORBIDDEN, NOT_FOUND, INVALID, CONFLICT, PRECONDITION_FAILED }

    private final Kind kind;
    private final String code;

    public ArchiveProblemException(Kind kind, String code, String message) {
        super(message);
        this.kind = kind;
        this.code = code;
    }

    public Kind kind() { return kind; }

    public String code() { return code; }
}
