package ec.edu.unl.sec.identityaccess.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.edu.unl.sec.identityaccess.support.TestSupport;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IdentityAccessAuditTest {
    private TestSupport.RecordingAuditPort port;
    private IdentityAccessAuditService audit;

    @BeforeEach
    void setUp() {
        port = TestSupport.audit();
        audit = new IdentityAccessAuditService(port, TestSupport.clock());
    }

    @Test
    void recordsAnAllowlistedActionWithCorrelationAndSafeMetadata() {
        UUID correlation = UUID.randomUUID();
        UUID eventId = audit.record(
                TestSupport.ADMIN,
                correlation,
                "SESSION_LOGIN",
                "ACCESS_SESSION",
                UUID.randomUUID(),
                TestSupport.CAREER,
                null,
                "SUCCESS",
                "AUTHENTICATED",
                Map.of(
                        "technical", false,
                        "password", "must-not-appear",
                        "tokenHash", "must-not-appear",
                        "subjectHmacSecret", "must-not-appear",
                        "nested", Map.of("ok", true)));

        assertThat(eventId).isNotNull();
        assertThat(port.accepted).hasSize(1);
        var command = port.accepted.getFirst();
        assertThat(command.action()).isEqualTo("SESSION_LOGIN");
        assertThat(command.correlationId()).isEqualTo(correlation);
        assertThat(command.actorUserId()).isEqualTo(TestSupport.ADMIN);
        assertThat(command.technicalAccountId()).isNull();
        assertThat(command.metadata())
                .containsEntry("technical", false)
                .containsEntry("nested", Map.of("ok", true))
                .doesNotContainKeys("password", "tokenHash", "subjectHmacSecret");
    }

    @Test
    void rejectionUsesTheIndependentRejectionPortAndRejectedOutcome() {
        UUID correlation = UUID.randomUUID();
        audit.rejection(
                IdentityAccessAuditService.SYSTEM_ACTOR,
                correlation,
                "SESSION_LOGIN_FAILED",
                "ACCESS_IDENTITY",
                null,
                null,
                null,
                "AUTHENTICATION_FAILED",
                Map.of("subjectKey", "synthetic"));

        assertThat(port.accepted).isEmpty();
        assertThat(port.rejected).hasSize(1);
        var command = port.rejected.getFirst();
        assertThat(command.outcome()).isEqualTo("REJECTED");
        assertThat(command.actorUserId()).isNull();
        assertThat(command.technicalAccountId()).isEqualTo(IdentityAccessAuditService.SYSTEM_ACTOR);
        assertThat(command.objectId()).isEqualTo(IdentityAccessAuditService.SYSTEM_ACTOR);
        assertThat(command.reasonCode()).isEqualTo("AUTHENTICATION_FAILED");
    }

    @Test
    void refusesActionsAndOutcomesOutsideTheAllowlist() {
        assertThatThrownBy(() -> audit.record(
                        TestSupport.ADMIN,
                        UUID.randomUUID(),
                        "ARBITRARY_ACTION",
                        "ACCESS_IDENTITY",
                        TestSupport.ADMIN,
                        null,
                        null,
                        "SUCCESS",
                        "CREATED",
                        Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported audit action");

        assertThatThrownBy(() -> audit.record(
                        TestSupport.ADMIN,
                        UUID.randomUUID(),
                        "IDENTITY_CREATED",
                        "ACCESS_IDENTITY",
                        TestSupport.ADMIN,
                        null,
                        null,
                        "UNKNOWN",
                        "CREATED",
                        Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unsupported audit outcome");

        assertThat(port.accepted).isEmpty();
        assertThat(port.rejected).isEmpty();
    }
}
