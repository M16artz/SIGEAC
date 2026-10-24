package ec.edu.unl.sec.documentreview.infrastructure;

import ec.edu.unl.sec.documentreview.application.DocumentReviewRepository;
import ec.edu.unl.sec.documentreview.application.SubmissionCorrectionPort;
import ec.edu.unl.sec.evidence.application.CreateEvidenceSuccessorService;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ports the correction flow into the documentary responsibility module while
 * recording which review decision spawned the corrective successor, so the
 * P10 trace is explicit and double corrections are prevented.
 */
@Component
public class EvidenceSubmissionCorrectionAdapter implements SubmissionCorrectionPort {
    private final CreateEvidenceSuccessorService successors;
    private final DocumentReviewRepository repository;

    public EvidenceSubmissionCorrectionAdapter(CreateEvidenceSuccessorService successors,
            DocumentReviewRepository repository) {
        this.successors = successors;
        this.repository = repository;
    }

    @Override
    @Transactional
    public Successor correct(UUID correctedVersionId, UUID triggeringDecisionId) {
        var created = successors.createSuccessor(correctedVersionId);
        repository.linkSuccessor(triggeringDecisionId, created.versionId());
        return new Successor(created.versionId(), created.evidenceId(), created.revisionNumber());
    }
}
