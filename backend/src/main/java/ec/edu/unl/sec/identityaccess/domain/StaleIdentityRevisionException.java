package ec.edu.unl.sec.identityaccess.domain;

public class StaleIdentityRevisionException extends RuntimeException {
    public StaleIdentityRevisionException() {
        super("La revision enviada esta desactualizada.");
    }
}
