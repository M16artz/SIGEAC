package ec.edu.unl.sec.improvement.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.unl.sec.identityaccess.application.AuthorizationPort;
import ec.edu.unl.sec.improvement.application.ActionProgressService;
import ec.edu.unl.sec.improvement.application.ImprovementPlanService;
import ec.edu.unl.sec.improvement.application.ImprovementProblemException;
import ec.edu.unl.sec.improvement.domain.ImprovementAction;
import ec.edu.unl.sec.support.D8ImprovementData;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@ActiveProfiles({"sqlite-test", "d8-improvement-test"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ImprovementPersistenceTest {
    private static final String DATABASE = "sqlite-improvement-" + UUID.randomUUID();
    @org.junit.jupiter.api.io.TempDir static java.nio.file.Path fileRoot;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:file:" + DATABASE + "?mode=memory&cache=shared");
        registry.add("sec.files.root", () -> fileRoot.toString());
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @Autowired ImprovementPlanService plans;
    @Autowired ActionProgressService progress;
    @Autowired AuthorizationPort authorization;

    @Test
    void proposesApprovesExecutesAndVerifiesWithAppendOnlyHistory() throws Exception {
        var evidence = UUID.randomUUID();
        asRole("coordinador");
        var plan = plans.create(D8ImprovementData.PROCESS, planTotal(), new ImprovementPlanService.PlanCommand(
                "Plan de mejora del proceso sintético", D8ImprovementData.REPORT_VERSION), UUID.randomUUID());
        var action = plans.createAction(D8ImprovementData.PROCESS, command(plan.id(),
                Instant.now().minusSeconds(3600)), UUID.randomUUID());
        assertThat(action.state()).isEqualTo(ImprovementAction.State.PROPOSED);
        var listed = plans.list(D8ImprovementData.PROCESS, 0, 100);
        assertThat(listed.totalElements()).isGreaterThanOrEqualTo(1);
        assertThat(plans.view(action.id()).overdue()).isTrue();

        assertProblem(() -> progress.decide(action.id(), 1L, decision(ImprovementAction.Transition.START,
                D8ImprovementData.RESPONSIBLE_ASSIGNMENT), UUID.randomUUID()), "IMPROVEMENT_CAPABILITY_REQUIRED");
        var approved = progress.decide(action.id(), 1L, decision(ImprovementAction.Transition.APPROVE,
                D8ImprovementData.COORDINATOR_ASSIGNMENT), UUID.randomUUID());
        assertThat(approved.action()).isEqualTo(ImprovementAction.Transition.APPROVE);


        asRole("revisor");
        assertProblem(() -> progress.decide(action.id(), 2L, decision(ImprovementAction.Transition.VERIFY,
                D8ImprovementData.REVIEWER_ASSIGNMENT), UUID.randomUUID()), "IMPROVEMENT_TRANSITION_INVALID");

        asRole("responsable");
        progress.decide(action.id(), 2L, decision(ImprovementAction.Transition.START,
                D8ImprovementData.RESPONSIBLE_ASSIGNMENT), UUID.randomUUID());
        var recorded = progress.recordProgress(action.id(), 3L, new ActionProgressService.ProgressCommand(
                "Avance sintético verificado con soporte documental", "Brecha reducida al 40%", List.of(evidence),
                Instant.now()), UUID.randomUUID());
        assertThat(recorded.actionId()).isEqualTo(action.id());
        progress.decide(action.id(), 4L, decision(ImprovementAction.Transition.SUBMIT_VERIFICATION,
                D8ImprovementData.RESPONSIBLE_ASSIGNMENT), UUID.randomUUID());

        asRole("revisor");
        progress.decide(action.id(), 5L, decision(ImprovementAction.Transition.VERIFY,
                D8ImprovementData.REVIEWER_ASSIGNMENT), UUID.randomUUID());
        var detail = plans.view(action.id());
        assertThat(detail.action().state()).isEqualTo(ImprovementAction.State.VERIFIED);
        assertThat(detail.overdue()).isFalse();
        assertThat(detail.progress()).containsExactly(recorded);
        assertThat(detail.decisions()).hasSize(4);

        asRole("responsable");
        assertProblem(() -> progress.recordProgress(action.id(), 6L, new ActionProgressService.ProgressCommand(
                "Avance posterior que no debe admitirse", "Intento tardío", List.of(evidence), Instant.now()),
                UUID.randomUUID()), "IMPROVEMENT_ACTION_CLOSED");
        asRole("coordinador");
        assertProblem(() -> plans.revise(action.id(), 6L, command(plan.id(), Instant.now()), UUID.randomUUID()),
                "IMPROVEMENT_ACTION_CLOSED");
        assertThat(jdbc.queryForObject("select count(*) from improvement_action_revision where action_id=?",
                Long.class, action.id())).isEqualTo(6L);
        assertThat(jdbc.queryForObject("select count(*) from audit_event where object_id=? and outcome='SUCCESS'",
                Long.class, bytes(action.id()))).isEqualTo(6L);
        assertThat(json.readValue(jdbc.queryForObject("select payload_json from improvement_progress where id=?",
                String.class, recorded.id()), ec.edu.unl.sec.improvement.domain.ActionProgress.class))
                .isEqualTo(recorded);
    }

    @Test
    void revisionKeepsThePreviousChainAndReturnsToProposed() {
        asRole("coordinador");
        var plan = plans.create(D8ImprovementData.PROCESS, planTotal(), new ImprovementPlanService.PlanCommand(
                "Plan de mejora revisado", D8ImprovementData.REPORT_VERSION), UUID.randomUUID());
        var first = plans.createAction(D8ImprovementData.PROCESS, command(plan.id(), Instant.now().plusSeconds(86400)),
                UUID.randomUUID());
        progress.decide(first.id(), 1L, decision(ImprovementAction.Transition.APPROVE,
                D8ImprovementData.COORDINATOR_ASSIGNMENT), UUID.randomUUID());
        var newDue = Instant.now().plusSeconds(172800);
        var revised = plans.revise(first.id(), 2L, command(plan.id(), newDue), UUID.randomUUID());
        assertThat(revised.revision()).isEqualTo(3L);
        assertThat(revised.state()).isEqualTo(ImprovementAction.State.PROPOSED);
        assertThat(jdbc.queryForList("select revision from improvement_action_revision where action_id=? order by revision",
                Long.class, first.id())).containsExactly(1L, 2L, 3L);
        assertThat(plans.view(first.id()).action().data().dueAt())
                .isEqualTo(newDue.truncatedTo(java.time.temporal.ChronoUnit.MILLIS));
    }

    private static byte[] bytes(UUID id) {
        return java.nio.ByteBuffer.allocate(16).putLong(id.getMostSignificantBits())
                .putLong(id.getLeastSignificantBits()).array();
    }

    private long planTotal() {
        var total = jdbc.queryForObject("select count(*) from improvement_plan", Long.class);
        return total == null ? 0L : total;
    }

    private ImprovementPlanService.ActionCommand command(UUID planId, Instant dueAt) {
        return new ImprovementPlanService.ActionCommand(planId, List.of(D8ImprovementData.FINDING),
                "Acción de mejora sintética", "Descripción suficientemente amplia de la acción de mejora.",
                D8ImprovementData.RESPONSIBLE_ASSIGNMENT, dueAt,
                "Reducir la brecha en un 50% verificable", "Revisión documental y muestreo aleatorio");
    }

    private ActionProgressService.DecisionCommand decision(ImprovementAction.Transition transition, UUID assignment) {
        return new ActionProgressService.DecisionCommand(transition, new ActionProgressService.Provenance(assignment,
                "Norma sintética aplicable", "Oficio sintético 001-2026",
                "Decisión sintética suficientemente fundamentada para la prueba."));
    }

    private void assertProblem(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, String code) {
        assertThatThrownBy(call).isInstanceOf(ImprovementProblemException.class).extracting("code").isEqualTo(code);
    }

    private void asRole(String login) {
        var id = UUID.nameUUIDFromBytes(("sec-unl:" + login).getBytes(StandardCharsets.UTF_8));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(id, null,
                List.of(new SimpleGrantedAuthority("ROLE_HUMAN"))));
    }
}
