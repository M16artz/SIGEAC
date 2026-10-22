package ec.edu.unl.sec.identityaccess.application;

import ec.edu.unl.sec.identityaccess.domain.*;
import java.util.UUID;

public interface AuthorizationPort {
    AuthorizationDecision decide(UUID identityId, OperationCode operation, AuthorizationScope scope);

    boolean hasOperation(UUID identityId, OperationCode operation);

    default java.util.Set<UUID> authorizedCareerIds(UUID identityId,OperationCode operation) {return java.util.Set.of();}

    default boolean isAllowed(UUID identityId, OperationCode operation, AuthorizationScope scope) {
        return decide(identityId, operation, scope).allowed();
    }
}
