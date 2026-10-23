package ec.edu.unl.sec.filestorage.infrastructure;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

class JpaFileValidationPolicyRepositoryTest {
    @Test void readsOnlyActiveConfiguredPolicyWithoutRewritingHistoricalPolicy() {
        try (var dataSource = new SingleConnectionDataSource("jdbc:sqlite::memory:", true)) {
            new ResourceDatabasePopulator(new ClassPathResource("db/sqlite/schema.sql")).execute(dataSource);
            var jdbc = new JdbcTemplate(dataSource);
            var repository = new JpaFileValidationPolicyRepository(jdbc, new ObjectMapper());
            UUID oldId = UUID.randomUUID(), currentId = UUID.randomUUID();
            insert(jdbc, oldId, "synthetic-v1", false, false);
            insert(jdbc, currentId, "synthetic-v2", true, true);
            assertThat(repository.findActiveByCode("synthetic-v1")).isEmpty();
            assertThat(repository.findActiveByCode("unknown")).isEmpty();
            var current = repository.findActiveByCode("synthetic-v2").orElseThrow();
            assertThat(current.id()).isEqualTo(currentId);
            current.validate(0, "text/plain", "text/plain");
            var historic = repository.findById(oldId).orElseThrow();
            assertThat(historic.allowEmpty()).isFalse();
            assertThatThrownBy(() -> historic.validate(0, "text/plain", "text/plain")).hasMessage("FILE_EMPTY");
            assertThat(current.compatibility().get("text/plain")).containsExactly("text/plain");
        }
    }

    private static void insert(JdbcTemplate jdbc, UUID id, String code, boolean active, boolean allowEmpty) {
        jdbc.update("insert into file_validation_policy(id,policy_code,max_bytes,allow_empty,allowed_detected_types,"
                        + "compatibility_json,active,created_at) values(?,?,?,?,?,?,?,?)",
                id.toString(), code, 26_214_400, allowEmpty, "[\"text/plain\"]",
                "{\"text/plain\":[\"text/plain\"]}", active, "2026-10-22T00:00:00Z");
    }
}
