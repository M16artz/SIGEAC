package ec.edu.unl.sec.evidence.api;

import ec.edu.unl.sec.evidence.domain.ExternalReference;
import java.util.UUID;

public record ExternalReferenceView(UUID id, String uri, String description) {
    static ExternalReferenceView from(ExternalReference value) {
        return new ExternalReferenceView(value.id(), value.uri(), value.description());
    }
}
