package ec.edu.unl.sec.evaluationmodel.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ec.edu.unl.sec.evaluationmodel.domain.IndicatorDefinition;
import ec.edu.unl.sec.evaluationmodel.domain.ModelCareerApplicability;
import ec.edu.unl.sec.evaluationmodel.domain.ModelConfiguration;
import ec.edu.unl.sec.evaluationmodel.domain.ModelVersion;
import ec.edu.unl.sec.evaluationmodel.domain.NormativeReference;
import ec.edu.unl.sec.evaluationmodel.support.ConfiguredModelFixtures;
import ec.edu.unl.sec.evaluationmodel.support.InMemoryEvaluationModelRepository;
import ec.edu.unl.sec.evaluationmodel.support.InMemoryNormativeReferenceRepository;
import ec.edu.unl.sec.evaluationmodel.support.ModelFixtures;
import ec.edu.unl.sec.identityaccess.application.AuthorizationPort;
import ec.edu.unl.sec.identityaccess.application.CurrentActorPort;
import ec.edu.unl.sec.identityaccess.domain.OperationCode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NormativeApplicabilityTest {
    private final InMemoryEvaluationModelRepository models = new InMemoryEvaluationModelRepository();
    private final InMemoryNormativeReferenceRepository references = new InMemoryNormativeReferenceRepository();
    private final AtomicReference<CurrentActorPort.Actor> actor = new AtomicReference<>();
    private final AuthorizationPort authorization = mock(AuthorizationPort.class);
    private final EvaluationModelAuditService audit = mock(EvaluationModelAuditService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-22T12:00:00Z"), ZoneOffset.UTC);
    private final UUID careerId = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private EvaluationModelService modelsService;
    private ModelApplicabilityService service;

    @BeforeEach void setup() {
        actor.set(new CurrentActorPort.Actor(ModelFixtures.AUTHOR, false));
        when(authorization.hasOperation(any(), any())).thenReturn(true);
        modelsService = new EvaluationModelService(models, () -> Optional.of(actor.get()), authorization,
                new EvaluationModelProperties(null, null), audit);
        service = new ModelApplicabilityService(models, references, () -> Optional.of(actor.get()), authorization, audit, clock);
        references.careers.add(careerId);
    }

    @Test void doesNotInferApplicabilityFromPublication() {
        ModelVersion published = publish(references(false));
        assertProblem(() -> service.assertUsableForActivation(published.id(), careerId, LocalDate.parse("2026-10-22")),
                "MODEL_APPLICABILITY_PENDING");
    }

    @Test void blocksApplicableDecisionWhenInstitutionalVerificationIsPending() {
        ModelVersion published = publish(references(false));
        actor.set(new CurrentActorPort.Actor(ModelFixtures.APPROVER, false));
        assertProblem(() -> service.decide(published.id(), applicable(LocalDate.parse("2026-10-01"), null), 0, UUID.randomUUID()),
                "MODEL_APPLICABILITY_UNRESOLVED");
        assertThat(references.decisions).isEmpty();
    }

    @Test void allowsActivationOnlyWithVerifiedReferencesAndExplicitDecision() {
        ModelVersion published = publish(references(true));
        actor.set(new CurrentActorPort.Actor(ModelFixtures.APPROVER, false));
        ModelCareerApplicability decision = service.decide(published.id(),
                applicable(LocalDate.parse("2026-10-01"), LocalDate.parse("2027-09-30")), 0, UUID.randomUUID());
        assertThat(decision.revision()).isEqualTo(1);
        assertThat(service.assertUsableForActivation(published.id(), careerId, LocalDate.parse("2026-10-22")))
                .isEqualTo(decision);
        assertThat(references.links.get(published.id())).hasSize(2);
    }

    @Test void blocksActivationOutsideExplicitValidityAndAfterNonApplicableDecision() {
        ModelVersion published = publish(references(true));
        actor.set(new CurrentActorPort.Actor(ModelFixtures.APPROVER, false));
        service.decide(published.id(), applicable(LocalDate.parse("2026-11-01"), null), 0, UUID.randomUUID());
        assertProblem(() -> service.assertUsableForActivation(published.id(), careerId, LocalDate.parse("2026-10-22")),
                "MODEL_NOT_APPLICABLE");
        service.decide(published.id(), new ModelApplicabilityService.ApplicabilityInput(careerId,
                ModelCareerApplicability.Status.NOT_APPLICABLE, LocalDate.parse("2026-10-01"), null,
                provenance()), 1, UUID.randomUUID());
        assertProblem(() -> service.assertUsableForActivation(published.id(), careerId, LocalDate.parse("2026-10-22")),
                "MODEL_NOT_APPLICABLE");
    }

    @Test void appendsDecisionsAndRejectsStaleRevision() {
        ModelVersion published = publish(references(true));
        actor.set(new CurrentActorPort.Actor(ModelFixtures.APPROVER, false));
        var first = service.decide(published.id(), applicable(LocalDate.parse("2026-10-01"), null), 0, UUID.randomUUID());
        var second = service.decide(published.id(), applicable(LocalDate.parse("2026-10-02"), null), 1, UUID.randomUUID());
        assertThat(first.revision()).isEqualTo(1);
        assertThat(second.revision()).isEqualTo(2);
        assertThat(second.predecessorId()).isEqualTo(first.id());
        assertProblem(() -> service.decide(published.id(), applicable(LocalDate.parse("2026-10-03"), null), 1,
                UUID.randomUUID()), "STALE_MODEL_APPLICABILITY");
        assertThat(references.decisions).hasSize(2);
    }

    @Test void rejectsVerifiedReferenceClaimedByAnotherIdentity() {
        NormativeReference forged = verifiedReference(UUID.randomUUID(), ModelFixtures.AUTHOR);
        actor.set(new CurrentActorPort.Actor(ModelFixtures.APPROVER, false));
        assertProblem(() -> service.registerReference(forged, UUID.randomUUID()), "NORMATIVE_VERIFIER_MISMATCH");
    }

    @Test void requiresTheApplicabilityAuthorityOperation() {
        ModelVersion published = publish(references(true));
        when(authorization.hasOperation(any(), any())).thenAnswer(invocation ->
                invocation.getArgument(1, OperationCode.class).equals(OperationCode.MODEL_READ));
        actor.set(new CurrentActorPort.Actor(ModelFixtures.APPROVER, false));
        assertProblem(() -> service.decide(published.id(), applicable(LocalDate.parse("2026-10-01"), null), 0,
                UUID.randomUUID()), "MODEL_SCOPE_DENIED");
    }

    private ModelVersion publish(ModelConfiguration configuration) {
        var fixture = ConfiguredModelFixtures.first();
        var model = modelsService.createModel(fixture.name(), "Sintetico", UUID.randomUUID());
        var draft = modelsService.createDraft(model.id(), fixture.source(), fixture.nodes(), configuration, UUID.randomUUID());
        actor.set(new CurrentActorPort.Actor(ModelFixtures.APPROVER, false));
        return modelsService.publishConfigured(draft.id(), 0, ModelFixtures.APPROVER, UUID.randomUUID());
    }

    private ModelConfiguration references(boolean verified) {
        var fixture = ConfiguredModelFixtures.first().configuration();
        List<IndicatorDefinition> indicators = fixture.indicators().stream().map(indicator -> {
            NormativeReference reference = verified
                    ? verifiedReference(UUID.randomUUID(), ModelFixtures.APPROVER)
                    : pendingReference(UUID.randomUUID());
            return new IndicatorDefinition(indicator.nodeId(), indicator.nature(), indicator.standardText(),
                    indicator.fundamentalElements(), indicator.evaluationPeriodDefinition(), indicator.ratingScaleId(),
                    indicator.valuationRuleId(), indicator.sources(), List.of(reference));
        }).toList();
        return new ModelConfiguration(indicators, fixture.scales(), fixture.rules());
    }

    private static NormativeReference pendingReference(UUID id) {
        return new NormativeReference(id, "Referencia sintetica pendiente", null, null,
                "fixture://normative/pending", null, null, null,
                NormativeReference.VerificationStatus.PENDING_VALIDATION, null, null);
    }

    private static NormativeReference verifiedReference(UUID id, UUID verifier) {
        return new NormativeReference(id, "Referencia sintetica verificada", "Emisor sintetico", "2026",
                "fixture://normative/verified", "a".repeat(64), "1-2", "Disposicion sintetica",
                NormativeReference.VerificationStatus.VERIFIED, verifier, Instant.parse("2026-10-01T00:00:00Z"));
    }

    private ModelApplicabilityService.ApplicabilityInput applicable(LocalDate from, LocalDate until) {
        return new ModelApplicabilityService.ApplicabilityInput(careerId, ModelCareerApplicability.Status.APPLICABLE,
                from, until, provenance());
    }

    private static ModelCareerApplicability.Decision provenance() {
        return new ModelCareerApplicability.Decision("Direccion de Carrera", "Decisión sintetica CL-011",
                "fixture://decisions/cl-011", "Solo prueba de software");
    }

    private static void assertProblem(org.assertj.core.api.ThrowableAssert.ThrowingCallable action, String code) {
        assertThatThrownBy(action).isInstanceOf(ModelProblemException.class)
                .extracting(error -> ((ModelProblemException) error).code()).isEqualTo(code);
    }
}
