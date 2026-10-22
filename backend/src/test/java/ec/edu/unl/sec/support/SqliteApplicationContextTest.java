package ec.edu.unl.sec.support;

import static org.assertj.core.api.Assertions.assertThat;
import ec.edu.unl.sec.identityaccess.application.SessionService;
import ec.edu.unl.sec.identityaccess.application.AuthorizationPort;
import ec.edu.unl.sec.identityaccess.domain.AuthorizationScope;
import ec.edu.unl.sec.identityaccess.domain.OperationCode;
import ec.edu.unl.sec.audit.application.AuditQueryPort;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@ActiveProfiles("sqlite-test")
class SqliteApplicationContextTest {
    private static final String DATABASE = "sqlite-context-" + UUID.randomUUID();

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:file:" + DATABASE + "?mode=memory&cache=shared");
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired SessionService sessions;
    @Autowired AuthorizationPort authorization;
    @Autowired AuditQueryPort audit;

    @Test
    void startsWithSyntheticPilotData() {
        assertThat(jdbc.queryForObject("select count(*) from access_identity", Integer.class)).isEqualTo(5);
        assertThat(jdbc.queryForObject("select count(*) from self_assessment_process", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from process_assignment", Integer.class)).isEqualTo(2);
    }

    @Test
    void authenticatesAndUsesTheSessionAgainstSqlite() {
        var login = sessions.login("admin", "Synthetic-Admin-2026", "127.0.0.1", UUID.randomUUID());

        assertThat(sessions.authenticateForRequest(login.token()))
                .isPresent()
                .get()
                .extracting(SessionService.AuthenticatedSession::displayName)
                .isEqualTo("Administrador Sintetico");
        assertThat(audit.search(List.of(), true, null, null, 0, 20)).isNotEmpty();
        sessions.logout(login.token(), login.identityId(), UUID.randomUUID());
        assertThat(sessions.authenticateForRequest(login.token())).isEmpty();
    }

    @Test
    void responsibleCanCollectWithoutAFalseFunctionConflict() {
        UUID responsible = UUID.nameUUIDFromBytes("sec-unl:responsable".getBytes(StandardCharsets.UTF_8));
        assertThat(authorization.isAllowed(
                responsible,
                OperationCode.EVIDENCE_COLLECT,
                new AuthorizationScope(
                        UUID.fromString("11111111-1111-1111-1111-111111111111"),
                        UUID.fromString("40000000-0000-0000-0000-000000000001"),
                        UUID.fromString("50000000-0000-0000-0000-000000000001"))))
                .isTrue();
    }
}
