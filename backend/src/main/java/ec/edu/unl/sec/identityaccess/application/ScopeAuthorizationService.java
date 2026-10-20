package ec.edu.unl.sec.identityaccess.application;

import ec.edu.unl.sec.identityaccess.domain.AuthorizationDecision;
import ec.edu.unl.sec.identityaccess.domain.AuthorizationScope;
import ec.edu.unl.sec.identityaccess.domain.FunctionalAssignment;
import ec.edu.unl.sec.identityaccess.domain.OperationCode;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ScopeAuthorizationService implements AuthorizationPort {
    private final IdentityAccessRepository repository;
    private final Clock clock;

    public ScopeAuthorizationService(IdentityAccessRepository repository) {
        this(repository, Clock.systemUTC());
    }

    ScopeAuthorizationService(IdentityAccessRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public AuthorizationDecision decide(UUID identityId, OperationCode operation, AuthorizationScope requested) {
        Instant now = clock.instant();
        boolean allowed = repository.activeAssignments(identityId, now).stream()
                .filter(assignment -> assignment.activeAt(now))
                .filter(assignment -> assignment.scope().contains(requested))
                .filter(assignment -> repository.functionAllows(assignment.functionId(), operation))
                .filter(assignment -> !repository.hasUnexcusedConflict(
                        identityId, assignment.functionId(), requested, now))
                .map(FunctionalAssignment::functionId)
                .findAny()
                .isPresent();
        return allowed ? AuthorizationDecision.allow() : AuthorizationDecision.deny();
    }

    @Override
    public boolean hasOperation(UUID identityId, OperationCode operation) {
        return repository.hasOperation(identityId, operation, clock.instant());
    }
}
