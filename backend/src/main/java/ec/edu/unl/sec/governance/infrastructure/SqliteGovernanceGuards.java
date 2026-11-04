package ec.edu.unl.sec.governance.infrastructure;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("sqlite-test")
@Order(-1)
public class SqliteGovernanceGuards implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    public SqliteGovernanceGuards(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    @Override public void run(ApplicationArguments args) {
        // SQLite trigger bodies cannot be parsed by Spring's schema statement splitter.
        for(String table:new String[]{"governance_finding","governance_finding_consolidation","governance_consolidation_finding",
                "governance_report_version","governance_report_observation","governance_report_reply",
                "governance_report_socialization","governance_report_decision"})
            for (String operation : new String[]{"update", "delete"})
                jdbc.execute("create trigger if not exists "+table+"_no_"+operation+" before "+operation
                        +" on "+table+" begin select raise(abort,'GOVERNANCE_REPORT_IMMUTABLE'); end");
        jdbc.execute("create trigger if not exists governance_report_processing_no_delete before delete"
                +" on governance_report_processing begin select raise(abort,'GOVERNANCE_REPORT_IMMUTABLE'); end");
    }
}
