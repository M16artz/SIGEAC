package ec.edu.unl.sec.selfassessment.infrastructure;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.unl.sec.evaluationmodel.application.*;
import ec.edu.unl.sec.evaluationmodel.domain.*;
import ec.edu.unl.sec.evaluationmodel.infrastructure.JdbcEvaluationModelRepository;
import ec.edu.unl.sec.evaluationmodel.infrastructure.JpaNormativeReferenceRepository;
import ec.edu.unl.sec.evaluationmodel.infrastructure.SqliteModelGuards;
import ec.edu.unl.sec.evaluationmodel.support.ConfiguredModelFixtures;
import ec.edu.unl.sec.identityaccess.application.*;
import ec.edu.unl.sec.identityaccess.infrastructure.SyntheticAccessData;
import ec.edu.unl.sec.selfassessment.application.*;
import ec.edu.unl.sec.selfassessment.domain.*;
import ec.edu.unl.sec.shared.infrastructure.JdbcValueSupport;
import ec.edu.unl.sec.support.PostgresTestSupport;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

class ProcessGovernancePersistenceTest {
    @TempDir Path directory;
    private static final Instant FROM = Instant.parse("2026-10-22T00:00:00Z");
    private static final Instant UNTIL = Instant.parse("2026-10-23T00:00:00Z");
    private static final UUID ACTOR = identity("coordinador"), MEMBER = identity("responsable");
    private static final UUID INDICATOR = SyntheticPilotData.INDICATOR_A_ID;

    static Stream<String> databases() {
        return Boolean.parseBoolean(System.getenv().getOrDefault("SEC_RUN_GOVERNANCE_DATABASE_TESTS", "false"))
                ? Stream.of("sqlite", "postgres") : Stream.of("sqlite");
    }

    @ParameterizedTest @MethodSource("databases")
    void normativeLinksUseTheExactConfiguredSnapshotIncludingSuccessors(String database) throws Exception {
        try (var f = new Fixture(database)) {
            var models = new JdbcEvaluationModelRepository(f.jdbc, f.json);
            var references = new JpaNormativeReferenceRepository(f.jdbc);
            var fixture = ConfiguredModelFixtures.first();
            UUID modelId = UUID.randomUUID();
            models.saveModel(new EvaluationModel(modelId, "D3 applicability", "Synthetic", f.process.createdBy(), FROM));
            var draft = new ModelVersion(null, modelId, 1, ModelVersionStatus.DRAFT, fixture.source(),
                    f.process.createdBy(), null, null, 0, FROM, null, fixture.nodes(), null, null, fixture.configuration());
            models.saveVersion(draft, -1);
            var published = draft.publish(ACTOR, FROM);
            models.saveVersion(published, 0);
            var reference = new NormativeReference(null, "Synthetic", "Test issuer", "2026", "fixture://d3",
                    "a".repeat(64), "1", "Synthetic disposition", NormativeReference.VerificationStatus.VERIFIED, ACTOR, FROM);
            references.saveReference(reference);
            UUID indicatorId = fixture.configuration().indicators().getFirst().nodeId();
            var link = new NormativeReferenceRepository.IndicatorLink(indicatorId, reference.id());
            references.replaceIndicatorLinks(published.id(), List.of(link));
            assertThat(references.indicatorLinks(published.id())).containsExactly(link);
            var successor = published.successor(2, f.process.createdBy(), FROM);
            models.saveVersion(successor, -1);
            references.replaceIndicatorLinks(successor.id(), List.of(link));
            assertThat(references.indicatorLinks(successor.id())).containsExactly(link);
            assertThatThrownBy(() -> references.replaceIndicatorLinks(published.id(), List.of(
                    new NormativeReferenceRepository.IndicatorLink(UUID.randomUUID(), reference.id()))))
                    .isInstanceOf(DataAccessException.class);
            assertThat(references.indicatorLinks(published.id())).containsExactly(link);
        }
    }

