package ec.edu.unl.sec.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.unl.sec.identityaccess.infrastructure.SyntheticAccessData;
import ec.edu.unl.sec.selfassessment.domain.ProcessState;
import ec.edu.unl.sec.selfassessment.domain.SelfAssessmentProcess;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Synthetic P16 fixture: approved report, preserved documentary closure and archive authority. */
@Component
@Profile("d9-archive-test")
@Order(7)
public class D9ArchiveData extends D8ImprovementData {
    public static final UUID AUTHORITY_ASSIGNMENT = UUID.fromString("d9000000-0000-0000-0000-000000000901");
    public static final UUID EXCEPTION_DECISION = UUID.fromString("d9000000-0000-0000-0000-000000000920");
    public static final UUID DOCUMENTARY_SNAPSHOT = UUID.fromString("d9000000-0000-0000-0000-000000000930");
    public static final UUID DOCUMENTARY_CLOSURE = UUID.fromString("d9000000-0000-0000-0000-000000000931");
    private static final UUID PROCESS_AUTHORITY = UUID.fromString("20000000-0000-0000-0000-000000000006");
    private static final String DIGEST = "c".repeat(64);

    private final JdbcTemplate database;
    private final ObjectMapper mapper;

    public D9ArchiveData(JdbcTemplate jdbc, ObjectMapper json, PasswordEncoder passwords) {
        super(jdbc, json, passwords);
        this.database = jdbc;
        this.mapper = json;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        super.run(args);
        approveFinalReport();
        authority();
        documentaryClosure();
    }

    private void approveFinalReport() {
        database.update("update governance_report_processing set state='APPROVED',revision=revision+1,updated_at=?"
                + " where process_id=? and version_id=?", Timestamp.from(Instant.now()), PROCESS, REPORT_VERSION);
    }

    private void authority() {
        database.update("insert into functional_assignment(id,identity_id,function_id,career_id,process_id,indicator_id,"
                        + "valid_from,valid_to,granted_by,granted_at,revision) values(?,?,?,?,?,null,?,?,?,?,0)"
                        + " on conflict do nothing",
                AUTHORITY_ASSIGNMENT, user("coordinador"), PROCESS_AUTHORITY, SyntheticAccessData.PILOT_CAREER,
                PROCESS, Timestamp.from(Instant.parse("2020-01-01T00:00:00Z")),
                Timestamp.from(Instant.parse("2030-01-01T00:00:00Z")), user("admin"),
                Timestamp.from(Instant.now()));
        database.update("insert into process_authorization(id,process_id,decision_type,authority_assignment_id,"
                        + "legal_basis,document_reference,reason,decided_by,decided_at) values(?,?,?,?,?,?,?,?,?)"
                        + " on conflict do nothing",
                EXCEPTION_DECISION, PROCESS, "CLOSURE_EXCEPTION", AUTHORITY_ASSIGNMENT,
                "Reglamento interno de cierre integral", "Acta de excepción D9",
                "Excepción autorizada para acción vencida durante el cierre integral.",
                user("coordinador"), Timestamp.from(Instant.now()));
    }

    private void documentaryClosure() throws Exception {
        var now = Instant.now();
        database.update("insert into documentary_snapshot(id,process_id,career_id,model_version_id,process_revision,"
                        + "created_by,created_at,reason,formula_version,progress_state,required_source_total,"
                        + "registered_source_total,accepted_source_total,sha256,payload_json)"
                        + " values(?,?,?,?,?,?,?,?, 'FORMULA_V1','CONFIGURED',1,1,1,?,?) on conflict do nothing",
                DOCUMENTARY_SNAPSHOT, PROCESS, SyntheticAccessData.PILOT_CAREER,
                UUID.fromString("d7000000-0000-0000-0000-000000000002"), 1, user("coordinador"),
                Timestamp.from(now), "Instantánea documental sintética para el cierre integral P16.", DIGEST,
                "{\"fixture\":\"d9-documentary-snapshot\"}");
        database.update("insert into documentary_closure(id,process_id,career_id,snapshot_id,process_revision,"
                        + "policy_version,closed_by,closed_at,reason,sha256,payload_json)"
                        + " values(?,?,?,?,1,'DOCUMENTARY_STRICT_V1',?,?,?,?,?) on conflict do nothing",
                DOCUMENTARY_CLOSURE, PROCESS, SyntheticAccessData.PILOT_CAREER, DOCUMENTARY_SNAPSHOT,
                user("coordinador"), Timestamp.from(now),
                "Cierre documental sintético conservado para el cierre integral P16.",
                "d".repeat(64), "{\"fixture\":\"d9-documentary-closure\"}");
        var raw = database.queryForObject("select snapshot_json from self_assessment_process where id=?",
                String.class, PROCESS);
        var process = mapper.readValue(raw, SelfAssessmentProcess.class);
        if (process.state() == ProcessState.ACTIVE) {
            var closed = process.transition(ProcessState.DOCUMENTARY_CLOSED);
            database.update("update self_assessment_process set state=?,revision=?,snapshot_json=? where id=?",
                    closed.state().name(), closed.revision(), mapper.writeValueAsString(closed), PROCESS);
        }
    }

    private static UUID user(String login) {
        return UUID.nameUUIDFromBytes(("sec-unl:" + login).getBytes(StandardCharsets.UTF_8));
    }
}
