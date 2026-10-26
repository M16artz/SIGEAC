package ec.edu.unl.sec.archive.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.unl.sec.archive.domain.IntegralClosure;
import ec.edu.unl.sec.support.PostgresTestSupport;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

@EnabledIfEnvironmentVariable(named = "SEC_RUN_FILE_DATABASE_TESTS", matches = "true")
class IntegralClosureBytesTest {
    @Test void postgresRoundTripPreservesSealedUtf8BytesIncludingWhitespaceAndKeyOrder() throws Exception {
        var source = new SingleConnectionDataSource(PostgresTestSupport.jdbcUrl(),
                PostgresTestSupport.username(), PostgresTestSupport.password(), true);
        try {
            var jdbc = new JdbcTemplate(source);
            jdbc.execute("create temporary table integral_closure(id uuid,process_id uuid,documentary_closure_id uuid,"
                    + "policy_version text,payload_json text,sha256 text,pending_ids text,exception_decision_ids text,"
                    + "closed_by uuid,closed_at timestamptz,revision bigint)");
            String payload = "{\"z\": \"evaluación sintética\", \"a\": {\"b\":2,\"a\":1}}";
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(payload.getBytes(StandardCharsets.UTF_8)));
            var closure = new IntegralClosure(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                    "INTEGRAL_STRICT_V1", payload, hash, List.of(), List.of(), UUID.randomUUID(), Instant.now());
            var repository = new JdbcIntegralClosureRepository(jdbc, new ObjectMapper());
            repository.append(closure);
            var loaded = repository.find(closure.id()).orElseThrow();
            assertThat(loaded.payloadJson()).isEqualTo(payload);
            assertThat(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(loaded.payloadJson().getBytes(StandardCharsets.UTF_8)))).isEqualTo(loaded.sha256());
        } finally { source.destroy(); }
    }
}
