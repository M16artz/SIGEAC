package ec.edu.unl.sec.governance.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import ec.edu.unl.sec.audit.application.AuditEventCommand;
import ec.edu.unl.sec.audit.application.AuditPort;
import ec.edu.unl.sec.academicevaluation.application.AcademicConsolidationPort;
import ec.edu.unl.sec.governance.domain.ApprovalDecision;
import ec.edu.unl.sec.governance.domain.Consolidation;
import ec.edu.unl.sec.governance.domain.ReportObservation;
import ec.edu.unl.sec.governance.domain.ReportVersion;
import ec.edu.unl.sec.identityaccess.application.AuthorizationPort;
import ec.edu.unl.sec.identityaccess.application.CurrentActorPort;
import ec.edu.unl.sec.identityaccess.application.InstitutionalAuthorityPort;
import ec.edu.unl.sec.identityaccess.domain.OperationCode;
import ec.edu.unl.sec.selfassessment.application.DocumentaryProcessPort;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ReportWorkflowServiceTest {
    final UUID process = UUID.randomUUID(), actor = UUID.randomUUID(), career = UUID.randomUUID(),
            model = UUID.randomUUID(), consolidation = UUID.randomUUID(), assignment = UUID.randomUUID();
    final CurrentActorPort actors = mock(CurrentActorPort.class);
    final AuthorizationPort auth = mock(AuthorizationPort.class);
    final DocumentaryProcessPort processes = mock(DocumentaryProcessPort.class);
    final AcademicConsolidationPort academic = mock(AcademicConsolidationPort.class);
    final FindingRepository findings = mock(FindingRepository.class);
    final ReportRepository reports = mock(ReportRepository.class);
    final InstitutionalAuthorityPort authority = mock(InstitutionalAuthorityPort.class);
    final AuditPort audit = mock(AuditPort.class);
    final ReportWorkflowService service =
            new ReportWorkflowService(actors, auth, processes, academic, findings, reports, authority, audit);
    final Map<UUID, Long> revisions = new HashMap<>();
    final Map<UUID, String> states = new HashMap<>();
    final Map<UUID, ReportVersion> store = new HashMap<>();

    @BeforeEach void setup() {
        when(actors.currentActor()).thenReturn(Optional.of(new CurrentActorPort.Actor(actor, false)));
        when(auth.hasOperation(any(), any())).thenReturn(true);
        when(auth.isAllowed(any(), any(), any())).thenReturn(true);
        state("ACTIVE");
        when(reports.latest(any(UUID.class))).thenAnswer(call -> {
            var processId = call.getArgument(0, UUID.class);
            return store.values().stream().filter(v -> v.processId().equals(processId))
                    .max(java.util.Comparator.comparingLong(ReportVersion::versionNumber));
        });
        when(reports.hasApprovedFinal(process)).thenReturn(false);
        when(reports.findVersion(any())).thenAnswer(call -> Optional.ofNullable(store.get(call.getArgument(0, UUID.class))));
        when(reports.observations(any())).thenReturn(List.of());
        when(reports.socializations(any())).thenReturn(List.of());
        when(reports.repliesForVersion(any())).thenReturn(List.of());
        when(reports.decisions(any())).thenReturn(List.of());
        when(reports.findObservation(any())).thenReturn(Optional.empty());
        when(findings.latestConsolidation(process)).thenReturn(Optional.of(new Consolidation(consolidation, process,
                new Consolidation.Data(List.of(UUID.randomUUID()), null, "Consolidación sintética registrada."),
                actor, Instant.now(), null, 1L)));
        when(authority.isAuthorizedAssignment(any(), any(), any(), any())).thenReturn(true);
        when(reports.advanceProcessing(any(UUID.class), anyLong(), any())).thenAnswer(call -> {
            var id = call.getArgument(0, UUID.class); var expected = call.getArgument(1, Long.class);
            var current = revisions.getOrDefault(id, 0L);
            if (current != expected) return false;
            revisions.put(id, current + 1);
            var state = call.getArgument(2, String.class);
            if (state != null) states.put(id, state);
            return true;
        });
        when(reports.processing(any(UUID.class))).thenAnswer(call -> {
            var id = call.getArgument(0, UUID.class);
            var state = states.containsKey(id) ? states.get(id)
                    : (store.containsKey(id) ? store.get(id).data().kind().name() : "PRELIMINARY");
            return Optional.of(new ReportRepository.Processing(state, revisions.getOrDefault(id, 0L)));
        });
        doAnswer(call -> {var v = call.getArgument(0, ReportVersion.class); store.put(v.id(), v); revisions.put(v.id(), 0L);
                return null; })
                .when(reports).appendVersion(any(ReportVersion.class));
    }
    void state(String state) {
        var context = new DocumentaryProcessPort.Context(process, career, model, state, 7, List.of());
        when(processes.context(process)).thenReturn(Optional.of(context));
        when(processes.lock(process)).thenReturn(Optional.of(context));
    }
    ReportWorkflowService.ReportCommand command(String kind, UUID predecessor) {
        return new ReportWorkflowService.ReportCommand(kind, consolidation, "Plantilla sintética P14 v1", true,
                "Contenido sustentado de autoevaluación. No constituye acreditación.", predecessor);
    }
    ReportVersion emit(String kind, UUID predecessor) {
        return service.create(process, 0, command(kind, predecessor), UUID.randomUUID());
    }
    void rejected(String expected, Runnable action) {
        try { action.run(); } catch (ec.edu.unl.sec.governance.application.GovernanceProblemException e) {
            assertThat(expected).as("code esperado fue %s", e.code()).isEqualTo(e.code()); return; }
        catch (Exception e) { throw new AssertionError(e); }
        Assertions.fail("Se esperaba el rechazo " + expected);
    }

    @Test void emitsPreliminarySnapshotAfterConsolidationAndDeclaresNoAccreditation() {
        var emitted = emit("PRELIMINARY", null);
        assertThat(emitted.data().kind()).isEqualTo(ReportVersion.Kind.PRELIMINARY);
        assertThat(emitted.contentSha256()).hasSize(64);
        assertThat(emitted.data().content()).contains("No constituye acreditación");
        assertThat(ReportWorkflowService.DISCLAIMER).contains("no constituye acreditación").contains("autoevaluación");
        verify(reports).appendVersion(emitted);
        verify(audit).record(any(AuditEventCommand.class));
        when(findings.latestConsolidation(process)).thenReturn(Optional.empty());
        rejected("REPORT_CONSOLIDATION_NOT_FOUND", () -> emit("PRELIMINARY", null));
        verify(reports, times(1)).appendVersion(any());
    }

    @Test void finalVersionRequiresLatestPredecessorAndPreservesApprovedVersionWhenCorrected() {
        var emitted = emit("PRELIMINARY", null);
        rejected("REPORT_PREDECESSOR_REQUIRED", () -> emit("FINAL", null));
        rejected("REPORT_VERSION_NOT_FOUND", () -> emit("FINAL", UUID.randomUUID()));
        var corrected = service.create(process, revisions.getOrDefault(emitted.id(), 0L),
                command("FINAL", emitted.id()), UUID.randomUUID());
        assertThat(corrected.data().predecessorVersionId()).isEqualTo(emitted.id());
        assertThat(corrected.versionNumber()).isEqualTo(2L);
        assertThat(corrected.contentSha256()).hasSize(64);
        assertThat(store.get(corrected.id())).isSameAs(corrected);
        when(findings.latestConsolidation(process)).thenReturn(Optional.empty());
        rejected("REPORT_CONSOLIDATION_NOT_FOUND", () -> service.create(emitted.processId(), 0, command("FINAL", emitted.id()), UUID.randomUUID()));
        when(reports.hasApprovedFinal(process)).thenReturn(true);
        when(findings.latestConsolidation(process)).thenReturn(Optional.of(new Consolidation(consolidation, process,
                new Consolidation.Data(List.of(), null, "Consolidación sintética registrada."), actor, Instant.now(), null, 1L)));
        rejected("REPORT_PREDECESSOR_NOT_LATEST", () -> emit("FINAL", emitted.id()));
        states.put(corrected.id(), "APPROVED");
        var successor = service.create(process, revisions.get(corrected.id()),
                command("FINAL", corrected.id()), UUID.randomUUID());
        assertThat(successor.predecessorVersionId()).isEqualTo(corrected.id());
        assertThat(successor.versionNumber()).isEqualTo(3L);
        assertThat(service.versionDetail(process, successor.id()).state()).isEqualTo("FINAL");
        assertThat(service.versionDetail(process, corrected.id()).state()).isEqualTo("APPROVED");
        assertThat(store.get(corrected.id())).isSameAs(corrected);
    }

    @ParameterizedTest @ValueSource(strings = {"PREPARATION", "SUSPENDED", "CLOSING", "CLOSED"})
    void rejectsInactivePhasesWithoutTouchingReports(String state) {
        state(state);
        rejected("REPORT_PHASE_CLOSED", () -> emit("PRELIMINARY", null));
        verify(reports, never()).appendVersion(any());
        verify(reports, never()).appendObservation(any());
        verify(reports, never()).appendDecision(any());
    }

    @Test void observationMarksUnderObservationProjectionWithoutChangingEmittedBytes() {
        var emitted = emit("PRELIMINARY", null);
        var stored = store.get(emitted.id());
        var event = service.observe(emitted.id(), revisions.get(emitted.id()),
                new ReportWorkflowService.ObservationCommand("Corrija la cobertura faltante de la fuente B.", "Sección III"), UUID.randomUUID());
        verify(reports).appendObservation(event);
        assertThat(stored).isSameAs(store.get(emitted.id()));
        when(reports.observations(emitted.id())).thenReturn(List.of(event));
        var detail = service.versionDetail(process, emitted.id());
        assertThat(detail.state()).isEqualTo("UNDER_OBSERVATION");
        assertThat(detail.contentSha256()).hasSize(64);
        verify(reports, times(1)).appendVersion(any());
        when(reports.repliesForVersion(emitted.id())).thenReturn(List.of(new ec.edu.unl.sec.governance.domain.ObservationReply(
                UUID.randomUUID(), event.id(), "Respuesta suficiente de la comisión.", null, actor, Instant.now())));
        assertThat(service.versionDetail(process, emitted.id()).state()).isEqualTo("PRELIMINARY");
    }

    @Test void replyLinksCorrectedVersionAndStaysAppendOnly() {
        var emitted = emit("PRELIMINARY", null);
        var observedAt = Instant.parse("2026-10-25T10:30:00Z");
        var observation = new ReportObservation(UUID.randomUUID(), emitted.id(), "Sección III", "Corrija la cobertura faltante.", actor, observedAt);
        when(reports.findObservation(observation.id())).thenReturn(Optional.of(observation));
        var corrected = service.create(process, revisions.get(emitted.id()), command("FINAL", emitted.id()), UUID.randomUUID());
        store.put(corrected.id(), corrected);
        rejected("REPORT_INVALID_CORRECTION_LINK", () -> service.reply(observation.id(), revisions.get(emitted.id()),
                new ReportWorkflowService.ReplyCommand("Respuesta sustentada de la comisión.", UUID.randomUUID()), UUID.randomUUID()));
        var reply = service.reply(observation.id(), revisions.get(emitted.id()),
                new ReportWorkflowService.ReplyCommand("Respuesta sustentada de la comisión.", corrected.id()), UUID.randomUUID());
        verify(reports).appendReply(reply);
        assertThat(reply.observationId()).isEqualTo(observation.id());
        when(reports.repliesForVersion(emitted.id())).thenReturn(List.of(reply));
        rejected("REPORT_OBSERVATION_ALREADY_ANSWERED", () -> service.reply(observation.id(), revisions.get(emitted.id()),
                new ReportWorkflowService.ReplyCommand("Respuesta duplicada rationalizada.", corrected.id()), UUID.randomUUID()));
        when(reports.findObservation(observation.id())).thenReturn(Optional.empty());
        rejected("REPORT_OBSERVATION_NOT_FOUND", () -> service.reply(observation.id(), revisions.get(emitted.id()),
                new ReportWorkflowService.ReplyCommand("Respuesta duplicada rationalizada.", corrected.id()), UUID.randomUUID()));
    }

    @Test void approvalsRequireFinalStateCompetentAuthorityAndSingleDecision() {
        var emitted = emit("PRELIMINARY", null);
        var command = new ReportWorkflowService.ApprovalDecisionCommand("APPROVED", new ReportWorkflowService.Provenance(
                assignment, "Competencia sintética registrada", "fixture://authority", "Decisión sustentada suficiente"));
        rejected("REPORT_NOT_FINAL", () -> service.decide(emitted.id(), revisions.get(emitted.id()), command, UUID.randomUUID()));
        verify(reports, never()).appendDecision(any());
        when(authority.isAuthorizedAssignment(eq(assignment), eq(actor), eq(OperationCode.REPORT_APPROVE), any())).thenReturn(false);
        var finalVersion = service.create(process, revisions.get(emitted.id()), command("FINAL", emitted.id()), UUID.randomUUID());
        store.put(finalVersion.id(), finalVersion);
        rejected("REPORT_AUTHORITY_DENIED", () -> service.decide(finalVersion.id(), revisions.get(finalVersion.id()), command, UUID.randomUUID()));
        when(authority.isAuthorizedAssignment(any(), any(), any(), any())).thenReturn(true);
        var approved = service.decide(finalVersion.id(), revisions.get(finalVersion.id()), command, UUID.randomUUID());
        verify(reports).appendDecision(approved);
        assertThat(service.versionDetail(process, finalVersion.id()).state()).isEqualTo("APPROVED");
        when(reports.decisions(finalVersion.id())).thenReturn(List.of(approved));
        rejected("REPORT_DECISION_ALREADY_RECORDED", () -> service.decide(finalVersion.id(), revisions.get(finalVersion.id()), command, UUID.randomUUID()));
        var invalid = new ReportWorkflowService.ApprovalDecisionCommand("REJECTED", command.provenance());
        rejected("REPORT_DECISION_UNSUPPORTED", () -> service.decide(finalVersion.id(), revisions.get(finalVersion.id()), invalid, UUID.randomUUID()));
    }

    @Test void staleRevisionGuardsRejectConcurrentEvents() {
        var emitted = emit("PRELIMINARY", null);
        var revision = revisions.get(emitted.id());
        revisions.put(emitted.id(), revision + 1);
        assertThatThrownBy(() -> service.socialize(emitted.id(), revision,
                new ReportWorkflowService.SocializationCommand(Instant.now(), List.of(assignment), "fixture://acta-sintetica"),
                UUID.randomUUID())).isInstanceOf(org.springframework.dao.ConcurrencyFailureException.class);
        verify(reports, never()).appendSocialization(any());
    }

    @Test void hidesUnknownVersionsBehindCapabilityAndScope() {
        when(auth.hasOperation(any(), eq(OperationCode.REPORT_READ))).thenReturn(false);
        rejected("RESOURCE_NOT_FOUND", () -> service.viewVersion(process, UUID.randomUUID()));
        verify(reports, never()).findVersion(any());
        when(auth.hasOperation(any(), eq(OperationCode.REPORT_READ))).thenReturn(true);
        when(auth.isAllowed(any(), any(), any())).thenReturn(false);
        rejected("RESOURCE_NOT_FOUND", () -> service.viewVersion(process, UUID.randomUUID()));
        verify(reports, never()).findVersion(any());
        verify(processes, never()).lock(any());
    }

    @Test void sessionsAreTheOnlyActorsAndTechnicalAccountsAreRejected() {
        when(actors.currentActor()).thenReturn(Optional.of(new CurrentActorPort.Actor(actor, true)));
        rejected("REPORT_ACTOR_REQUIRED", () -> emit("PRELIMINARY", null));
        when(actors.currentActor()).thenReturn(Optional.empty());
        rejected("REPORT_ACTOR_REQUIRED", () -> emit("PRELIMINARY", null));
        verify(reports, never()).appendVersion(any());
    }
}
