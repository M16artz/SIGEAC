package ec.edu.unl.sec.evidence.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EvidenceSuccessorTest {
    private static final UUID EVIDENCE = UUID.fromString("00000000-0000-0000-0000-000000000a01");
    private static final UUID SOURCE = UUID.fromString("00000000-0000-0000-0000-000000000a02");
    private static final UUID ACTOR = UUID.fromString("00000000-0000-0000-0000-000000000a03");

    @Test
    void successorHasSequentialRevisionAndDirectPredecessor() {
        var submitted = new EvidenceVersion(
                SOURCE, EVIDENCE, 1, null,
                EvidenceVersionStatus.SUBMITTED,
                "T", "D", "P",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30),
                ACTOR, Instant.parse("2026-10-20T15:00:00Z"), Instant.parse("2026-10-20T16:00:00Z"), 1,
                List.of(new VersionIndicator(SOURCE, UUID.randomUUID(), "IND-1", "I", 1)));
        assertThat(submitted.predecessorId()).isNull();

        var successor = new EvidenceVersion(
                UUID.randomUUID(), EVIDENCE, 2, SOURCE,
                EvidenceVersionStatus.DRAFT,
                submitted.title(), submitted.description(), submitted.provenance(),
                submitted.coveredFrom(), submitted.coveredTo(),
                ACTOR, Instant.parse("2026-10-20T17:00:00Z"), null, 0,
                List.of(new VersionIndicator(submitted.evidenceId(), UUID.randomUUID(), "IND-1", "I", 1)));
        assertThat(successor.revisionNumber()).isEqualTo(2);
        assertThat(successor.predecessorId()).isEqualTo(SOURCE);
        assertThat(successor.status()).isEqualTo(EvidenceVersionStatus.DRAFT);
    }
}
