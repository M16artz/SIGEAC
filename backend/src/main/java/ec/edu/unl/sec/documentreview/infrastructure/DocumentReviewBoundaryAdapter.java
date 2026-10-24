package ec.edu.unl.sec.documentreview.infrastructure;

import ec.edu.unl.sec.documentreview.application.DocumentReviewAuthorizationPort;
import ec.edu.unl.sec.identityaccess.application.AuthorizationPort;
import ec.edu.unl.sec.identityaccess.application.CurrentActorPort;
import ec.edu.unl.sec.identityaccess.domain.AuthorizationScope;
import ec.edu.unl.sec.identityaccess.domain.OperationCode;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Maps documentary review access onto the EVIDENCE_REVIEW capability. */
@Component
public class DocumentReviewBoundaryAdapter implements DocumentReviewAuthorizationPort {
    private final CurrentActorPort actors;
    private final AuthorizationPort authorization;

    public DocumentReviewBoundaryAdapter(CurrentActorPort actors, AuthorizationPort authorization) {
        this.actors = actors;
        this.authorization = authorization;
    }

    @Override
    public Optional<Actor> currentActor() {
        return actors.currentActor().map(actor -> new Actor(actor.identityId(), actor.technical()));
    }

    @Override
    public boolean canReview(UUID actorId, UUID careerId, UUID processId) {
        return authorization.isAllowed(actorId, OperationCode.EVIDENCE_REVIEW,
                new AuthorizationScope(careerId, processId, null));
    }
}
