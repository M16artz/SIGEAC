package ec.edu.unl.sec.academicevaluation.infrastructure;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("sqlite-test")
@Order(-1)
public class SqliteManualEvaluationGuards implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    public SqliteManualEvaluationGuards(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    @Override public void run(ApplicationArguments args) {
        // SQLite trigger bodies cannot be parsed by Spring's schema statement splitter.
        for(String table:new String[]{"manual_academic_evaluation","quantitative_input_set","quantitative_verification","quantitative_calculation",
                "academic_reconciliation_policy","academic_disagreement_record","academic_reconciliation_resolution"})
            for (String operation : new String[]{"update", "delete"})
                jdbc.execute("create trigger if not exists "+table+"_no_"+operation+" before "+operation
                        +" on "+table+" begin select raise(abort,'ACADEMIC_RECORD_IMMUTABLE'); end");
    }
}
