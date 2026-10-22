package ec.edu.unl.sec.selfassessment.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.unl.sec.selfassessment.domain.ProcessState;
import ec.edu.unl.sec.selfassessment.domain.SelfAssessmentProcess;
import ec.edu.unl.sec.evaluationmodel.domain.ModelNode;
import ec.edu.unl.sec.evaluationmodel.domain.ModelVersion;
import ec.edu.unl.sec.evaluationmodel.domain.ModelVersionStatus;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(2)
@ConditionalOnProperty(prefix = "sec", name = "demo-data", havingValue = "true")
public class SyntheticPilotData implements ApplicationRunner {
    public static final UUID PROCESS_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");
    public static final UUID INDICATOR_A_ID = UUID.fromString("50000000-0000-0000-0000-000000000001");
    public static final UUID INDICATOR_B_ID = UUID.fromString("50000000-0000-0000-0000-000000000002");

    private static final UUID CAREER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID MODEL_ID = UUID.fromString("60000000-0000-0000-0000-000000000001");
    private static final UUID MODEL_VERSION_ID = UUID.fromString("60000000-0000-0000-0000-000000000002");
    private static final UUID ADMIN_ID = identityId("admin");
    private static final UUID RESPONSIBLE_ID = identityId("responsable");
    private static final Instant START = Instant.parse("2025-01-01T00:00:00Z");
    private static final Instant END = Instant.parse("9999-12-31T00:00:00Z");

    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public SyntheticPilotData(JdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Integer present = jdbc.queryForObject(
                "select count(*) from self_assessment_process where id=?", Integer.class, PROCESS_ID);

        jdbc.update(
                "insert into evaluation_model(id,name,description,created_by,created_at) values(?,?,?,?,?) on conflict(id) do nothing",
                MODEL_ID, "Modelo sintético", "Modelo local para pruebas", ADMIN_ID, Timestamp.from(START));
        UUID approver = identityId("coordinador");
        var nodes = List.of(new ModelNode(null,"CRI-SYN","Criterio sintético","",1,List.of(
                new ModelNode(null,"EST-SYN","Estándar sintético","",1,List.of(
                        new ModelNode(INDICATOR_A_ID,"IND-SQL-A","Indicador sintético A","",1,List.of()),
                        new ModelNode(INDICATOR_B_ID,"IND-SQL-B","Indicador sintético B","",2,List.of()))))));
        var version = new ModelVersion(MODEL_VERSION_ID, MODEL_ID, 1, ModelVersionStatus.PUBLISHED,
                "fixture://synthetic-pilot", ADMIN_ID, approver, null, 0, START, START, nodes, null, null);
        jdbc.update(
                "insert into model_version(id,model_id,version_number,status,source,author_id,approver_id,revision,created_at,published_at,snapshot_json) "
                        + "values(?,?,?,?,?,?,?,?,?,?," + jsonPlaceholder() + ") on conflict(id) do nothing",
                MODEL_VERSION_ID, MODEL_ID, 1, "PUBLISHED", version.source(), ADMIN_ID, approver, 0,
                Timestamp.from(START), Timestamp.from(START), encode(version));
        // Repair only the known invalid demo placeholder, never a user-configured model or institutional history.
        jdbc.update("update model_version set approver_id=?,source=?,snapshot_json=" + jsonPlaceholder()
                        + " where id=? and model_id=? and source='fixture://sqlite' and "
                        + (jsonPlaceholder().contains("jsonb") ? "snapshot_json='{}'::jsonb" : "snapshot_json='{}'"),
                approver,version.source(),encode(version),MODEL_VERSION_ID,MODEL_ID);
        if (present != null && present > 0) return;

        var indicators = List.of(
                new SelfAssessmentProcess.IndicatorSnapshot(INDICATOR_A_ID, "IND-SQL-A", "Indicador sintético A"),
                new SelfAssessmentProcess.IndicatorSnapshot(INDICATOR_B_ID, "IND-SQL-B", "Indicador sintético B"));
        var process = new SelfAssessmentProcess(
                PROCESS_ID, CAREER_ID, "PROCESS-SQLITE", "Proceso sintético SQLite",
                LocalDate.of(2025, 1, 1), LocalDate.of(2026, 12, 31), MODEL_VERSION_ID, 1,
                ProcessState.ACTIVE, 1, ADMIN_ID, START, indicators);
        jdbc.update(
                "insert into self_assessment_process(id,career_id,code,name,period_start,period_end,model_version_id,"
                        + "model_version_number,state,revision,created_by,created_at,snapshot_json) values(?,?,?,?,?,?,?,?,?,?,?,?,"
                        + jsonPlaceholder() + ")",
                process.id(), process.careerId(), process.code(), process.name(), process.periodStart(), process.periodEnd(),
                process.modelVersionId(), process.modelVersionNumber(), process.state().name(), process.revision(),
                process.createdBy(), Timestamp.from(process.createdAt()), encode(process));

        for (var indicator : indicators) {
            jdbc.update(
                    "insert into process_indicator(process_id,indicator_id,code,name) values(?,?,?,?)",
                    PROCESS_ID, indicator.id(), indicator.code(), indicator.name());
            jdbc.update(
                    "insert into process_assignment(id,process_id,indicator_id,identity_id,function_code,valid_from,valid_to,created_by,reason,revision) "
                            + "values(?,?,?,?,?,?,?,?,?,0)",
                    UUID.randomUUID(), PROCESS_ID, indicator.id(), RESPONSIBLE_ID, "RESPONSIBLE",
                    Timestamp.from(START), Timestamp.from(END), ADMIN_ID, "Asignación sintética para pruebas SQLite");
        }
    }

    private String jsonPlaceholder() {
        return jdbc.execute((ConnectionCallback<Boolean>) connection ->
                        connection.getMetaData().getDatabaseProductName().contains("PostgreSQL"))
                ? "?::jsonb"
                : "?";
    }

    private String encode(Object process) {
        try {
            return json.writeValueAsString(process);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No se pudo crear el fixture sintético", exception);
        }
    }

    private static UUID identityId(String login) {
        return UUID.nameUUIDFromBytes(("sec-unl:" + login).getBytes(StandardCharsets.UTF_8));
    }
}
