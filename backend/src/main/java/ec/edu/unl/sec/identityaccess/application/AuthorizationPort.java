package ec.edu.unl.sec.identityaccess.application;

import ec.edu.unl.sec.identityaccess.domain.*;
import java.util.UUID;

public interface AuthorizationPort {
    AuthorizationDecision decide(UUID identityId, OperationCode operation, AuthorizationScope scope);

    boolean hasOperation(UUID identityId, OperationCode operation);

    default boolean isAllowed(UUID identityId, OperationCode operation, AuthorizationScope scope) {
        return decide(identityId, operation, scope).allowed();
    }
}
