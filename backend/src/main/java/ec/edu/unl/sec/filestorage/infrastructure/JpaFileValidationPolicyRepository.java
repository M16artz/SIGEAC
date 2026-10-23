package ec.edu.unl.sec.filestorage.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.unl.sec.filestorage.application.FileValidationPolicyRepository;
import ec.edu.unl.sec.filestorage.domain.FileValidationPolicy;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JpaFileValidationPolicyRepository implements FileValidationPolicyRepository {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public JpaFileValidationPolicyRepository(JdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Override public Optional<FileValidationPolicy> findActiveByCode(String code) {
        return jdbc.query("select * from file_validation_policy where policy_code=? and active=true",
                (row, number) -> map(row), code).stream().findFirst();
    }

    @Override public Optional<FileValidationPolicy> findById(UUID id) {
        return jdbc.query("select * from file_validation_policy where id=?",
                (row, number) -> map(row), id).stream().findFirst();
    }

    private FileValidationPolicy map(ResultSet row) throws SQLException {
        try {
            return new FileValidationPolicy(UUID.fromString(row.getString("id")), row.getString("policy_code"),
                    row.getLong("max_bytes"), row.getBoolean("allow_empty"),
                    json.readValue(row.getString("allowed_detected_types"), new TypeReference<Set<String>>() {}),
                    json.readValue(row.getString("compatibility_json"), new TypeReference<Map<String, Set<String>>>() {}));
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("FILE_POLICY_DESERIALIZATION_FAILED", error);
        }
    }
}
