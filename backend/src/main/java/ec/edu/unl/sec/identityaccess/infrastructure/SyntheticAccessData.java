package ec.edu.unl.sec.identityaccess.infrastructure;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Synthetic pilot catalog. It never contains institutional data and is disabled by default.
 */
@Component
@Order(1)
@ConditionalOnProperty(prefix = "sec", name = "demo-data", havingValue = "true")
public class SyntheticAccessData implements ApplicationRunner {
    public static final UUID PILOT_CAREER = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Instant OPEN_ENDED = Instant.parse("9999-12-31T00:00:00Z");

    private static final UUID FUNCTIONAL_ADMIN = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID COORDINATOR = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID RESPONSIBLE = UUID.fromString("20000000-0000-0000-0000-000000000003");
    private static final UUID REVIEWER = UUID.fromString("20000000-0000-0000-0000-000000000004");
    private static final UUID EVALUATOR = UUID.fromString("20000000-0000-0000-0000-000000000005");
    private static final UUID CONFLICT_POLICY = UUID.fromString("30000000-0000-0000-0000-000000000001");

    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;

    public SyntheticAccessData(JdbcTemplate jdbc, PasswordEncoder passwordEncoder) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        // Register the capability, never grant institutional authority implicitly to an existing user.
        function(UUID.fromString("20000000-0000-0000-0000-000000000006"), "PROCESS_AUTHORITY", "Autoridad de inicio del proceso", List.of("ACCESS_READ_SELF", "PROCESS_AUTHORIZE", "CAREER_READ", "MODEL_APPLICABILITY_AUTHORIZE"));
        functions();
        Long users = jdbc.queryForObject("select count(*) from access_identity", Long.class);
        if (users != null && users > 0) return;

        UUID admin = identity("admin", "Administrador Sintetico", "Synthetic-Admin-2026");
        UUID coordinator = identity("coordinador", "Coordinador Sintetico", "Synthetic-User-2026");
        UUID responsible = identity("responsable", "Responsable Sintetico", "Synthetic-User-2026");
        UUID reviewer = identity("revisor", "Revisor Sintetico", "Synthetic-User-2026");
        UUID evaluator = identity("evaluador", "Evaluador Sintetico", "Synthetic-User-2026");

        assignment(admin, admin, FUNCTIONAL_ADMIN);
        assignment(admin, coordinator, COORDINATOR);
        assignment(admin, responsible, RESPONSIBLE);
        assignment(admin, reviewer, REVIEWER);
        assignment(admin, evaluator, EVALUATOR);

        jdbc.update(
                "insert into conflict_policy(id,left_function_id,right_function_id,valid_from,valid_to,active) values(?,?,?,?,?,true)",
                CONFLICT_POLICY,
                RESPONSIBLE,
                REVIEWER,
                Timestamp.from(Instant.parse("2020-01-01T00:00:00Z")),
                Timestamp.from(OPEN_ENDED));
    }

    private void functions() {
        function(FUNCTIONAL_ADMIN, "FUNCTIONAL_ADMIN", "Administrador funcional", List.of(
                "CAREER_ADMIN", "CAREER_READ",
                "ACCESS_READ_SELF",
                "ACCESS_MANAGE_USERS",
                "ACCESS_ASSIGN_SCOPE",
                "ACCESS_MANAGE_EXCEPTIONS",
                "ACCESS_READ_AUDIT",
                "MODEL_MANAGE",
                "MODEL_READ",
                "MODEL_AUDIT_READ",
                "MODEL_APPLICABILITY_AUTHORIZE",
                "PROCESS_MANAGE"));
        function(COORDINATOR, "COORDINATOR", "Coordinador de autoevaluacion", List.of(
                "CAREER_READ", "ACCESS_READ_SELF", "ACCESS_READ_AUDIT", "MODEL_READ", "MODEL_PUBLISH", "MODEL_AUDIT_READ", "PROCESS_COORDINATE", "PROCESS_MANAGE", "PROGRESS_READ", "DOCUMENTARY_SNAPSHOT_CREATE", "PROCESS_DOCUMENTARY_CLOSE"));
        function(RESPONSIBLE, "RESPONSIBLE", "Responsable de evidencia", List.of(
                "CAREER_READ", "ACCESS_READ_SELF", "EVIDENCE_COLLECT"));
        function(REVIEWER, "REVIEWER", "Revisor documental", List.of(
                "CAREER_READ", "ACCESS_READ_SELF", "EVIDENCE_REVIEW"));
        function(EVALUATOR, "EVALUATOR", "Evaluador academico", List.of(
                "CAREER_READ", "ACCESS_READ_SELF", "INDICATOR_EVALUATE"));
    }

    private void function(UUID id, String code, String name, List<String> operations) {
        jdbc.update(
                "insert into process_function(id,code,name,active) values(?,?,?,true) on conflict (code) do nothing",
                id,
                code,
                name);
        for (String operation : operations) {
            jdbc.update(
                    "insert into function_operation(function_id,operation_code) values(?,?) on conflict do nothing",
                    id,
                    operation);
        }
    }

    private UUID identity(String login, String displayName, String password) {
        UUID id = UUID.nameUUIDFromBytes(("sec-unl:" + login).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        Instant now = Instant.now();
        jdbc.update(
                "insert into access_identity(id,login_normalized,display_name,identity_type,active,revision,created_at) values(?,?,?,?,true,0,?)",
                id,
                login,
                displayName,
                "HUMAN",
                Timestamp.from(now));
        jdbc.update(
                "insert into access_credential(id,identity_id,bcrypt_hash,active,created_at) values(?,?,?,true,?)",
                UUID.randomUUID(),
                id,
                passwordEncoder.encode(password),
                Timestamp.from(now));
        return id;
    }

    private void assignment(UUID grantedBy, UUID identityId, UUID functionId) {
        jdbc.update(
                "insert into functional_assignment(id,identity_id,function_id,career_id,process_id,indicator_id,valid_from,valid_to,granted_by,granted_at,revision) values(?,?,?,?,null,null,?,?,?,?,0)",
                UUID.randomUUID(),
                identityId,
                functionId,
                PILOT_CAREER,
                Timestamp.from(Instant.parse("2020-01-01T00:00:00Z")),
                Timestamp.from(OPEN_ENDED),
                grantedBy,
                Timestamp.from(Instant.now()));
    }
}
