package ec.edu.unl.sec.improvement.api;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.unl.sec.audit.application.FailureAuditPort;
import ec.edu.unl.sec.identityaccess.application.CurrentActorPort;
import ec.edu.unl.sec.identityaccess.application.SessionService;
import ec.edu.unl.sec.identityaccess.infrastructure.AccessProblemWriter;
import ec.edu.unl.sec.identityaccess.infrastructure.SessionAuthenticationFilter;
import ec.edu.unl.sec.improvement.application.ActionProgressService;
import ec.edu.unl.sec.improvement.application.ImprovementPlanService;
import ec.edu.unl.sec.improvement.application.ImprovementProblemException;
import ec.edu.unl.sec.improvement.domain.ActionDecision;
import ec.edu.unl.sec.improvement.domain.ActionProgress;
import ec.edu.unl.sec.improvement.domain.ImprovementAction;
import ec.edu.unl.sec.improvement.domain.ImprovementPlan;
import ec.edu.unl.sec.shared.api.ApiProblemHandler;
import ec.edu.unl.sec.shared.domain.DecisionProvenance;
import ec.edu.unl.sec.shared.infrastructure.CorrelationFilter;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ImprovementControllerTest {
    final UUID process = UUID.randomUUID(), career = UUID.randomUUID(), actor = UUID.randomUUID(),
            plan = UUID.randomUUID(), action = UUID.randomUUID(), finding = UUID.randomUUID(),
            assignment = UUID.randomUUID(), evidence = UUID.randomUUID(), version = UUID.randomUUID();
    final ImprovementPlanService plans = mock(ImprovementPlanService.class);
    final ActionProgressService progress = mock(ActionProgressService.class);
    final FailureAuditPort failures = mock(FailureAuditPort.class);
    final String csrf = "synthetic-improvement-csrf";
    MockMvc mvc;

    Cookie session() { return new Cookie("SEC_SESSION", "session"); }

    @BeforeEach
    void setup() {
        var sessions = mock(SessionService.class);
        when(sessions.authenticateForRequest("session")).thenReturn(java.util.Optional.of(
                new SessionService.AuthenticatedSession(UUID.randomUUID(), actor, "Coordinador sintético", false,
                        SessionService.sha256Hex(csrf))));
        mvc = MockMvcBuilders.standaloneSetup(new ImprovementController(plans, progress, failures,
                        () -> java.util.Optional.of(new CurrentActorPort.Actor(actor, false))))
                .setControllerAdvice(new ApiProblemHandler(), new ImprovementProblemHandler())
                .addFilters(new CorrelationFilter(), new SessionAuthenticationFilter(sessions,
                        new AccessProblemWriter(new ObjectMapper().findAndRegisterModules())))
                .build();
    }

    private Instant stamp() { return Instant.now().truncatedTo(ChronoUnit.MILLIS); }

    private ImprovementAction head(ImprovementAction.State state, long revision) {
        return new ImprovementAction(action, process, new ImprovementAction.Data(plan, List.of(finding),
                "Acción de mejora sintética", "Descripción suficientemente amplia de la acción.",
                assignment, stamp().plus(30, ChronoUnit.DAYS), "Reducir la brecha en un 50%",
                "Revisión documental y muestreo aleatorio"), state, actor, stamp(), revision);
    }

    private String planRequest() {
        return "{\"title\":\"Plan de mejora del proceso sintético\",\"reportVersionId\":\"" + version + "\"}";
    }

    private String actionRequest() {
        return "{\"planId\":\"" + plan + "\",\"findingIds\":[\"" + finding + "\"],"
                + "\"title\":\"Acción de mejora sintética\","
                + "\"description\":\"Descripción suficientemente amplia de la acción de mejora.\","
                + "\"responsibleAssignmentId\":\"" + assignment + "\",\"dueAt\":\"2027-01-31T00:00:00Z\","
                + "\"target\":\"Reducir la brecha en un 50%\",\"verificationMethod\":\"Revisión documental\"}";
    }

    @Test
    void createsPlanWithStrongPrecondition() throws Exception {
        var planned = new ImprovementPlan(plan, process, version, "Plan de mejora del proceso sintético", actor, stamp());
        when(plans.create(eq(process), eq(0L), any(), any())).thenReturn(planned);
        mvc.perform(post("/processes/" + process + "/improvement-plans").cookie(session())
                        .header("X-CSRF-TOKEN", csrf).header("If-Match", "\"0\"")
                        .contentType(MediaType.APPLICATION_JSON).content(planRequest()))
                .andExpect(status().isCreated()).andExpect(header().string("ETag", "\"1\""))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.id").value(plan.toString()))
                .andExpect(jsonPath("$.data.reportVersionId").value(version.toString()));
        mvc.perform(post("/processes/" + process + "/improvement-plans").cookie(session())
                        .header("X-CSRF-TOKEN", csrf).contentType(MediaType.APPLICATION_JSON).content(planRequest()))
                .andExpect(status().isPreconditionRequired());
        mvc.perform(post("/processes/" + process + "/improvement-plans").cookie(session())
                        .header("If-Match", "\"0\"").contentType(MediaType.APPLICATION_JSON).content(planRequest()))
                .andExpect(status().isForbidden());
    }

    @Test
    void listsActionsWithAggregateEtagAndOverdue() throws Exception {
        var detail = new ImprovementPlanService.ActionDetail(head(ImprovementAction.State.APPROVED, 2L), true,
                List.of(), List.of());
        when(plans.list(process, 0, 20)).thenReturn(new ImprovementPlanService.ActionPage(List.of(detail), 3));
        mvc.perform(get("/processes/" + process + "/improvement-actions").cookie(session()))
                .andExpect(status().isOk()).andExpect(header().string("ETag", "\"3\""))
                .andExpect(jsonPath("$.items[0].state").value("APPROVED"))
                .andExpect(jsonPath("$.items[0].overdue").value(true))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void viewsActionWithRevisionEtagProgressAndDecisions() throws Exception {
        var recorded = new ActionProgress(UUID.randomUUID(), action, new ActionProgress.Data(
                "Avance sintético verificado con evidencia", "Brecha reducida al 25%", List.of(evidence), stamp()),
                actor, stamp());
        var provenance = new DecisionProvenance(actor, assignment, "Norma sintética aplicable",
                "Oficio sintético 001-2026", "Decisión sintética suficientemente fundamentada.", stamp());
        var decided = new ActionDecision(UUID.randomUUID(), action, ImprovementAction.Transition.START, provenance);
        when(plans.view(action)).thenReturn(new ImprovementPlanService.ActionDetail(
                head(ImprovementAction.State.IN_PROGRESS, 2L), false, List.of(recorded), List.of(decided)));
        mvc.perform(get("/improvement-actions/" + action).cookie(session()))
                .andExpect(status().isOk()).andExpect(header().string("ETag", "\"2\""))
                .andExpect(jsonPath("$.progress[0].data.achievedValue").value("Brecha reducida al 25%"))
                .andExpect(jsonPath("$.decisions[0].action").value("START"));
    }

    @Test
    void recordsProgressAndRequiresEvidence() throws Exception {
        var recorded = new ActionProgress(UUID.randomUUID(), action, new ActionProgress.Data(
                "Avance sintético verificado con evidencia", "Brecha reducida al 25%", List.of(evidence), stamp()),
                actor, stamp());
        when(progress.recordProgress(eq(action), eq(1L), any(), any())).thenReturn(recorded);
        var body = "{\"description\":\"Avance sintético verificado con evidencia\","
                + "\"achievedValue\":\"Brecha reducida al 25%\",\"evidenceVersionIds\":[\"" + evidence + "\"],"
                + "\"recordedAt\":\"2026-10-25T12:00:00Z\"}";
        mvc.perform(post("/improvement-actions/" + action + "/progress").cookie(session())
                        .header("X-CSRF-TOKEN", csrf).header("If-Match", "\"1\"")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(header().string("ETag", "\"2\""))
                .andExpect(jsonPath("$.data.evidenceVersionIds[0]").value(evidence.toString()));
        mvc.perform(post("/improvement-actions/" + action + "/progress").cookie(session())
                        .header("X-CSRF-TOKEN", csrf).header("If-Match", "\"1\"")
                        .contentType(MediaType.APPLICATION_JSON).content(body.replace("[\"" + evidence + "\"]", "[]")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void decidesWithRevisionGuardAndMapsConflicts() throws Exception {
        var provenance = new DecisionProvenance(actor, assignment, "Norma sintética aplicable",
                "Oficio sintético 001-2026", "Decisión sintética suficientemente fundamentada.", stamp());
        var decided = new ActionDecision(UUID.randomUUID(), action, ImprovementAction.Transition.VERIFY, provenance);
        when(progress.decide(eq(action), eq(2L), any(), any())).thenReturn(decided);
        var body = "{\"action\":\"VERIFY\",\"provenance\":{\"authorityAssignmentId\":\"" + assignment + "\","
                + "\"legalBasis\":\"Norma sintética aplicable\",\"documentReference\":\"Oficio sintético 001-2026\","
                + "\"reason\":\"Decisión sintética suficientemente fundamentada.\"}}";
        mvc.perform(post("/improvement-actions/" + action + "/decisions").cookie(session())
                        .header("X-CSRF-TOKEN", csrf).header("If-Match", "\"2\"")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(header().string("ETag", "\"3\""))
                .andExpect(jsonPath("$.action").value("VERIFY"));

        when(progress.decide(eq(action), eq(2L), any(), any())).thenThrow(new ImprovementProblemException(
                ImprovementProblemException.Kind.CONFLICT, "IMPROVEMENT_TRANSITION_INVALID",
                "La transición indicada no procede desde el estado actual de la acción."));
        mvc.perform(post("/improvement-actions/" + action + "/decisions").cookie(session())
                        .header("X-CSRF-TOKEN", csrf).header("If-Match", "\"2\"")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IMPROVEMENT_TRANSITION_INVALID"));

        when(progress.decide(eq(action), eq(2L), any(), any()))
                .thenThrow(new ConcurrencyFailureException("stale improvement action"));
        mvc.perform(post("/improvement-actions/" + action + "/decisions").cookie(session())
                        .header("X-CSRF-TOKEN", csrf).header("If-Match", "\"2\"")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isPreconditionFailed())
                .andExpect(jsonPath("$.code").value("STALE_WORKFLOW"));
        verify(failures, atLeast(2)).recordAfterRollback(argThat(event ->
                "IMPROVEMENT_DECISION_REJECTED".equals(event.action())));
    }

    @Test
    void revisesActionResettingStateAndExposingNewRevision() throws Exception {
        var revised = head(ImprovementAction.State.PROPOSED, 2L);
        when(plans.revise(eq(action), eq(1L), any(), any())).thenReturn(revised);
        when(plans.view(action)).thenReturn(new ImprovementPlanService.ActionDetail(revised, false, List.of(),
                List.of()));
        mvc.perform(patch("/improvement-actions/" + action).cookie(session())
                        .header("X-CSRF-TOKEN", csrf).header("If-Match", "\"1\"")
                        .contentType(MediaType.APPLICATION_JSON).content(actionRequest()))
                .andExpect(status().isOk()).andExpect(header().string("ETag", "\"2\""))
                .andExpect(jsonPath("$.state").value("PROPOSED"));
    }
}
