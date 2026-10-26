package ec.edu.unl.sec.support;

import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Explicit test-only role for API acceptance; no production actor receives these powers. */
@Component
@Profile("complete-workflow-acceptance-test")
@Order(4)
public class CompleteWorkflowAcceptanceData implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    public CompleteWorkflowAcceptanceData(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Override public void run(ApplicationArguments args) {
        jdbc.update("insert into process_function(id,code,name,active) values(?,?,?,true) on conflict do nothing",
                java.util.UUID.fromString("f1250000-0000-0000-0000-000000000001"), "ACCEPTANCE_COMMITTEE", "Comite exclusivamente sintetico T125");
        for (String operation : List.of("ACCESS_READ_SELF", "CAREER_READ", "INDICATOR_EVALUATE", "ACADEMIC_RECONCILE", "PROGRESS_READ")) {
            jdbc.update("insert into function_operation(function_id,operation_code) values(?,?) on conflict do nothing",
                    java.util.UUID.fromString("f1250000-0000-0000-0000-000000000001"), operation);
        }
    }
}
