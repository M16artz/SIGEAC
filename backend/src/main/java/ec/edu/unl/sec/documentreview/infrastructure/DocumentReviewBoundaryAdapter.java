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
    private final ec.edu.unl.sec.selfassessment.application.DocumentaryProcessPort processes;

    public DocumentReviewBoundaryAdapter(CurrentActorPort actors, AuthorizationPort authorization,
            ec.edu.unl.sec.selfassessment.application.DocumentaryProcessPort processes) {
        this.actors = actors;
        this.authorization = authorization;
        this.processes = processes;
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

    @Override
    public void requireOpenForWrite(UUID processId) {
        var process=processes.lock(processId).orElseThrow(() -> new ec.edu.unl.sec.documentreview.application.DocumentReviewProblemException(
                ec.edu.unl.sec.documentreview.application.DocumentReviewProblemException.Kind.NOT_FOUND,"PROCESS_NOT_FOUND","No se encontró el proceso."));
        if (!"ACTIVE".equals(process.state())) throw new ec.edu.unl.sec.documentreview.application.DocumentReviewProblemException(
                ec.edu.unl.sec.documentreview.application.DocumentReviewProblemException.Kind.CONFLICT,"PROCESS_NOT_ACTIVE","La revisión ordinaria requiere un proceso activo.");
    }
}
