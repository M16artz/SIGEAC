package ec.edu.unl.sec.identityaccess.domain;

public class IdentityAccessDeniedException extends RuntimeException {
    private final String code;

    public IdentityAccessDeniedException(String code) {
        super("La operacion no esta permitida en el ambito actual.");
        this.code = code;
    }

    public String code() {
        return code;
    }
}