    @ParameterizedTest @MethodSource("databases")
    void reloadsTeamsMembersAssignmentsActivitiesAndPreviousRevisions(String database) throws Exception {
        try (var f = new Fixture(database)) {
            var team = f.plan.createTeam(f.process.id(), 1, "Equipo", "COLLECTION", FROM, UNTIL, UUID.randomUUID());
            UUID role = f.jdbc.query("select id from functional_assignment where identity_id=?",
                    (row, index) -> JdbcValueSupport.uuid(row, 1), MEMBER).getFirst();
            var member = f.plan.addMember(team.value().id(), team.processRevision(), MEMBER, role, FROM, UNTIL, UUID.randomUUID());
            var assigned = f.plan.assignParticipant(f.process.id(), member.processRevision(), INDICATOR, null,
                    team.value().id(), ProcessAssignment.Function.RESPONSIBLE, FROM, UNTIL, "Asignacion sintetica", UUID.randomUUID());
            var first = f.plan.createActivity(f.process.id(), assigned.processRevision(),
                    input(assigned.value().id(), "Recolectar", List.of()), UUID.randomUUID());
            var second = f.plan.createActivity(f.process.id(), first.processRevision(),
                    input(assigned.value().id(), "Revisar", List.of(first.value().id())), UUID.randomUUID());
            var changed = f.plan.updateActivity(second.value().id(), second.processRevision(),
                    input(assigned.value().id(), "Revisar corregida", List.of()), UUID.randomUUID());

            // A separate connection verifies durable SQL state, without the original repository or service instances.
            try (var reopened = f.connection()) {
                var jdbc = new JdbcTemplate(reopened);
                var repository = new JdbcSelfAssessmentRepository(jdbc, f.json);
                assertThat(repository.findTeam(team.value().id()).orElseThrow().name()).isEqualTo("Equipo");
                assertThat(repository.memberships(team.value().id())).singleElement().satisfies(value -> {
                    assertThat(value.userId()).isEqualTo(MEMBER);
                    assertThat(value.roleAssignmentId()).isEqualTo(role);
                    assertThat(value.validFrom()).isEqualTo(FROM);
                });
                assertThat(repository.assignments(f.process.id())).contains(assigned.value());
                assertThat(repository.findActivity(second.value().id())).contains(changed.value());
                String previous = jdbc.queryForObject("select snapshot_json from process_activity_revision where activity_id=?",
                        String.class, second.value().id());
                assertThat(f.json.readValue(previous, ProcessActivity.class)).isEqualTo(second.value());
                assertThat(jdbc.queryForObject("select count(*) from process_activity_dependency where activity_id=?",
                        Integer.class, second.value().id())).isZero();
                assertThat(repository.find(f.process.id()).orElseThrow().revision()).isEqualTo(changed.processRevision());
            }
        }
    }

    @ParameterizedTest @MethodSource("databases")
    void dependencyWriteFailureRollsBackActivityHistoryAndPreviousDependencies(String database) throws Exception {
        try (var f = new Fixture(database)) {
            var first = f.plan.createActivity(f.process.id(), 1, input(f.assignment.id(), "Primera", List.of()), UUID.randomUUID());
            var second = f.plan.createActivity(f.process.id(), first.processRevision(),
                    input(f.assignment.id(), "Segunda", List.of(first.value().id())), UUID.randomUUID());
            var current = second.value();
            var invalid = new ProcessActivity(current.id(), current.processId(), current.indicatorId(), null,
                    current.assignmentId(), "No debe persistir", current.dueAt(), current.status(), List.of(UUID.randomUUID()),
                    null, null, null, current.createdBy(), current.createdAt(), current.revision() + 1);
            assertThatThrownBy(() -> f.repository.saveActivity(invalid, current.revision())).isInstanceOf(DataAccessException.class);
            assertThat(f.repository.findActivity(current.id())).contains(current);
            assertThat(f.jdbc.queryForObject("select count(*) from process_activity_revision where activity_id=?",
                    Integer.class, current.id())).isZero();
            assertThat(f.jdbc.query("select depends_on_activity_id from process_activity_dependency where activity_id=?",
                    (row, index) -> JdbcValueSupport.uuid(row, 1), current.id())).containsExactly(first.value().id());
        }
    }

