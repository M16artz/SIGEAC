package ec.edu.unl.sec.governance.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import ec.edu.unl.sec.audit.application.AuditEventCommand;
import ec.edu.unl.sec.audit.application.AuditPort;
import ec.edu.unl.sec.governance.domain.Consolidation;
import ec.edu.unl.sec.governance.domain.Finding;
import ec.edu.unl.sec.identityaccess.application.AuthorizationPort;
import ec.edu.unl.sec.identityaccess.application.CurrentActorPort;
import ec.edu.unl.sec.identityaccess.domain.OperationCode;
import ec.edu.unl.sec.selfassessment.application.DocumentaryProcessPort;
import ec.edu.unl.sec.academicevaluation.application.AcademicConsolidationPort;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FindingServiceTest {
    final UUID process = UUID.randomUUID(), actor = UUID.randomUUID(), career = UUID.randomUUID(),
            indicator = UUID.randomUUID(), foreignIndicator = UUID.randomUUID();
    final CurrentActorPort actors = mock(CurrentActorPort.class);
    final AuthorizationPort auth = mock(AuthorizationPort.class);
    final DocumentaryProcessPort processes = mock(DocumentaryProcessPort.class);
    final AcademicConsolidationPort academic = mock(AcademicConsolidationPort.class);
    final FindingRepository findings = mock(FindingRepository.class);
    final AuditPort audit = mock(AuditPort.class);
    final FindingService service = new FindingService(actors, auth, processes, academic, findings, audit);

    void state(String state) {
        var context = new DocumentaryProcessPort.Context(process, career, UUID.randomUUID(), state, 7,
                List.of(indicator));
        when(processes.context(process)).thenReturn(Optional.of(context));
        when(processes.lock(process)).thenReturn(Optional.of(context));
    }
    void rejected(String expected, Runnable action) {
        try { action.run(); } catch (ec.edu.unl.sec.governance.application.GovernanceProblemException e) {
            assertThat(expected).as("code esperado fue %s", e.code()).isEqualTo(e.code()); return; }
        catch (Exception e) { throw new AssertionError(e); }
        org.junit.jupiter.api.Assertions.fail("Se esperaba el rechazo " + expected);
    }

    @Test void registersFindingLinkedToProcessIndicatorsAndHidesForeignSupports() {
        when(actors.currentActor()).thenReturn(Optional.of(new CurrentActorPort.Actor(actor, false)));
        when(auth.hasOperation(any(), any())).thenReturn(true);
        when(auth.isAllowed(any(), any(), any())).thenReturn(true);
        state("ACTIVE");
        var command = new FindingService.RegisterCommand("WEAKNESS", "Cobertura insuficiente de la fuente B",
                "La evidencia documentada no cubre los indicadores declarados del programa.", List.of(indicator),
                List.of(UUID.randomUUID()), List.of());
        var finding = service.register(process, command, UUID.randomUUID());
        assertThat(finding.data().type()).isEqualTo(Finding.Type.WEAKNESS);
        assertThat(finding.supports().stream().filter(s -> s.kind() == Finding.Kind.EVALUATION).count()).isEqualTo(1);
        verify(findings).appendFinding(finding);
        verify(audit).record(any(AuditEventCommand.class));
        assertThat(service.findings(process, 0, 20).items()).isEmpty();
        when(findings.findings(process, 0, 20)).thenReturn(List.of(finding));
        when(findings.totalFindings(process)).thenReturn(1L);
        assertThat(service.findings(process, 0, 20).totalElements()).isEqualTo(1L);
        var foreign = new FindingService.RegisterCommand("OBSERVATION", "Hallazgo fuera de ámbito sintético",
                "Este soporte apunta a un indicador que no pertenece al proceso.", List.of(foreignIndicator), List.of(), List.of());
        rejected("REPORT_FINDING_SUPPORT_FOREIGN", () -> service.register(process, foreign, UUID.randomUUID()));
        when(findings.totalFindings(process)).thenReturn(1L);
    }

    @ParameterizedTest @ValueSource(strings = {"PREPARATION", "SUSPENDED", "CLOSING", "CLOSED"})
    void rejectsInactivePhasesWithoutTouchingRecords(String state) {
        when(actors.currentActor()).thenReturn(Optional.of(new CurrentActorPort.Actor(actor, false)));
        when(auth.hasOperation(any(), any())).thenReturn(true);
        when(auth.isAllowed(any(), any(), any())).thenReturn(true);
        state(state);
        rejected("REPORT_PHASE_CLOSED", () -> service.register(process,
                new FindingService.RegisterCommand("STRENGTH", "Fortaleza argumentada sintética",
                        "La revisión institucional sintética respalda la cobertura declarada.", List.of(indicator), List.of(), List.of()),
                UUID.randomUUID()));
        verify(findings, never()).appendFinding(any());
        verify(findings, never()).appendConsolidation(any());
    }

    @Test void consolidatesFindingsOnceAndDemandsValidatedRules() {
        when(actors.currentActor()).thenReturn(Optional.of(new CurrentActorPort.Actor(actor, false)));
        when(auth.hasOperation(any(), any())).thenReturn(true);
        when(auth.isAllowed(any(), any(), any())).thenReturn(true);
        state("ACTIVE");
        var finding = new Finding(UUID.randomUUID(), process, new Finding.Data(Finding.Type.WEAKNESS,
                "Debilitamiento trazable sintético", "La medición académica revela una brecha documentada.", List.of(indicator),
                List.of(), List.of()), actor, Instant.now());
        when(findings.byIds(eq(process), anyList())).thenReturn(List.of(finding));
        when(findings.latestConsolidation(process)).thenReturn(Optional.empty());
        when(findings.findingsPresentInConsolidations(process)).thenReturn(Set.of());
        var rule = UUID.randomUUID();
        when(academic.allowsAggregation(any(), any(UUID.class))).thenReturn(false);
        rejected("ACADEMIC_AGGREGATION_RULE_UNVALIDATED", () -> service.consolidate(process, 0,
                new FindingService.ConsolidateCommand(List.of(finding.id()), rule, "Consolidación académica trazable sintética."),
                UUID.randomUUID()));
        verify(findings, never()).appendConsolidation(any());
        when(academic.allowsAggregation(any(), any(UUID.class))).thenReturn(true);
        var consolidation = service.consolidate(process, 0,
                new FindingService.ConsolidateCommand(List.of(finding.id()), rule, "Consolidación académica trazable sintética."),
                UUID.randomUUID());
        verify(findings).appendConsolidation(consolidation);
        assertThat(consolidation.revision()).isEqualTo(1L);
        assertThat(consolidation.data().findingIds()).containsExactly(finding.id());
        when(findings.latestConsolidation(process)).thenReturn(Optional.of(consolidation));
        when(findings.findingsPresentInConsolidations(process)).thenReturn(Set.of(finding.id()));
        var foreign = new Finding(UUID.randomUUID(), UUID.randomUUID(), finding.data(), actor, Instant.now());
        when(findings.byIds(eq(process), anyList())).thenReturn(List.of());
        rejected("REPORT_FINDING_NOT_FOUND", () -> service.consolidate(process, 1,
                new FindingService.ConsolidateCommand(List.of(foreign.id()), null, "Consolidación repetida sintética."),
                UUID.randomUUID()));
        when(findings.byIds(eq(process), anyList())).thenReturn(List.of(finding));
        rejected("REPORT_FINDING_ALREADY_CONSOLIDATED", () -> service.consolidate(process, 1,
                new FindingService.ConsolidateCommand(List.of(finding.id()), null, "Consolidación repetida sintética."),
                UUID.randomUUID()));
        var fresh = new Finding(UUID.randomUUID(), process, finding.data(), actor, Instant.now());
        when(findings.byIds(eq(process), anyList())).thenReturn(List.of(fresh));
        when(findings.findingsPresentInConsolidations(process)).thenReturn(Set.of(finding.id()));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.consolidate(process, 0,
                new FindingService.ConsolidateCommand(List.of(fresh.id()), null, "Consolidación conciliada de nuevo."), UUID.randomUUID()))
                .isInstanceOf(org.springframework.dao.ConcurrencyFailureException.class);
        verify(findings, times(1)).appendConsolidation(any());
    }

    @Test void hidesRegistryBehindCapabilityAndScope() {
        when(actors.currentActor()).thenReturn(Optional.of(new CurrentActorPort.Actor(actor, false)));
        when(auth.hasOperation(any(), eq(OperationCode.FINDING_READ))).thenReturn(false);
        rejected("RESOURCE_NOT_FOUND", () -> service.findings(process, 0, 20));
        verify(processes, never()).context(any());
        when(auth.hasOperation(any(), any())).thenReturn(true);
        when(auth.isAllowed(any(), any(), any())).thenReturn(false);
        rejected("RESOURCE_NOT_FOUND", () -> service.findings(process, 0, 20));
        verify(processes, never()).lock(any());
        when(auth.isAllowed(any(), any(), any())).thenReturn(true);
        state("ACTIVE");
        rejected("REPORT_PAGE_LIMIT_EXCEEDED", () -> service.findings(process, 0, 200));
    }

    @Test void sessionsAreTheOnlyActorsAndTechnicalAccountsAreRejected() {
        when(actors.currentActor()).thenReturn(Optional.of(new CurrentActorPort.Actor(actor, true)));
        when(auth.hasOperation(any(), any())).thenReturn(true);
        when(auth.isAllowed(any(), any(), any())).thenReturn(true);
        state("ACTIVE");
        rejected("REPORT_ACTOR_REQUIRED", () -> service.register(process,
                new FindingService.RegisterCommand("OBSERVATION", "Registro técnico rechazado sustentado",
                        "Las cuentas técnicas no registran hallazgos del comité.", List.of(indicator), List.of(), List.of()),
                UUID.randomUUID()));
        when(actors.currentActor()).thenReturn(Optional.empty());
        rejected("REPORT_ACTOR_REQUIRED", () -> service.register(process,
                new FindingService.RegisterCommand("OBSERVATION", "Registro técnico rechazado sustentado",
                        "Las cuentas técnicas no registran hallazgos del comité.", List.of(indicator), List.of(), List.of()),
                UUID.randomUUID()));
        verify(findings, never()).appendFinding(any());
    }
}
