package ec.edu.unl.sec.identityaccess.domain;

import java.util.UUID;

public record InstitutionalAuthority(UUID identityId, UUID careerId, String type) {
    public InstitutionalAuthority {
        if (identityId == null || careerId == null) {
            throw new IllegalArgumentException("institutional authority requires identity and career");
        }
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("institutional authority type required");
        }
    }
}
