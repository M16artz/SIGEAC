package ec.edu.unl.sec.identityaccess.domain;

import java.util.UUID;

public record AuthorizationScope(UUID careerId, UUID processId, UUID indicatorId) {
    public AuthorizationScope {
        if (careerId == null) throw new IllegalArgumentException("careerId required");
        if (indicatorId != null && processId == null) throw new IllegalArgumentException("indicator requires process");
    }

    public boolean contains(AuthorizationScope requested) {
        return careerId.equals(requested.careerId)
                && (processId == null || processId.equals(requested.processId))
                && (indicatorId == null || indicatorId.equals(requested.indicatorId));
    }

    public boolean overlaps(AuthorizationScope other) {
        if (other == null || !careerId.equals(other.careerId)) return false;
        if (processId != null && other.processId != null && !processId.equals(other.processId)) return false;
        if (indicatorId != null && other.indicatorId != null && !indicatorId.equals(other.indicatorId)) return false;
        return true;
    }
}
