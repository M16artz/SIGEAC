package ec.edu.unl.sec.evidence.application;

public class EvidenceProblemException extends RuntimeException {
    private final Kind kind;
    private final String code;

    public EvidenceProblemException(Kind kind, String code, String safeMessage) {
        super(safeMessage);
        this.kind = kind;
        this.code = code;
    }

    public Kind kind() {
        return kind;
    }

    public String code() {
        return code;
    }

    public enum Kind {
        UNAUTHORIZED,
        NOT_FOUND,
        CONFLICT,
        INVALID,
        PRECONDITION_REQUIRED,
        PRECONDITION_FAILED
    }
}
