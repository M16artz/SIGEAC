package ec.edu.unl.sec.documentreview.application;

import java.util.Optional;
import java.util.UUID;

/** Authorization of documentary reviewers within their career/process scope. */
public interface DocumentReviewAuthorizationPort {
    Optional<Actor> currentActor();

    boolean canReview(UUID actorId, UUID careerId, UUID processId);
    void requireOpenForWrite(UUID processId);

    record Actor(UUID identityId, boolean technical) {}
}
