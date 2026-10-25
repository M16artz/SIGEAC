package ec.edu.unl.sec.governance.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Canonical, single reply per observation; the chain points to the corrected version it triggered. */
public record ObservationReply(UUID id, UUID observationId, String response, UUID correctedVersionId,
        UUID recordedBy, Instant recordedAt) {
    public ObservationReply {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(observationId, "observationId");
        if (response == null || response.strip().length() < 10 || response.strip().length() > 4000)
            throw new IllegalArgumentException("reply text out of bounds");
        Objects.requireNonNull(recordedBy, "recordedBy");
        Objects.requireNonNull(recordedAt, "recordedAt");
        response = response.strip();
        recordedAt = recordedAt.truncatedTo(ChronoUnit.MILLIS);
    }
}
