package ec.edu.unl.sec.archive.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IntegralClosurePolicyTest {
    private static final Instant NOW = Instant.parse("2026-06-01T00:00:00Z");
    private final UUID documentary = UUID.randomUUID();
    private final UUID action = UUID.randomUUID();
    private final IntegralClosurePolicy policy = IntegralClosurePolicy.of(IntegralClosurePolicy.INTEGRAL_STRICT_V1);

    @Test
    void acceptsStrictClosureWithApprovedFinalReportAndNoOverdueActions() {
        var evaluation = policy.evaluate(new IntegralClosurePolicy.Evidence("DOCUMENTARY_CLOSED", documentary,
                "APPROVED", List.of(new IntegralClosurePolicy.ActionSnapshot(action, "Acción", "VERIFIED",
                        NOW.minusSeconds(3600))), NOW, Set.of()));

        assertThat(evaluation.eligible()).isTrue();
        assertThat(evaluation.blockers()).isEmpty();
        assertThat(evaluation.warnings()).isEmpty();
    }

    @Test
    void blocksMissingDocumentaryClosureAndPendingFinalApproval() {
        var evaluation = policy.evaluate(new IntegralClosurePolicy.Evidence("ACTIVE", null, "FINAL",
                List.of(), NOW, Set.of()));

        assertThat(evaluation.eligible()).isFalse();
        assertThat(evaluation.blockers()).extracting(IntegralClosurePolicy.Blocker::code)
                .containsExactly("PROCESS_NOT_DOCUMENTARY_CLOSED", "DOCUMENTARY_CLOSURE_MISSING",
                        "FINAL_REPORT_NOT_APPROVED");
    }

    @Test
    void blocksOverdueActionAndReportsItsIdentifier() {
        var evaluation = policy.evaluate(new IntegralClosurePolicy.Evidence("DOCUMENTARY_CLOSED", documentary,
                "APPROVED", List.of(new IntegralClosurePolicy.ActionSnapshot(action, "Acción vencida",
                        "IN_PROGRESS", NOW.minusSeconds(60))), NOW, Set.of()));

        assertThat(evaluation.eligible()).isFalse();
        assertThat(evaluation.blockers()).singleElement().satisfies(blocker -> {
            assertThat(blocker.code()).isEqualTo("OVERDUE_ACTION");
            assertThat(blocker.subjectId()).isEqualTo(action);
        });
        assertThat(evaluation.pendingIds()).containsExactly(action);
    }

    @Test
    void authorizedExceptionWaivesOverdueActionButKeepsWarning() {
        var evaluation = policy.evaluate(new IntegralClosurePolicy.Evidence("DOCUMENTARY_CLOSED", documentary,
                "APPROVED", List.of(new IntegralClosurePolicy.ActionSnapshot(action, "Acción vencida",
                        "IN_PROGRESS", NOW.minusSeconds(60))), NOW, Set.of("OVERDUE_ACTION")));

        assertThat(evaluation.eligible()).isTrue();
        assertThat(evaluation.pendingIds()).isEmpty();
        assertThat(evaluation.warnings()).singleElement().asString().contains("excepciones");
    }

    @Test
    void warnsThatOpenActionsSurviveClosure() {
        var evaluation = policy.evaluate(new IntegralClosurePolicy.Evidence("DOCUMENTARY_CLOSED", documentary,
                "APPROVED", List.of(new IntegralClosurePolicy.ActionSnapshot(action, "Acción futura",
                        "IN_PROGRESS", NOW.plusSeconds(3600))), NOW, Set.of()));

        assertThat(evaluation.eligible()).isTrue();
        assertThat(evaluation.warnings()).singleElement().asString().contains("continúan");
    }
}
