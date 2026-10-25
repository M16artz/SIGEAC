package ec.edu.unl.sec.evidence.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ec.edu.unl.sec.evidence.domain.Evidence;
import ec.edu.unl.sec.evidence.domain.EvidenceVersion;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EvidenceDraftServiceTest {
    private static final Instant NOW = Instant.parse("2026-10-20T15:00:00Z");
    private static final UUID ACTOR = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID CAREER = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private static final UUID PROCESS = UUID.fromString("00000000-0000-0000-0000-000000000301");
    private static final UUID INDICATOR_A = UUID.fromString("00000000-0000-0000-0000-000000000401");
    private static final UUID INDICATOR_B = UUID.fromString("00000000-0000-0000-0000-000000000402");

    private EvidenceDraftRepository repository;
    private EvidenceAuthorizationPort authorization;
    private EvidenceContextPort context;
    private EvidenceDraftService service;

    @BeforeEach
    void setUp() {
        repository = mock(EvidenceDraftRepository.class);
        authorization = mock(EvidenceAuthorizationPort.class);
        context = mock(EvidenceContextPort.class);
        when(authorization.currentActor()).thenReturn(Optional.of(new EvidenceAuthorizationPort.Actor(ACTOR, false)));
        when(context.process(PROCESS)).thenReturn(Optional.of(process(INDICATOR_A, INDICATOR_B)));
        when(authorization.canCollect(ACTOR, CAREER, PROCESS, INDICATOR_A)).thenReturn(true);
        when(authorization.canCollect(ACTOR, CAREER, PROCESS, INDICATOR_B)).thenReturn(true);
        when(context.hasCurrentResponsibleAssignment(PROCESS, INDICATOR_A, ACTOR, NOW)).thenReturn(true);
        when(context.hasCurrentResponsibleAssignment(PROCESS, INDICATOR_B, ACTOR, NOW)).thenReturn(true);
        service = new EvidenceDraftService(repository, authorization, context, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createsDraftOnlyForIndicatorsInTheFixedModelAndCurrentActorScope() {
        EvidenceDraftService.EvidenceDetails created =
                service.create(command(List.of(INDICATOR_A, INDICATOR_B)), UUID.randomUUID());
        EvidenceVersion draft = created.latestVersion();

        assertThat(draft.authorId()).isEqualTo(ACTOR);
        assertThat(draft.indicators()).extracting("indicatorId").containsExactly(INDICATOR_A, INDICATOR_B);
        assertThat(draft.lockRevision()).isZero();
    }

    @Test
    void rejectsIndicatorOutsideTheProcessModel() {
        UUID foreignIndicator = UUID.randomUUID();

        assertProblem(
                () -> service.create(command(List.of(foreignIndicator)), UUID.randomUUID()),
                "INDICATOR_OUT_OF_SCOPE");
    }

    @Test
    void rejectsExpiredAssignmentEvenWhenTheActorHasTheCollectionCapability() {
        when(context.hasCurrentResponsibleAssignment(PROCESS, INDICATOR_A, ACTOR, NOW)).thenReturn(false);

        assertProblem(
                () -> service.create(command(List.of(INDICATOR_A)), UUID.randomUUID()),
                "INDICATOR_OUT_OF_SCOPE");
    }

    @Test
    void filtersCollectionsToTheActorsCurrentScope() {
        EvidenceDraftService.EvidenceDetails visible = storedDetails(INDICATOR_A);
        EvidenceDraftService.EvidenceDetails hidden = storedDetails(INDICATOR_B);
        when(repository.listByProcess(PROCESS)).thenReturn(List.of(visible, hidden));
        when(authorization.canCollect(ACTOR, CAREER, PROCESS, INDICATOR_B)).thenReturn(false);

        assertThat(service.list(PROCESS, Optional.empty())).containsExactly(visible);
    }

    @Test
    void hidesKnownDirectIdsWhenCurrentScopeDoesNotCoverThem() {
        EvidenceVersion hidden = storedDraft(PROCESS, INDICATOR_A);
        when(repository.findVersion(hidden.id())).thenReturn(Optional.of(hidden));
        when(repository.findEvidence(hidden.evidenceId()))
                .thenReturn(Optional.of(new EvidenceDraftService.EvidenceDetails(
                        new Evidence(hidden.evidenceId(), PROCESS, ACTOR, NOW, UUID.randomUUID(), 1), hidden)));
        when(authorization.canCollect(ACTOR, CAREER, PROCESS, INDICATOR_A)).thenReturn(false);

        assertProblem(() -> service.getVersion(hidden.id()), "RESOURCE_NOT_FOUND");
    }

    @Test
    void requiresCurrentStrongEtagBeforeUpdating() {
        EvidenceVersion draft = storedDraft(PROCESS, INDICATOR_A);
        when(repository.findVersion(draft.id())).thenReturn(Optional.of(draft));
        when(repository.findEvidence(draft.evidenceId()))
                .thenReturn(Optional.of(new EvidenceDraftService.EvidenceDetails(
                        new Evidence(draft.evidenceId(), PROCESS, ACTOR, NOW, UUID.randomUUID(), 1), draft)));

        assertProblem(() -> service.update(draft.id(), null, update(), UUID.randomUUID()), "PRECONDITION_REQUIRED");
        assertProblem(() -> service.update(draft.id(), "\"9\"", update(), UUID.randomUUID()), "STALE_DRAFT");
    }

    @Test
    void rejectsNewDraftsWhenTheProcessIsNotActive() {
        var active = process(INDICATOR_A);
        for (String state : List.of("DRAFT", "PREPARATION", "SUSPENDED", "CLOSING", "CLOSED", "REOPENED", "DOCUMENTARY_CLOSED")) {
            when(context.process(PROCESS)).thenReturn(Optional.of(new EvidenceContextPort.ProcessContext(
                    active.id(), active.careerId(), active.fixedModelVersionId(), active.modelVersionNumber(), state, active.indicators())));
            assertProblem(() -> service.create(command(List.of(INDICATOR_A)), UUID.randomUUID()), "PROCESS_NOT_ACTIVE");
        }
        org.mockito.Mockito.verifyNoInteractions(repository);
    }

    @Test
    void closingTheProcessBlocksEditsButKeepsAuthorizedHistoricalReads() {
        var stored = storedDetails(INDICATOR_A);
        var current = process(INDICATOR_A);
        when(repository.findVersion(stored.latestVersion().id())).thenReturn(Optional.of(stored.latestVersion()));
        when(repository.findEvidence(stored.evidence().id())).thenReturn(Optional.of(stored));
        when(context.process(PROCESS)).thenReturn(Optional.of(new EvidenceContextPort.ProcessContext(
                current.id(), current.careerId(), current.fixedModelVersionId(), current.modelVersionNumber(), "CLOSED", current.indicators())));
        assertThat(service.getVersion(stored.latestVersion().id())).isEqualTo(stored.latestVersion());
        assertProblem(() -> service.update(stored.latestVersion().id(), "\"0\"", update(), UUID.randomUUID()), "PROCESS_NOT_ACTIVE");
        org.mockito.Mockito.verify(repository, org.mockito.Mockito.never()).update(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void sourceEditsRequireAnActiveProcessTheAuthorAndTheCurrentDraftEtag() {
        var stored = storedDetails(INDICATOR_A);
        var version = stored.latestVersion();
        when(repository.findVersion(version.id())).thenReturn(Optional.of(version));
        when(repository.findEvidence(stored.evidence().id())).thenReturn(Optional.of(stored));
        assertThat(service.requireEditableVersion(version.id(), "\"0\"")).isEqualTo(version);
        assertProblem(() -> service.requireEditableVersion(version.id(), null), "PRECONDITION_REQUIRED");
        assertProblem(() -> service.requireEditableVersion(version.id(), "\"1\""), "STALE_DRAFT");
        var current = process(INDICATOR_A);
        when(context.process(PROCESS)).thenReturn(Optional.of(new EvidenceContextPort.ProcessContext(
                current.id(), current.careerId(), current.fixedModelVersionId(), 1, "SUSPENDED", current.indicators())));
        assertProblem(() -> service.requireEditableVersion(version.id(), "\"0\""), "PROCESS_NOT_ACTIVE");
        when(context.process(PROCESS)).thenReturn(Optional.of(current));
        UUID other = UUID.randomUUID();
        when(authorization.currentActor()).thenReturn(Optional.of(new EvidenceAuthorizationPort.Actor(other, false)));
        when(authorization.canCollect(other, CAREER, PROCESS, INDICATOR_A)).thenReturn(true);
        when(context.hasCurrentResponsibleAssignment(PROCESS, INDICATOR_A, other, NOW)).thenReturn(true);
        assertProblem(() -> service.requireEditableVersion(version.id(), "\"0\""), "RESOURCE_NOT_FOUND");
        when(authorization.currentActor()).thenReturn(Optional.of(new EvidenceAuthorizationPort.Actor(ACTOR, false)));
        when(repository.findVersion(version.id())).thenReturn(Optional.of(version.submit(NOW)));
        assertProblem(() -> service.requireEditableVersion(version.id(), "\"1\""), "DRAFT_REQUIRED");
    }

    @Test
    void createsSuccessorOnlyFromVisibleSubmittedVersionInActiveProcess() {
        var draft = storedDraft(PROCESS, INDICATOR_A);
        var predecessor = draft.submit(NOW);
        var evidence = new EvidenceDraftService.EvidenceDetails(
                new Evidence(draft.evidenceId(), PROCESS, ACTOR, NOW, UUID.randomUUID(), 1), predecessor);
        when(repository.findVersion(predecessor.id())).thenReturn(Optional.of(predecessor));
        when(repository.findEvidence(predecessor.evidenceId())).thenReturn(Optional.of(evidence));

        var successor = service.createSuccessor(predecessor.id());

        assertThat(successor.evidenceId()).isEqualTo(predecessor.evidenceId());
        assertThat(successor.predecessorId()).isEqualTo(predecessor.id());
        assertThat(successor.revisionNumber()).isEqualTo(2);
        assertThat(successor.authorId()).isEqualTo(ACTOR);
        org.mockito.Mockito.verify(repository).createSuccessor(predecessor, successor);
    }

    private static EvidenceDraftService.CreateCommand command(List<UUID> indicators) {
        return new EvidenceDraftService.CreateCommand(
                PROCESS,
                "Informe sintético",
                "Descripción sintética de la evidencia",
                "Generado para pruebas",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 6, 30),
                indicators);
    }

    private static EvidenceDraftService.UpdateCommand update() {
        return new EvidenceDraftService.UpdateCommand(
                "Informe actualizado",
                "Descripción actualizada",
                "Procedencia actualizada",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 6, 30),
                List.of(INDICATOR_A));
    }

    private static EvidenceContextPort.ProcessContext process(UUID... indicators) {
        return new EvidenceContextPort.ProcessContext(
                PROCESS,
                CAREER,
                UUID.randomUUID(),
                1,
                "ACTIVE",
                java.util.stream.IntStream.range(0, indicators.length)
                        .mapToObj(index -> new EvidenceContextPort.Indicator(
                                indicators[index], "IND-" + (index + 1), "Indicador " + (index + 1)))
                        .toList());
    }

    private static EvidenceDraftService.EvidenceDetails storedDetails(UUID indicatorId) {
        UUID evidenceId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        Evidence evidence = new Evidence(evidenceId, PROCESS, ACTOR, NOW, UUID.randomUUID(), 1);
        EvidenceVersion version = new EvidenceVersion(
                versionId,
                evidenceId,
                1,
                null,
                null,
                "Informe sintético",
                "Descripción sintética",
                "Procedencia sintética",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 6, 30),
                ACTOR,
                NOW,
                null,
                0,
                List.of(new ec.edu.unl.sec.evidence.domain.VersionIndicator(
                        versionId, indicatorId, "IND-1", "Indicador 1", 1)));
        return new EvidenceDraftService.EvidenceDetails(evidence, version);
    }

    private static EvidenceVersion storedDraft(UUID processId, UUID indicatorId) {
        return storedDetails(indicatorId).latestVersion();
    }

    private static void assertProblem(org.assertj.core.api.ThrowableAssert.ThrowingCallable operation, String code) {
        assertThatThrownBy(operation)
                .isInstanceOf(EvidenceProblemException.class)
                .satisfies(error -> assertThat(((EvidenceProblemException) error).code()).isEqualTo(code));
    }
}
