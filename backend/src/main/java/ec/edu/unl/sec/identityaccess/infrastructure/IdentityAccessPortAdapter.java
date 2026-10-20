package ec.edu.unl.sec.identityaccess.infrastructure;

import ec.edu.unl.sec.identityaccess.application.CurrentActorPort;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class IdentityAccessPortAdapter implements CurrentActorPort {
    @Override
    public Optional<Actor> currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UUID identityId)) {
            return Optional.empty();
        }
        boolean technical = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_TECHNICAL".equals(authority.getAuthority()));
        return Optional.of(new Actor(identityId, technical));
    }
}
