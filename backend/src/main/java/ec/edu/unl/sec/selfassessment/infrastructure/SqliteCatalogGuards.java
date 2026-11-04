package ec.edu.unl.sec.selfassessment.infrastructure;
import org.springframework.boot.*;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
@Component @Profile("sqlite-test") @Order(-1)
public class SqliteCatalogGuards implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    public SqliteCatalogGuards(JdbcTemplate jdbc){this.jdbc=jdbc;}
    public void run(ApplicationArguments args){
        // Spring's SQL script splitter cannot parse semicolons inside SQLite trigger bodies.
        for(String table:new String[]{"career","academic_unit","career_history","academic_unit_history"}) {
            jdbc.execute("create trigger if not exists "+table+"_no_delete before delete on "+table+" begin select raise(abort,'catalog history is retained'); end");
            if(table.endsWith("history"))jdbc.execute("create trigger if not exists "+table+"_no_update before update on "+table+" begin select raise(abort,'catalog history is append-only'); end");
        }
    }
}
