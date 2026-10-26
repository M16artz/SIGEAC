package ec.edu.unl.sec.identityaccess.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ec.edu.unl.sec.identityaccess.domain.*;
import ec.edu.unl.sec.identityaccess.support.InMemoryIdentityAccessRepository;
import ec.edu.unl.sec.identityaccess.support.TestSupport;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IdentityAdministrationServiceTest {
    private InMemoryIdentityAccessRepository repository;
    private TestSupport.MutableClock clock;
    private TestSupport.RecordingAuditPort audit;
    private IdentityAdministrationService service;
    private IdentityAdministrationService.Actor actor;

    @BeforeEach
    void setUp() {
        repository = new InMemoryIdentityAccessRepository();
        TestSupport.admin(repository);
        TestSupport.user(repository);
        clock = TestSupport.clock();
        audit = TestSupport.audit();
        var auditService = new IdentityAccessAuditService(audit, clock);
        var sessions = new SessionService(repository, TestSupport.properties(), auditService, clock);
        service = new IdentityAdministrationService(
                repository, new ScopeAuthorizationService(repository, clock), auditService, sessions, clock);
        actor = new IdentityAdministrationService.Actor(TestSupport.ADMIN, UUID.randomUUID());
    }

    @Test
    void revocationReauthorizesImmediatelyAndPreservesTheOriginalAssignment() {
        var assignment = TestSupport.assignment(TestSupport.USER, TestSupport.RESPONSIBLE_FUNCTION,
                TestSupport.CAREER, TestSupport.PROCESS, TestSupport.INDICATOR);
        repository.createAssignment(assignment);
        var auth = new ScopeAuthorizationService(repository, clock);
        assertThat(auth.isAllowed(TestSupport.USER, OperationCode.EVIDENCE_COLLECT, assignment.scope())).isTrue();
        assertThat(service.revokeAssignment(actor, TestSupport.USER, assignment.id(), 0,
                "Revocacion sintetica con motivo explicito", UUID.randomUUID())).isEqualTo(1);
        assertThat(auth.isAllowed(TestSupport.USER, OperationCode.EVIDENCE_COLLECT, assignment.scope())).isFalse();
        var retained = repository.findAssignment(assignment.id()).orElseThrow();
        assertThat(retained.scope()).isEqualTo(assignment.scope());
        assertThat(retained.validFrom()).isEqualTo(assignment.validFrom());
        assertThat(retained.validTo()).isEqualTo(assignment.validTo());
        assertThat(retained.revokedAt()).isEqualTo(clock.instant());
        assertThat(retained.revision()).isEqualTo(1);
        assertThat(audit.accepted).anyMatch(event -> event.action().equals("ASSIGNMENT_REVOKED"));
    }

    @Test
    void revocationChecksIdentityScopeAndRevisionBeforeChangingAnything() {
        var assignment = TestSupport.assignment(TestSupport.USER, TestSupport.RESPONSIBLE_FUNCTION,
                TestSupport.CAREER, TestSupport.PROCESS, TestSupport.INDICATOR);
        repository.createAssignment(assignment);
        assertThatThrownBy(() -> service.revokeAssignment(actor, UUID.randomUUID(), assignment.id(), 0,
                "Motivo sintetico de prueba", UUID.randomUUID())).isInstanceOf(IdentityNotFoundException.class);
        assertThatThrownBy(() -> service.revokeAssignment(actor, TestSupport.USER, assignment.id(), 9,
                "Motivo sintetico de prueba", UUID.randomUUID())).isInstanceOf(StaleIdentityRevisionException.class);
        var foreign = TestSupport.assignment(TestSupport.USER, TestSupport.RESPONSIBLE_FUNCTION, UUID.randomUUID(), null, null);
        repository.createAssignment(foreign);
        assertThatThrownBy(() -> service.revokeAssignment(actor, TestSupport.USER, foreign.id(), 0,
                "Motivo sintetico de prueba", UUID.randomUUID())).isInstanceOf(IdentityNotFoundException.class);
        assertThat(repository.findAssignment(assignment.id()).orElseThrow()).isEqualTo(assignment);
        assertThat(repository.findAssignment(foreign.id()).orElseThrow()).isEqualTo(foreign);
    }

    @Test
    void createsIdentitiesWithNormalizedLoginAndHashedPassword() {
        UUID id = service.createUser(
                actor, "  Responsable.Nuevo  ", "Responsable Nuevo", "Synthetic-Pass-2026", false, UUID.randomUUID());

        assertThat(repository.findUser(id)).isPresent();
        assertThat(repository.findUser(id).orElseThrow().login()).isEqualTo("responsable.nuevo");
        assertThat(repository.findLogin("responsable.nuevo").orElseThrow().bcryptHash()).startsWith("$2");
        assertThat(repository.findLogin("responsable.nuevo").orElseThrow().bcryptHash())
                .doesNotContain("Synthetic-Pass-2026");
        assertThat(audit.accepted).extracting("action").contains("IDENTITY_CREATED");
    }

    @Test
    void refusesDuplicateLogins() {
        service.createUser(actor, "duplicado", "Persona Uno", "Synthetic-Pass-2026", false, UUID.randomUUID());

        assertThatThrownBy(() -> service.createUser(
                        actor, "DUPLICADO", "Persona Dos", "Synthetic-Pass-2026", false, UUID.randomUUID()))
                .isInstanceOf(IdentityConflictException.class);
    }

    @Test
    void technicalAccountsCannotReceiveProcessFunctions() {
        UUID technical = service.createUser(
                actor, "tecnico", "Cuenta Tecnica", "Synthetic-Pass-2026", true, UUID.randomUUID());

        assertThatThrownBy(() -> service.assign(
                        actor,
                        technical,
                        "RESPONSIBLE",
                        TestSupport.CAREER,
                        null,
                        null,
                        TestSupport.NOW,
                        TestSupport.NOW.plus(Duration.ofDays(30)),
                        null,
                        UUID.randomUUID()))
                .isInstanceOf(IdentityConflictException.class)
                .satisfies(error -> assertThat(((IdentityConflictException) error).code())
                        .isEqualTo("TECHNICAL_CANNOT_HOLD_FUNCTION"));
    }

    @Test
    void createsScopedAssignmentAndAuditsIt() {
        UUID id = service.assign(
                actor,
                TestSupport.USER,
                "RESPONSIBLE",
                TestSupport.CAREER,
                TestSupport.PROCESS,
                TestSupport.INDICATOR,
                TestSupport.NOW,
                TestSupport.NOW.plus(Duration.ofDays(30)),
                null,
                UUID.randomUUID());

        assertThat(repository.assignments()).anyMatch(assignment -> assignment.id().equals(id));
        assertThat(audit.accepted).extracting("action").contains("ASSIGNMENT_CREATED");
    }

    @Test
    void rejectsIdenticalOverlappingAssignments() {
        service.assign(
                actor,
                TestSupport.USER,
                "RESPONSIBLE",
                TestSupport.CAREER,
                null,
                null,
                TestSupport.NOW,
                TestSupport.NOW.plus(Duration.ofDays(30)),
                null,
                UUID.randomUUID());

        assertThatThrownBy(() -> service.assign(
                        actor,
                        TestSupport.USER,
                        "RESPONSIBLE",
                        TestSupport.CAREER,
                        null,
                        null,
                        TestSupport.NOW.plus(Duration.ofDays(1)),
                        TestSupport.NOW.plus(Duration.ofDays(10)),
                        null,
                        UUID.randomUUID()))
                .isInstanceOf(IdentityConflictException.class)
                .satisfies(error -> assertThat(((IdentityConflictException) error).code())
                        .isEqualTo("ASSIGNMENT_OVERLAP"));
    }

    @Test
    void requiresAnAuditableExceptionForConfiguredFunctionConflicts() {
        repository.addPolicy(new InMemoryIdentityAccessRepository.PolicyRecord(
                UUID.randomUUID(),
                TestSupport.RESPONSIBLE_FUNCTION,
                TestSupport.REVIEWER_FUNCTION,
                true,
                TestSupport.NOW.minus(Duration.ofDays(30)),
                TestSupport.NOW.plus(Duration.ofDays(30))));
        service.assign(
                actor,
                TestSupport.USER,
                "RESPONSIBLE",
                TestSupport.CAREER,
                null,
                null,
                TestSupport.NOW,
                TestSupport.NOW.plus(Duration.ofDays(30)),
                null,
                UUID.randomUUID());

        assertThatThrownBy(() -> service.assign(
                        actor,
                        TestSupport.USER,
                        "REVIEWER",
                        TestSupport.CAREER,
                        null,
                        null,
                        TestSupport.NOW,
                        TestSupport.NOW.plus(Duration.ofDays(30)),
                        null,
                        UUID.randomUUID()))
                .isInstanceOf(IdentityConflictException.class)
                .satisfies(error -> assertThat(((IdentityConflictException) error).code())
                        .isEqualTo("FUNCTION_CONFLICT"));

        service.assign(
                actor,
                TestSupport.USER,
                "REVIEWER",
                TestSupport.CAREER,
                null,
                null,
                TestSupport.NOW,
                TestSupport.NOW.plus(Duration.ofDays(30)),
                "Excepcion sintetica autorizada por el piloto",
                UUID.randomUUID());

        assertThat(repository.exceptions()).hasSize(1);
        assertThat(repository.exceptions().getFirst().exception().approvedBy()).isEqualTo(TestSupport.ADMIN);
    }

    @Test
    void deniesAdministrationWithoutTheRequiredOperation() {
        var unauthorized = new IdentityAdministrationService.Actor(TestSupport.USER, UUID.randomUUID());

        assertThatThrownBy(() -> service.listUsers(unauthorized, 0, 20))
                .isInstanceOf(IdentityAccessDeniedException.class);
        assertThat(audit.rejected).extracting("reasonCode").contains("ACCESS_MANAGE_USERS");
    }

    @Test
    void detectsStaleRevisionsWhenChangingState() {
        UUID id = service.createUser(
                actor, "temporal", "Persona Temporal", "Synthetic-Pass-2026", false, UUID.randomUUID());

        assertThatThrownBy(() -> service.changeState(actor, id, false, 99, UUID.randomUUID()))
                .isInstanceOf(StaleIdentityRevisionException.class);
    }

    @Test
    void createsFiniteConflictExceptionsForTheAffectedIdentity() {
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
                null,
                null));

        UUID id = service.createException(
                actor,
                policyId,
                TestSupport.USER,
                TestSupport.CAREER,
                null,
                null,
                "Justificacion sintetica autorizada",
                TestSupport.NOW,
                TestSupport.NOW.plus(Duration.ofDays(2)),
                UUID.randomUUID());

        assertThat(repository.exceptions()).anyMatch(exception -> exception.exception().id().equals(id));
        assertThat(audit.accepted).extracting("action").contains("CONFLICT_EXCEPTION_CREATED");
    }

    @Test
    void refusesExceptionsForIdentitiesWithoutTheConflict() {
        UUID policyId = UUID.randomUUID();
        repository.addPolicy(new InMemoryIdentityAccessRepository.PolicyRecord(
                policyId,
                TestSupport.RESPONSIBLE_FUNCTION,
                TestSupport.REVIEWER_FUNCTION,
                true,
                TestSupport.NOW.minus(Duration.ofDays(30)),
                TestSupport.NOW.plus(Duration.ofDays(30))));

        assertThatThrownBy(() -> service.createException(
                        actor,
                        policyId,
                        TestSupport.USER,
                        TestSupport.CAREER,
                        null,
                        null,
                        "Justificacion sintetica autorizada",
                        TestSupport.NOW,
                        TestSupport.NOW.plus(Duration.ofDays(2)),
                        UUID.randomUUID()))
                .isInstanceOf(IdentityConflictException.class)
                .satisfies(error -> assertThat(((IdentityConflictException) error).code())
                        .isEqualTo("CONFLICT_NOT_APPLICABLE"));
    }
}

