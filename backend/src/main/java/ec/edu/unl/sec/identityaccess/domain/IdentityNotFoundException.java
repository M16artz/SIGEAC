package ec.edu.unl.sec.identityaccess.domain;

public class IdentityNotFoundException extends RuntimeException {
    public IdentityNotFoundException() {
        super("El recurso no existe o no es visible en el ambito actual.");
    }
}
