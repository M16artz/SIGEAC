package ec.edu.unl.sec.evidence.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ec.edu.unl.sec.audit.application.AuditPort;
import ec.edu.unl.sec.evidence.domain.ExternalReference;
import ec.edu.unl.sec.evidence.domain.EvidenceVersion;
import ec.edu.unl.sec.filestorage.domain.FileAsset;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SubmitEvidenceServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-20T15:00:00Z");
    private static final UUID ACTOR = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID CAREER = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private static final UUID PROCESS = UUID.fromString("00000000-0000-0000-0000-000000000301");
    private static final UUID INDICATOR = UUID.fromString("00000000-0000-0000-0000-000000000401");
    private static final UUID SOURCE = UUID.randomUUID(), MODEL = UUID.randomUUID();

    private EvidenceDraftService drafts;
    private EvidenceGovernanceRepository governance;
    private EvidenceContextPort context;
    private EvidenceSourceService sources;
    private AuditPort audit; private RequiredSourceCatalogPort catalog; private EvidenceDownloadService downloads;
    private SubmitEvidenceService service;
    private EvidenceVersion draft;
    private EvidenceDraftService.EvidenceDetails details;

    @BeforeEach
    void setUp() {
        drafts = org.mockito.Mockito.mock(EvidenceDraftService.class);
        governance = org.mockito.Mockito.mock(EvidenceGovernanceRepository.class);
        context = org.mockito.Mockito.mock(EvidenceContextPort.class);
        sources = org.mockito.Mockito.mock(EvidenceSourceService.class);
        audit = org.mockito.Mockito.mock(AuditPort.class); catalog = org.mockito.Mockito.mock(RequiredSourceCatalogPort.class); downloads = org.mockito.Mockito.mock(EvidenceDownloadService.class);
        draft = draft(0, null);
        details = new EvidenceDraftService.EvidenceDetails(
                new ec.edu.unl.sec.evidence.domain.Evidence(
                        draft.evidenceId(), PROCESS, ACTOR, NOW, UUID.randomUUID(), 1), draft);
        when(drafts.getVersion(draft.id())).thenReturn(draft);
        when(drafts.getEvidence(draft.evidenceId())).thenReturn(details);
        when(context.process(PROCESS)).thenReturn(Optional.of(process("ACTIVE")));
        when(sources.snapshot(draft.id())).thenReturn(snapshot(List.of(), 1));
        when(governance.coverages(draft.id())).thenReturn(List.of(new ec.edu.unl.sec.evidence.domain.SourceCoverage(UUID.randomUUID(), draft.id(), SOURCE, "Nota", ACTOR, NOW)));
        when(catalog.model(MODEL)).thenReturn(Optional.of(new RequiredSourceCatalogPort.ModelSources(MODEL, List.of(new RequiredSourceCatalogPort.Entry(SOURCE, INDICATOR, "IND-1", "SRC-1", "Enlace", "EXTERNAL_LINK", true, 1, null)))));
        service = new SubmitEvidenceService(drafts, governance, sources, context, audit, catalog, downloads,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void acceptsStructuredDataWithoutAnArtificialFileOrExternalLink() {
        structuredSource();
        when(sources.snapshot(draft.id())).thenReturn(EvidenceSourceService.Sources.empty());
        when(governance.markSubmitted(eq(draft.id()), eq(NOW), eq(0L))).thenReturn(true);
        assertThat(service.submit(draft.id(), "\"0\"").status()).isEqualTo(ec.edu.unl.sec.evidence.domain.EvidenceVersionStatus.SUBMITTED);
        verifyNoInteractions(downloads);
    }

    @Test
    void rejectsMissingTypedSnapshotAndForeignCoverage() {
        structuredSource();
        when(governance.structuredSnapshots(draft.id())).thenReturn(List.of());
        assertProblem(() -> service.submit(draft.id(), "\"0\""), "STRUCTURED_SOURCE_REQUIRED");
        when(governance.coverages(draft.id())).thenReturn(List.of(new ec.edu.unl.sec.evidence.domain.SourceCoverage(
                null, draft.id(), UUID.randomUUID(), "Nota", ACTOR, NOW)));
        assertProblem(() -> service.submit(draft.id(), "\"0\""), "SOURCE_NOT_COMPATIBLE");
        verify(governance, never()).markSubmitted(any(), any(), org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void verifiesFileBytesBeforeFreezingTheSnapshot() {
        var fileId = UUID.randomUUID();
        when(sources.snapshot(draft.id())).thenReturn(snapshot(List.of(new EvidenceSourceService.SourceFile(
                new ec.edu.unl.sec.evidence.domain.VersionFile(draft.id(), fileId, "test.txt", "text/plain", "text/plain", 9, "a".repeat(64), 1),
                FileAsset.Status.AVAILABLE)), 1));
        when(downloads.prepare(draft.id(), fileId)).thenThrow(new EvidenceProblemException(
                EvidenceProblemException.Kind.CONFLICT, "FILE_INTEGRITY_FAILED", "Archivo alterado"));
        assertProblem(() -> service.submit(draft.id(), "\"0\""), "FILE_INTEGRITY_FAILED");
        verify(governance, never()).markSubmitted(any(), any(), org.mockito.ArgumentMatchers.anyLong());
        verifyNoInteractions(audit);
    }

    private void structuredSource() {
        when(catalog.model(MODEL)).thenReturn(Optional.of(new RequiredSourceCatalogPort.ModelSources(MODEL, List.of(
                new RequiredSourceCatalogPort.Entry(SOURCE, INDICATOR, "IND-1", "S-1", "Datos", "STRUCTURED_DATA", true, 1, null)))));
        when(governance.structuredSnapshots(draft.id())).thenReturn(List.of(new ec.edu.unl.sec.evidence.domain.StructuredEvidence(
                null, draft.id(), SOURCE, ec.edu.unl.sec.evidence.domain.StructuredEvidence.Kind.STRUCTURED_DATA,
                ec.edu.unl.sec.evidence.domain.StructuredEvidence.TypedValue.number(java.math.BigDecimal.ONE), "unidad",
                draft.coveredFrom(), draft.coveredTo(), null, ACTOR, NOW)));
    }

    @Test
    void rejectsMissingCoverageEvenWhenAReferenceExists() {
        when(governance.coverages(draft.id())).thenReturn(List.of());
        when(governance.markSubmitted(eq(draft.id()), eq(NOW), eq(0L))).thenReturn(true);
        assertProblem(() -> service.submit(draft.id(), "\"0\""), "COVERAGE_REQUIRED");
        verify(governance, never()).markSubmitted(any(), any(), org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void requiresEditableAccessEvenWhenTheVersionIsVisible() {
        when(drafts.requireEditableVersion(draft.id(), "\"0\""))
                .thenThrow(EvidenceDraftService.notFound());
        when(governance.markSubmitted(eq(draft.id()), eq(NOW), eq(0L))).thenReturn(true);
        assertProblem(() -> service.submit(draft.id(), "\"0\""), "RESOURCE_NOT_FOUND");
        verifyNoInteractions(audit);
    }

    @Test
    void submitsAMutableDraftAfterExplicitCoverage() {
        when(governance.coverages(draft.id())).thenReturn(List.of(
                new ec.edu.unl.sec.evidence.domain.SourceCoverage(
                        UUID.randomUUID(), draft.id(), SOURCE, "Nota", ACTOR, NOW)));
        when(governance.markSubmitted(eq(draft.id()), eq(NOW), eq(0L))).thenReturn(true);

        var submitted = service.submit(draft.id(), "\"0\"");

        assertThat(submitted.status()).isEqualTo(ec.edu.unl.sec.evidence.domain.EvidenceVersionStatus.SUBMITTED);
        assertThat(submitted.submittedAt()).isEqualTo(NOW);
        assertThat(submitted.lockRevision()).isEqualTo(1);
        verify(audit).record(any());
    }

    @Test
    void rejectsSubmissionsWithoutFilesOrReferences() {
        when(governance.coverages(draft.id())).thenReturn(List.of(
                new ec.edu.unl.sec.evidence.domain.SourceCoverage(
                        UUID.randomUUID(), draft.id(), SOURCE, "Nota", ACTOR, NOW)));
        when(sources.snapshot(draft.id())).thenReturn(new EvidenceSourceService.Sources(List.of(), List.of()));
        assertProblem(() -> service.submit(draft.id(), "\"0\""), "SOURCE_REQUIRED");
        verify(audit, never()).record(any());
        verify(governance, never()).markSubmitted(any(), any(), org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void rejectsSubmissionsWhenAnAttachedFileIsNotAvailable() {
        when(catalog.model(MODEL)).thenReturn(Optional.of(new RequiredSourceCatalogPort.ModelSources(MODEL, List.of(
                new RequiredSourceCatalogPort.Entry(SOURCE, INDICATOR, "IND-1", "S-1", "Archivo", "FILE", true, 1, null)))));
        when(governance.coverages(draft.id())).thenReturn(List.of(
                new ec.edu.unl.sec.evidence.domain.SourceCoverage(
                        UUID.randomUUID(), draft.id(), SOURCE, "Nota", ACTOR, NOW)));
        var unavailable = new EvidenceSourceService.Sources(List.of(new EvidenceSourceService.SourceFile(
                new ec.edu.unl.sec.evidence.domain.VersionFile(draft.id(),
                        UUID.fromString("00000000-0000-0000-0000-000000000601"), "synthetic.txt",
                        "text/plain", "text/plain", 9, "a".repeat(64), 1), FileAsset.Status.QUARANTINED)),
                List.of());
        when(sources.snapshot(draft.id())).thenReturn(unavailable);
        assertProblem(() -> service.submit(draft.id(), "\"0\""), "FILE_NOT_AVAILABLE");
        verify(audit, never()).record(any());
        verify(governance, never()).markSubmitted(any(), any(), org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void rejectsStaleDraftsAndAlreadySubmittedVersions() {
        when(governance.coverages(draft.id())).thenReturn(List.of(
                new ec.edu.unl.sec.evidence.domain.SourceCoverage(
                        UUID.randomUUID(), draft.id(), SOURCE, "Nota", ACTOR, NOW)));
        when(governance.markSubmitted(eq(draft.id()), eq(NOW), eq(0L))).thenReturn(false);
        assertProblem(() -> service.submit(draft.id(), "\"0\""), "STALE_DRAFT");
        assertProblem(() -> service.submit(draft.id(), null), "PRECONDITION_REQUIRED");
        assertProblem(() -> service.submit(draft.id(), "\"9\""), "STALE_DRAFT");

        var submittedVersion = draft.submit(NOW);
        when(drafts.getVersion(submittedVersion.id())).thenReturn(submittedVersion);
        when(drafts.getEvidence(submittedVersion.evidenceId())).thenReturn(details);
        assertProblem(() -> service.submit(submittedVersion.id(), "\"1\""), "EVIDENCE_VERSION_IMMUTABLE");
    }

    @Test
    void rejectsSubmissionsWhenTheProcessIsNotActive() {
        when(drafts.getVersion(draft.id())).thenReturn(draft);
        when(context.process(PROCESS)).thenReturn(Optional.of(process("SUSPENDED")));
        when(governance.coverages(draft.id())).thenReturn(List.of(
                new ec.edu.unl.sec.evidence.domain.SourceCoverage(
                        UUID.randomUUID(), draft.id(), SOURCE, "Nota", ACTOR, NOW)));
        assertProblem(() -> service.submit(draft.id(), "\"0\""), "PROCESS_NOT_ACTIVE");
        verify(governance, never()).markSubmitted(any(), any(), org.mockito.ArgumentMatchers.anyLong());
    }

    private EvidenceVersion draft(long lockRevision, UUID predecessorId) {
        return new EvidenceVersion(
                UUID.fromString("00000000-0000-0000-0000-000000000501"),
                UUID.fromString("00000000-0000-0000-0000-000000000511"),
                1,
                predecessorId,
                null,
                "Informe sintético",
                "Descripción sintética",
                "Procedencia sintética",
                java.time.LocalDate.of(2026, 1, 1),
                java.time.LocalDate.of(2026, 6, 30),
                ACTOR,
                NOW,
                null,
                lockRevision,
                List.of(new ec.edu.unl.sec.evidence.domain.VersionIndicator(
                        UUID.fromString("00000000-0000-0000-0000-000000000501"), INDICATOR, "IND-1", "Indicador", 1)));
    }

    private EvidenceSourceService.Sources snapshot(List<EvidenceSourceService.SourceFile> files, int references) {
        var list = java.util.stream.IntStream.range(0, references)
                .mapToObj(index -> new ExternalReference(UUID.randomUUID(), draft.id(),
                        "urn:synthetic:document:" + index, "Declaracion sintetica", index + 1))
                .toList();
        return new EvidenceSourceService.Sources(files, list);
    }

    private static EvidenceContextPort.ProcessContext process(String state) {
        return new EvidenceContextPort.ProcessContext(
                PROCESS, CAREER, MODEL, 1, state,
                List.of(new EvidenceContextPort.Indicator(INDICATOR, "IND-1", "Indicador sintético")));
    }

    private static void assertProblem(java.util.function.Supplier<?> operation, String code) {
        assertThatThrownBy(() -> operation.get())
                .isInstanceOf(EvidenceProblemException.class)
                .satisfies(error -> assertThat(((EvidenceProblemException) error).code()).isEqualTo(code));
    }
}
