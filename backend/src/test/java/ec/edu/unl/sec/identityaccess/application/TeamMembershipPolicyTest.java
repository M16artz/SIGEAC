package ec.edu.unl.sec.identityaccess.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import ec.edu.unl.sec.identityaccess.domain.AuthorizationScope;
import ec.edu.unl.sec.identityaccess.domain.FunctionalAssignment;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TeamMembershipPolicyTest {
    private final UUID user = UUID.randomUUID(), role = UUID.randomUUID(), function = UUID.randomUUID();
    private final AuthorizationScope scope = new AuthorizationScope(UUID.randomUUID(), UUID.randomUUID(), null);
    private final Instant from = Instant.parse("2026-10-01T00:00:00Z"), to = from.plusSeconds(3600);
    private IdentityAccessRepository repository;
    private FunctionalAssignmentPolicyService policy;

    @BeforeEach void setup() {
        repository = mock(IdentityAccessRepository.class);
        when(repository.findUser(user)).thenReturn(Optional.of(
                new IdentityAccessRepository.UserRecord(user, "member", "Integrante", false, true, 0)));
        when(repository.activeAssignments(user, from)).thenReturn(List.of(assignment(scope, from, to, null)));
        when(repository.findConflictingPolicy(any(), any(), any(), any(), any(), any())).thenReturn(Optional.empty());
        policy = new FunctionalAssignmentPolicyService(repository);
    }

    @Test void acceptsOnlyTheMembersOwnRoleCoveringTheWholeMembership() {
        assertThat(policy.evaluateMembership(user, role, scope, from, to).allowed()).isTrue();
        assertThat(policy.evaluateMembership(user, UUID.randomUUID(), scope, from, to).reasonCode())
                .isEqualTo("MEMBERSHIP_ROLE_INVALID");
        assertThat(policy.evaluateMembership(user, role, scope, from, to.plusNanos(1)).allowed()).isFalse();
        assertThat(policy.evaluateMembership(user, role, scope, to, from).reasonCode()).isEqualTo("INVALID_VIGENCY");
    }

    @Test void rejectsInactiveTechnicalOrUnknownIdentities() {
        for (var record : List.of(
                new IdentityAccessRepository.UserRecord(user, "member", "Integrante", false, false, 0),
                new IdentityAccessRepository.UserRecord(user, "member", "Integrante", true, true, 0))) {
            when(repository.findUser(user)).thenReturn(Optional.of(record));
            assertThat(policy.evaluateMembership(user, role, scope, from, to).reasonCode()).isEqualTo("IDENTITY_NOT_ELIGIBLE");
        }
        when(repository.findUser(user)).thenReturn(Optional.empty());
        assertThat(policy.evaluateMembership(user, role, scope, from, to).allowed()).isFalse();
    }

    @Test void rejectsForeignRevokedFutureAndExpiredRoles() {
        for (var assignment : List.of(
                assignment(new AuthorizationScope(UUID.randomUUID(), null, null), from, to, null),
                assignment(new AuthorizationScope(scope.careerId(), UUID.randomUUID(), null), from, to, null),
                assignment(new AuthorizationScope(scope.careerId(), scope.processId(), UUID.randomUUID()), from, to, null),
                assignment(scope, from, to, from),
                assignment(scope, from.plusNanos(1), to, null),
                assignment(scope, from.minusSeconds(1), from, null),
                new FunctionalAssignment(role, UUID.randomUUID(), function, scope, from, to, user, null, 0))) {
            when(repository.activeAssignments(user, from)).thenReturn(List.of(assignment));
            assertThat(policy.evaluateMembership(user, role, scope, from, to).allowed()).isFalse();
        }
    }

    @Test void rejectsUnexceptedConflicts() {
        when(repository.findConflictingPolicy(eq(user), eq(function), eq(scope), eq(from), eq(to), any()))
                .thenReturn(Optional.of(UUID.randomUUID()));
        assertThat(policy.evaluateMembership(user, role, scope, from, to).reasonCode()).isEqualTo("FUNCTION_CONFLICT");
    }

    private FunctionalAssignment assignment(AuthorizationScope assignedScope, Instant start, Instant end, Instant revoked) {
        return new FunctionalAssignment(role, user, function, assignedScope, start, end, user, revoked, 0);
    }
}
