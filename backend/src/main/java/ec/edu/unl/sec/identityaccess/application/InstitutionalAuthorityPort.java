package ec.edu.unl.sec.identityaccess.application;

import ec.edu.unl.sec.identityaccess.domain.AuthorizationScope;
import ec.edu.unl.sec.identityaccess.domain.OperationCode;
import java.util.UUID;

public interface InstitutionalAuthorityPort {
    boolean isAuthorizedAssignment(UUID assignmentId, UUID actorId, OperationCode operation, AuthorizationScope scope);
}
