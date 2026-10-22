package ec.edu.unl.sec.evidence.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import ec.edu.unl.sec.evidence.domain.ApplicabilityDecision;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApplicabilityDecisionServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-20T15:00:00Z");
    private static final UUID ACTOR = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID CAREER = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private static final UUID PROCESS = UUID.fromString("00000000-0000-0000-0000-000000000301");
    private static final UUID INDICATOR = UUID.fromString("00000000-0000-0000-0000-000000000401");
    private static final UUID MODEL = UUID.fromString("00000000-0000-0000-0000-000000000701");

    private EvidenceGovernanceRepository governance;
    private RequiredSourceCatalogPort catalog;
    private EvidenceContextPort context;
    private EvidenceAuthorizationPort authorization;
    private ApplicabilityDecisionService service;

    @BeforeEach
    void setUp() {
        governance = org.mockito.Mockito.mock(EvidenceGovernanceRepository.class);
        catalog = org.mockito.Mockito.mock(RequiredSourceCatalogPort.class);
        context = org.mockito.Mockito.mock(EvidenceContextPort.class);
        authorization = org.mockito.Mockito.mock(EvidenceAuthorizationPort.class);
        when(context.process(PROCESS)).thenReturn(Optional.of(process("ACTIVE")));
        when(authorization.currentActor()).thenReturn(Optional.of(new EvidenceAuthorizationPort.Actor(ACTOR, false)));
        when(authorization.canDecideApplicability(ACTOR, CAREER, PROCESS)).thenReturn(true);
        service = new ApplicabilityDecisionService(governance, context, authorization, catalog,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void recordsFirstDecisionWithoutProvenanceInvention() {
        var decision = service.decide(PROCESS, input(
                ApplicabilityDecision.SubjectType.INDICATOR, INDICATOR,
                ApplicabilityDecision.Decision.NOT_APPLICABLE, "CUR-3 fuera de la carrera", 0));

        assertThat(decision.revision()).isZero();
        assertThat(decision.predecessorId()).isNull();
        assertThat(decision.authorizedBy()).isEqualTo(ACTOR);
        assertThat(decision.decidedAt()).isEqualTo(NOW);
        verify(governance).saveApplicability(decision);
    }

    @Test
    void supersedesThePreviousLiveDecisionWithPredecessor() {
        var previous = new ApplicabilityDecision(UUID.randomUUID(), PROCESS,
                ApplicabilityDecision.SubjectType.INDICATOR, INDICATOR,
                ApplicabilityDecision.Decision.NOT_APPLICABLE, "Regla previa", "Justificación previa extendida",
                "fundamento", "fixture://decision", ACTOR, NOW, null, 0);
        when(governance.latestApplicability(PROCESS, ApplicabilityDecision.SubjectType.INDICATOR, INDICATOR))
                .thenReturn(Optional.of(previous));

        var decision = service.decide(PROCESS, input(
                ApplicabilityDecision.SubjectType.INDICATOR, INDICATOR,
                ApplicabilityDecision.Decision.APPLICABLE, "Cambio de regla vigente", 0));

        assertThat(decision.revision()).isEqualTo(1);
        assertThat(decision.predecessorId()).isEqualTo(previous.id());
        assertThat(decision.decision()).isEqualTo(ApplicabilityDecision.Decision.APPLICABLE);
    }

    @Test
    void rejectsStaleRevisionsAndUnknownSubjects() {
        var previous = new ApplicabilityDecision(UUID.randomUUID(), PROCESS,
                ApplicabilityDecision.SubjectType.INDICATOR, INDICATOR,
                ApplicabilityDecision.Decision.APPLICABLE, "Regla", "Justificación vigente extendida",
                "fundamento", "fixture://decision", ACTOR, NOW, null, 3);
        when(governance.latestApplicability(PROCESS, ApplicabilityDecision.SubjectType.INDICATOR, INDICATOR))
                .thenReturn(Optional.of(previous));
        assertProblem(() -> service.decide(PROCESS, input(
                ApplicabilityDecision.SubjectType.INDICATOR, INDICATOR,
                ApplicabilityDecision.Decision.APPLICABLE, "Regla vigente", 2)), "STALE_APPLICABILITY");
        assertProblem(() -> service.decide(PROCESS, input(
                ApplicabilityDecision.SubjectType.INDICATOR, UUID.randomUUID(),
                ApplicabilityDecision.Decision.APPLICABLE, "Indicador ajeno", 0)),
                "APPLICABILITY_SUBJECT_UNKNOWN");
        var foreign = UUID.randomUUID();
        when(authorization.canDecideApplicability(ACTOR, CAREER, PROCESS)).thenReturn(false);
        assertProblem(() -> service.decide(PROCESS, input(
                ApplicabilityDecision.SubjectType.INDICATOR, INDICATOR,
                ApplicabilityDecision.Decision.APPLICABLE, "Sin competencia", 0)),
                "APPLICABILITY_DECISION_DENIED");
        assertThat(foreign).isNotNull();
    }

    @Test
    void requiredSourceSubjectsMustBelongToTheFixedModel() {
        when(catalog.model(MODEL)).thenReturn(Optional.of(new RequiredSourceCatalogPort.ModelSources(
                MODEL, List.of(new RequiredSourceCatalogPort.Entry(
                        UUID.fromString("00000000-0000-0000-0000-000000000601"), INDICATOR, "IND-1", "SRC-1",
                        "Fuente", "FILE", true, 1, null)))));

        var decision = service.decide(PROCESS, input(
                ApplicabilityDecision.SubjectType.REQUIRED_SOURCE,
                UUID.fromString("00000000-0000-0000-0000-000000000601"),
                ApplicabilityDecision.Decision.NOT_APPLICABLE, "Fuente no significativa", 0));
        assertThat(decision.subjectType()).isEqualTo(ApplicabilityDecision.SubjectType.REQUIRED_SOURCE);

        assertProblem(() -> service.decide(PROCESS, input(
                ApplicabilityDecision.SubjectType.REQUIRED_SOURCE, UUID.randomUUID(),
                ApplicabilityDecision.Decision.NOT_APPLICABLE, "Fuente ajena al modelo", 0)),
                "APPLICABILITY_SUBJECT_UNKNOWN");
    }

    @Test
    void historyRequiresTheSameCompetentAuthority() {
        when(governance.applicabilityHistory(PROCESS)).thenReturn(List.of());
        assertThat(service.history(PROCESS)).isEmpty();
        when(authorization.canDecideApplicability(ACTOR, CAREER, PROCESS)).thenReturn(false);
        assertProblem(() -> service.history(PROCESS), "APPLICABILITY_DECISION_DENIED");
    }

    private static ApplicabilityDecisionService.ApplicabilityInput input(
            ApplicabilityDecision.SubjectType subjectType, UUID subjectId,
            ApplicabilityDecision.Decision decision, String ruleReference, long expectedRevision) {
        return new ApplicabilityDecisionService.ApplicabilityInput(
                subjectType, subjectId, decision, ruleReference, expectedRevision,
                new ApplicabilityDecisionService.DecisionInput(
                        UUID.randomUUID(), "Reglamento sintético de la institución",
                        "fixture://applicability", "Decisión sintética con fundamento y autoría."));
    }

    private static EvidenceContextPort.ProcessContext process(String state) {
        return new EvidenceContextPort.ProcessContext(
                PROCESS, CAREER, MODEL, 1, state,
                List.of(new EvidenceContextPort.Indicator(INDICATOR, "IND-1", "Indicador sintético")));
    }

    private static void assertProblem(Runnable operation, String code) {
        assertThatThrownBy(() -> operation.run())
                .isInstanceOf(EvidenceProblemException.class)
                .satisfies(error -> assertThat(((EvidenceProblemException) error).code()).isEqualTo(code));
    }
}
