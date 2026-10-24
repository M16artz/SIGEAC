package ec.edu.unl.sec.reporting.application;

public class ReportingProblemException extends RuntimeException {
    private final Kind kind;
    private final String code;
    private java.util.Map<String,String> details=java.util.Map.of();

    public ReportingProblemException(Kind kind, String code, String message) {
        super(message);
        this.kind = kind;
        this.code = code;
    }

    public Kind kind() { return kind; }
    public String code() { return code; }
    public ReportingProblemException(Kind kind,String code,String message,java.util.Map<String,String> details) {
        this(kind,code,message);this.details=java.util.Map.copyOf(details);
    }
    public java.util.Map<String,String> details() { return details; }
    public enum Kind { UNAUTHORIZED, FORBIDDEN, NOT_FOUND, INVALID, CONFLICT, PRECONDITION_REQUIRED, PRECONDITION_FAILED }
}