    @ParameterizedTest @MethodSource("databases")
    void storesInitialSuspensionAndResumptionDecisionsWithoutInventingLegacyAuthorization(String database) throws Exception {
        try (var f = new Fixture(database)) {
            var models = mock(ModelCatalogPort.class);
            when(models.publishedVersion(f.process.modelVersionId())).thenReturn(Optional.of(new ModelCatalogPort.PublishedModel(
                    UUID.randomUUID(), f.process.modelVersionId(), 1, f.process.indicators().stream()
                    .map(i -> new ModelCatalogPort.Indicator(i.id(), i.code(), i.name())).toList(), null, null)));
            var assessments = f.transactional(new SelfAssessmentService(f.repository, models, f.actors, f.auth, f.audit,
                    f.authority, mock(CareerAdministrationService.class), mock(ModelApplicabilityService.class)));
            var preparation = assessments.create(f.process.careerId(), "P-PERSIST", "Proceso", f.process.periodStart(),
                    f.process.periodEnd(), f.process.modelVersionId(), UUID.randomUUID());
            UUID authorityAssignment = UUID.randomUUID();
            var authorized = assessments.authorize(preparation.id(), 0, authorityAssignment, "Designacion sintetica",
                    "fixture://initial", "Inicio autorizado sintetico", UUID.randomUUID());
            var active = assessments.activate(preparation.id(), authorized.revision(), UUID.randomUUID());
            var transitions = f.transactional(new ProcessTransitionService(f.repository, f.actors, f.auth, f.authority, f.audit));
            var decision = new ProcessTransitionService.Decision(authorityAssignment, "Designacion sintetica", "fixture://decision");
            var suspended = transitions.transition(active.id(), active.revision(), ProcessState.SUSPENDED, decision,
                    "Suspension justificada", UUID.randomUUID());
            var resumed = transitions.transition(active.id(), suspended.revision(), ProcessState.ACTIVE, decision,
                    "Reanudacion justificada", UUID.randomUUID());
            assertThat(f.repository.find(active.id())).contains(resumed);
            assertThat(resumed.authorization()).isEqualTo(authorized.authorization());
            assertThat(resumed.indicators()).isEqualTo(active.indicators());
            assertThat(f.jdbc.queryForList("select decision_type from process_authorization where process_id=? order by decided_at",
                    String.class, active.id())).containsExactly("INITIAL", "SUSPENSION", "RESUMPTION");
            assertThat(f.jdbc.queryForObject("select count(*) from process_transition where process_id=?",
                    Integer.class, active.id())).isEqualTo(3);
            assertThat(f.repository.find(f.process.id()).orElseThrow().authorization()).isNull();
            assertThat(f.jdbc.queryForObject("select count(*) from process_authorization where process_id=?",
                    Integer.class, f.process.id())).isZero();
        }
    }

    @ParameterizedTest @MethodSource("databases")
    void duplicateDecisionRollsBackTheTransitionAndPreservesItsIdentifier(String database) throws Exception {
        try (var f = new Fixture(database)) {
            var decision = new ProcessAuthorization(null, UUID.randomUUID(), "Designacion sintetica", "fixture://decision",
                    "Decision sintetica", ACTOR, FROM);
            f.repository.recordTransition(f.process.id(), ProcessState.ACTIVE, ProcessState.SUSPENDED, ACTOR,
                    decision.reason(), UUID.randomUUID(), 2, decision);
            assertThat(f.jdbc.query("select id from process_authorization where process_id=?",
                    (row, index) -> JdbcValueSupport.uuid(row, 1), f.process.id())).containsExactly(decision.id());
            assertThatThrownBy(() -> f.repository.recordTransition(f.process.id(), ProcessState.ACTIVE, ProcessState.SUSPENDED,
                    ACTOR, decision.reason(), UUID.randomUUID(), 2, decision)).isInstanceOf(DataAccessException.class);
            assertThat(f.jdbc.queryForObject("select count(*) from process_transition where process_id=?",
                    Integer.class, f.process.id())).isEqualTo(1);
        }
    }

    @ParameterizedTest @MethodSource("databases")
    void staleDatabaseRevisionsReturnPreconditionsAndDoNotAppendHistory(String database) throws Exception {
        try (var f = new Fixture(database)) {
            f.repository.save(f.process.advanceRevision(), 1);
            assertThatThrownBy(() -> f.repository.save(f.process.advanceRevision(), 1))
                    .isInstanceOf(ProcessProblemException.class).extracting("kind", "code")
                    .containsExactly(ProcessProblemException.Kind.PRECONDITION, "STALE_PROCESS");
            var created = f.plan.createActivity(f.process.id(), 2, input(f.assignment.id(), "Inicial", List.of()), UUID.randomUUID());
            var changed = f.plan.updateActivity(created.value().id(), created.processRevision(),
                    input(f.assignment.id(), "Corregida", List.of()), UUID.randomUUID());
            assertThatThrownBy(() -> f.repository.saveActivity(created.value(), 0))
                    .isInstanceOf(ProcessProblemException.class).extracting("kind", "code")
                    .containsExactly(ProcessProblemException.Kind.PRECONDITION, "STALE_ACTIVITY");
            assertThat(f.repository.findActivity(created.value().id())).contains(changed.value());
            assertThat(f.jdbc.queryForObject("select count(*) from process_activity_revision where activity_id=?",
                    Integer.class, created.value().id())).isEqualTo(1);
        }
    }

