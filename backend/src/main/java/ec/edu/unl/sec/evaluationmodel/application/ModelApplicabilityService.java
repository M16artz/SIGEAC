package ec.edu.unl.sec.evaluationmodel.application;

import ec.edu.unl.sec.evaluationmodel.domain.IndicatorDefinition;
import ec.edu.unl.sec.evaluationmodel.domain.ModelCareerApplicability;
import ec.edu.unl.sec.evaluationmodel.domain.ModelVersion;
import ec.edu.unl.sec.evaluationmodel.domain.ModelVersionStatus;
import ec.edu.unl.sec.evaluationmodel.domain.NormativeReference;
import ec.edu.unl.sec.identityaccess.application.AuthorizationPort;
import ec.edu.unl.sec.identityaccess.application.CurrentActorPort;
import ec.edu.unl.sec.identityaccess.domain.OperationCode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ModelApplicabilityService {
    private final EvaluationModelRepository models;
    private final NormativeReferenceRepository references;
    private final CurrentActorPort actors;
    private final AuthorizationPort authorization;
    private final EvaluationModelAuditService audit;
    private final Clock clock;

    @Autowired public ModelApplicabilityService(EvaluationModelRepository models, NormativeReferenceRepository references,
            CurrentActorPort actors, AuthorizationPort authorization, EvaluationModelAuditService audit) {
        this(models, references, actors, authorization, audit, Clock.systemUTC());
    }

    ModelApplicabilityService(EvaluationModelRepository models, NormativeReferenceRepository references,
            CurrentActorPort actors, AuthorizationPort authorization, EvaluationModelAuditService audit, Clock clock) {
        this.models = models; this.references = references; this.actors = actors;
        this.authorization = authorization; this.audit = audit; this.clock = clock;
    }

    @Transactional
    public NormativeReference registerReference(NormativeReference reference, UUID correlation) {
        UUID actor = actor(OperationCode.MODEL_APPLICABILITY_AUTHORIZE);
        if (reference.verified() && !actor.equals(reference.verifiedBy()))
            throw problem(ModelProblemException.Kind.FORBIDDEN, "NORMATIVE_VERIFIER_MISMATCH",
                    "La referencia solo puede verificarla la identidad autenticada.");
        references.findReference(reference.id()).ifPresent(existing -> {
            if (!existing.equals(reference))
                throw problem(ModelProblemException.Kind.CONFLICT, "NORMATIVE_REFERENCE_IMMUTABLE",
                        "La referencia normativa es inmutable.");
        });
        references.saveReference(reference);
        audit.record(actor, correlation, "NORMATIVE_REFERENCE_REGISTERED", "NORMATIVE_REFERENCE", reference.id(),
                Map.of("verificationStatus", reference.verificationStatus().name()));
        return reference;
    }

    @Transactional
    public ModelCareerApplicability decide(UUID versionId, ApplicabilityInput input, long expectedRevision, UUID correlation) {
        UUID actor = actor(OperationCode.MODEL_APPLICABILITY_AUTHORIZE);
        ModelVersion version = models.findVersion(versionId).orElseThrow(ModelApplicabilityService::versionNotFound);
        if (!references.careerExists(input.careerId()))
            throw problem(ModelProblemException.Kind.NOT_FOUND, "CAREER_NOT_FOUND", "La carrera no existe.");
        if (version.configuration() == null)
            throw problem(ModelProblemException.Kind.INVALID, "MODEL_CONFIGURATION_REQUIRED",
                    "Se requiere la configuracion por indicador.");
        if (version.status() != ModelVersionStatus.PUBLISHED)
            throw problem(ModelProblemException.Kind.PRECONDITION, "MODEL_VERSION_NOT_PUBLISHED",
                    "La version del modelo no esta publicada.");
        ModelCareerApplicability latest = references.latestApplicability(versionId, input.careerId()).orElse(null);
        long actualRevision = latest == null ? 0 : latest.revision();
        if (actualRevision != expectedRevision)
            throw problem(ModelProblemException.Kind.PRECONDITION, "STALE_MODEL_APPLICABILITY",
                    "La aplicabilidad cambio; recargue antes de continuar.");
        if (input.decision() == ModelCareerApplicability.Status.APPLICABLE) requireVerified(version);
        Set<NormativeReferenceRepository.IndicatorLink> links = new LinkedHashSet<>();
        for (IndicatorDefinition indicator : version.configuration().indicators()) {
            for (NormativeReference reference : indicator.normativeReferences()) {
                if (reference.verified() && !actor.equals(reference.verifiedBy()))
                    throw problem(ModelProblemException.Kind.FORBIDDEN, "NORMATIVE_VERIFIER_MISMATCH",
                            "La referencia solo puede verificarla la identidad autenticada.");
                references.findReference(reference.id()).ifPresent(existing -> {
                    if (!existing.equals(reference))
                        throw problem(ModelProblemException.Kind.CONFLICT, "NORMATIVE_REFERENCE_IMMUTABLE",
                                "La referencia normativa es inmutable.");
                });
                references.saveReference(reference);
            }
            links.addAll(links(indicator));
        }
        references.replaceIndicatorLinks(versionId, links);
        Instant now = clock.instant();
        ModelCareerApplicability decision = new ModelCareerApplicability(null, versionId, input.careerId(),
                input.decision(), input.validFrom(), input.validUntil(), input.provenance(),
                latest == null ? null : latest.id(), (int) actualRevision + 1, actor, now);
        references.appendApplicability(decision);
        audit.record(actor, correlation, "MODEL_APPLICABILITY_RECORDED", "MODEL_VERSION", versionId,
                Map.of("careerId", input.careerId().toString(), "decision", input.decision().name(),
                        "revision", decision.revision()));
        return decision;
    }

    @Transactional
    public ModelCareerApplicability assertUsableForActivation(UUID versionId, UUID careerId, LocalDate date) {
        ModelVersion version = models.findVersion(versionId).orElseThrow(ModelApplicabilityService::versionNotFound);
        if (version.status() != ModelVersionStatus.PUBLISHED)
            throw problem(ModelProblemException.Kind.PRECONDITION, "MODEL_VERSION_NOT_PUBLISHED",
                    "La version del modelo no esta publicada.");
        ModelCareerApplicability latest = references.latestApplicability(versionId, careerId)
                .orElseThrow(() -> problem(ModelProblemException.Kind.PRECONDITION, "MODEL_APPLICABILITY_PENDING",
                        "La aplicabilidad institucional no ha sido decidida."));
        requireVerified(version);
        if (!latest.effectiveOn(date))
            throw problem(ModelProblemException.Kind.PRECONDITION, "MODEL_NOT_APPLICABLE",
                    "El modelo no es aplicable a la carrera en la fecha indicada.");
        return latest;
    }

    @Transactional(readOnly = true)
    public List<ModelCareerApplicability> history(UUID versionId) {
        actor(OperationCode.MODEL_READ);
        if (models.findVersion(versionId).isEmpty()) throw versionNotFound();
        return references.listApplicability(versionId);
    }

    @Transactional(readOnly = true)
    public List<NormativeReference> references(UUID versionId) {
        actor(OperationCode.MODEL_READ);
        if (models.findVersion(versionId).isEmpty()) throw versionNotFound();
        return references.indicatorLinks(versionId).stream()
                .map(link -> references.findReference(link.referenceId()).orElse(null))
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toCollection(LinkedHashSet::new), List::copyOf));
    }

    private void requireVerified(ModelVersion version) {
        if (version.configuration() == null || version.configuration().indicators().isEmpty())
            throw problem(ModelProblemException.Kind.INVALID, "MODEL_APPLICABILITY_UNRESOLVED",
                    "La configuracion no registra referencias normativas verificadas.");
        for (IndicatorDefinition indicator : version.configuration().indicators()) {
            if (indicator.normativeReferences().isEmpty() || indicator.normativeReferences().stream().anyMatch(r -> !r.verified()))
                throw problem(ModelProblemException.Kind.PRECONDITION, "MODEL_APPLICABILITY_UNRESOLVED",
                        "Existen referencias normativas sin verificar para el indicador " + indicator.nodeId() + ".");
        }
    }

    private static Set<NormativeReferenceRepository.IndicatorLink> links(IndicatorDefinition indicator) {
        Set<NormativeReferenceRepository.IndicatorLink> links = new LinkedHashSet<>();
        for (NormativeReference reference : indicator.normativeReferences())
            links.add(new NormativeReferenceRepository.IndicatorLink(indicator.nodeId(), reference.id()));
        return links;
    }

    private UUID actor(OperationCode operation) {
        var current = actors.currentActor().orElseThrow(() -> problem(ModelProblemException.Kind.UNAUTHORIZED,
                "AUTHENTICATION_REQUIRED", "Se requiere autenticacion."));
        if (current.technical() || !authorization.hasOperation(current.identityId(), operation))
            throw problem(ModelProblemException.Kind.FORBIDDEN, "MODEL_SCOPE_DENIED", "La operacion no esta permitida.");
        return current.identityId();
    }

    private static ModelProblemException versionNotFound() {
        return problem(ModelProblemException.Kind.NOT_FOUND, "MODEL_VERSION_NOT_FOUND",
                "El recurso no existe o no esta visible.");
    }

    private static ModelProblemException problem(ModelProblemException.Kind kind, String code, String message) {
        return new ModelProblemException(kind, code, message);
    }

    public record ApplicabilityInput(UUID careerId, ModelCareerApplicability.Status decision, LocalDate validFrom,
            LocalDate validUntil, ModelCareerApplicability.Decision provenance) {
        public ApplicabilityInput {
            if (careerId == null || decision == null || validFrom == null || provenance == null)
                throw new IllegalArgumentException("career, decision, validFrom and provenance required");
            if (decision == ModelCareerApplicability.Status.PENDING_VALIDATION)
                throw new IllegalArgumentException("pending validation is not an institutional decision");
        }
    }
}
