package ec.edu.unl.sec.evidence.application;

import ec.edu.unl.sec.evidence.domain.ApplicabilityDecision;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Append-only process-level applicability decisions for indicators and
 * required sources; each new decision supersedes the previous live one.
 */
@Service
public class ApplicabilityDecisionService {
    private final EvidenceGovernanceRepository governance;
    private final EvidenceContextPort context;
    private final EvidenceAuthorizationPort authorization;
    private final RequiredSourceCatalogPort catalog;
    private final Clock clock;

    @Autowired
    public ApplicabilityDecisionService(EvidenceGovernanceRepository governance, EvidenceContextPort context,
            EvidenceAuthorizationPort authorization, RequiredSourceCatalogPort catalog) {
        this(governance, context, authorization, catalog, Clock.systemUTC());
    }

    ApplicabilityDecisionService(EvidenceGovernanceRepository governance, EvidenceContextPort context,
            EvidenceAuthorizationPort authorization, RequiredSourceCatalogPort catalog, Clock clock) {
        this.governance = governance;
        this.context = context;
        this.authorization = authorization;
        this.catalog = catalog;
        this.clock = clock;
    }

    @Transactional
    public ApplicabilityDecision decide(UUID processId, ApplicabilityInput input) {
        var actor = currentActor();
        if (actor.technical()) throw forbidden();
        var process = context.process(processId)
                .orElseThrow(() -> new EvidenceProblemException(
                        EvidenceProblemException.Kind.NOT_FOUND,
                        "RESOURCE_NOT_FOUND",
                        "No se encontró el proceso."));
        if (!authorization.canDecideApplicability(actor.identityId(), process.careerId(), processId)) {
            throw forbidden();
        }
        validateSubject(process, input.subjectType(), input.subjectId());
        var latest = governance.latestApplicability(processId, input.subjectType(), input.subjectId()).orElse(null);
        long expected = latest == null ? 0 : latest.revision();
        if (expected != input.expectedRevision()) {
            throw new EvidenceProblemException(EvidenceProblemException.Kind.PRECONDITION_FAILED,
                    "STALE_APPLICABILITY",
                    "La aplicabilidad cambió; recargue antes de continuar.");
        }
        var decision = new ApplicabilityDecision(
                null,
                processId,
                input.subjectType(),
                input.subjectId(),
                input.decision(),
                input.ruleReference(),
                input.provenance().reason(),
                input.provenance().legalBasis(),
                input.provenance().documentReference(),
                actor.identityId(),
                clock.instant(),
                latest == null ? null : latest.id(),
                latest == null ? 0 : latest.revision() + 1);
        var saved = governance.saveApplicability(decision);
        return saved == null ? decision : saved;
    }

    @Transactional(readOnly = true)
    public List<ApplicabilityDecision> history(UUID processId) {
        var actor = currentActor();
        if (actor.technical()) throw forbidden();
        var process = context.process(processId)
                .orElseThrow(() -> new EvidenceProblemException(
                        EvidenceProblemException.Kind.NOT_FOUND,
                        "RESOURCE_NOT_FOUND",
                        "No se encontró el proceso."));
        if (!authorization.canDecideApplicability(actor.identityId(), process.careerId(), processId)) {
            throw forbidden();
        }
        return governance.applicabilityHistory(processId);
    }

    public List<ApplicabilityDecision> decisionsFor(List<ApplicabilityDecision> history,
            ApplicabilityDecision.SubjectType subjectType, UUID subjectId) {
        return history.stream()
                .filter(decision -> decision.subjectType() == subjectType && decision.subjectId().equals(subjectId))
                .toList();
    }

    private void validateSubject(EvidenceContextPort.ProcessContext process,
            ApplicabilityDecision.SubjectType subjectType, UUID subjectId) {
        boolean valid = switch (subjectType) {
            case INDICATOR -> process.indicators().stream().anyMatch(indicator -> indicator.id().equals(subjectId));
            case REQUIRED_SOURCE -> catalog.model(process.fixedModelVersionId())
                    .map(RequiredSourceCatalogPort.ModelSources::sources)
                    .orElse(List.of())
                    .stream()
                    .anyMatch(source -> source.id().equals(subjectId));
        };
        if (!valid) {
            throw new EvidenceProblemException(EvidenceProblemException.Kind.INVALID,
                    "APPLICABILITY_SUBJECT_UNKNOWN",
                    "El sujeto de la decisión no pertenece al proceso ni al modelo fijado.");
        }
    }

    private EvidenceAuthorizationPort.Actor currentActor() {
        return authorization.currentActor()
                .orElseThrow(() -> new EvidenceProblemException(
                        EvidenceProblemException.Kind.UNAUTHORIZED,
                        "AUTHENTICATION_REQUIRED",
                        "Se requiere una sesión vigente."));
    }

    private static EvidenceProblemException forbidden() {
        return new EvidenceProblemException(EvidenceProblemException.Kind.FORBIDDEN,
                "APPLICABILITY_DECISION_DENIED",
                "La decisión de aplicabilidad requiere una asignación competente.");
    }

    public record DecisionInput(UUID authorityAssignmentId, String legalBasis, String documentReference,
            String reason) {}

    public record ApplicabilityInput(ApplicabilityDecision.SubjectType subjectType, UUID subjectId,
            ApplicabilityDecision.Decision decision, String ruleReference, long expectedRevision,
            DecisionInput provenance) {}
}
