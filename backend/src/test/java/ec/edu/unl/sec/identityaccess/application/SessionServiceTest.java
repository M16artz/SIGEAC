package ec.edu.unl.sec.identityaccess.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.edu.unl.sec.identityaccess.support.InMemoryIdentityAccessRepository;
import ec.edu.unl.sec.identityaccess.support.TestSupport;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SessionServiceTest {
    private InMemoryIdentityAccessRepository repository;
    private TestSupport.MutableClock clock;
    private TestSupport.RecordingAuditPort audit;
    private SessionService sessions;

    @BeforeEach
    void setUp() {
        repository = new InMemoryIdentityAccessRepository();
        TestSupport.admin(repository);
        clock = TestSupport.clock();
        audit = TestSupport.audit();
        sessions = new SessionService(
                repository, TestSupport.properties(), new IdentityAccessAuditService(audit, clock), clock);
    }

    @Test
    void createsOpaqueSessionAndNeverStoresTheRawToken() {
        SessionService.CreatedSession created =
                sessions.login("admin", TestSupport.ADMIN_PASSWORD, "127.0.0.1", UUID.randomUUID());

        assertThat(created.token()).isNotBlank();
        assertThat(created.csrfToken()).isNotBlank();
        assertThat(created.token()).isNotEqualTo(created.csrfToken());
        assertThat(created.expiresAt()).isEqualTo(TestSupport.NOW.plus(Duration.ofMinutes(30)));
        assertThat(repository.findSession(created.token())).isEmpty();
        assertThat(repository.findSession(SessionService.sha256Hex(created.token()))).isPresent();
        assertThat(audit.accepted).extracting(ec.edu.unl.sec.audit.application.AuditEventCommand::action).contains("SESSION_LOGIN");
    }

    @Test
    void authenticatesAndExtendsTheSessionOnActivity() {
        SessionService.CreatedSession created =
                sessions.login("admin", TestSupport.ADMIN_PASSWORD, "127.0.0.1", UUID.randomUUID());
        clock.advance(Duration.ofMinutes(29));

        Optional<SessionService.AuthenticatedSession> authenticated =
                sessions.authenticateForRequest(created.token());

        assertThat(authenticated).isPresent();
        assertThat(authenticated.orElseThrow().identityId()).isEqualTo(TestSupport.ADMIN);
        assertThat(authenticated.orElseThrow().csrfHash()).isEqualTo(SessionService.sha256Hex(created.csrfToken()));
    }

    @Test
    void rejectsSessionAfterThirtyMinutesOfInactivity() {
        SessionService.CreatedSession created =
                sessions.login("admin", TestSupport.ADMIN_PASSWORD, "127.0.0.1", UUID.randomUUID());
        clock.advance(Duration.ofMinutes(31));

        assertThat(sessions.authenticateForRequest(created.token())).isEmpty();
    }

    @Test
    void logoutRevokesTheSessionImmediately() {
        SessionService.CreatedSession created =
                sessions.login("admin", TestSupport.ADMIN_PASSWORD, "127.0.0.1", UUID.randomUUID());

        sessions.logout(created.token(), created.identityId(), UUID.randomUUID());

        assertThat(sessions.authenticateForRequest(created.token())).isEmpty();
        assertThat(audit.accepted).extracting(ec.edu.unl.sec.audit.application.AuditEventCommand::action).contains("SESSION_LOGOUT");
    }

    @Test
    void locksAfterFiveFailuresInsideTheWindowAndKeepsResponsesUniform() {
        for (int attempt = 1; attempt <= 4; attempt++) {
            assertThatThrownBy(() -> sessions.login("admin", "wrong-password", "127.0.0.1", UUID.randomUUID()))
                    .isInstanceOf(SessionService.AuthenticationRejected.class)
                    .satisfies(error -> assertThat(((SessionService.AuthenticationRejected) error).code())
                            .isEqualTo("AUTHENTICATION_FAILED"));
        }

        assertThatThrownBy(() -> sessions.login("admin", "wrong-password", "127.0.0.1", UUID.randomUUID()))
                .isInstanceOf(SessionService.AuthenticationRejected.class)
                .satisfies(error -> assertThat(((SessionService.AuthenticationRejected) error).code())
                        .isEqualTo("AUTHENTICATION_LIMITED"));

        assertThatThrownBy(() -> sessions.login("admin", TestSupport.ADMIN_PASSWORD, "127.0.0.1", UUID.randomUUID()))
                .isInstanceOf(SessionService.AuthenticationRejected.class)
                .satisfies(error -> assertThat(((SessionService.AuthenticationRejected) error).code())
                        .isEqualTo("AUTHENTICATION_LIMITED"));

        assertThat(audit.accepted)
                .filteredOn(event -> event.outcome().equals("REJECTED"))
                .extracting(ec.edu.unl.sec.audit.application.AuditEventCommand::reasonCode)
                .contains("AUTHENTICATION_FAILED", "AUTHENTICATION_LIMITED");
        assertThat(audit.rejected).isEmpty();
    }

    @Test
    void unknownAndWrongCredentialsShareTheSameFailureCode() {
        assertThatThrownBy(() -> sessions.login("missing", "wrong-password", "127.0.0.1", UUID.randomUUID()))
                .isInstanceOf(SessionService.AuthenticationRejected.class)
                .satisfies(error -> assertThat(((SessionService.AuthenticationRejected) error).code())
                        .isEqualTo("AUTHENTICATION_FAILED"));

        assertThatThrownBy(() -> sessions.login("admin", "wrong-password", "127.0.0.1", UUID.randomUUID()))
                .isInstanceOf(SessionService.AuthenticationRejected.class)
                .satisfies(error -> assertThat(((SessionService.AuthenticationRejected) error).code())
                        .isEqualTo("AUTHENTICATION_FAILED"));

        assertThat(audit.accepted)
                .extracting(ec.edu.unl.sec.audit.application.AuditEventCommand::outcome)
                .containsExactly("REJECTED", "REJECTED");
        assertThat(audit.rejected).isEmpty();
    }

    @Test
    void successfulLoginResetsConsecutiveFailuresEvenAtTheSameTimestamp() {
        for (int attempt = 0; attempt < 4; attempt++) {
            assertThatThrownBy(() -> sessions.login("admin", "wrong", "127.0.0.1", UUID.randomUUID()))
                    .extracting("code").isEqualTo("AUTHENTICATION_FAILED");
        }
        sessions.login("admin", TestSupport.ADMIN_PASSWORD, "127.0.0.1", UUID.randomUUID());
        for (int attempt = 0; attempt < 4; attempt++) {
            assertThatThrownBy(() -> sessions.login("admin", "wrong", "127.0.0.1", UUID.randomUUID()))
                    .extracting("code").isEqualTo("AUTHENTICATION_FAILED");
        }
        assertThatThrownBy(() -> sessions.login("admin", "wrong", "127.0.0.1", UUID.randomUUID()))
                .extracting("code").isEqualTo("AUTHENTICATION_LIMITED");
        clock.advance(Duration.ofMinutes(15).plusMillis(1));
        assertThat(sessions.login("admin", TestSupport.ADMIN_PASSWORD, "127.0.0.1", UUID.randomUUID())).isNotNull();
    }

    @Test
    void globalAdmissionRejectsDifferentAccountsAndSourcesBeforeRepositoryWork() {
        var properties = TestSupport.properties();
        properties.setLoginRequestsPerMinute(2);
        var observed = org.mockito.Mockito.spy(repository);
        sessions = new SessionService(observed, properties, new IdentityAccessAuditService(audit, clock), clock);
        for (int attempt = 0; attempt < 2; attempt++) {
            int number = attempt;
            assertThatThrownBy(() -> sessions.login("missing-" + number, "wrong", "192.0.2." + number, UUID.randomUUID()))
                    .extracting("code").isEqualTo("AUTHENTICATION_FAILED");
        }
        org.mockito.Mockito.clearInvocations(observed);
        assertThatThrownBy(() -> sessions.login("another-account", "wrong", "198.51.100.1", UUID.randomUUID()))
                .extracting("code").isEqualTo("AUTHENTICATION_LIMITED");
        org.mockito.Mockito.verifyNoInteractions(observed);
        clock.advance(Duration.ofSeconds(60));
        assertThatThrownBy(() -> sessions.login("another-account", "wrong", "198.51.100.1", UUID.randomUUID()))
                .extracting("code").isEqualTo("AUTHENTICATION_FAILED");
    }

    @Test
    void boundsConcurrentLoginsAndReleasesAdmissionAfterFailure() throws Exception {
        var properties = TestSupport.properties();
        properties.setMaxConcurrentLogins(1);
        var observed = org.mockito.Mockito.spy(repository);
        var entered = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        org.mockito.Mockito.doAnswer(call -> {
            entered.countDown();
            if (!release.await(5,java.util.concurrent.TimeUnit.SECONDS)) throw new AssertionError("Login did not resume");
            return Optional.empty();
        }).when(observed).findLogin("first");
        sessions = new SessionService(observed,properties,new IdentityAccessAuditService(audit,clock),clock);
        try (var executor = java.util.concurrent.Executors.newSingleThreadExecutor()) {
            var first = executor.submit(() -> {
                assertThatThrownBy(() -> sessions.login("first","wrong","192.0.2.1",UUID.randomUUID()))
                        .extracting("code").isEqualTo("AUTHENTICATION_FAILED");
            });
            try {
                assertThat(entered.await(5,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
                assertThatThrownBy(() -> sessions.login("second","wrong","192.0.2.2",UUID.randomUUID()))
                        .extracting("code").isEqualTo("AUTHENTICATION_LIMITED");
                org.mockito.Mockito.verify(observed,org.mockito.Mockito.never()).findLogin("second");
            } finally {
                release.countDown();
            }
            first.get(5,java.util.concurrent.TimeUnit.SECONDS);
            assertThatThrownBy(() -> sessions.login("second","wrong","192.0.2.2",UUID.randomUUID()))
                    .extracting("code").isEqualTo("AUTHENTICATION_FAILED");
        }
    }
}
