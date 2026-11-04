package ec.edu.unl.sec.evidence.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ec.edu.unl.sec.audit.application.AuditPort;
import ec.edu.unl.sec.evidence.domain.EvidenceVersion;
import ec.edu.unl.sec.evidence.domain.ExternalReference;
import ec.edu.unl.sec.evidence.domain.SourceCoverage;
import ec.edu.unl.sec.evidence.domain.StructuredEvidence;
import ec.edu.unl.sec.evidence.domain.VersionFile;
import ec.edu.unl.sec.evidence.domain.VersionIndicator;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Correction successor rules (T054, P10): the successor draft inherits every
 * declared coverage, structured snapshot, reference and file of the current
 * submitted version, advances the evidence lineage, and leaves the
 * predecessor untouched for the reviewer history.
 */
class CreateEvidenceSuccessorServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-23T12:00:00Z");
    private static final UUID RESPONDER = UUID.fromString("00000000-0000-0000-0000-000000000c31");
    private static final UUID EVIDENCE = UUID.fromString("00000000-0000-0000-0000-000000000c43");
    private static final UUID SOURCE_VERSION = UUID.fromString("00000000-0000-0000-0000-000000000c01");
    private static final UUID SUCCESSOR = UUID.fromString("00000000-0000-0000-0000-000000000c02");
    private static final UUID SOURCE = UUID.fromString("00000000-0000-0000-0000-000000000c11");

    private EvidenceDraftService drafts;
    private EvidenceDraftRepository draftRepository;
    private EvidenceSourceRepository sources;
    private EvidenceGovernanceRepository governance;
    private AuditPort audit;
    private CreateEvidenceSuccessorService service;

    private final EvidenceVersion predecessor = submitted();
    private final EvidenceVersion successor = draftSuccessor();

    @BeforeEach
    void setUp() {
        drafts = org.mockito.Mockito.mock(EvidenceDraftService.class);
        draftRepository = org.mockito.Mockito.mock(EvidenceDraftRepository.class);
        sources = org.mockito.Mockito.mock(EvidenceSourceRepository.class);
        governance = org.mockito.Mockito.mock(EvidenceGovernanceRepository.class);
        audit = org.mockito.Mockito.mock(AuditPort.class);
        service = new CreateEvidenceSuccessorService(drafts, draftRepository, sources, governance, audit,
                Clock.fixed(NOW, ZoneOffset.UTC));
        when(drafts.createSuccessorDraft(SOURCE_VERSION)).thenReturn(successor);
        when(sources.nextFilePosition(SUCCESSOR)).thenReturn(1);
        when(sources.nextReferencePosition(SUCCESSOR)).thenReturn(1);
    }

    @Test
    void copiesCoveragesSnapshotsReferencesAndFilesIntoTheSuccessor() {
        var coverage = new SourceCoverage(UUID.randomUUID(), SOURCE_VERSION, SOURCE,
                "Cobertura corregida por el responsable.", RESPONDER, NOW.minusSeconds(1200));
        var snapshot = new StructuredEvidence(UUID.randomUUID(), SOURCE_VERSION, SOURCE,
                StructuredEvidence.Kind.STRUCTURED_DATA, StructuredEvidence.TypedValue.number(new BigDecimal("4")),
                "puntos", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30), null, RESPONDER,
                NOW.minusSeconds(1200));
        var reference = new ExternalReference(UUID.randomUUID(), SOURCE_VERSION,
                "urn:synthetic:document:v1", "Declaracion sintetica", 1);
        var file = new VersionFile(SOURCE_VERSION, UUID.randomUUID(), "informe.pdf",
                "application/pdf", "application/pdf", 10, "a".repeat(64), 1);
        when(governance.coverages(SOURCE_VERSION)).thenReturn(List.of(coverage));
        when(governance.structuredSnapshots(SOURCE_VERSION)).thenReturn(List.of(snapshot));
        when(sources.references(SOURCE_VERSION)).thenReturn(List.of(reference));
        when(sources.files(SOURCE_VERSION)).thenReturn(List.of(file));

        var created = service.createSuccessor(SOURCE_VERSION);

        assertThat(created.versionId()).isEqualTo(SUCCESSOR);
        assertThat(created.revisionNumber()).isEqualTo(2);

        var savedCoverage = ArgumentCaptor.forClass(SourceCoverage.class);
        verify(governance).saveCoverage(savedCoverage.capture());
        assertThat(savedCoverage.getValue().versionId()).isEqualTo(SUCCESSOR);
        assertThat(savedCoverage.getValue().requiredSourceId()).isEqualTo(SOURCE);

        var savedSnapshot = ArgumentCaptor.forClass(StructuredEvidence.class);
        verify(governance).saveStructured(savedSnapshot.capture());
        assertThat(savedSnapshot.getValue().versionId()).isEqualTo(SUCCESSOR);
        assertThat(savedSnapshot.getValue().value().number()).isEqualByComparingTo(new BigDecimal("4"));

        var savedReference = ArgumentCaptor.forClass(ExternalReference.class);
        verify(sources).addReference(savedReference.capture());
        assertThat(savedReference.getValue().versionId()).isEqualTo(SUCCESSOR);

        var savedFile = ArgumentCaptor.forClass(VersionFile.class);
        verify(sources).addFile(savedFile.capture());
        assertThat(savedFile.getValue().versionId()).isEqualTo(SUCCESSOR);
        assertThat(savedFile.getValue().fileId()).isEqualTo(file.fileId());

        verify(draftRepository).createSuccessor(any(), eq(SOURCE_VERSION));
        assertThat(predecessor.status()).isNotEqualTo(successor.status());
        verify(audit).record(any());
    }

    private static EvidenceVersion submitted() {
        return new EvidenceVersion(
                SOURCE_VERSION, EVIDENCE, 1, null,
                ec.edu.unl.sec.evidence.domain.EvidenceVersionStatus.SUBMITTED,
                "Informe hospitalario",
                "Descripción sintética",
                "Procedencia sintética",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30),
                RESPONDER, NOW, NOW, 2,
                List.of(new VersionIndicator(SOURCE_VERSION, UUID.randomUUID(), "IND-1", "Indicador", 1)));
    }

    private static EvidenceVersion draftSuccessor() {
        return new EvidenceVersion(
                SUCCESSOR, EVIDENCE, 2, SOURCE_VERSION,
                ec.edu.unl.sec.evidence.domain.EvidenceVersionStatus.DRAFT,
                "Informe hospitalario",
                "Descripción sintética",
                "Procedencia sintética",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30),
                RESPONDER, NOW, null, 0,
                List.of(new VersionIndicator(SUCCESSOR, UUID.randomUUID(), "IND-1", "Indicador", 1)));
    }
}
