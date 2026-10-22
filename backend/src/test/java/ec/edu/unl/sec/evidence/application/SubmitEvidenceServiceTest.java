package ec.edu.unl.sec.evidence.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ec.edu.unl.sec.evidence.domain.ApplicabilityDecision;
import ec.edu.unl.sec.evidence.domain.EvidenceVersion;
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
    private static final String TOKEN = "opaque-session";

    private EvidenceDraftService drafts;
    private EvidenceGovernanceRepository governance;
    private EvidenceContextPort context;
    private SubmitEvidenceService service;
    private EvidenceVersion draft;
    private EvidenceDraftService.EvidenceDetails details;

    @BeforeEach
    void setUp() {
        drafts = org.mockito.Mockito.mock(EvidenceDraftService.class);
        governance = org.mockito.Mockito.mock(EvidenceGovernanceRepository.class);
        context = org.mockito.Mockito.mock(EvidenceContextPort.class);
        draft = draft(0, null);
        details = new EvidenceDraftService.EvidenceDetails(
                new ec.edu.unl.sec.evidence.domain.Evidence(
                        draft.evidenceId(), PROCESS, ACTOR, NOW, UUID.randomUUID(), 1), draft);
        when(drafts.getVersion(draft.id())).thenReturn(draft);
        when(drafts.getEvidence(draft.evidenceId())).thenReturn(details);
        when(context.process(PROCESS)).thenReturn(Optional.of(process("ACTIVE")));
        service = new SubmitEvidenceService(drafts, governance, context, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void submitsAMutableDraftAfterExplicitCoverage() {
        when(governance.coverages(draft.id())).thenReturn(List.of(
                new ec.edu.unl.sec.evidence.domain.SourceCoverage(
                        UUID.randomUUID(), draft.id(), UUID.randomUUID(), "Nota", ACTOR, NOW)));
        when(governance.markSubmitted(eq(draft.id()), eq(NOW), eq(0L))).thenReturn(true);

        var submitted = service.submit(draft.id(), "\"0\"");

        assertThat(submitted.status()).isEqualTo(ec.edu.unl.sec.evidence.domain.EvidenceVersionStatus.SUBMITTED);
        assertThat(submitted.submittedAt()).isEqualTo(NOW);
        assertThat(submitted.lockRevision()).isEqualTo(1);
    }

    @Test
    void rejectsSubmissionsWithoutDeclaredCoverage() {
        when(governance.coverages(draft.id())).thenReturn(List.of());

        assertProblem(() -> service.submit(draft.id(), "\"0\""), "COVERAGE_REQUIRED");
        verify(governance, never()).markSubmitted(any(), any(), org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void rejectsStaleDraftsAndAlreadySubmittedVersions() {
        when(governance.coverages(draft.id())).thenReturn(List.of(
                new ec.edu.unl.sec.evidence.domain.SourceCoverage(
                        UUID.randomUUID(), draft.id(), UUID.randomUUID(), "Nota", ACTOR, NOW)));
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
                        UUID.randomUUID(), draft.id(), UUID.randomUUID(), "Nota", ACTOR, NOW)));
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

    private static EvidenceContextPort.ProcessContext process(String state) {
        return new EvidenceContextPort.ProcessContext(
                PROCESS, CAREER, UUID.randomUUID(), 1, state,
                List.of(new EvidenceContextPort.Indicator(INDICATOR, "IND-1", "Indicador sintético")));
    }

    private static void assertProblem(java.util.function.Supplier<?> operation, String code) {
        assertThatThrownBy(() -> operation.get())
                .isInstanceOf(EvidenceProblemException.class)
                .satisfies(error -> assertThat(((EvidenceProblemException) error).code()).isEqualTo(code));
    }
}
