package ec.edu.unl.sec.evaluationmodel.application;

public class ModelProblemException extends RuntimeException {
    public enum Kind { UNAUTHORIZED, FORBIDDEN, NOT_FOUND, CONFLICT, PRECONDITION, INVALID }
    private final Kind kind; private final String code;
    public ModelProblemException(Kind kind,String code,String message){super(message);this.kind=kind;this.code=code;}
    public Kind kind(){return kind;} public String code(){return code;}
}
