package ec.edu.unl.sec.evidence.infrastructure;

import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("sqlite-test")
@Order(-1)
public class SqliteEvidenceGuards implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    public SqliteEvidenceGuards(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override public void run(ApplicationArguments arguments) {
        // Execute trigger bodies directly, as the SQL script splitter splits their semicolons.
        jdbc.execute("""
                CREATE TRIGGER IF NOT EXISTS evidence_version_insert_guard BEFORE INSERT ON evidence_version
                WHEN NEW.status <> 'DRAFT' OR NEW.submitted_at IS NOT NULL
                BEGIN SELECT RAISE(ABORT, 'evidence must be created as a draft'); END
                """);
        jdbc.execute("""
                CREATE TRIGGER IF NOT EXISTS evidence_version_delete_guard BEFORE DELETE ON evidence_version
                WHEN OLD.status = 'SUBMITTED'
                BEGIN SELECT RAISE(ABORT, 'submitted evidence version is immutable'); END
                """);
        jdbc.execute("""
                CREATE TRIGGER IF NOT EXISTS evidence_submitted_delete_guard BEFORE DELETE ON evidence
                WHEN EXISTS (SELECT 1 FROM evidence_version WHERE evidence_id=OLD.id AND status='SUBMITTED')
                BEGIN SELECT RAISE(ABORT, 'submitted evidence identity is immutable'); END
                """);
        jdbc.execute("""
                CREATE TRIGGER IF NOT EXISTS evidence_submitted_identity_guard BEFORE UPDATE ON evidence
                WHEN EXISTS (SELECT 1 FROM evidence_version WHERE evidence_id=OLD.id AND status='SUBMITTED')
                    AND (NEW.id IS NOT OLD.id OR NEW.process_id IS NOT OLD.process_id OR NEW.owner_id IS NOT OLD.owner_id
                        OR NEW.created_at IS NOT OLD.created_at OR NEW.correlation_id IS NOT OLD.correlation_id)
                BEGIN SELECT RAISE(ABORT, 'submitted evidence identity is immutable'); END
                """);
        String frozenFields = List.of("id", "evidence_id", "revision_number", "predecessor_id", "title", "description",
                        "provenance", "covered_from", "covered_to", "author_id", "created_at").stream()
                .map(field -> "NEW." + field + " IS NOT OLD." + field).collect(java.util.stream.Collectors.joining(" OR "));
        jdbc.execute("""
                CREATE TRIGGER IF NOT EXISTS evidence_version_update_guard BEFORE UPDATE ON evidence_version
                WHEN OLD.status = 'SUBMITTED' OR NEW.status NOT IN ('DRAFT','SUBMITTED')
                    OR (NEW.status = 'DRAFT' AND NEW.submitted_at IS NOT NULL)
                    OR (NEW.status = 'SUBMITTED' AND (NEW.submitted_at IS NULL
                        OR NEW.lock_revision <> OLD.lock_revision+1 OR %s))
                BEGIN SELECT RAISE(ABORT, 'submitted evidence version is immutable'); END
                """.formatted(frozenFields));
        for (String table : List.of("evidence_version_indicator", "evidence_version_file", "evidence_external_reference",
                "source_coverage", "evidence_structured_value")) {
            for (String operation : List.of("INSERT", "UPDATE", "DELETE")) {
                String condition = operation.equals("INSERT") ? "" : "COALESCE((SELECT status FROM evidence_version WHERE id=OLD.version_id),'') <> 'DRAFT'";
                if (!operation.equals("DELETE")) {
                    condition += condition.isEmpty() ? "" : " OR ";
                    condition += "COALESCE((SELECT status FROM evidence_version WHERE id=NEW.version_id),'') <> 'DRAFT'";
                }
                jdbc.execute("CREATE TRIGGER IF NOT EXISTS " + table + "_" + operation.toLowerCase(java.util.Locale.ROOT)
                        + "_submitted_guard BEFORE " + operation + " ON " + table + " WHEN " + condition
                        + " BEGIN SELECT RAISE(ABORT, 'submitted evidence children are immutable'); END");
            }
        }
    }
}
