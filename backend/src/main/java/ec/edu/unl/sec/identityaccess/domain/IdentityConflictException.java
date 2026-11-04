package ec.edu.unl.sec.identityaccess.domain;

import java.util.UUID;

public class IdentityConflictException extends RuntimeException {
    private final String code;
    private final UUID policyId;

    public IdentityConflictException(String code, String message) {
        this(code, message, null);
    }

    public IdentityConflictException(String code, String message, UUID policyId) {
        super(message);
        this.code = code;
        this.policyId = policyId;
    }

    public String code() {
        return code;
    }

    public UUID policyId() {
        return policyId;
    }
}
