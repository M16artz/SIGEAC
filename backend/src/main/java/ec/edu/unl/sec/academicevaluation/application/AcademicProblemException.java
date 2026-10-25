package ec.edu.unl.sec.academicevaluation.application;

public class AcademicProblemException extends RuntimeException {
    public enum Kind { UNAUTHORIZED, FORBIDDEN, NOT_FOUND, INVALID, CONFLICT, PRECONDITION_FAILED }
    private final Kind kind;
    private final String code;
    public AcademicProblemException(Kind kind, String code, String message) {
        super(message); this.kind=kind; this.code=code;
    }
    public Kind kind() { return kind; }
    public String code() { return code; }
}
