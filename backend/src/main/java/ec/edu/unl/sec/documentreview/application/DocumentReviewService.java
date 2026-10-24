package ec.edu.unl.sec.documentreview.application;

import ec.edu.unl.sec.audit.application.AuditEventCommand;
import ec.edu.unl.sec.audit.application.AuditPort;
import ec.edu.unl.sec.documentreview.domain.DocumentReviewDecision;
import ec.edu.unl.sec.documentreview.domain.ReviewCoverageOutcome;
import java.time.Clock;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Documentary review of submitted evidence versions (D5, P09): one justified
 * decision per coverage outcome, derived global status, self-review blocked
 * except for an explicit auditable exception, and append-only rectifications.
 * Accepting documentation never creates an academic valuation (FR-023).
 */
@Service
public class DocumentReviewService {
    private final ReviewSubmissionPort submissions;
    private final DocumentReviewRepository repository;
    private final DocumentReviewAuthorizationPort authorization;
    private final SubmissionCorrectionPort correction;
    private final AuditPort audit;
    private final Clock clock;

    @Autowired
    public DocumentReviewService(ReviewSubmissionPort submissions, DocumentReviewRepository repository,
            DocumentReviewAuthorizationPort authorization, SubmissionCorrectionPort correction, AuditPort audit) {
        this(submissions, repository, authorization, correction, audit, Clock.systemUTC());
    }

