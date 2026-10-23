package ec.edu.unl.sec.selfassessment.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import ec.edu.unl.sec.evaluationmodel.application.ModelCatalogPort;
import ec.edu.unl.sec.identityaccess.application.*;
import ec.edu.unl.sec.identityaccess.domain.OperationCode;
import ec.edu.unl.sec.selfassessment.application.*;
import ec.edu.unl.sec.selfassessment.domain.*;
import ec.edu.unl.sec.selfassessment.infrastructure.ProcessAssignmentCatalogAdapter;
import ec.edu.unl.sec.selfassessment.support.InMemorySelfAssessmentRepository;
import ec.edu.unl.sec.shared.api.ApiProblemHandler;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ProcessGovernanceControllerTest {
    private final UUID planner = UUID.randomUUID(), member = UUID.randomUUID(), stranger = UUID.randomUUID();
    private final UUID indicator = UUID.randomUUID();
    private final Instant now = Instant.now(), from = now.minusSeconds(60), until = now.plusSeconds(3600);
    private InMemorySelfAssessmentRepository repository;
    private CurrentActorPort actors;
    private AuthorizationPort authorization;
    private FunctionalAssignmentPolicyPort policy;
    private SelfAssessmentProcess process;
    private ProcessAssignment assignment;
    private MockMvc mvc;

    @BeforeEach void setup() {
        repository = new InMemorySelfAssessmentRepository();
        actors = mock(CurrentActorPort.class);
        as(planner);
        authorization = mock(AuthorizationPort.class);
        when(authorization.isAllowed(any(), any(), any())).thenAnswer(call -> {
            UUID user = call.getArgument(0); OperationCode operation = call.getArgument(1);
            return planner.equals(user) || member.equals(user) && OperationCode.EVIDENCE_COLLECT.equals(operation);
        });
        policy = mock(FunctionalAssignmentPolicyPort.class);
        when(policy.evaluateMembership(any(), any(), any(), any(), any())).thenReturn(FunctionalAssignmentPolicyPort.Decision.allow());
        when(policy.evaluate(any(), any(), any(), any(), any())).thenReturn(FunctionalAssignmentPolicyPort.Decision.allow());
        process = new SelfAssessmentProcess(null, UUID.randomUUID(), "P", "Proceso", LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(2), UUID.randomUUID(), 1, ProcessState.ACTIVE, 1, planner, now,
                List.of(new SelfAssessmentProcess.IndicatorSnapshot(indicator, "I", "Indicador")));
        repository.save(process, -1);
        assignment = new ProcessAssignment(null, process.id(), indicator, member, ProcessAssignment.Function.RESPONSIBLE,
                from, until, planner, "Designacion", null, 0);
        repository.saveAssignment(assignment);
        var participants = new ProcessAssignmentCatalogAdapter(repository, policy);
        var audit = mock(SelfAssessmentAuditService.class);
        var planning = new ProcessPlanningService(repository, mock(ModelCatalogPort.class), actors, authorization,
                mock(InstitutionalAuthorityPort.class), policy, audit, participants);
        var assignments = new ProcessAssignmentService(repository, actors, authorization, policy, audit, participants);
        mvc = MockMvcBuilders.standaloneSetup(new ProcessGovernanceController(planning),
                new ProcessAssignmentController(assignments, planning))
                .setControllerAdvice(new ProcessExceptionHandler(), new ApiProblemHandler()).build();
    }

    @Test void createsTeamsAndMembershipsWithProcessEtagsAndRejectsStaleWrites() throws Exception {
        String teams = "/processes/" + process.id() + "/teams";
        String body = """
                {"name":"Equipo","processRole":"COLLECTION","validFrom":"%s","validUntil":"%s"}
                """.formatted(from, until);
        mvc.perform(post(teams).contentType("application/json").content(body))
                .andExpect(status().isPreconditionRequired());
        mvc.perform(post(teams).header("If-Match", "\"1\"").contentType("application/json").content(body))
                .andExpect(status().isCreated()).andExpect(header().string("ETag", "\"2\""));
        var team = repository.teams(process.id()).getFirst();
        mvc.perform(get("/teams/" + team.id())).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Equipo"));
        mvc.perform(post(teams).header("If-Match", "\"1\"").contentType("application/json").content(body))
                .andExpect(status().isPreconditionFailed()).andExpect(jsonPath("$.code").value("STALE_PROCESS"));
        String memberships = "/teams/" + team.id() + "/memberships";
        String input = """
                {"userId":"%s","roleAssignmentId":"%s","validFrom":"%s","validUntil":"%s"}
                """.formatted(member, UUID.randomUUID(), from, until);
        mvc.perform(post(memberships).header("If-Match", "\"2\"").contentType("application/json").content(input))
                .andExpect(status().isCreated()).andExpect(header().string("ETag", "\"3\""));
        mvc.perform(get(memberships)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        assertThat(repository.teams(process.id())).hasSize(1);
    }

    @Test void createsAndUpdatesActivitiesWithRevisionChecks() throws Exception {
        String path = "/processes/" + process.id() + "/activities";
        mvc.perform(post(path).header("If-Match", "\"1\"").contentType("application/json").content(activityInput("Primera", "[]")))
                .andExpect(status().isCreated()).andExpect(header().string("ETag", "\"2\""));
        var activity = repository.activities(process.id()).getFirst();
        String detail = "/activities/" + activity.id();
        mvc.perform(patch(detail).header("If-Match", "\"2\"").contentType("application/json").content(activityInput("Corregida", "[]")))
                .andExpect(status().isOk()).andExpect(header().string("ETag", "\"3\""))
                .andExpect(jsonPath("$.revision").value(1));
        mvc.perform(patch(detail).header("If-Match", "\"2\"").contentType("application/json").content(activityInput("Obsoleta", "[]")))
                .andExpect(status().isPreconditionFailed());
        assertThat(repository.findActivity(activity.id()).orElseThrow().title()).isEqualTo("Corregida");
    }

    @Test void participantsSeeOnlyTheirActivitiesAndCannotMutateThePlan() throws Exception {
        var own = activity(assignment, "Propia");
        var foreignAssignment = new ProcessAssignment(null, process.id(), indicator, stranger,
                assignment.function(), from, until, planner, "Otro responsable", null, 0);
        repository.saveAssignment(foreignAssignment);
        var foreign = activity(foreignAssignment, "Ajena");
        as(member);
        mvc.perform(get("/processes/" + process.id() + "/activities")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(own.id().toString()));
        mvc.perform(get("/activities/" + own.id())).andExpect(status().isOk());
        mvc.perform(get("/activities/" + foreign.id())).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("PROCESS_NOT_FOUND"));
        mvc.perform(get("/activities/" + UUID.randomUUID())).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("PROCESS_NOT_FOUND"));
        mvc.perform(patch("/activities/" + own.id()).header("If-Match", "\"1\"")
                .contentType("application/json").content(activityInput("No permitida", "[]"))).andExpect(status().isNotFound());
        assertThat(repository.findActivity(own.id())).contains(own);
        assertThat(repository.find(process.id()).orElseThrow().revision()).isEqualTo(1);
        as(stranger);
        mvc.perform(get("/processes/" + process.id() + "/activities")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROCESS_NOT_FOUND"));
        mvc.perform(get("/processes/" + UUID.randomUUID() + "/activities")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROCESS_NOT_FOUND"));
    }

    @Test void teamActivityAccessRequiresCurrentMembershipAndIndependentPermission() throws Exception {
        var team = new ProcessTeam(null, process.id(), "Equipo", "COLLECTION", from, until, planner, from, 0);
        repository.saveTeam(team);
        repository.saveMembership(new TeamMembership(null, team.id(), member, UUID.randomUUID(), from, until, planner, from));
        var teamAssignment = new ProcessAssignment(null, process.id(), indicator, null, team.id(),
                assignment.function(), from, until, planner, "Equipo responsable", null, 0);
        repository.saveAssignment(teamAssignment);
        var activity = activity(teamAssignment, "De equipo");
        String path = "/activities/" + activity.id();
        as(member);
        mvc.perform(get(path)).andExpect(status().isOk());
        when(policy.evaluateMembership(any(), any(), any(), any(), any()))
                .thenReturn(FunctionalAssignmentPolicyPort.Decision.deny("MEMBERSHIP_ROLE_INVALID"));
        mvc.perform(get(path)).andExpect(status().isNotFound());
        when(policy.evaluateMembership(any(), any(), any(), any(), any())).thenReturn(FunctionalAssignmentPolicyPort.Decision.allow());
        when(authorization.isAllowed(any(), any(), any())).thenReturn(false);
        mvc.perform(get(path)).andExpect(status().isNotFound());
    }

    @Test void expiredAssignmentsAndTechnicalActorsCannotReadActivities() throws Exception {
        var expired = new ProcessAssignment(null, process.id(), indicator, member, assignment.function(),
                from.minusSeconds(60), from, planner, "Vencida", null, 0);
        repository.saveAssignment(expired);
        var activity = activity(expired, "Vencida");
        as(member);
        mvc.perform(get("/activities/" + activity.id())).andExpect(status().isNotFound());
        when(actors.currentActor()).thenReturn(Optional.of(new CurrentActorPort.Actor(planner, true)));
        mvc.perform(get("/processes/" + process.id() + "/activities")).andExpect(status().isNotFound());
        when(actors.currentActor()).thenReturn(Optional.empty());
        mvc.perform(get("/activities/" + activity.id())).andExpect(status().isUnauthorized());
    }

    @Test void rejectsNullDependenciesAndConflictingIdentityAliasesBeforeMutation() throws Exception {
        mvc.perform(post("/processes/" + process.id() + "/activities").header("If-Match", "\"1\"")
                .contentType("application/json").content(activityInput("Invalida", "[null]")))
                .andExpect(status().isBadRequest());
        String body = """
                {"indicatorId":"%s","identityId":"%s","userId":"%s","function":"RESPONSIBLE",
                 "validFrom":"%s","validTo":"%s","reason":"Designacion"}
                """.formatted(indicator, member, stranger, from, until);
        mvc.perform(post("/processes/" + process.id() + "/assignments").header("If-Match", "\"1\"")
                .contentType("application/json").content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("ASSIGNMENT_IDENTITY_CONFLICT"));
        assertThat(repository.activities(process.id())).isEmpty();
        assertThat(repository.assignments(process.id())).hasSize(1);
        assertThat(repository.find(process.id()).orElseThrow().revision()).isEqualTo(1);
    }

    private void as(UUID identity) { when(actors.currentActor()).thenReturn(Optional.of(new CurrentActorPort.Actor(identity, false))); }
    private String activityInput(String title, String dependencies) {
        return """
                {"title":"%s","indicatorId":"%s","assignmentId":"%s","dueAt":"%s","dependencyIds":%s}
                """.formatted(title, indicator, assignment.id(), now.plusSeconds(60), dependencies);
    }
    private ProcessActivity activity(ProcessAssignment assigned, String title) {
        var activity = new ProcessActivity(null, process.id(), indicator, null, assigned.id(), title,
                now.plusSeconds(60), ProcessActivity.Status.PLANNED, List.of(), null, null, null, planner, now, 0);
        repository.saveActivity(activity, -1);
        return activity;
    }
}
