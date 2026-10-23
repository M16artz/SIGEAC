package ec.edu.unl.sec.migration;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.unl.sec.identityaccess.infrastructure.SyntheticAccessData;
import ec.edu.unl.sec.selfassessment.domain.*;
import ec.edu.unl.sec.selfassessment.infrastructure.*;
import ec.edu.unl.sec.shared.infrastructure.JdbcValueSupport;
import ec.edu.unl.sec.support.PostgresTestSupport;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class ProcessGovernanceMigrationTest {
    @Test
    @EnabledIfEnvironmentVariable(named = "SEC_RUN_GOVERNANCE_DATABASE_TESTS", matches = "true")
    void upgradesV009WithoutInventingLegacyDecisionsOrChangingAssignmentSubjects() throws Exception {
        String schema = "d3_migration_" + UUID.randomUUID().toString().replace("-", "");
        String url = PostgresTestSupport.jdbcUrl() + (PostgresTestSupport.jdbcUrl().contains("?") ? "&" : "?")
                + "currentSchema=" + schema;
        try (var source = new SingleConnectionDataSource(url, PostgresTestSupport.username(), PostgresTestSupport.password(), true)) {
            var jdbc = new JdbcTemplate(source);
            try {
                Flyway.configure().dataSource(source).schemas(schema).defaultSchema(schema).target("9").load().migrate();
                var json = new ObjectMapper().findAndRegisterModules();
                new SyntheticCareerData(new JdbcCareerCatalogRepository(jdbc, json)).run(null);
                new SyntheticAccessData(jdbc, new BCryptPasswordEncoder(4)).run(null);
                new SyntheticPilotData(jdbc, json).run(null);
                UUID processId = SyntheticPilotData.PROCESS_ID;
                String snapshot = jdbc.queryForObject("select snapshot_json::text from self_assessment_process where id=?", String.class, processId);
                var original = json.readValue(snapshot, SelfAssessmentProcess.class);
                var assignmentIds = jdbc.query("select id from process_assignment order by id", (row, index) -> JdbcValueSupport.uuid(row, 1));
                UUID transitionId = UUID.randomUUID();
                jdbc.update("insert into process_transition(id,process_id,from_state,to_state,actor_id,reason,occurred_at,correlation_id,resulting_revision) "
                                + "values(?,?,'DRAFT','ACTIVE',?,'LEGACY',?,?,1)", transitionId, processId, original.createdBy(),
                        Timestamp.from(Instant.parse("2025-01-01T00:00:00Z")), UUID.randomUUID());

                var migrated = Flyway.configure().dataSource(source).schemas(schema).defaultSchema(schema).target("10").load();
                assertThat(migrated.migrate().migrationsExecuted).isEqualTo(1);
                migrated.validate();
                var repository = new JdbcSelfAssessmentRepository(jdbc, json);
                assertThat(repository.find(processId)).contains(original);
                assertThat(jdbc.queryForObject("select snapshot_json::text from self_assessment_process where id=?", String.class, processId)).isEqualTo(snapshot);
                assertThat(repository.assignments(processId)).extracting(ProcessAssignment::id).containsExactlyInAnyOrderElementsOf(assignmentIds);
                assertThat(repository.assignments(processId)).allSatisfy(a -> {
                    assertThat(a.identityId()).isNotNull(); assertThat(a.teamId()).isNull();
                });
                assertThat(jdbc.queryForObject("select count(*) from process_authorization", Integer.class)).isZero();
                assertThat(jdbc.queryForObject("select authority_assignment_id from process_transition where id=?", UUID.class, transitionId)).isNull();

                var first = repository.assignments(processId).getFirst();
                var team = new ProcessTeam(null, processId, "Equipo", "COLLECTION", first.validFrom(), first.validTo(),
                        first.createdBy(), first.validFrom(), 0);
                repository.saveTeam(team);
                var teamAssignment = new ProcessAssignment(null, processId, first.indicatorId(), null, team.id(),
                        first.function(), first.validFrom(), first.validTo(), first.createdBy(), "Equipo sintetico", null, 0);
                repository.saveAssignment(teamAssignment);
                assertThat(repository.assignments(processId)).contains(teamAssignment);
                for (boolean bothSubjects : new boolean[] {false, true}) {
                    assertThatThrownBy(() -> jdbc.update("insert into process_assignment(id,process_id,indicator_id,identity_id,team_id,function_code,valid_from,valid_to,created_by,reason,revision) "
                                    + "values(?,?,?,?,?,'RESPONSIBLE',?,?,?,'Invalid',0)", UUID.randomUUID(), processId,
                            first.indicatorId(), bothSubjects ? first.identityId() : null, bothSubjects ? team.id() : null,
                            Timestamp.from(first.validFrom()), Timestamp.from(first.validTo()), first.createdBy()))
                            .isInstanceOf(DataAccessException.class);
                }
                assertThat(repository.assignments(processId)).hasSize(assignmentIds.size() + 1);
            } finally {
                jdbc.execute("drop schema if exists " + schema + " cascade");
            }
        }
    }
}
