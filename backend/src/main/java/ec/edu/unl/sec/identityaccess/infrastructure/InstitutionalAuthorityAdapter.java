package ec.edu.unl.sec.identityaccess.infrastructure;

import ec.edu.unl.sec.identityaccess.application.IdentityAccessRepository;
import ec.edu.unl.sec.identityaccess.application.InstitutionalAuthorityPort;
import ec.edu.unl.sec.identityaccess.domain.AuthorizationScope;
import ec.edu.unl.sec.identityaccess.domain.OperationCode;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class InstitutionalAuthorityAdapter implements InstitutionalAuthorityPort {
    private final IdentityAccessRepository repository;

    public InstitutionalAuthorityAdapter(IdentityAccessRepository repository) { this.repository = repository; }

    @Override
    public boolean isAuthorizedAssignment(UUID assignmentId, UUID actorId, OperationCode operation, AuthorizationScope scope) {
        if (repository.findUser(actorId).filter(user -> user.active() && !user.technical()).isEmpty()) return false;
        Instant now = Instant.now();
        return repository.activeAssignments(actorId, now).stream()
                .anyMatch(assignment -> assignment.id().equals(assignmentId) && assignment.identityId().equals(actorId) && assignment.activeAt(now)
                        && assignment.scope().contains(scope) && repository.functionAllows(assignment.functionId(), operation)
                        && !repository.hasUnexcusedConflict(actorId, assignment.functionId(), scope, now));
    }
}
