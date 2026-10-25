package ec.edu.unl.sec.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.unl.sec.identityaccess.infrastructure.SyntheticAccessData;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.boot.*;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Synthetic actors only on the explicit reconciliation test profile. No production authority is granted. */
@Component @Profile("d7-reconciliation-test") @Order(4)
public class D7ReconciliationData implements ApplicationRunner {
    public static final UUID FUNCTION=UUID.fromString("d7000000-0000-0000-0000-000000000900");
    public static final UUID ASSIGNMENT=UUID.fromString("d7000000-0000-0000-0000-000000000901");
    private final JdbcTemplate jdbc;private final ObjectMapper json;private final PasswordEncoder passwords;
    public D7ReconciliationData(JdbcTemplate jdbc,ObjectMapper json,PasswordEncoder passwords){this.jdbc=jdbc;this.json=json;this.passwords=passwords;}
    @Override public void run(ApplicationArguments args)throws Exception {
        new D7ManualEvaluationData(jdbc,json).run(null);
        jdbc.update("insert into process_function(id,code,name) values(?,?,?) on conflict do nothing",FUNCTION,"ACADEMIC_COMMITTEE","Comité sintético de conciliación");
        for(String operation:List.of("ACCESS_READ_SELF","CAREER_READ","INDICATOR_EVALUATE","ACADEMIC_RECONCILE"))
            jdbc.update("insert into function_operation(function_id,operation_code) values(?,?) on conflict do nothing",FUNCTION,operation);
        identity("evaluador2");identity("conciliador");
        assignment(UUID.fromString("d7000000-0000-0000-0000-000000000902"),"evaluador2",UUID.fromString("20000000-0000-0000-0000-000000000005"));
        assignment(ASSIGNMENT,"conciliador",FUNCTION);
    }
    private void identity(String login) {
        var id=user(login);if(jdbc.queryForObject("select count(*) from access_identity where id=?",Long.class,id)>0)return;
        jdbc.update("insert into access_identity(id,login_normalized,display_name,identity_type,active,revision,created_at) values(?,?,?,'HUMAN',true,0,?)",id,login,login+" sintético",Timestamp.from(Instant.now()));
        jdbc.update("insert into access_credential(id,identity_id,bcrypt_hash,active,created_at) values(?,?,?,true,?)",UUID.randomUUID(),id,passwords.encode("Synthetic-User-2026"),Timestamp.from(Instant.now()));
    }
    private void assignment(UUID id,String login,UUID function) {
        jdbc.update("insert into functional_assignment(id,identity_id,function_id,career_id,process_id,indicator_id,valid_from,valid_to,granted_by,granted_at,revision) values(?,?,?,?,?,null,?,?,?,?,0) on conflict do nothing",
                id,user(login),function,SyntheticAccessData.PILOT_CAREER,D7ManualEvaluationData.PROCESS,Timestamp.from(Instant.parse("2020-01-01T00:00:00Z")),
                Timestamp.from(Instant.parse("2030-01-01T00:00:00Z")),user("admin"),Timestamp.from(Instant.now()));
    }
    private static UUID user(String login){return UUID.nameUUIDFromBytes(("sec-unl:"+login).getBytes(StandardCharsets.UTF_8));}
}
