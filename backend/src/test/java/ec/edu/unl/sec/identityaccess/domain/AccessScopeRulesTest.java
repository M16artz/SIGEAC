package ec.edu.unl.sec.identityaccess.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AccessScopeRulesTest {
    private static final UUID CAREER = UUID.randomUUID();
    private static final UUID PROCESS = UUID.randomUUID();
    private static final UUID INDICATOR = UUID.randomUUID();

    @Test
    void aScopeContainsNarrowerScopesButNeverAWiderOne() {
        AuthorizationScope career = new AuthorizationScope(CAREER, null, null);
        AuthorizationScope process = new AuthorizationScope(CAREER, PROCESS, null);
        AuthorizationScope indicator = new AuthorizationScope(CAREER, PROCESS, INDICATOR);

        assertThat(career.contains(process)).isTrue();
        assertThat(career.contains(indicator)).isTrue();
        assertThat(process.contains(indicator)).isTrue();
        assertThat(indicator.contains(process)).isFalse();
        assertThat(process.contains(career)).isFalse();
    }

    @Test
    void indicatorWithoutProcessIsInvalid() {
        assertThatThrownBy(() -> new AuthorizationScope(CAREER, null, INDICATOR))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void conflictPoliciesAndExceptionsHonourVigency() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        UUID left = UUID.randomUUID();
        UUID right = UUID.randomUUID();
        ConflictPolicy policy = new ConflictPolicy(
                UUID.randomUUID(), left, right, now.minus(Duration.ofDays(1)), now.plus(Duration.ofDays(1)), true);

        assertThat(policy.activeAt(now)).isTrue();
        assertThat(policy.activeAt(now.plus(Duration.ofDays(2)))).isFalse();

        ConflictException exception = new ConflictException(
                UUID.randomUUID(),
                policy.id(),
                UUID.randomUUID(),
                new AuthorizationScope(CAREER, null, null),
                "Justificacion sintetica valida",
                now.minus(Duration.ofHours(1)),
                now.plus(Duration.ofHours(1)),
                UUID.randomUUID(),
                null);

        assertThat(exception.activeAt(now)).isTrue();
        assertThat(exception.activeAt(now.plus(Duration.ofHours(2)))).isFalse();
    }

    @Test
    void exceptionsRequireAJustificationOfAtLeastTenCharacters() {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        assertThatThrownBy(() -> new ConflictException(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        new AuthorizationScope(CAREER, null, null),
                        "corta",
                        now,
                        now.plus(Duration.ofDays(1)),
                        UUID.randomUUID(),
                        null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