    DocumentReviewService(ReviewSubmissionPort submissions, DocumentReviewRepository repository,
            DocumentReviewAuthorizationPort authorization, SubmissionCorrectionPort correction, AuditPort audit,
            Clock clock) {
        this.submissions = submissions;
        this.repository = repository;
        this.authorization = authorization;
        this.correction = correction;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public DocumentReviewDecision decide(UUID versionId, ReviewInput input) {
        var actor = currentActor();
        if (actor.technical()) throw forbidden("REVIEW_NOT_AUTHORIZED");
        var submission = submissions.submission(versionId).orElseThrow(() ->
                new DocumentReviewProblemException(DocumentReviewProblemException.Kind.NOT_FOUND,
                        "DOCUMENT_REVIEW_SUBMISSION_NOT_FOUND", "No se encontró la versión remitida."));
        if (!authorization.canReview(actor.identityId(), submission.careerId(), submission.processId())) {
            throw forbidden("REVIEW_NOT_AUTHORIZED");
        }
        if (submissions.latestRevisionNumber(submission.evidenceId()) != submission.revisionNumber()) {
            throw new DocumentReviewProblemException(DocumentReviewProblemException.Kind.CONFLICT,
                    "VERSION_SUPERSEDED", "La versión ya tiene una sucesora; revise la versión vigente.");
        }
        var latest = repository.latest(versionId);
        long expected = latest.map(decision -> decision.revision() + 1).orElse(0L);
        if (expected != input.expectedRevision()) {
            throw new DocumentReviewProblemException(DocumentReviewProblemException.Kind.PRECONDITION_FAILED,
                    "STALE_REVIEW", "La revisión cambió; recargue antes de continuar.");
        }
        boolean selfReview = submission.submitterId().equals(actor.identityId());
        if (selfReview && !input.selfReviewException()) {
            throw forbidden("SELF_REVIEW_FORBIDDEN");
        }
        var outcomes = coverOutcomes(input.outcomes(), submission);
        var derived = DocumentReviewDecision.derive(outcomes);
        var decision = new DocumentReviewDecision(
                null,
                versionId,
                derived,
                input.justification(),
                actor.identityId(),
                clock.instant(),
                selfReview,
                input.exceptionReason(),
                latest.map(DocumentReviewDecision::id).orElse(null),
                null,
                latest.map(DocumentReviewDecision::revision).orElse(-1L) + 1,
                outcomes);
        return persist(decision, submission);
    }

    @Transactional(readOnly = true)
    public List<ReviewSubmissionPort.PendingSubmission> inbox(UUID processId) {
        return scoped(processId, submissions.pending(processId));
    }

    @Transactional(readOnly = true)
    public List<DocumentReviewDecision> history(UUID processId) {
        scoped(processId, List.of());
        return repository.historyForProcess(processId);
    }

    private <T> List<T> scoped(UUID processId, List<T> pending) {
        var actor = currentActor();
        if (actor.technical()) throw forbidden("REVIEW_NOT_AUTHORIZED");
        var career = submissions.career(processId).orElseThrow(() ->
                new DocumentReviewProblemException(DocumentReviewProblemException.Kind.NOT_FOUND,
                        "PROCESS_NOT_FOUND", "No se encontró el proceso."));
        if (!authorization.canReview(actor.identityId(), career, processId)) {
            throw forbidden("REVIEW_NOT_AUTHORIZED");
        }
        return pending;
    }

    @Transactional(readOnly = true)
    public Optional<DocumentReviewDecision> versionReview(UUID versionId) {
        currentActor();
        return repository.latest(versionId);
    }

    /**
     * Creates the correction successor (P10) linked to the triggering review
     * decision; the predecessor and its decisions stay immutable and no
     * acceptance is inherited.
     */
    @Transactional
    public Correction correct(UUID decisionId, long expectedDecisionRevision) {
        var actor = currentActor();
        if (actor.technical()) throw forbidden("CORRECTION_NOT_AUTHORIZED");
        var decision = repository.decision(decisionId).orElseThrow(() ->
                new DocumentReviewProblemException(DocumentReviewProblemException.Kind.NOT_FOUND,
                        "REVIEW_DECISION_NOT_FOUND", "No se encontró la decisión de revisión."));
        if (decision.successorVersionId() != null) {
            throw new DocumentReviewProblemException(DocumentReviewProblemException.Kind.CONFLICT,
                    "SUCCESSOR_EXISTS", "La decisión ya tiene una corrección registrada.");
        }
        if (decision.revision() != expectedDecisionRevision) {
            throw new DocumentReviewProblemException(DocumentReviewProblemException.Kind.PRECONDITION_FAILED,
                    "STALE_REVIEW", "La revisión cambió; recargue antes de continuar.");
        }
        var submission = submissions.submission(decision.versionId()).orElseThrow(() ->
                new DocumentReviewProblemException(DocumentReviewProblemException.Kind.NOT_FOUND,
                        "DOCUMENT_REVIEW_SUBMISSION_NOT_FOUND", "No se encontró la versión remitida."));
        if (submissions.latestRevisionNumber(submission.evidenceId()) != submission.revisionNumber()) {
            throw new DocumentReviewProblemException(DocumentReviewProblemException.Kind.CONFLICT,
                    "SUCCESSOR_EXISTS", "La versión ya tiene una sucesora directa.");
        }
        var successor = correction.correct(submission.versionId(), decision.id());
        audit.record(new AuditEventCommand(
                clock.instant(),
                actor.identityId(),
                null,
                UUID.randomUUID(),
                "EVIDENCE_CORRECTION",
                "EVIDENCE_VERSION",
                successor.versionId(),
                submission.careerId(),
                submission.processId(),
                "SUCCESS",
                "CORRECTION_CREATED",
                java.util.Map.of("decisionId", decision.id().toString(),
                        "revisionNumber", successor.revisionNumber())));
        return new Correction(decision.versionId(), successor.versionId(), successor.revisionNumber());
    }

    private List<ReviewCoverageOutcome> coverOutcomes(List<ReviewCoverageOutcome> input,
            ReviewSubmissionPort.SubmittedVersion submission) {
        if (input == null || input.isEmpty()) {
            throw new DocumentReviewProblemException(DocumentReviewProblemException.Kind.INVALID,
                    "COVERAGE_OUTCOME_REQUIRED",
                    "La revisión requiere el resultado de cada cobertura declarada.");
        }
        Set<UUID> covered = new LinkedHashSet<>();
        for (UUID requiredSourceId : input.stream().map(ReviewCoverageOutcome::requiredSourceId).toList()) {
            if (covered.add(requiredSourceId) && (!submission.coveredRequiredSourceIds().contains(requiredSourceId))) {
                throw new DocumentReviewProblemException(DocumentReviewProblemException.Kind.INVALID,
                        "OUTCOME_WITHOUT_COVERAGE",
                        "Un resultado no corresponde a una cobertura declarada de la versión.");
            }
        }
        return List.copyOf(input);
    }

    private DocumentReviewDecision persist(DocumentReviewDecision decision,
            ReviewSubmissionPort.SubmittedVersion submission) {
        var saved = repository.save(decision);
        audit.record(new AuditEventCommand(
                clock.instant(),
                decision.reviewerId(),
                null,
                UUID.randomUUID(),
                "DOCUMENT_REVIEW_DECISION",
                "EVIDENCE_VERSION",
                decision.versionId(),
                submission.careerId(),
                submission.processId(),
                "SUCCESS",
                decision.decision().name(),
                Map.of("revision", decision.revision(),
                        "outcomes", decision.outcomes().size(),
                        "selfReviewException", decision.selfReviewException())));
        return saved == null ? decision : saved;
    }

    private DocumentReviewAuthorizationPort.Actor currentActor() {
        return authorization.currentActor().orElseThrow(() ->
                new DocumentReviewProblemException(DocumentReviewProblemException.Kind.UNAUTHORIZED,
                        "AUTHENTICATION_REQUIRED", "Se requiere una sesión vigente."));
    }

    private static DocumentReviewProblemException forbidden(String code) {
        return new DocumentReviewProblemException(DocumentReviewProblemException.Kind.FORBIDDEN,
                code, "La revisión documental requiere un revisor competente.");
    }

    public record ReviewInput(long expectedRevision, boolean selfReviewException, String exceptionReason,
            String justification, List<ReviewCoverageOutcome> outcomes) {}

    public record Correction(UUID correctedVersionId, UUID successorVersionId, int successorRevision) {}
}
