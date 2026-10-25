package ec.edu.unl.sec.governance.api;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.unl.sec.audit.application.FailureAuditPort;
import ec.edu.unl.sec.governance.application.FindingService;
import ec.edu.unl.sec.governance.application.ReportWorkflowService;
import ec.edu.unl.sec.governance.domain.ApprovalDecision;
import ec.edu.unl.sec.governance.domain.Consolidation;
import ec.edu.unl.sec.governance.domain.Finding;
import ec.edu.unl.sec.governance.domain.Finding.Type;
import ec.edu.unl.sec.governance.domain.ObservationReply;
import ec.edu.unl.sec.governance.domain.ReportObservation;
import ec.edu.unl.sec.governance.domain.ReportVersion;
import ec.edu.unl.sec.identityaccess.application.CurrentActorPort;
import ec.edu.unl.sec.identityaccess.application.SessionService;
import ec.edu.unl.sec.identityaccess.infrastructure.*;
import ec.edu.unl.sec.shared.api.ApiProblemHandler;
import ec.edu.unl.sec.shared.infrastructure.CorrelationFilter;
import ec.edu.unl.sec.shared.domain.DecisionProvenance;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ReportControllerTest {
    final UUID process = UUID.randomUUID(), career = UUID.randomUUID(), actor = UUID.randomUUID(),
            indicator = UUID.randomUUID(), consolidation = UUID.randomUUID(), version = UUID.randomUUID(),
            assignment = UUID.randomUUID(), observation = UUID.randomUUID();
    final FindingService findings = mock(FindingService.class);
    final ReportWorkflowService reports = mock(ReportWorkflowService.class);
    final FailureAuditPort failures = mock(FailureAuditPort.class);
    final String csrf = "synthetic-report-workflow-csrf";
    MockMvc mvc;
    Cookie session() { return new Cookie("SEC_SESSION", "session"); }

    @BeforeEach
    void setup() {
        var sessions = mock(SessionService.class);
        when(sessions.authenticateForRequest("session")).thenReturn(java.util.Optional.of(new SessionService.AuthenticatedSession(
                UUID.randomUUID(), actor, "Evaluador sintético", false, SessionService.sha256Hex(csrf))));
        mvc = MockMvcBuilders.standaloneSetup(new ReportController(findings, reports, failures,
                        () -> java.util.Optional.of(new CurrentActorPort.Actor(actor, false))))
                .setControllerAdvice(new ApiProblemHandler(), new GovernanceProblemHandler())
                .addFilters(new CorrelationFilter(),
                        new SessionAuthenticationFilter(sessions,
                                new AccessProblemWriter(new ObjectMapper().findAndRegisterModules())))
                .build();
    }

    @AfterEach
    void clear() { org.springframework.security.core.context.SecurityContextHolder.clearContext(); }

    private Instant stamp() { return Instant.now().truncatedTo(ChronoUnit.MILLIS); }

    private String sha(String content) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(content.getBytes(StandardCharsets.UTF_8)));
    }

    private Finding finding() {
        return new Finding(UUID.randomUUID(), process, new Finding.Data(Type.WEAKNESS,
                "Debilidad crítica sustentada", "Soportada en evidencia y evaluaciones registradas del proceso",
                List.of(indicator), List.of(), List.of()), actor, stamp());
    }

    private ReportVersion emitted() throws Exception {
        var content = "Contenido preliminar sustentado de la autoevaluación.";
        return new ReportVersion(version, process, new ReportVersion.Data(ReportVersion.Kind.PRELIMINARY,
                consolidation, "Plantilla institucional informe P14", true, content, null), actor, stamp(),
                null, 1L, 1L, sha(content));
    }

    @Test
    void listsFindingsWithStrongAggregateRevision() throws Exception {
        var f = finding();
        when(findings.findings(process, 0L, 20L)).thenReturn(new FindingService.FindingPage(List.of(f), 3));
        mvc.perform(get("/processes/" + process + "/findings").cookie(session()))
                .andExpect(status().isOk()).andExpect(header().string("ETag", "\"3\""))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.items[0].data.type").value("WEAKNESS"))
                .andExpect(jsonPath("$.totalElements").value(3));
        verify(findings).findings(process, 0L, 20L);
    }

    @Test
    void registersFindingWithStrongPreconditionAndAudit() throws Exception {
        var f = finding();
        when(findings.register(eq(process), any(), any())).thenReturn(f);
        mvc.perform(post("/processes/" + process + "/findings").cookie(session()).header("X-CSRF-TOKEN", csrf)
                        .header("If-Match", "\"0\"").contentType(MediaType.APPLICATION_JSON)
                        .content(findRequest()))
                .andExpect(status().isCreated()).andExpect(header().string("ETag", "\"1\""))
                .andExpect(jsonPath("$.data.title").value("Debilidad crítica sustentada"));
        mvc.perform(post("/processes/" + process + "/findings").cookie(session()).header("X-CSRF-TOKEN", csrf)
                        .contentType(MediaType.APPLICATION_JSON).content(findRequest()))
                .andExpect(status().isPreconditionRequired());
        mvc.perform(post("/processes/" + process + "/findings").cookie(session()).header("If-Match", "\"0\"")
                        .contentType(MediaType.APPLICATION_JSON).content(findRequest()))
                .andExpect(status().isForbidden());
    }

    @Test
    void consolidatesFindingWithRevisionGuardAndStaleAudit() throws Exception {
        var c = new Consolidation(UUID.randomUUID(), process,
                new Consolidation.Data(List.of(UUID.randomUUID()), null,
                        "Consolidación registrada sobre resultados parciales validados."), actor, stamp(), null, 3);
        when(findings.consolidate(eq(process), eq(2L), any(), any())).thenReturn(c);
        mvc.perform(post("/processes/" + process + "/consolidations").cookie(session())
                        .header("X-CSRF-TOKEN", csrf).header("If-Match", "\"2\"")
                        .contentType(MediaType.APPLICATION_JSON).content(consolidationRequest()))
                .andExpect(status().isCreated()).andExpect(header().string("ETag", "\"3\""));
        when(findings.consolidate(any(), anyLong(), any(), any())).thenThrow(new ConcurrencyFailureException("stale"));
        mvc.perform(post("/processes/" + process + "/consolidations").cookie(session())
                        .header("X-CSRF-TOKEN", csrf).header("If-Match", "\"2\"")
                        .contentType(MediaType.APPLICATION_JSON).content(consolidationRequest()))
                .andExpect(status().isPreconditionFailed()).andExpect(jsonPath("$.code").value("STALE_WORKFLOW"))
                .andExpect(jsonPath("$.correlationId").isNotEmpty());
        verify(failures).recordAfterRollback(argThat(event -> event.action().equals("REPORT_CONSOLIDATION_REJECTED")
                && event.outcome().equals("REJECTED") && event.reasonCode().equals("STALE_WORKFLOW")));
    }

    @Test
    void listsReportVersionsWithProcessingRevision() throws Exception {
        var v = emitted();
        when(reports.listVersions(process, 0L, 20L)).thenReturn(
                new ReportWorkflowService.VersionPage(List.of(v), 1, 5));
        when(reports.processingRevision(version)).thenReturn(2L);
        mvc.perform(get("/processes/" + process + "/report-versions").cookie(session()))
                .andExpect(status().isOk()).andExpect(header().string("ETag", "\"5\""))
                .andExpect(jsonPath("$.items[0].data.kind").value("PRELIMINARY"))
                .andExpect(jsonPath("$.items[0].revision").value(2));
    }

    @Test
    void emitsVersionWithProcessingRevisionAndState() throws Exception {
        var v = emitted();
        when(reports.create(eq(process), eq(0L), any(), any())).thenReturn(v);
        when(reports.viewVersion(process, version)).thenReturn(new ReportWorkflowService.ReportVersionDetail(
                v, 0L, "PRELIMINARY", List.of(), List.of(), List.of(), List.of(),
                ReportWorkflowService.DISCLAIMER));
        mvc.perform(post("/processes/" + process + "/report-versions").cookie(session())
                        .header("X-CSRF-TOKEN", csrf).header("If-Match", "\"0\"")
                        .contentType(MediaType.APPLICATION_JSON).content(issueRequest()))
                .andExpect(status().isCreated()).andExpect(header().string("ETag", "\"0\""))
                .andExpect(jsonPath("$.state").value("PRELIMINARY"))
                .andExpect(jsonPath("$.data.kind").value("PRELIMINARY"));
        mvc.perform(post("/processes/" + process + "/report-versions").cookie(session())
                        .header("X-CSRF-TOKEN", csrf).header("If-Match", "\"0\"")
                        .contentType(MediaType.APPLICATION_JSON).content(issueRequest().replace("}","\",extra\":1}")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void showsVersionDetailWithProcessingRevision() throws Exception {
        var v = emitted();
        when(reports.viewVersion(version)).thenReturn(new ReportWorkflowService.ReportVersionDetail(
                v, 4L, "UNDER_OBSERVATION", List.of(), List.of(), List.of(), List.of(),
                ReportWorkflowService.DISCLAIMER));
        mvc.perform(get("/report-versions/" + version).cookie(session()))
                .andExpect(status().isOk()).andExpect(header().string("ETag", "\"4\""))
                .andExpect(jsonPath("$.state").value("UNDER_OBSERVATION"));
    }

    @Test
    void recordsObservationAndMapsStaleRejection() throws Exception {
        var result = new ReportObservation(observation, version, "Capítulo 3 de matrícula",
                "La tasa observada sigue una desviación mayor al 3% respecto del parámetro.", actor, stamp());
        when(reports.observe(eq(version), eq(0L), any(), any())).thenReturn(result);
        mvc.perform(post("/report-versions/" + version + "/observations").cookie(session())
                        .header("X-CSRF-TOKEN", csrf).header("If-Match", "\"0\"")
                        .contentType(MediaType.APPLICATION_JSON).content(observationRequest()))
                .andExpect(status().isCreated()).andExpect(header().string("ETag", "\"1\""))
                .andExpect(jsonPath("$.revision").value(1))
                .andExpect(jsonPath("$.data.sectionReference").value("Capítulo 3 de matrícula"));
        when(reports.observe(any(), anyLong(), any(), any())).thenThrow(new ConcurrencyFailureException("stale"));
        mvc.perform(post("/report-versions/" + version + "/observations").cookie(session())
                        .header("X-CSRF-TOKEN", csrf).header("If-Match", "\"0\"")
                        .contentType(MediaType.APPLICATION_JSON).content(observationRequest()))
                .andExpect(status().isPreconditionFailed()).andExpect(jsonPath("$.code").value("STALE_WORKFLOW"));
        verify(failures).recordAfterRollback(argThat(event -> event.action().equals("REPORT_OBSERVATION_REJECTED")
                && event.outcome().equals("REJECTED") && event.reasonCode().equals("STALE_WORKFLOW")));
    }

    @Test
    void repliesObservationWithCorrectionLink() throws Exception {
        var result = new ObservationReply(UUID.randomUUID(), observation,
                "La corrección registra la explicación del parámetro observado y la fuente.", version, actor, stamp());
        when(reports.reply(eq(observation), eq(1L), any(), any())).thenReturn(result);
        mvc.perform(post("/report-observations/" + observation + "/responses").cookie(session())
                        .header("X-CSRF-TOKEN", csrf).header("If-Match", "\"1\"")
                        .contentType(MediaType.APPLICATION_JSON).content(replyRequest()))
                .andExpect(status().isCreated()).andExpect(header().string("ETag", "\"2\""))
                .andExpect(jsonPath("$.data.correctedVersionId").value(version.toString()));
    }

    @Test
    void decidesApprovalWithStrongPrecondition() throws Exception {
        var result = new ApprovalDecision(UUID.randomUUID(), version, ApprovalDecision.Decision.APPROVED,
                new DecisionProvenance(actor, assignment, "Ley 132-18 y reglamento institucional UNL",
                        "Acta de cierre 2026-01", "Sustentado en el ciclo completo del proceso evaluado", stamp()));
        when(reports.decide(eq(version), eq(2L), any(), any())).thenReturn(result);
        mvc.perform(post("/report-versions/" + version + "/approval-decisions").cookie(session())
                        .header("X-CSRF-TOKEN", csrf).header("If-Match", "\"2\"")
                        .contentType(MediaType.APPLICATION_JSON).content(approvalRequest()))
                .andExpect(status().isCreated()).andExpect(header().string("ETag", "\"3\""))
                .andExpect(jsonPath("$.data.decision").value("APPROVED"));
    }

    private String findRequest() {
        return "{\"type\":\"WEAKNESS\",\"title\":\"Debilidad crítica sustentada\","
                + "\"description\":\"Soportada en evidencia y evaluaciones registradas del proceso\","
                + "\"indicatorIds\":[\"" + indicator + "\"],\"evaluationIds\":[],\"evidenceVersionIds\":[]}";
    }

    private String consolidationRequest() {
        return "{\"findingIds\":[\"" + UUID.randomUUID() + "\"],\"aggregationRuleId\":null,"
                + "\"reason\":\"Consolidación registrada sobre resultados parciales validados.\"}";
    }

    private String issueRequest() {
        return "{\"kind\":\"PRELIMINARY\",\"consolidationId\":\"" + consolidation + "\","
                + "\"templateReference\":\"Plantilla institucional informe P14\",\"templateValidated\":true,"
                + "\"content\":\"Contenido preliminar sustentado de la autoevaluación.\",\"predecessorVersionId\":null}";
    }

    private String observationRequest() {
        return "{\"text\":\"La tasa observada sigue una desviación mayor al 3% respecto del parámetro.\","
                + "\"sectionReference\":\"Capítulo 3 de matrícula\"}";
    }

    private String replyRequest() {
        return "{\"response\":\"La corrección registra la explicación del parámetro observado y la fuente.\","
                + "\"correctedVersionId\":\"" + version + "\"}";
    }

    private String approvalRequest() {
        return "{\"decision\":\"APPROVED\",\"provenance\":{\"authorityAssignmentId\":\"" + assignment + "\","
                + "\"legalBasis\":\"Ley 132-18 y reglamento institucional UNL\","
                + "\"documentReference\":\"Acta de cierre 2026-01\",\"reason\":\"Sustentado en el ciclo completo del proceso evaluado\"}}";
    }
}
