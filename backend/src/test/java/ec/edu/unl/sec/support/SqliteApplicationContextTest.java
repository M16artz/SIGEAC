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
    @org.junit.jupiter.api.io.TempDir static java.nio.file.Path fileRoot;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:file:" + DATABASE + "?mode=memory&cache=shared");
        registry.add("sec.files.root", () -> fileRoot.toString());
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired SessionService sessions;
    @Autowired AuthorizationPort authorization;
    @Autowired AuditQueryPort audit;
    @Autowired ec.edu.unl.sec.evidence.application.EvidenceDraftService evidenceDrafts;
    @Autowired ec.edu.unl.sec.evidence.application.EvidenceSourceService evidenceSources;
    @Autowired ec.edu.unl.sec.evidence.api.EvidenceDraftController evidenceController;

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

    @Test
    void authorEditsReferencesWithSharedJpaJdbcTransactionsAndVersionReadsIncludeThem() {
        UUID responsible = UUID.nameUUIDFromBytes("sec-unl:responsable".getBytes(StandardCharsets.UTF_8));
        var security = org.springframework.security.core.context.SecurityContextHolder.getContext();
        security.setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                responsible, null, List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_HUMAN"))));
        try {
            var version = evidenceDrafts.create(new ec.edu.unl.sec.evidence.application.EvidenceDraftService.CreateCommand(
                    UUID.fromString("40000000-0000-0000-0000-000000000001"), "Referencia sintetica", "Descripcion sintetica",
                    "Fixture de integracion", java.time.LocalDate.of(2026, 1, 1), java.time.LocalDate.of(2026, 6, 30),
                    List.of(UUID.fromString("50000000-0000-0000-0000-000000000001"))), UUID.randomUUID()).latestVersion();
            var added = evidenceSources.addReference(version.id(), "\"0\"", "https://synthetic.invalid/document", "Declaracion");
            assertThat(added.revision()).isEqualTo(1);
            assertThat(evidenceDrafts.getVersion(version.id()).lockRevision()).isEqualTo(1);
            var response = evidenceController.getVersion(version.id());
            assertThat(response.getHeaders().getETag()).isEqualTo("\"1\"");
            assertThat(response.getBody().externalReferences()).hasSize(1);
            assertThat(response.getBody().externalReferences().getFirst().uri()).isEqualTo("https://synthetic.invalid/document");
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> evidenceSources.removeReference(version.id(), added.value().id(), "\"0\""))
                    .isInstanceOf(ec.edu.unl.sec.evidence.application.EvidenceProblemException.class);
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> evidenceSources.addReference(version.id(), "\"1\"", "relative/path", "Invalida"))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThat(evidenceDrafts.getVersion(version.id()).lockRevision()).isEqualTo(1);
            assertThat(evidenceSources.snapshot(version.id()).externalReferences()).hasSize(1);
            evidenceSources.updateReference(version.id(), added.value().id(), "\"1\"", "urn:synthetic:updated", "Actualizada");
            assertThat(evidenceSources.removeReference(version.id(), added.value().id(), "\"2\"")).isEqualTo(3);
            assertThat(evidenceSources.snapshot(version.id()).externalReferences()).isEmpty();
        } finally { org.springframework.security.core.context.SecurityContextHolder.clearContext(); }
    }
}
