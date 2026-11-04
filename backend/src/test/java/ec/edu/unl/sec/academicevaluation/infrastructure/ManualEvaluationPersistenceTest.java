package ec.edu.unl.sec.academicevaluation.infrastructure;

import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.unl.sec.academicevaluation.domain.ManualAcademicEvaluation;
import ec.edu.unl.sec.identityaccess.infrastructure.SyntheticAccessData;
import ec.edu.unl.sec.selfassessment.infrastructure.*;
import ec.edu.unl.sec.support.PostgresTestSupport;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

class ManualEvaluationPersistenceTest {
    static Stream<String> databases() {
        return Boolean.parseBoolean(System.getenv().getOrDefault("SEC_RUN_FILE_DATABASE_TESTS","false"))
                ? Stream.of("sqlite","postgres-clean","postgres-upgrade20","postgres-upgrade21","postgres-upgrade22") : Stream.of("sqlite");
    }
    @ParameterizedTest @MethodSource("databases")
    void roundTripsOwnRevisionsAndRejectsMutationDuplicateRevisionAndForeignPredecessor(String database) {
        String schema=database.startsWith("postgres") ? "manual_"+UUID.randomUUID().toString().replace("-","") : null;
        String url=schema==null ? "jdbc:sqlite::memory:" : PostgresTestSupport.jdbcUrl()
                +(PostgresTestSupport.jdbcUrl().contains("?")?"&":"?")+"currentSchema="+schema;
        try (var source=schema==null ? new SingleConnectionDataSource(url,true)
                : new SingleConnectionDataSource(url,PostgresTestSupport.username(),PostgresTestSupport.password(),true)) {
            var jdbc=new JdbcTemplate(source);
            try {
                if (schema==null) {
                    jdbc.execute("pragma foreign_keys=on");
                    new ResourceDatabasePopulator(new ClassPathResource("db/sqlite/schema.sql")).execute(source);
                    new SqliteManualEvaluationGuards(jdbc).run(null);
                } else {
                    if (database.equals("postgres-upgrade20")) Flyway.configure().dataSource(source).schemas(schema).defaultSchema(schema).target("20").load().migrate();
                    if (database.equals("postgres-upgrade21")) Flyway.configure().dataSource(source).schemas(schema).defaultSchema(schema).target("21").load().migrate();
                    if (database.equals("postgres-upgrade22")) Flyway.configure().dataSource(source).schemas(schema).defaultSchema(schema).target("22").load().migrate();
                    var flyway=Flyway.configure().dataSource(source).schemas(schema).defaultSchema(schema).load();
                    flyway.migrate(); flyway.validate(); assertThat(flyway.info().current().getVersion())
                            .isEqualTo(org.flywaydb.core.api.MigrationVersion.fromVersion("26"));
                }
                var json=new ObjectMapper().findAndRegisterModules();
                new SyntheticCareerData(new JdbcCareerCatalogRepository(jdbc,json)).run(null);
                new SyntheticAccessData(jdbc,new BCryptPasswordEncoder(4)).run(null);
                new SyntheticPilotData(jdbc,json).run(null);
                var repository=new JdbcManualEvaluationRepository(jdbc,json);
                var actor=UUID.nameUUIDFromBytes("sec-unl:evaluador".getBytes(StandardCharsets.UTF_8));
                var other=UUID.nameUUIDFromBytes("sec-unl:coordinador".getBytes(StandardCharsets.UTF_8));
                verifyQuantitativeHistory(jdbc,json,actor,other);
                var first=value(actor,1,null); repository.append(first);
                var second=value(actor,2,first.id()); repository.append(second);
                var independent=value(other,1,null); repository.append(independent);
                assertThat(repository.latest(first.processId(),first.indicatorId(),actor)).contains(second);
                assertThat(repository.current(first.processId(),first.indicatorId())).containsExactlyInAnyOrder(second,independent);
                verifyReconciliation(jdbc,json,first,second,independent,actor,other);
                assertThat(repository.latest(first.processId(),first.indicatorId(),other)).contains(independent);
                assertThat(repository.history(first.processId(),first.indicatorId(),0,20)).containsExactlyInAnyOrder(first,second,independent);
                assertThat(repository.history(first.processId(),first.indicatorId(),1,1)).hasSize(1);
                assertThatThrownBy(()->repository.append(value(actor,2,first.id()))).isInstanceOf(DataAccessException.class);
                assertThatThrownBy(()->repository.append(value(other,2,second.id()))).isInstanceOf(DataAccessException.class);
                assertThatThrownBy(()->jdbc.update("update manual_academic_evaluation set payload_json='{}' where id=?",first.id())).isInstanceOf(DataAccessException.class);
                assertThatThrownBy(()->jdbc.update("delete from manual_academic_evaluation where id=?",second.id())).isInstanceOf(DataAccessException.class);
                var tx=new TransactionTemplate(new DataSourceTransactionManager(source));
                assertThatThrownBy(()->tx.execute(status -> {
                    repository.append(value(actor,3,second.id()));
                    throw new IllegalStateException("Simulated audit failure");
                })).isInstanceOf(IllegalStateException.class);
                assertThat(repository.latest(first.processId(),first.indicatorId(),actor)).contains(second);
                assertThat(repository.history(first.processId(),first.indicatorId(),0,20)).hasSize(3);
            } finally { if(schema!=null) jdbc.execute("drop schema "+schema+" cascade"); }
        }
    }
    private static void verifyReconciliation(JdbcTemplate jdbc,ObjectMapper json,ManualAcademicEvaluation first,ManualAcademicEvaluation second,
            ManualAcademicEvaluation independent,UUID actor,UUID other) {
        var repository=new JdbcReconciliationRepository(jdbc,json);
        var assignment=jdbc.query("select id from functional_assignment where identity_id=?",(r,n)->ec.edu.unl.sec.shared.infrastructure.JdbcValueSupport.uuid(r,1),actor).getFirst();
        var policy=new ec.edu.unl.sec.academicevaluation.domain.AcademicReconciliation.Policy(UUID.randomUUID(),first.processId(),first.indicatorId(),1,null,
                "Procedimiento sintético 1",ec.edu.unl.sec.academicevaluation.domain.AcademicReconciliation.METHOD,"Fundamento sintético de competencia.","fixture://policy",assignment,actor,Instant.now());
        repository.append(policy);
        var judgements=List.of(judgement(second),judgement(independent));
        var fingerprint=ec.edu.unl.sec.academicevaluation.domain.AcademicReconciliation.fingerprint(judgements);
        var disagreement=new ec.edu.unl.sec.academicevaluation.domain.AcademicReconciliation.Disagreement(UUID.randomUUID(),first.processId(),first.indicatorId(),1,null,fingerprint,
                judgements,"Diferencia sintética entre fundamentos académicos.",actor,Instant.now());repository.append(disagreement);
        var resolution=new ec.edu.unl.sec.academicevaluation.domain.AcademicReconciliation.Resolution(UUID.randomUUID(),first.processId(),first.indicatorId(),disagreement.id(),policy,
                judgements.getFirst(),List.of(actor,other),"Resolución sintética con selección fundamentada.",assignment,actor,Instant.now(),fingerprint);repository.append(resolution);
        assertThat(repository.policy(first.processId(),first.indicatorId())).contains(policy);
        assertThat(repository.latest(first.processId(),first.indicatorId())).contains(disagreement);
        assertThat(repository.resolution(disagreement.id())).contains(resolution);
        assertThat(repository.history(first.processId(),first.indicatorId(),0,20)).containsExactly(disagreement);
        assertThatThrownBy(()->repository.append(resolution)).isInstanceOf(DataAccessException.class);
        var foreign=new ec.edu.unl.sec.academicevaluation.domain.AcademicReconciliation.Policy(UUID.randomUUID(),first.processId(),SyntheticPilotData.INDICATOR_B_ID,1,null,
                "Otro procedimiento",policy.method(),policy.legalBasis(),policy.documentReference(),assignment,actor,Instant.now());repository.append(foreign);
        assertThatThrownBy(()->repository.append(new ec.edu.unl.sec.academicevaluation.domain.AcademicReconciliation.Policy(UUID.randomUUID(),first.processId(),first.indicatorId(),2,foreign.id(),
                policy.procedureReference(),policy.method(),policy.legalBasis(),policy.documentReference(),assignment,actor,Instant.now()))).isInstanceOf(DataAccessException.class);
        for(String table:List.of("academic_reconciliation_policy","academic_disagreement_record","academic_reconciliation_resolution")) {
            assertThatThrownBy(()->jdbc.update("update "+table+" set payload_json='{}'")).isInstanceOf(DataAccessException.class);
            assertThatThrownBy(()->jdbc.update("delete from "+table)).isInstanceOf(DataAccessException.class);
        }
    }
    private static ec.edu.unl.sec.academicevaluation.domain.AcademicReconciliation.Judgement judgement(ManualAcademicEvaluation v) {
        return new ec.edu.unl.sec.academicevaluation.domain.AcademicReconciliation.Judgement("MANUAL",v.id(),v.evaluatorId(),v.modelVersionId(),v.ruleId(),v.scaleId(),v.ratingOptionId(),v.ratingLabel(),null,
                v.method(),v.justification(),v.limitations(),v.evaluatedAt(),v.supports(),List.of());
    }
    private static void verifyQuantitativeHistory(JdbcTemplate jdbc,ObjectMapper json,UUID actor,UUID other) {
        var repository=new JdbcQuantitativeRepository(jdbc,json);
        var inputs=inputs(actor,1,null);
        repository.append(inputs);
        var verification=new ec.edu.unl.sec.academicevaluation.domain.QuantitativeEvaluation.Verification(inputs.id(),actor,Instant.now(),"Cifras, fuentes y período verificados.");
        repository.verify(verification);
        var scale=new ec.edu.unl.sec.evaluationmodel.domain.RatingScale(UUID.randomUUID(),"RATIO","Escala sintética",List.of(
                new ec.edu.unl.sec.evaluationmodel.domain.RatingScale.Option(UUID.randomUUID(),"OK","Suficiente",1,null)));
        var rule=new ec.edu.unl.sec.evaluationmodel.domain.ValuationRule(inputs.ruleId(),inputs.indicatorId(),
                ec.edu.unl.sec.evaluationmodel.domain.ValuationRule.Type.QUANTITATIVE,"Razón porcentual sintética","A / B * 100","ratio-percent","1",scale.id(),
                List.of(new ec.edu.unl.sec.evaluationmodel.domain.ValuationVariable("A","Cantidad A","personas",ec.edu.unl.sec.evaluationmodel.domain.ValuationVariable.DataType.INTEGER,true,"NON_NEGATIVE"),
                        new ec.edu.unl.sec.evaluationmodel.domain.ValuationVariable("B","Cantidad B","personas",ec.edu.unl.sec.evaluationmodel.domain.ValuationVariable.DataType.INTEGER,true,"POSITIVE")),
                List.of(new ec.edu.unl.sec.evaluationmodel.domain.ValuationThreshold(scale.options().getFirst().id(),java.math.BigDecimal.ZERO,null,true,false)),"REJECT",
                ec.edu.unl.sec.evaluationmodel.domain.ValuationRule.Status.VALIDATED);
        var calculation=ec.edu.unl.sec.academicevaluation.domain.RatioPercentStrategy.calculate(rule,scale,Map.of("A",new java.math.BigDecimal("3"),"B",new java.math.BigDecimal("4")));
        var run=new ec.edu.unl.sec.academicevaluation.domain.QuantitativeEvaluation.Run(UUID.randomUUID(),inputs,verification,rule,scale,
                calculation.value(),calculation.threshold(),calculation.option().id(),calculation.option().label(),"DECIMAL128_HALF_EVEN_34_DIGITS",actor,Instant.now(),"Justificación del cálculo sintético.","Limitado al piloto sintético de pruebas.");
        repository.append(run);
        assertThat(repository.inputs(inputs.id())).contains(inputs);
        assertThat(repository.verification(inputs.id())).contains(verification);
        assertThat(repository.run(inputs.id())).contains(run);
        assertThat(repository.history(inputs.processId(),inputs.indicatorId(),0,20)).containsExactly(run);
        assertThat(repository.current(inputs.processId(),inputs.indicatorId())).containsExactly(run);
        var correction=inputs(actor,2,inputs.id());repository.append(correction);
        assertThat(repository.latest(inputs.processId(),inputs.indicatorId(),actor)).contains(correction);
        assertThat(repository.run(inputs.id())).contains(run);
        assertThatThrownBy(()->repository.append(inputs(other,2,correction.id()))).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(()->repository.verify(verification)).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(()->repository.append(run)).isInstanceOf(DataAccessException.class);
        for(String table:List.of("quantitative_input_set","quantitative_verification","quantitative_calculation")) {
            assertThatThrownBy(()->jdbc.update("update "+table+" set payload_json='{}'" )).isInstanceOf(DataAccessException.class);
            assertThatThrownBy(()->jdbc.update("delete from "+table)).isInstanceOf(DataAccessException.class);
        }
    }
    private static ec.edu.unl.sec.academicevaluation.domain.QuantitativeEvaluation.Inputs inputs(UUID actor,long revision,UUID predecessor) {
        var support=new ManualAcademicEvaluation.Support(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID());
        return new ec.edu.unl.sec.academicevaluation.domain.QuantitativeEvaluation.Inputs(UUID.randomUUID(),SyntheticPilotData.PROCESS_ID,SyntheticPilotData.INDICATOR_A_ID,
                UUID.randomUUID(),UUID.randomUUID(),actor,Instant.now(),revision,predecessor,java.time.LocalDate.of(2026,1,1),java.time.LocalDate.of(2026,6,30),List.of(
                new ec.edu.unl.sec.academicevaluation.domain.QuantitativeEvaluation.Variable("A",new java.math.BigDecimal("3"),"personas",List.of(support)),
                new ec.edu.unl.sec.academicevaluation.domain.QuantitativeEvaluation.Variable("B",new java.math.BigDecimal("4"),"personas",List.of(support))));
    }
    private static ManualAcademicEvaluation value(UUID actor,long revision,UUID predecessor) {
        return new ManualAcademicEvaluation(UUID.randomUUID(),SyntheticPilotData.PROCESS_ID,SyntheticPilotData.INDICATOR_A_ID,
                UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),"Cumple","Método cualitativo sintético",
                "Limitaciones registradas", "Justificación académica conservada",
                List.of(new ManualAcademicEvaluation.Support(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID())),actor,Instant.now(),revision,predecessor);
    }
}
