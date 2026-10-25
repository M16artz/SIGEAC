package ec.edu.unl.sec.evidence.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EvidenceDraftTest {
    UUID evidence = UUID.randomUUID();
    UUID version = UUID.randomUUID();
    UUID indicator = UUID.randomUUID();
    UUID actor = UUID.randomUUID();

    @Test
    void createsStableLineageAndValidDraft() {
        var root = new Evidence(null, UUID.randomUUID(), actor, Instant.now(), UUID.randomUUID(), 0);
        var draft = draft(List.of(new VersionIndicator(version, indicator, "I-1", "Indicador", 1)));

        assertThat(root.latestRevision()).isZero();
        assertThat(draft.revisionNumber()).isEqualTo(1);
        assertThat(draft.status()).isEqualTo(EvidenceVersionStatus.DRAFT);
    }

    @Test
    void rejectsInvalidRangesDatesPredecessorAndDuplicateIndicators() {
        var item = new VersionIndicator(version, indicator, "I", "Indicador", 1);
        assertThatThrownBy(() -> draft(List.of(item, new VersionIndicator(version, indicator, "I", "Indicador", 2))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EvidenceVersion(version, evidence, 1, UUID.randomUUID(),
                EvidenceVersionStatus.DRAFT, "T", "D", "P", LocalDate.now(), LocalDate.now(), actor,
                Instant.now(), null, 0, List.of(item))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EvidenceVersion(version, evidence, 1, null, EvidenceVersionStatus.DRAFT,
                "T", "D", "P", LocalDate.now(), LocalDate.now().minusDays(1), actor, Instant.now(), null, 0,
                List.of(item))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void submittedDraftCannotBeUpdated() {
        var item = new VersionIndicator(version, indicator, "I", "Indicador", 1);
        var submitted = submitted(item);

        assertThatThrownBy(() -> submitted.update("T", "D", "P", LocalDate.now(), LocalDate.now(), List.of(item)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void submittedVersionCreatesSequentialDraftSuccessorWithoutChangingPredecessor() {
        var now = Instant.now();
        var item = new VersionIndicator(version, indicator, "I", "Indicador", 1);
        var submitted = submitted(item);
        var successorId = UUID.randomUUID();
        var successor = submitted.successor(successorId, UUID.randomUUID(), now.plusSeconds(1));

        assertThat(successor.evidenceId()).isEqualTo(evidence);
        assertThat(successor.revisionNumber()).isEqualTo(2);
        assertThat(successor.predecessorId()).isEqualTo(version);
        assertThat(successor.status()).isEqualTo(EvidenceVersionStatus.DRAFT);
        assertThat(successor.lockRevision()).isZero();
        assertThat(successor.indicators()).containsExactly(new VersionIndicator(successorId, indicator, "I", "Indicador", 1));
        assertThat(successor.title()).isEqualTo(submitted.title());
        assertThat(successor.authorId()).isNotEqualTo(submitted.authorId());
        assertThat(submitted.revisionNumber()).isEqualTo(1);
        assertThatThrownBy(() -> draft(List.of(item)).successor(successorId, actor, now))
                .isInstanceOf(IllegalStateException.class);
    }

    private EvidenceVersion submitted(VersionIndicator item) {
        var now = Instant.now();
        return new EvidenceVersion(version, evidence, 1, null, EvidenceVersionStatus.SUBMITTED,
                "T", "D", "P", LocalDate.now(), LocalDate.now(), actor, now, now, 1, List.of(item));
    }

    private EvidenceVersion draft(List<VersionIndicator> indicators) {
        return new EvidenceVersion(version, evidence, 1, null, EvidenceVersionStatus.DRAFT,
                "Título", "Descripción", "Procedencia", LocalDate.now(), LocalDate.now(), actor,
                Instant.now(), null, 0, indicators);
    }
}
