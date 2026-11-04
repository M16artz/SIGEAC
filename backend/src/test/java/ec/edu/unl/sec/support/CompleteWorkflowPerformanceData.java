package ec.edu.unl.sec.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.unl.sec.evaluationmodel.domain.*;
import ec.edu.unl.sec.evaluationmodel.infrastructure.JdbcEvaluationModelRepository;
import ec.edu.unl.sec.identityaccess.infrastructure.SyntheticAccessData;
import ec.edu.unl.sec.selfassessment.domain.*;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.boot.*;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Test classpath only: explicit PostgreSQL load fixture, never institutional data. */
@Component
@Profile("complete-workflow-performance-test")
@Order(3)
public class CompleteWorkflowPerformanceData implements ApplicationRunner {
    public static final UUID PROCESS = id(11, 1);
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final PasswordEncoder passwords;

    public CompleteWorkflowPerformanceData(JdbcTemplate jdbc, ObjectMapper json, PasswordEncoder passwords) {
        this.jdbc = jdbc; this.json = json; this.passwords = passwords;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        if (!Boolean.TRUE.equals(jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<Boolean>) c ->
                c.getMetaData().getDatabaseProductName().contains("PostgreSQL")))) {
            throw new IllegalStateException("Performance acceptance requires disposable PostgreSQL");
        }
        if (jdbc.queryForObject("select count(*) from self_assessment_process where id=?", Long.class, PROCESS) == 0) {
            var admin = UUID.nameUUIDFromBytes("sec-unl:admin".getBytes(StandardCharsets.UTF_8));
            var now = Instant.now();
            var version = model(admin, now);
            var models = new JdbcEvaluationModelRepository(jdbc, json);
            models.saveModel(new EvaluationModel(version.modelId(), "Carga sintetica T106",
                    "500 indicadores sin validez institucional", admin, now));
            models.saveVersion(version, -1);
            var indicators = version.criteria().getFirst().children().stream().map(node ->
                    new SelfAssessmentProcess.IndicatorSnapshot(node.id(), node.code(), node.name())).toList();
            var process = new SelfAssessmentProcess(PROCESS, SyntheticAccessData.PILOT_CAREER, "PERF-SYNTHETIC",
                    "Carga sintetica T106", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                    version.id(), 1, ProcessState.ACTIVE, 1, admin, now, indicators);
            jdbc.update("insert into self_assessment_process(id,career_id,code,name,period_start,period_end,"
                            + "model_version_id,model_version_number,state,revision,created_by,created_at,snapshot_json)"
                            + " values(?,?,?,?,?,?,?,1,'ACTIVE',1,?,?,?::jsonb)", PROCESS, process.careerId(),
                    process.code(), process.name(), process.periodStart(), process.periodEnd(), version.id(), admin,
                    Timestamp.from(now), json.writeValueAsString(process));
            // Shared hash is restricted to these disposable synthetic identities.
            jdbc.execute((org.springframework.jdbc.core.ConnectionCallback<Void>) connection -> {
                ScriptUtils.executeSqlScript(connection, new ClassPathResource("fixtures/complete-workflow-performance.sql"));
                return null;
            });
            jdbc.update("insert into access_credential(id,identity_id,bcrypt_hash,active,created_at)"
                            + " select ('f1060000-0000-0000-0009-'||lpad((n+1000)::text,12,'0'))::uuid,"
                            + " ('f1060000-0000-0000-0007-'||lpad(n::text,12,'0'))::uuid,?,true,now()"
                            + " from generate_series(1,100) n", passwords.encode("Synthetic-Perf-2026"));
        }
        verifyDataset();
    }

    static ModelVersion model(UUID admin, Instant now) {
        var scale = new RatingScale(id(10, 3), "PERF-SCALE", "Escala sintetica",
                List.of(new RatingScale.Option(id(10, 4), "SUPPORTED", "Sustentado", 1, null)));
        var nodes = new ArrayList<ModelNode>();
        var definitions = new ArrayList<IndicatorDefinition>();
        var rules = new ArrayList<ValuationRule>();
        for (int n = 1; n <= 500; n++) {
            var indicator = id(1, n);
            nodes.add(new ModelNode(indicator, "PERF-" + String.format("%03d", n), "Indicador sintetico " + n,
                    "Carga reproducible T106", n, List.of(), "INDICATOR"));
            var rule = new ValuationRule(id(8, n), indicator, ValuationRule.Type.MANUAL_QUALITATIVE,
                    "Juicio sintetico fundamentado", null, null, null, scale.id(), List.of(), List.of(),
                    "Declarar pendiente", ValuationRule.Status.VALIDATED);
            rules.add(rule);
            var source = new RequiredSource(id(2, n), indicator, "SRC-" + n, RequiredSource.SourceType.EXTERNAL_LINK,
                    "Fuente sintetica " + n, "Referencia de software sin validez institucional", true, 1, null,
                    "Periodo sintetico 2026", 1);
            definitions.add(new IndicatorDefinition(indicator, IndicatorDefinition.Nature.QUALITATIVE,
                    "Estandar sintetico de prueba", List.of(), "Periodo 2026", scale.id(), rule.id(), List.of(source)));
        }
        var version = new ModelVersion(id(10, 2), id(10, 1), 1, ModelVersionStatus.DRAFT,
                "fixture://performance/synthetic-no-institutional-validity", admin, null, null, 0, now, null,
                List.of(new ModelNode(id(10, 5), "PERF", "Criterio sintetico", "Carga T106", 1, nodes, "CRITERION")),
                null, null, new ModelConfiguration(definitions, List.of(scale), rules));
        var approver = UUID.nameUUIDFromBytes("sec-unl:coordinador".getBytes(StandardCharsets.UTF_8));
        var issues = PublicationValidator.validate(version, approver, "sec.progress/v1");
        if (!issues.isEmpty()) throw new IllegalStateException("Invalid performance model: " + issues);
        return version.publish(approver, now);
    }

    private void verifyDataset() {
        if (jdbc.queryForObject("select count(*) from process_indicator where process_id=?", Integer.class, PROCESS) != 500
                || jdbc.queryForObject("select count(*) from evidence_version v join evidence e on e.id=v.evidence_id"
                        + " where e.process_id=? and v.status='SUBMITTED'", Integer.class, PROCESS) != 10000
                || jdbc.queryForObject("select count(*) from access_identity where login_normalized ~ '^perf[0-9]{3}$'",
                        Integer.class) != 100) throw new IllegalStateException("Incomplete synthetic performance dataset");
    }

    static UUID id(int kind, long sequence) {
        return UUID.fromString(String.format(Locale.ROOT, "f1060000-0000-0000-%04d-%012d", kind, sequence));
    }
}
