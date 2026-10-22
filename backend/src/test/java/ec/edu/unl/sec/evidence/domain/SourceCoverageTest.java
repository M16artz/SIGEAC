package ec.edu.unl.sec.evidence.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SourceCoverageTest {
    private static final UUID VERSION = UUID.fromString("00000000-0000-0000-0000-000000000501");
    private static final UUID SOURCE = UUID.fromString("00000000-0000-0000-0000-000000000601");
    private static final UUID ACTOR = UUID.fromString("00000000-0000-0000-0000-000000000101");

    @Test
    void completesIdentityAndTimestampsWithoutAssumingThem() {
        var coverage = new SourceCoverage(null, VERSION, SOURCE, "Nota sintética de cobertura", ACTOR, null);
        assertThat(coverage.id()).isNotNull();
        assertThat(coverage.declaredAt()).isNotNull();
        assertThat(coverage.versionId()).isEqualTo(VERSION);
        assertThat(coverage.requiredSourceId()).isEqualTo(SOURCE);
        assertThat(coverage.coverageNote()).isEqualTo("Nota sintética de cobertura");
    }

    @Test
    void rejectsBlankNotesAndOversizedNotes() {
        assertThatThrownBy(() -> new SourceCoverage(null, VERSION, SOURCE, "   ", ACTOR, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SourceCoverage(null, VERSION, SOURCE, "x".repeat(4001), ACTOR, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(new SourceCoverage(null, VERSION, SOURCE, "  ".repeat(1) + "nota  ", ACTOR, Instant.now())
                .coverageNote()).isEqualTo("nota");
    }

    @Test
    void requiresVersionSourceAndDeclarant() {
        assertThatThrownBy(() -> new SourceCoverage(null, null, SOURCE, "nota", ACTOR, Instant.now()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new SourceCoverage(null, VERSION, null, "nota", ACTOR, Instant.now()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new SourceCoverage(null, VERSION, SOURCE, "nota", null, Instant.now()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void requiresCompleteAuthorityProvenanceForApplicabilityDecisions() {
        var decidedAt = Instant.parse("2026-10-20T15:00:00Z");
        var decision = new ApplicabilityDecision(null, UUID.randomUUID(),
                ApplicabilityDecision.SubjectType.REQUIRED_SOURCE, SOURCE,
                ApplicabilityDecision.Decision.NOT_APPLICABLE, "Regla unidad", "Justificación completa",
                "Reglamento sintético", "fixture://decision", ACTOR, decidedAt, null, 0);
        assertThat(decision.decidedAt()).isEqualTo(decidedAt);
        assertThatThrownBy(() -> new ApplicabilityDecision(null, UUID.randomUUID(),
                ApplicabilityDecision.SubjectType.INDICATOR, SOURCE,
                ApplicabilityDecision.Decision.APPLICABLE, "Regla", "corta",
                "fundamento", "fixture://decision", ACTOR, decidedAt, null, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(new ApplicabilityDecision(UUID.randomUUID(), UUID.randomUUID(),
                ApplicabilityDecision.SubjectType.INDICATOR, SOURCE,
                ApplicabilityDecision.Decision.APPLICABLE, "Regla", "Justificación previa vigente",
                "fundamento", "fixture://decision", ACTOR, decidedAt, UUID.randomUUID(), 2)
                .supersedes(null)).isFalse();
    }
}
