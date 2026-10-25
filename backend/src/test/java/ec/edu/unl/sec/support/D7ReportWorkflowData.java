package ec.edu.unl.sec.support;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ec.edu.unl.sec.identityaccess.infrastructure.SyntheticAccessData;

/** Synthetic committee authority only on the explicit report test profile. No production authority is granted. */
@Component @Profile("d7-report-test") @Order(5)
public class D7ReportWorkflowData extends D7ReconciliationData {
    public static final UUID COMMITTEE_FUNCTION=UUID.fromString("d7000000-0000-0000-0000-000000000910");
    public static final UUID COMMITTEE_ASSIGNMENT=UUID.fromString("d7000000-0000-0000-0000-000000000911");
    public D7ReportWorkflowData(JdbcTemplate jdbc, com.fasterxml.jackson.databind.ObjectMapper json, PasswordEncoder passwords) {
        super(jdbc, json, passwords);
    }
    @Override public void run(ApplicationArguments args) throws Exception {
        super.run(args);
        database().update("insert into process_function(id,code,name) values(?,?,?) on conflict do nothing",
                COMMITTEE_FUNCTION,"REPORT_COMMITTEE","Comité sintético de decisión del informe");
        for(String operation:List.of("ACCESS_READ_SELF","CAREER_READ","PROGRESS_READ","REPORT_READ","REPORT_APPROVE"))
            database().update("insert into function_operation(function_id,operation_code) values(?,?) on conflict do nothing",
                    COMMITTEE_FUNCTION,operation);
        identity("presidente");
        database().update("insert into functional_assignment(id,identity_id,function_id,career_id,process_id,indicator_id,valid_from,valid_to,granted_by,granted_at,revision) values(?,?,?,?,?,null,?,?,?,?,0) on conflict do nothing",
                COMMITTEE_ASSIGNMENT,user("presidente"),COMMITTEE_FUNCTION,SyntheticAccessData.PILOT_CAREER,D7ManualEvaluationData.PROCESS,
                Timestamp.from(Instant.parse("2020-01-01T00:00:00Z")),Timestamp.from(Instant.parse("2030-01-01T00:00:00Z")),
                user("admin"),Timestamp.from(Instant.now()));
    }
    private void identity(String login) {
        var id=user(login);if(database().queryForObject("select count(*) from access_identity where id=?",Long.class,id)>0)return;
        database().update("insert into access_identity(id,login_normalized,display_name,identity_type,active,revision,created_at) values(?,?,?,'HUMAN',true,0,?)",id,login,login+" sintético",Timestamp.from(Instant.now()));
        database().update("insert into access_credential(id,identity_id,bcrypt_hash,active,created_at) values(?,?,?,true,?)",UUID.randomUUID(),id,encoder().encode("Synthetic-User-2026"),Timestamp.from(Instant.now()));
    }
    private static UUID user(String login){return UUID.nameUUIDFromBytes(("sec-unl:"+login).getBytes(StandardCharsets.UTF_8));}
}
