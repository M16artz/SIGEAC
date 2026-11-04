package ec.edu.unl.sec.identityaccess.application;

import ec.edu.unl.sec.identityaccess.domain.AuthorizationScope;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service public class FunctionalAssignmentPolicyService implements FunctionalAssignmentPolicyPort {
    private final IdentityAccessRepository repository;private final Clock clock=Clock.systemUTC();
    public FunctionalAssignmentPolicyService(IdentityAccessRepository repository){this.repository=repository;}
    @Override
    public Decision evaluateMembership(UUID identityId, UUID assignmentId, AuthorizationScope scope,
            Instant from, Instant to) {
        if (from == null || to == null || !from.isBefore(to)) return Decision.deny("INVALID_VIGENCY");
        if (identityId == null || assignmentId == null || repository.findUser(identityId)
                .filter(user -> user.active() && !user.technical()).isEmpty())
            return Decision.deny("IDENTITY_NOT_ELIGIBLE");
        var assignment = repository.activeAssignments(identityId, from).stream()
                .filter(a -> a.id().equals(assignmentId) && a.identityId().equals(identityId))
                .filter(a -> a.activeAt(from) && !to.isAfter(a.validTo()) && a.scope().contains(scope))
                .findFirst();
        if (assignment.isEmpty()) return Decision.deny("MEMBERSHIP_ROLE_INVALID");
        if (repository.findConflictingPolicy(identityId, assignment.get().functionId(), scope,
                from, to, clock.instant()).isPresent()) return Decision.deny("FUNCTION_CONFLICT");
        return Decision.allow();
    }
    @Override public Decision evaluate(UUID identityId,String functionCode,AuthorizationScope scope,Instant from,Instant to){if(from==null||to==null||!from.isBefore(to))return Decision.deny("INVALID_VIGENCY");var user=repository.findUser(identityId).orElse(null);if(user==null||!user.active()||user.technical())return Decision.deny("IDENTITY_NOT_ELIGIBLE");var function=repository.findFunctionIdByCode(functionCode).orElse(null);if(function==null)return Decision.deny("FUNCTION_UNKNOWN");if(repository.findConflictingPolicy(identityId,function,scope,from,to,clock.instant()).isPresent())return Decision.deny("FUNCTION_CONFLICT");return Decision.allow();}
}
