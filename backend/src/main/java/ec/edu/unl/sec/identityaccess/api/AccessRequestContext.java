package ec.edu.unl.sec.identityaccess.api;

import ec.edu.unl.sec.shared.infrastructure.CorrelationFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.security.core.Authentication;

final class AccessRequestContext {
    private AccessRequestContext() {}

    static UUID identityId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UUID identityId) {
            return identityId;
        }
        return null;
    }

    static UUID correlation(HttpServletRequest request) {
        Object value = request.getAttribute(CorrelationFilter.CORRELATION_ATTRIBUTE);
        if (value == null) return UUID.randomUUID();
        try {
            return UUID.fromString(value.toString());
        } catch (IllegalArgumentException invalid) {
            return UUID.randomUUID();
        }
    }
}
