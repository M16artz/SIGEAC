package ec.edu.unl.sec.identityaccess.application;

import static org.assertj.core.api.Assertions.assertThat;

import ec.edu.unl.sec.identityaccess.domain.*;
import ec.edu.unl.sec.identityaccess.support.InMemoryIdentityAccessRepository;
import ec.edu.unl.sec.identityaccess.support.TestSupport;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ScopeAuthorizationServiceTest {
    private InMemoryIdentityAccessRepository repository;
    private TestSupport.MutableClock clock;
    private ScopeAuthorizationService authorization;

    @BeforeEach
    void setUp() {
        repository = new InMemoryIdentityAccessRepository();
        TestSupport.admin(repository);
        TestSupport.user(repository);
        clock = TestSupport.clock();
        authorization = new ScopeAuthorizationService(repository, clock);
    }

    @Test
    void allowsOnlyOperationsAndScopesActuallyAssigned() {
        assertThat(authorization.decide(
                                TestSupport.ADMIN,
                                OperationCode.ACCESS_MANAGE_USERS,
                                new AuthorizationScope(TestSupport.CAREER, null, null))
                        .allowed())
                .isTrue();
        assertThat(authorization.decide(
                                TestSupport.ADMIN,
                                OperationCode.ACCESS_MANAGE_USERS,
                                new AuthorizationScope(TestSupport.CAREER, TestSupport.PROCESS, null))
                        .allowed())
                .isTrue();
        assertThat(authorization.decide(
                                TestSupport.ADMIN,
                                OperationCode.EVIDENCE_COLLECT,
                                new AuthorizationScope(TestSupport.CAREER, null, null))
                        .allowed())
                .isFalse();
        assertThat(authorization.decide(
                                TestSupport.ADMIN,
                                OperationCode.ACCESS_MANAGE_USERS,
                                new AuthorizationScope(UUID.randomUUID(), null, null))
                        .allowed())
                .isFalse();
    }

    @Test
    void ignoresExpiredAndRevokedAssignments() {
        repository.addAssignment(new FunctionalAssignment(
                UUID.randomUUID(),
                TestSupport.USER,
                TestSupport.RESPONSIBLE_FUNCTION,
                new AuthorizationScope(TestSupport.CAREER, TestSupport.PROCESS, TestSupport.INDICATOR),
                TestSupport.NOW.minus(Duration.ofDays(10)),
                TestSupport.NOW.minus(Duration.ofDays(1)),
                TestSupport.ADMIN,
                null,
                0));

        assertThat(authorization.decide(
                                TestSupport.USER,
                                OperationCode.EVIDENCE_COLLECT,
                                new AuthorizationScope(
                                        TestSupport.CAREER, TestSupport.PROCESS, TestSupport.INDICATOR))
                        .allowed())
                .isFalse();
    }

    @Test
    void appliesConfiguredFunctionConflictsUnlessAnExceptionCoversTheScope() {
        UUID policyId = UUID.randomUUID();
        repository.addPolicy(new InMemoryIdentityAccessRepository.PolicyRecord(
                policyId,
                TestSupport.RESPONSIBLE_FUNCTION,
                TestSupport.REVIEWER_FUNCTION,
                true,
                TestSupport.NOW.minus(Duration.ofDays(30)),
                TestSupport.NOW.plus(Duration.ofDays(30))));
        repository.addAssignment(TestSupport.assignment(
                TestSupport.USER,
                TestSupport.RESPONSIBLE_FUNCTION,
                TestSupport.CAREER,
                TestSupport.PROCESS,
                TestSupport.INDICATOR));
        repository.addAssignment(TestSupport.assignment(
                TestSupport.USER,
                TestSupport.REVIEWER_FUNCTION,
                TestSupport.CAREER,
                TestSupport.PROCESS,
                TestSupport.INDICATOR));

        AuthorizationScope scope =
                new AuthorizationScope(TestSupport.CAREER, TestSupport.PROCESS, TestSupport.INDICATOR);
        assertThat(authorization.decide(TestSupport.USER, OperationCode.EVIDENCE_REVIEW, scope)
                        .allowed())
                .isFalse();

        repository.createConflictException(new ConflictException(
                UUID.randomUUID(),
                policyId,
                TestSupport.USER,
                scope,
                "Excepcion sintetica autorizada",
                TestSupport.NOW.minus(Duration.ofHours(1)),
                TestSupport.NOW.plus(Duration.ofHours(1)),
                TestSupport.ADMIN,
                null));

        assertThat(authorization.decide(TestSupport.USER, OperationCode.EVIDENCE_REVIEW, scope)
                        .allowed())
                .isTrue();
    }

    @Test
    void exposesCollectionCapabilityWithoutACareerScope() {
        assertThat(authorization.hasOperation(TestSupport.ADMIN, OperationCode.ACCESS_MANAGE_USERS))
                .isTrue();
        assertThat(authorization.hasOperation(TestSupport.USER, OperationCode.ACCESS_MANAGE_USERS))
                .isFalse();
    }
}