    @ParameterizedTest @MethodSource("databases")
    void auditFailureRollsBackTheProcessRevisionAndTheTeam(String database) throws Exception {
        try (var f = new Fixture(database)) {
            doThrow(new IllegalStateException("AUDIT_FAILED")).when(f.audit).record(any(), any(), any(), any(), any(), any());
            assertThatThrownBy(() -> f.plan.createTeam(f.process.id(), 1, "No debe persistir", "COLLECTION",
                    FROM, UNTIL, UUID.randomUUID())).hasMessage("AUDIT_FAILED");
            assertThat(f.repository.teams(f.process.id())).isEmpty();
            assertThat(f.repository.find(f.process.id())).contains(f.process);
        }
    }

    private static ProcessPlanningService.ActivityInput input(UUID assignment, String title, List<UUID> dependencies) {
        return new ProcessPlanningService.ActivityInput(INDICATOR, null, assignment, title, FROM.plusSeconds(3600),
                dependencies, null, null, null);
    }

    private static UUID identity(String login) { return UUID.nameUUIDFromBytes(("sec-unl:" + login).getBytes(StandardCharsets.UTF_8)); }

    private class Fixture implements AutoCloseable {
        final String schema, url;
        final SingleConnectionDataSource dataSource;
        final JdbcTemplate jdbc;
        final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
        final CurrentActorPort actors = () -> Optional.of(new CurrentActorPort.Actor(ACTOR, false));
        final AuthorizationPort auth = mock(AuthorizationPort.class);
        final InstitutionalAuthorityPort authority = mock(InstitutionalAuthorityPort.class);
        final SelfAssessmentAuditService audit = mock(SelfAssessmentAuditService.class);
        final DataSourceTransactionManager transactions;
        final JdbcSelfAssessmentRepository repository;
        final ProcessPlanningService plan;
        final SelfAssessmentProcess process;
        final ProcessAssignment assignment;

        Fixture(String database) {
            schema = database.equals("postgres") ? "d3_" + UUID.randomUUID().toString().replace("-", "") : null;
            url = schema == null ? "jdbc:sqlite:" + directory.resolve(UUID.randomUUID() + ".sqlite")
                    : PostgresTestSupport.jdbcUrl() + (PostgresTestSupport.jdbcUrl().contains("?") ? "&" : "?") + "currentSchema=" + schema;
            dataSource = connection();
            jdbc = new JdbcTemplate(dataSource);
            if (schema == null) {
                jdbc.execute("PRAGMA foreign_keys=ON");
                new ResourceDatabasePopulator(new ClassPathResource("db/sqlite/schema.sql")).execute(dataSource);
                new SqliteModelGuards(jdbc).run(null);
            } else Flyway.configure().dataSource(dataSource).schemas(schema).defaultSchema(schema).load().migrate();
            new SyntheticCareerData(new JdbcCareerCatalogRepository(jdbc, json)).run(null);
            new SyntheticAccessData(jdbc, new BCryptPasswordEncoder(4)).run(null);
            new SyntheticPilotData(jdbc, json).run(null);
            transactions = new DataSourceTransactionManager(dataSource);
            repository = transactional(new JdbcSelfAssessmentRepository(jdbc, json));
            process = repository.find(SyntheticPilotData.PROCESS_ID).orElseThrow();
            assignment = repository.assignments(process.id()).stream().filter(a -> a.indicatorId().equals(INDICATOR)).findFirst().orElseThrow();
            when(auth.isAllowed(any(), any(), any())).thenReturn(true);
            when(authority.isAuthorizedAssignment(any(), any(), any(), any())).thenReturn(true);
            var policy = mock(FunctionalAssignmentPolicyPort.class);
            when(policy.evaluate(any(), any(), any(), any(), any())).thenReturn(FunctionalAssignmentPolicyPort.Decision.allow());
            when(policy.evaluateMembership(any(), any(), any(), any(), any())).thenReturn(FunctionalAssignmentPolicyPort.Decision.allow());
            plan = transactional(new ProcessPlanningService(repository, mock(ModelCatalogPort.class), actors, auth,
                    authority, policy, audit, new ProcessAssignmentCatalogAdapter(repository, policy)));
        }

        SingleConnectionDataSource connection() {
            return schema == null ? new SingleConnectionDataSource(url, true)
                    : new SingleConnectionDataSource(url, PostgresTestSupport.username(), PostgresTestSupport.password(), true);
        }

        @SuppressWarnings("unchecked")
        <T> T transactional(T target) {
            var proxy = new ProxyFactory(target);
            proxy.setProxyTargetClass(true);
            var interceptor = new TransactionInterceptor();
            interceptor.setTransactionManager(transactions);
            interceptor.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
            proxy.addAdvice(interceptor);
            return (T) proxy.getProxy();
        }

        @Override public void close() {
            try { if (schema != null) jdbc.execute("drop schema " + schema + " cascade"); }
            finally { dataSource.close(); }
        }
    }
}
