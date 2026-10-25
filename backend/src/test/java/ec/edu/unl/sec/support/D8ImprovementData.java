package ec.edu.unl.sec.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.unl.sec.governance.domain.Finding;
import ec.edu.unl.sec.governance.domain.ReportVersion;
import ec.edu.unl.sec.identityaccess.infrastructure.SyntheticAccessData;
import ec.edu.unl.sec.selfassessment.infrastructure.SyntheticPilotData;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Synthetic P15 authority, FINAL report and weakness only on the explicit improvement test profile. */
@Component
@Profile("d8-improvement-test")
@Order(6)
public class D8ImprovementData extends D7ReportWorkflowData {
    public static final UUID PROCESS = D7ManualEvaluationData.PROCESS;
    public static final UUID REPORT_VERSION = UUID.fromString("d8000000-0000-0000-0000-000000000100");
    public static final UUID PRELIMINARY_VERSION = UUID.fromString("d8000000-0000-0000-0000-000000000099");
    public static final UUID FINDING = UUID.fromString("d8000000-0000-0000-0000-000000000200");
    public static final UUID RESPONSIBLE_ASSIGNMENT = UUID.fromString("d8000000-0000-0000-0000-000000000901");
    public static final UUID REVIEWER_ASSIGNMENT = UUID.fromString("d8000000-0000-0000-0000-000000000902");
    public static final UUID COORDINATOR_ASSIGNMENT = UUID.fromString("d8000000-0000-0000-0000-000000000903");
    private static final UUID RESPONSIBLE_FUNCTION = UUID.fromString("20000000-0000-0000-0000-000000000003");
    private static final UUID REVIEWER_FUNCTION = UUID.fromString("20000000-0000-0000-0000-000000000004");
    private static final UUID COORDINATOR_FUNCTION = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final Instant OPEN_ENDED = Instant.parse("2030-01-01T00:00:00Z");
    private static final Instant OPEN_START = Instant.parse("2020-01-01T00:00:00Z");

    private final JdbcTemplate database;
    private final ObjectMapper mapper;

    public D8ImprovementData(JdbcTemplate jdbc, ObjectMapper json, PasswordEncoder passwords) {
        super(jdbc, json, passwords);
        this.database = jdbc;
        this.mapper = json;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        super.run(args);
        assignment(RESPONSIBLE_ASSIGNMENT, user("responsable"), RESPONSIBLE_FUNCTION);
        assignment(REVIEWER_ASSIGNMENT, user("revisor"), REVIEWER_FUNCTION);
        assignment(COORDINATOR_ASSIGNMENT, user("coordinador"), COORDINATOR_FUNCTION);
        if (count("select count(*) from governance_report_version where process_id=?", PROCESS) == 0) report();
        if (count("select count(*) from governance_finding where id=?", FINDING) == 0) finding();
    }

    private void assignment(UUID id, UUID identity, UUID function) {
        database.update("insert into functional_assignment(id,identity_id,function_id,career_id,process_id,indicator_id,"
                        + "valid_from,valid_to,granted_by,granted_at,revision) values(?,?,?,?,?,null,?,?,?,?,0)"
                        + " on conflict do nothing",
                id, identity, function, SyntheticAccessData.PILOT_CAREER, PROCESS,
                Timestamp.from(OPEN_START), Timestamp.from(OPEN_ENDED), user("admin"), Timestamp.from(Instant.now()));
    }

    private void report() throws Exception {
        var now = Instant.now();
        var preliminary = new ReportVersion(PRELIMINARY_VERSION, PROCESS,
                new ReportVersion.Data(ReportVersion.Kind.PRELIMINARY, UUID.fromString("d8000000-0000-0000-0000-000000000050"),
                        "Plantilla institucional sintética P14", true,
                        "Informe preliminar sintético del proceso de autoevaluación.", null),
                user("coordinador"), now, null, 1, 1, sha("Informe preliminar sintético del proceso de autoevaluación."));
        var finalVersion = new ReportVersion(REPORT_VERSION, PROCESS,
                new ReportVersion.Data(ReportVersion.Kind.FINAL, UUID.fromString("d8000000-0000-0000-0000-000000000051"),
                        "Plantilla institucional sintética P14", true,
                        "Informe final sintético del proceso de autoevaluación.", PRELIMINARY_VERSION),
                user("coordinador"), now, PRELIMINARY_VERSION, 2, 1,
                sha("Informe final sintético del proceso de autoevaluación."));
        insert(preliminary, "PRELIMINARY");
        insert(finalVersion, "FINAL");
    }

    private void insert(ReportVersion version, String state) throws Exception {
        database.update("insert into governance_report_version(id,process_id,version_number,predecessor_version_id,"
                        + "emitted_by,emitted_at,content_sha256,payload_json) values(?,?,?,?,?,?,?,?)",
                version.id(), PROCESS, version.versionNumber(), version.predecessorVersionId(), version.createdBy(),
                Timestamp.from(version.createdAt()), version.contentSha256(), mapper.writeValueAsString(version));
        database.update("insert into governance_report_processing(version_id,process_id,state,revision,updated_at)"
                + " values(?,?,?,1,?)", version.id(), PROCESS, state, Timestamp.from(version.createdAt()));
    }

    private void finding() throws Exception {
        var registered = new Finding(FINDING, PROCESS, new Finding.Data(Finding.Type.WEAKNESS,
                "Brecha sintética de seguimiento académico",
                "Debilidad sintética sustentada por el indicador piloto para el plan de mejora.",
                List.of(SyntheticPilotData.INDICATOR_A_ID), List.of(), List.of()),
                user("coordinador"), Instant.now());
        database.update("insert into governance_finding(id,process_id,registered_by,registered_at,payload_json)"
                        + " values(?,?,?,?,?)", registered.id(), PROCESS, registered.createdBy(),
                Timestamp.from(registered.createdAt()), mapper.writeValueAsString(registered));
    }

    private long count(String sql, Object... arguments) {
        Long total = database.queryForObject(sql, Long.class, arguments);
        return total == null ? 0 : total;
    }

    private static String sha(String content) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(content.getBytes(StandardCharsets.UTF_8)));
    }

    private static UUID user(String login) {
        return UUID.nameUUIDFromBytes(("sec-unl:" + login).getBytes(StandardCharsets.UTF_8));
    }
}
