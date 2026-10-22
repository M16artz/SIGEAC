package ec.edu.unl.sec.identityaccess.application;

import ec.edu.unl.sec.identityaccess.domain.AuthorizationDecision;
import ec.edu.unl.sec.identityaccess.domain.AuthorizationScope;
import ec.edu.unl.sec.identityaccess.domain.FunctionalAssignment;
import ec.edu.unl.sec.identityaccess.domain.OperationCode;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ScopeAuthorizationService implements AuthorizationPort {
    private final IdentityAccessRepository repository;
    private final Clock clock;

    @Autowired
    public ScopeAuthorizationService(IdentityAccessRepository repository) {
        this(repository, Clock.systemUTC());
    }

    ScopeAuthorizationService(IdentityAccessRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public AuthorizationDecision decide(UUID identityId, OperationCode operation, AuthorizationScope requested) {
        if (repository.findUser(identityId).filter(user -> user.active() && !user.technical()).isEmpty())
            return AuthorizationDecision.deny();
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
    public java.util.Set<UUID> authorizedCareerIds(UUID identityId, OperationCode operation) {
        return repository.authorizedCareerIds(identityId,operation,clock.instant()).stream()
                .filter(id->isAllowed(identityId,operation,new AuthorizationScope(id,null,null)))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    @Override
    public boolean hasOperation(UUID identityId, OperationCode operation) {
        return repository.findUser(identityId).filter(user -> user.active() && !user.technical()).isPresent()
                && repository.activeAssignments(identityId,clock.instant()).stream()
                        .filter(assignment->assignment.activeAt(clock.instant()))
                        .anyMatch(assignment->repository.functionAllows(assignment.functionId(),operation)
                                && !repository.hasUnexcusedConflict(identityId,assignment.functionId(),assignment.scope(),clock.instant()));
    }
}
