package ec.edu.unl.sec.documentreview.application;

public class DocumentReviewProblemException extends RuntimeException {
    private final Kind kind;
    private final String code;

    public DocumentReviewProblemException(Kind kind, String code, String safeMessage) {
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
        FORBIDDEN,
        NOT_FOUND,
        CONFLICT,
        INVALID,
        PRECONDITION_REQUIRED,
        PRECONDITION_FAILED
    }
}
