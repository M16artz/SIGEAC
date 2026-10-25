package ec.edu.unl.sec.archive.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.unl.sec.archive.domain.IntegralClosure;
import ec.edu.unl.sec.audit.application.AuditPort;
import ec.edu.unl.sec.identityaccess.application.AuthorizationPort;
import ec.edu.unl.sec.identityaccess.application.CurrentActorPort;
import ec.edu.unl.sec.identityaccess.application.InstitutionalAuthorityPort;
import ec.edu.unl.sec.selfassessment.application.DocumentaryProcessPort;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClosureReopeningPolicyTest {
    private final UUID process = UUID.randomUUID();
    private final UUID career = UUID.randomUUID();
    private final UUID model = UUID.randomUUID();
    private final UUID actor = UUID.randomUUID();
    private final UUID documentaryId = UUID.randomUUID();
    private final CurrentActorPort actors = mock(CurrentActorPort.class);
    private final AuthorizationPort auth = mock(AuthorizationPort.class);
    private final InstitutionalAuthorityPort authority = mock(InstitutionalAuthorityPort.class);
    private final DocumentaryProcessPort processes = mock(DocumentaryProcessPort.class);
    private final ClosureEvidencePort evidence = mock(ClosureEvidencePort.class);
    private final IntegralClosureRepository repository = mock(IntegralClosureRepository.class);
    private final AuditPort audit = mock(AuditPort.class);
    private final IntegralClosureService service = new IntegralClosureService(actors, auth, authority, processes,
            evidence, repository, audit, new ObjectMapper());
    private final Instant documentaryAt = Instant.parse("2026-04-01T00:00:00Z");
    private final Instant integralAt = Instant.parse("2026-05-01T00:00:00Z");
    private final IntegralClosure integral = new IntegralClosure(UUID.randomUUID(), process, documentaryId,
            "INTEGRAL_STRICT_V1", "{\"sealed\":true}", "a".repeat(64), List.of(), List.of(), actor, integralAt);

    @BeforeEach
    void setup() {
        when(actors.currentActor()).thenReturn(Optional.of(new CurrentActorPort.Actor(actor, false)));
        when(auth.hasOperation(any(), any())).thenReturn(true);
        when(auth.isAllowed(any(), any(), any())).thenReturn(true);
        context("REOPENED");
        when(evidence.documentaryClosures(process)).thenReturn(List.of(
                new ClosureEvidencePort.DocumentaryClosureEvidence(documentaryId, UUID.randomUUID(),
                        "DOCUMENTARY_STRICT_V1", "b".repeat(64), documentaryAt, actor)));
        when(evidence.finalReport(process)).thenReturn(Optional.of(new ClosureEvidencePort.ReportEvidence(
                UUID.randomUUID(), 3, "APPROVED")));
        when(evidence.actions(process)).thenReturn(List.of());
        when(repository.closures(process, 0, 1000)).thenReturn(List.of(integral));
    }

    @Test
    void keepsBothSnapshotsInHistoryAfterControlledReopening() {
        var page = service.history(process, 0, 20);

        assertThat(page.totalElements()).isEqualTo(2);
        assertThat(page.items()).extracting(IntegralClosureService.ClosureView::type)
                .containsExactly("INTEGRAL_CLOSURE", "DOCUMENTARY_CLOSURE");
        assertThat(page.items().getFirst().sha256()).isEqualTo("a".repeat(64));
        verify(repository, never()).append(any());
    }

    @Test
    void reopenedProcessCannotBeClosedAgainWithoutDocumentaryPhase() {
        assertThatThrownBy(() -> service.close(process, 9, new IntegralClosureService.CloseCommand(
                "INTEGRAL_STRICT_V1", new IntegralClosureService.Provenance(UUID.randomUUID(),
                        "Ley Orgánica de Educación Superior", "Acta 2026", "Reapertura controlada autorizada."),
                List.of()), UUID.randomUUID()))
                .isInstanceOf(ArchiveProblemException.class).extracting("code")
                .isEqualTo("CLOSURE_STATE_INVALID");
        verify(processes, never()).completeIntegralClosure(any(), anyLong(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void previewExplainsBlockersForReopenedProcess() {
        var preview = service.preview(process, "INTEGRAL_CLOSURE");

        assertThat(preview.eligible()).isFalse();
        assertThat(preview.blockers()).anyMatch(message -> message.contains("cierre documental"));
        assertThat(preview.policyVersion()).isEqualTo("INTEGRAL_STRICT_V1");
    }

    private void context(String state) {
        var context = new DocumentaryProcessPort.Context(process, career, model, state, 9, List.of());
        when(processes.context(process)).thenReturn(Optional.of(context));
        when(processes.lock(process)).thenReturn(Optional.of(context));
    }
}
