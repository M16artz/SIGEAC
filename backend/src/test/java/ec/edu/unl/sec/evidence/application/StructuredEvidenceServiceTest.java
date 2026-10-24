package ec.edu.unl.sec.evidence.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.edu.unl.sec.evidence.domain.EvidenceVersion;
import ec.edu.unl.sec.evidence.domain.EvidenceVersionStatus;
import ec.edu.unl.sec.evidence.domain.SourceCoverage;
import ec.edu.unl.sec.evidence.domain.StructuredEvidence;
import ec.edu.unl.sec.evidence.domain.VersionIndicator;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * AC-X16 / FR-069: typed snapshots conserve value/reference, unit and period;
 * files are never required for structured data or institutional references.
 */
class StructuredEvidenceServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-20T15:00:00Z");
    private static final UUID ACTOR = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID CAREER = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private static final UUID PROCESS = UUID.fromString("00000000-0000-0000-0000-000000000301");
    private static final UUID INDICATOR = UUID.fromString("00000000-0000-0000-0000-000000000401");
    private static final UUID EVIDENCE = UUID.fromString("00000000-0000-0000-0000-000000000451");
    private static final UUID VERSION = UUID.fromString("00000000-0000-0000-0000-000000000501");
    private static final UUID SOURCE = UUID.fromString("00000000-0000-0000-0000-000000000601");
    private static final UUID MODEL = UUID.fromString("00000000-0000-0000-0000-000000000701");

    private EvidenceDraftService drafts;
    private EvidenceGovernanceRepository governance;
    private RequiredSourceCatalogPort catalog;
    private EvidenceContextPort context;
    private EvidenceAuthorizationPort authorization;
    private StructuredEvidenceService service;

    @BeforeEach
    void setUp() {
        drafts = org.mockito.Mockito.mock(EvidenceDraftService.class);
        governance = org.mockito.Mockito.mock(EvidenceGovernanceRepository.class);
        catalog = org.mockito.Mockito.mock(RequiredSourceCatalogPort.class);
        context = org.mockito.Mockito.mock(EvidenceContextPort.class);
        authorization = org.mockito.Mockito.mock(EvidenceAuthorizationPort.class);
        var version = version(EvidenceVersionStatus.DRAFT, 0L);
        when(drafts.getVersion(VERSION)).thenReturn(version);
        when(drafts.getEvidence(EVIDENCE)).thenReturn(new EvidenceDraftService.EvidenceDetails(
                new ec.edu.unl.sec.evidence.domain.Evidence(EVIDENCE, PROCESS, ACTOR, NOW, UUID.randomUUID(), 1),
                version));
        when(context.process(PROCESS)).thenReturn(Optional.of(
                new EvidenceContextPort.ProcessContext(PROCESS, CAREER, MODEL, 1, "ACTIVE",
                        List.of(new EvidenceContextPort.Indicator(INDICATOR, "IND-1", "Indicador sintético")))));
        when(catalog.model(MODEL)).thenReturn(Optional.of(new RequiredSourceCatalogPort.ModelSources(MODEL, List.of(
                new RequiredSourceCatalogPort.Entry(SOURCE, INDICATOR, "IND-1", "SRC-1", "Datos institucionales",
                        "STRUCTURED_DATA", true, 1, null)))));
        when(governance.advanceDraftRevision(VERSION, 0L)).thenReturn(true);
        when(governance.coverages(VERSION)).thenReturn(List.of(
                new SourceCoverage(UUID.randomUUID(), VERSION, SOURCE, "Cobertura sintética", ACTOR, NOW)));
        when(authorization.currentActor())
                .thenReturn(Optional.of(new EvidenceAuthorizationPort.Actor(ACTOR, false)));
        service = new StructuredEvidenceService(drafts, governance, catalog, context, authorization,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void rejectsASnapshotWhenAnotherMutationHasAdvancedTheVersion() {
        when(governance.advanceDraftRevision(VERSION, 0L)).thenReturn(false);
        assertProblem(() -> service.register(VERSION, "\"0\"", new StructuredEvidenceService.StructuredCommand(
                SOURCE, StructuredEvidence.Kind.STRUCTURED_DATA, 1, "unidad",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30), null)), "STALE_DRAFT");
        verify(governance, never()).saveStructured(any());
    }

    @Test
    void conservesTypedValueUnitAndPeriodForStructuredData() {
        var registered = service.register(VERSION, "\"0\"", new StructuredEvidenceService.StructuredCommand(
                SOURCE, StructuredEvidence.Kind.STRUCTURED_DATA, new BigDecimal("42.5"), "m2",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30), null));

        assertThat((BigDecimal) registered.value().value().value()).isEqualByComparingTo(new BigDecimal("42.5"));
        assertThat(registered.value().value().type()).isEqualTo(StructuredEvidence.ValueType.NUMBER);
        assertThat(registered.value().unit()).isEqualTo("m2");
        assertThat(registered.value().periodStart()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(registered.value().periodEnd()).isEqualTo(LocalDate.of(2026, 6, 30));
        assertThat(registered.value().createdBy()).isEqualTo(ACTOR);
        assertThat(registered.versionRevision()).isEqualTo(1);
        verify(governance).saveStructured(registered.value());
    }

    @Test
    void conservesReferenceForInstitutionalSourcesWithoutDemandingFiles() {
        var institutional = UUID.fromString("00000000-0000-0000-0000-000000000602");
        when(catalog.model(MODEL)).thenReturn(Optional.of(new RequiredSourceCatalogPort.ModelSources(MODEL, List.of(
                new RequiredSourceCatalogPort.Entry(institutional, INDICATOR, "IND-1", "SRC-2", "Resolución institucional",
                        "INSTITUTIONAL_REFERENCE", true, 1, null)))));
        when(governance.advanceDraftRevision(VERSION, 0L)).thenReturn(true);
        when(governance.coverages(VERSION)).thenReturn(List.of(
                new SourceCoverage(UUID.randomUUID(), VERSION, institutional, "Cobertura institucional", ACTOR, NOW)));

        var registered = service.register(VERSION, "\"0\"", new StructuredEvidenceService.StructuredCommand(
                institutional, StructuredEvidence.Kind.INSTITUTIONAL_REFERENCE, "Normativa 2024 actualizada",
                null, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31), "Res. 128-2024 docuumento institucional"));

        assertThat(registered.value().kind()).isEqualTo(StructuredEvidence.Kind.INSTITUTIONAL_REFERENCE);
        assertThat(registered.value().value().type()).isEqualTo(StructuredEvidence.ValueType.STRING);
        assertThat(registered.value().value().value()).isEqualTo("Normativa 2024 actualizada");
        assertThat(registered.value().reference()).isEqualTo("Res. 128-2024 docuumento institucional");
        verify(governance).saveStructured(registered.value());
    }

    @Test
    void requiresDeclaredCoverageBeforeSnapshots() {
        when(governance.advanceDraftRevision(VERSION, 0L)).thenReturn(true);
        when(governance.coverages(VERSION)).thenReturn(List.of());
        assertProblem(() -> service.register(VERSION, "\"0\"", structured()),
                "STRUCTURED_SOURCE_NOT_COVERED");
        verify(governance, never()).saveStructured(any());
    }

    @Test
    void rejectsModalityMismatchesInsteadOfDemandingPdfs() {
        when(catalog.model(MODEL)).thenReturn(Optional.of(new RequiredSourceCatalogPort.ModelSources(MODEL, List.of(
                new RequiredSourceCatalogPort.Entry(SOURCE, INDICATOR, "IND-1", "SRC-1", "Acta firmada",
                        "FILE", true, 1, null)))));
        assertProblem(() -> service.register(VERSION, "\"0\"", structured()),
                "SOURCE_NOT_COMPATIBLE");
        verify(governance, never()).saveStructured(any());
    }

    @Test
    void rejectsForeignRequiredSourcesAndIndicatorMismatches() {
        var foreign = UUID.fromString("00000000-0000-0000-0000-000000000609");
        when(governance.advanceDraftRevision(VERSION, 0L)).thenReturn(true);
        when(governance.coverages(VERSION)).thenReturn(List.of(
                new SourceCoverage(UUID.randomUUID(), VERSION, foreign, "Cobertura foránea", ACTOR, NOW)));
        assertProblem(() -> service.register(VERSION, "\"0\"",
                new StructuredEvidenceService.StructuredCommand(
                        foreign,
                        StructuredEvidence.Kind.STRUCTURED_DATA, 1.0, null,
                        LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30), null)),
                "SOURCE_NOT_COMPATIBLE");
    }

    @Test
    void validatesPeriodUnitAndValueLength() {
        assertProblem(() -> service.register(VERSION, "\"0\"", new StructuredEvidenceService.StructuredCommand(
                SOURCE, StructuredEvidence.Kind.STRUCTURED_DATA, new BigDecimal("12"), "kg",
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 1, 1), null)), "STRUCTURED_PERIOD_INVALID");
        assertProblem(() -> service.register(VERSION, "\"0\"", new StructuredEvidenceService.StructuredCommand(
                SOURCE, StructuredEvidence.Kind.STRUCTURED_DATA, "", null,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30), null)), "STRUCTURED_VALUE_INVALID");
        assertProblem(() -> service.register(VERSION, "\"0\"", new StructuredEvidenceService.StructuredCommand(
                SOURCE, StructuredEvidence.Kind.STRUCTURED_DATA, "valor", "u".repeat(101),
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30), null)), "STRUCTURED_METADATA_INVALID");
        assertProblem(() -> service.register(VERSION, "\"0\"", new StructuredEvidenceService.StructuredCommand(
                SOURCE, StructuredEvidence.Kind.STRUCTURED_DATA, "valor", null,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30), "r".repeat(2001))),
                "STRUCTURED_METADATA_INVALID");
    }

    @Test
    void rejectsDuplicateSnapshotsPerSourceAndVersion() {
        when(governance.structuredSnapshot(VERSION, SOURCE)).thenReturn(Optional.of(new StructuredEvidence(
                UUID.randomUUID(), VERSION, SOURCE, StructuredEvidence.Kind.STRUCTURED_DATA,
                StructuredEvidence.TypedValue.number(new BigDecimal("7")), "kg", LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 6, 30), null, ACTOR, NOW)));
        assertProblem(() -> service.register(VERSION, "\"0\"", structured()), "STRUCTURED_SNAPSHOT_DUPLICATE");
    }

    @Test
    void rejectsSnapshotsOnSubmittedVersions() {
        var submitted = version(EvidenceVersionStatus.SUBMITTED, 1L);
        when(drafts.getVersion(VERSION)).thenReturn(submitted);
        assertProblem(() -> service.register(VERSION, "\"1\"", structured()),
                "STRUCTURED_SNAPSHOT_IMMUTABLE");
    }

    @Test
    void rejectsStaleIfMatchRevisions() {
        assertProblem(() -> service.register(VERSION, "\"9\"", structured()), "STALE_DRAFT");
    }

    @Test
    void listsSnapshotsOfTheVersion() {
        var snapshot = new StructuredEvidence(UUID.randomUUID(), VERSION, SOURCE,
                StructuredEvidence.Kind.STRUCTURED_DATA, StructuredEvidence.TypedValue.number(new BigDecimal("9")),
                "kg", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30), null, ACTOR, NOW);
        when(governance.structuredSnapshots(VERSION)).thenReturn(List.of(snapshot));

        assertThat(service.snapshots(VERSION)).containsExactly(snapshot);
    }

    private static StructuredEvidenceService.StructuredCommand structured() {
        return new StructuredEvidenceService.StructuredCommand(
                SOURCE, StructuredEvidence.Kind.STRUCTURED_DATA, new BigDecimal("42.5"), "m2",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30), null);
    }

    private static EvidenceVersion version(EvidenceVersionStatus status, long lockRevision) {
        return new EvidenceVersion(VERSION, EVIDENCE, 1, null, status, "Informe", "Descripción",
                "Procedencia", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30), ACTOR,
                status == EvidenceVersionStatus.SUBMITTED ? NOW : Instant.parse("2026-10-19T15:00:00Z"),
                status == EvidenceVersionStatus.SUBMITTED ? NOW : null, lockRevision,
                List.of(new VersionIndicator(VERSION, INDICATOR, "IND-1", "Indicador", 1)));
    }

    private static void assertProblem(Runnable operation, String code) {
        assertThatThrownBy(() -> operation.run())
                .isInstanceOf(EvidenceProblemException.class)
                .satisfies(error -> assertThat(((EvidenceProblemException) error).code()).isEqualTo(code));
    }
}
