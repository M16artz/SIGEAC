package ec.edu.unl.sec.evaluationmodel.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import ec.edu.unl.sec.evaluationmodel.domain.*;
import ec.edu.unl.sec.evaluationmodel.support.*;
import ec.edu.unl.sec.identityaccess.application.*;
import ec.edu.unl.sec.identityaccess.domain.OperationCode;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CompleteModelPublicationTest {
    private final InMemoryEvaluationModelRepository repository = new InMemoryEvaluationModelRepository();
    private final AtomicReference<CurrentActorPort.Actor> actor = new AtomicReference<>();
    private final AuthorizationPort authorization = mock(AuthorizationPort.class);
    private final EvaluationModelAuditService audit = mock(EvaluationModelAuditService.class);
    private final ConfiguredModelFixtures.Fixture fixture = ConfiguredModelFixtures.first();
    private EvaluationModelService service;
    private UUID modelId;

    @BeforeEach void setup() {
        actor.set(new CurrentActorPort.Actor(ModelFixtures.AUTHOR, false));
        when(authorization.hasOperation(any(), any())).thenReturn(true);
        service = service(Set.of());
        modelId = service.createModel(fixture.name(), "Sintético", UUID.randomUUID()).id();
    }

    @Test void publishesSourcesAndIndependentMethodsWithoutGlobalScaleOrFormula() {
        var draft = draft(fixture.configuration());
        var published = publish(draft);
        assertThat(published.status()).isEqualTo(ModelVersionStatus.PUBLISHED);
        assertThat(published.configuration()).isEqualTo(fixture.configuration());
        assertThat(published.scale()).isNull();
        assertThat(published.formula()).isNull();
        assertThat(published.approverId()).isEqualTo(ModelFixtures.APPROVER);
        verify(audit).record(eq(ModelFixtures.APPROVER), any(), eq("MODEL_PUBLISHED"), eq("MODEL_VERSION"), eq(draft.id()), anyMap());
    }

    @Test void rejectsMissingDefinitionsAndSourcesWithoutSavingPublication() {
        var original = fixture.configuration().indicators().getFirst();
        var withoutSources = definition(original, original.ratingScaleId(), original.valuationRuleId(), List.of());
        var configuration = new ModelConfiguration(List.of(withoutSources), fixture.configuration().scales(), fixture.configuration().rules());
        var draft = draft(configuration);
        var rejected = rejection(draft);
        assertThat(rejected.issues()).anyMatch(issue -> issue.startsWith("INDICATOR_SOURCE_REQUIRED:"))
                .anyMatch(issue -> issue.startsWith("INDICATOR_DEFINITION_REQUIRED:"));
        assertThat(repository.findVersion(draft.id())).contains(draft);
        verify(audit).reject(eq(ModelFixtures.APPROVER), any(), eq(draft.id()), eq("MODEL_PUBLICATION_INVALID"));
    }

    @Test void rejectsForeignScalesRulesAndNonIndicatorDefinitions() {
        var original = fixture.configuration().indicators().getFirst();
        var foreign = definition(original, UUID.randomUUID(), fixture.configuration().rules().getLast().id(), original.sources());
        var root = new IndicatorDefinition(fixture.nodes().getFirst().id(), original.nature(), original.standardText(),
                original.fundamentalElements(), original.evaluationPeriodDefinition(), null, null, List.of());
        var configuration = new ModelConfiguration(List.of(foreign, root, fixture.configuration().indicators().getLast()),
                fixture.configuration().scales(), fixture.configuration().rules());
        assertThat(rejection(draft(configuration)).issues()).anyMatch(issue -> issue.startsWith("INDICATOR_SCALE_NOT_FOUND:"))
                .anyMatch(issue -> issue.startsWith("RULE_INDICATOR_MISMATCH:"))
                .anyMatch(issue -> issue.startsWith("RULE_NATURE_MISMATCH:"))
                .anyMatch(issue -> issue.startsWith("INDICATOR_NODE_NOT_FOUND:"));
    }

    @Test void rejectsDuplicateDefinitionScaleAndRuleIdentities() {
        var configuration = fixture.configuration();
        var definitions = new ArrayList<>(configuration.indicators());
        definitions.add(definitions.getFirst());
        var scales = new ArrayList<>(configuration.scales());
        scales.add(scales.getFirst());
        var rules = new ArrayList<>(configuration.rules());
        rules.add(rules.getFirst());
        assertThat(rejection(draft(new ModelConfiguration(definitions, scales, rules))).issues())
                .anyMatch(issue -> issue.startsWith("INDICATOR_DEFINITION_DUPLICATE:"))
                .anyMatch(issue -> issue.startsWith("SCALE_ID_DUPLICATE:"))
                .anyMatch(issue -> issue.startsWith("RULE_ID_DUPLICATE:"))
                .anyMatch(issue -> issue.startsWith("SOURCE_ID_DUPLICATE:"));
    }

    @Test void draftQuantitativeRuleCanPublishAsDocumentationWithAutomationDisabled() {
        var rule = fixture.configuration().rules().getLast();
        var pending = new ValuationRule(rule.id(), rule.indicatorNodeId(), rule.ruleType(), rule.methodDefinition(),
                null, null, null, rule.scaleId(), List.of(), List.of(), rule.missingDataPolicy(), ValuationRule.Status.DRAFT);
        var configuration = withRule(pending);
        var published = publish(draft(configuration));
        assertThat(published.configuration().rules().getLast()).isEqualTo(pending);
        assertThat(pending.automaticCalculationAllowed(configuration.scales().getLast(), Set.of())).isFalse();
    }

    @Test void validatedRuleRequiresAnExactBackendValidatedStrategyAndCompleteCalculationMetadata() {
        var rule = fixture.configuration().rules().getLast();
        var validated = ConfiguredModelFixtures.withStatus(rule, ValuationRule.Status.VALIDATED);
        var draft = draft(withRule(validated));
        assertThat(rejection(draft).issues()).contains("STRATEGY_NOT_VALIDATED:" + rule.id());
        service = service(Set.of(new ValuationRule.StrategyReference(rule.strategyKey(), "other-version")));
        assertThat(rejection(draft).issues()).contains("STRATEGY_NOT_VALIDATED:" + rule.id());
        var registered = Set.of(new ValuationRule.StrategyReference(rule.strategyKey(), rule.strategyVersion()));
        service = service(registered);
        assertThat(publish(draft).status()).isEqualTo(ModelVersionStatus.PUBLISHED);
        assertThat(validated.automaticCalculationAllowed(fixture.configuration().scales().getLast(), registered)).isTrue();
        assertThat(rule.automaticCalculationAllowed(fixture.configuration().scales().getLast(), registered)).isFalse();
    }

    @Test void incompleteValidatedRulesCannotEnableAutomation() {
        var rule = fixture.configuration().rules().getLast();
        var incomplete = new ValuationRule(rule.id(), rule.indicatorNodeId(), rule.ruleType(), rule.methodDefinition(),
                null, null, null, rule.scaleId(), List.of(), List.of(), rule.missingDataPolicy(), ValuationRule.Status.VALIDATED);
        assertThat(rejection(draft(withRule(incomplete))).issues()).contains("RULE_FORMULA_REQUIRED:" + rule.id(),
                "RULE_VARIABLE_REQUIRED:" + rule.id(), "RULE_THRESHOLDS_REQUIRED:" + rule.id(), "RULE_STRATEGY_REQUIRED:" + rule.id());
    }

    @Test void rejectsForeignThresholdOptionsOverlapsAndRetiredRules() {
        var rule = fixture.configuration().rules().getLast();
        var first = rule.thresholds().getFirst();
        var wrong = new ValuationThreshold(UUID.randomUUID(), first.lowerBound(), first.upperBound(), true, true);
        var invalid = new ValuationRule(rule.id(), rule.indicatorNodeId(), rule.ruleType(), rule.methodDefinition(),
                rule.formulaDefinition(), rule.strategyKey(), rule.strategyVersion(), rule.scaleId(), rule.variables(),
                List.of(first, wrong), rule.missingDataPolicy(), ValuationRule.Status.RETIRED);
        assertThat(rejection(draft(withRule(invalid))).issues()).contains("RULE_RETIRED:" + rule.id(),
                "THRESHOLD_OPTION_NOT_FOUND:" + rule.id(), "THRESHOLD_OVERLAP:" + rule.id());
    }

    @Test void retainsAuthenticationPermissionsIndependentApprovalAndOptimisticRevision() {
        var draft = draft(fixture.configuration());
        assertProblem(() -> service.publishConfigured(draft.id(), 0, ModelFixtures.AUTHOR, UUID.randomUUID()), "MODEL_PUBLICATION_INVALID");
        actor.set(new CurrentActorPort.Actor(ModelFixtures.APPROVER, false));
        assertProblem(() -> service.publishConfigured(draft.id(), 1, ModelFixtures.APPROVER, UUID.randomUUID()), "STALE_MODEL_VERSION");
        assertProblem(() -> service.publishConfigured(draft.id(), 0, ModelFixtures.AUTHOR, UUID.randomUUID()), "APPROVER_IDENTITY_MISMATCH");
        when(authorization.hasOperation(ModelFixtures.APPROVER, OperationCode.MODEL_PUBLISH)).thenReturn(false);
        assertProblem(() -> publish(draft), "MODEL_SCOPE_DENIED");
        actor.set(new CurrentActorPort.Actor(ModelFixtures.APPROVER, true));
        assertProblem(() -> service.replaceConfigured(draft.id(), 0, fixture.source(), fixture.nodes(), fixture.configuration(), UUID.randomUUID()), "MODEL_SCOPE_DENIED");
        actor.set(null);
        assertProblem(() -> service.publishConfigured(draft.id(), 0, ModelFixtures.APPROVER, UUID.randomUUID()), "AUTHENTICATION_REQUIRED");
        assertThat(repository.findVersion(draft.id())).contains(draft);
    }

    @Test void concurrentWriteFailureDoesNotPublishOrRecordSuccess() {
        var draft = draft(fixture.configuration());
        var racingRepository = mock(EvaluationModelRepository.class);
        when(racingRepository.findVersion(draft.id())).thenReturn(Optional.of(draft));
        doThrow(new ModelProblemException(ModelProblemException.Kind.PRECONDITION, "STALE_MODEL_VERSION", "Recargue"))
                .when(racingRepository).saveVersion(any(), eq(0L));
        actor.set(new CurrentActorPort.Actor(ModelFixtures.APPROVER, false));
        var concurrentAudit = mock(EvaluationModelAuditService.class);
        var concurrentService = new EvaluationModelService(racingRepository, () -> Optional.of(actor.get()), authorization,
                new EvaluationModelProperties(null, null), concurrentAudit);
        assertProblem(() -> concurrentService.publishConfigured(draft.id(), 0, ModelFixtures.APPROVER, UUID.randomUUID()), "STALE_MODEL_VERSION");
        verifyNoInteractions(concurrentAudit);
        assertThat(repository.findVersion(draft.id())).contains(draft);
    }

    @Test void successorPreservesPublishedSourcesAndRulesAndRejectsDirectChanges() {
        var published = publish(draft(fixture.configuration()));
        assertProblem(() -> service.replaceConfigured(published.id(), published.revision(), "Otro", fixture.nodes(), fixture.configuration(), UUID.randomUUID()), "MODEL_VERSION_IMMUTABLE");
        assertProblem(() -> service.publishConfigured(published.id(), published.revision(), ModelFixtures.APPROVER, UUID.randomUUID()), "MODEL_VERSION_IMMUTABLE");
        actor.set(new CurrentActorPort.Actor(ModelFixtures.AUTHOR, false));
        var successors = new ModelSuccessionService(repository, () -> Optional.ofNullable(actor.get()), authorization, audit);
        var successor = successors.createConfigured(published.id(), UUID.randomUUID());
        assertThat(successor.configuration()).isEqualTo(published.configuration());
        assertThat(successor.predecessorId()).isEqualTo(published.id());
        var changed = service.replaceConfigured(successor.id(), 0, "Fuente sucesora sintética", successor.criteria(), successor.configuration(), UUID.randomUUID());
        publish(changed);
        var previous = repository.findVersion(published.id()).orElseThrow();
        assertThat(previous.status()).isEqualTo(ModelVersionStatus.SUPERSEDED);
        assertThat(previous.configuration()).isEqualTo(published.configuration());
        assertThat(previous.source()).isEqualTo(published.source());
    }

    private EvaluationModelService service(Set<ValuationRule.StrategyReference> strategies) {
        return new EvaluationModelService(repository, () -> Optional.ofNullable(actor.get()), authorization,
                new EvaluationModelProperties(null, null), audit, () -> strategies);
    }
    private ModelVersion draft(ModelConfiguration configuration) {
        actor.set(new CurrentActorPort.Actor(ModelFixtures.AUTHOR, false));
        return service.createDraft(modelId, fixture.source(), fixture.nodes(), configuration, UUID.randomUUID());
    }
    private ModelVersion publish(ModelVersion draft) {
        actor.set(new CurrentActorPort.Actor(ModelFixtures.APPROVER, false));
        return service.publishConfigured(draft.id(), draft.revision(), ModelFixtures.APPROVER, UUID.randomUUID());
    }
    private EvaluationModelService.PublicationRejected rejection(ModelVersion draft) {
        return catchThrowableOfType(() -> publish(draft), EvaluationModelService.PublicationRejected.class);
    }
    private ModelConfiguration withRule(ValuationRule rule) {
        return new ModelConfiguration(fixture.configuration().indicators(), fixture.configuration().scales(),
                List.of(fixture.configuration().rules().getFirst(), rule));
    }
    private static IndicatorDefinition definition(IndicatorDefinition original, UUID scale, UUID rule, List<RequiredSource> sources) {
        return new IndicatorDefinition(original.nodeId(), original.nature(), original.standardText(), original.fundamentalElements(),
                original.evaluationPeriodDefinition(), scale, rule, sources);
    }
    private static void assertProblem(Runnable command, String code) {
        assertThatThrownBy(command::run).isInstanceOf(ModelProblemException.class).extracting("code").isEqualTo(code);
    }
}
