package ec.edu.unl.sec.reporting.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.unl.sec.documentreview.domain.DocumentReviewDecision;
import ec.edu.unl.sec.documentreview.domain.ReviewCoverageOutcome;
import ec.edu.unl.sec.documentreview.infrastructure.JdbcDocumentReviewProgressAdapter;
import ec.edu.unl.sec.documentreview.infrastructure.JdbcDocumentReviewRepository;
import ec.edu.unl.sec.evidence.domain.ApplicabilityDecision;
import ec.edu.unl.sec.evidence.domain.SourceCoverage;
import ec.edu.unl.sec.evidence.infrastructure.JdbcEvidenceGovernanceRepository;
import ec.edu.unl.sec.evidence.infrastructure.JdbcEvidenceProgressAdapter;
import ec.edu.unl.sec.identityaccess.infrastructure.SyntheticAccessData;
import ec.edu.unl.sec.selfassessment.infrastructure.JdbcCareerCatalogRepository;
import ec.edu.unl.sec.selfassessment.infrastructure.SyntheticCareerData;
import ec.edu.unl.sec.selfassessment.infrastructure.SyntheticPilotData;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class DocumentaryProgressPersistenceTest {
    private static final Instant NOW = Instant.parse("2026-10-24T12:00:00Z");
    private final UUID first = UUID.randomUUID(), second = UUID.randomUUID(), actor = UUID.randomUUID();
    private SingleConnectionDataSource dataSource;
    private JdbcTemplate jdbc;
    private JdbcEvidenceGovernanceRepository governance;
    private JdbcEvidenceProgressAdapter evidence;
    private JdbcDocumentReviewProgressAdapter reviews;

    @BeforeEach
    void setUp() {
        dataSource = new SingleConnectionDataSource("jdbc:sqlite::memory:", true);
        new ResourceDatabasePopulator(new ClassPathResource("db/sqlite/schema.sql")).execute(dataSource);
        jdbc = new JdbcTemplate(dataSource);
        var json = new ObjectMapper().findAndRegisterModules();
        new SyntheticCareerData(new JdbcCareerCatalogRepository(jdbc, json)).run(null);
        new SyntheticAccessData(jdbc, new BCryptPasswordEncoder(4)).run(null);
        new SyntheticPilotData(jdbc, json).run(null);
        governance = new JdbcEvidenceGovernanceRepository(jdbc);
        evidence = new JdbcEvidenceProgressAdapter(jdbc);
        reviews = new JdbcDocumentReviewProgressAdapter(jdbc);
    }

    @AfterEach
    void close() { dataSource.close(); }

    @Test
    void currentCoverageIgnoresDraftsButReplacesTheEntireSetOnSubmission() {
        var lineage = UUID.randomUUID();
        var original = seedVersion(lineage, SyntheticPilotData.PROCESS_ID, 1, null, "SUBMITTED");
        cover(original, first);
        cover(original, second);
        var successor = seedVersion(lineage, SyntheticPilotData.PROCESS_ID, 2, original, "DRAFT");
        cover(successor, second);
        var independent = seedVersion(UUID.randomUUID(), SyntheticPilotData.PROCESS_ID, 1, null, "SUBMITTED");
        cover(independent, first);
        var foreign = seedVersion(UUID.randomUUID(), UUID.randomUUID(), 1, null, "SUBMITTED");
        cover(foreign, first);

        var before = evidence.currentCoverages(SyntheticPilotData.PROCESS_ID, Set.of(first));
        assertThat(before).extracting(coverage -> coverage.versionId()).containsExactlyInAnyOrder(original, independent);
        jdbc.update("update evidence_version set status='SUBMITTED' where id=?", key(successor));
        var after = evidence.currentCoverages(SyntheticPilotData.PROCESS_ID, Set.of(first, second));
        assertThat(after).hasSize(2);
        assertThat(after).extracting(coverage -> coverage.versionId()).containsExactlyInAnyOrder(successor, independent);
        assertThat(after).noneMatch(coverage -> coverage.versionId().equals(original));
        assertThat(evidence.currentCoverages(SyntheticPilotData.PROCESS_ID, Set.of())).isEmpty();
    }

    @Test
    void latestReviewUsesRectificationsPerVersionAndSource() {
        var version = seedVersion(UUID.randomUUID(), SyntheticPilotData.PROCESS_ID, 1, null, "SUBMITTED");
        var other = seedVersion(UUID.randomUUID(), SyntheticPilotData.PROCESS_ID, 1, null, "SUBMITTED");
        var repository = new JdbcDocumentReviewRepository(jdbc);
        var outcomes = List.of(new ReviewCoverageOutcome(first, ReviewCoverageOutcome.Outcome.ACCEPTED, null),
                new ReviewCoverageOutcome(second, ReviewCoverageOutcome.Outcome.OBSERVED, "Pendiente de subsanación documental."));
        var original = new DocumentReviewDecision(null, version, DocumentReviewDecision.derive(outcomes),
                "Revisión documental sintética para verificar resultados por fuente.", actor, NOW,
                false, null, null, null, 0, outcomes);
        repository.save(original);
        var corrected = original.next("Rectificación que acepta las dos fuentes verificadas.", false, null,
                actor, NOW.plusSeconds(1), List.of(
                        new ReviewCoverageOutcome(first, ReviewCoverageOutcome.Outcome.ACCEPTED, null),
                        new ReviewCoverageOutcome(second, ReviewCoverageOutcome.Outcome.ACCEPTED, null)));
        repository.save(corrected);
        var result = reviews.latestOutcomes(Set.of(version, other));
        assertThat(result).hasSize(2).allSatisfy(outcome -> {
            assertThat(outcome.decisionId()).isEqualTo(corrected.id());
            assertThat(outcome.outcome()).isEqualTo("ACCEPTED");
        });
        assertThat(reviews.latestOutcomes(Set.of())).isEmpty();
    }

    @Test
    void newerApplicableDecisionCancelsExclusionWithoutDependingOnTimestampOrder() {
        var excluded = applicability(ApplicabilityDecision.SubjectType.REQUIRED_SOURCE, first,
                ApplicabilityDecision.Decision.NOT_APPLICABLE, 0, null, NOW.plusSeconds(2));
        governance.saveApplicability(excluded);
        governance.saveApplicability(applicability(ApplicabilityDecision.SubjectType.INDICATOR, second,
                ApplicabilityDecision.Decision.NOT_APPLICABLE, 0, null, NOW));
        assertThat(evidence.applicability(SyntheticPilotData.PROCESS_ID).excludedSourceIds()).containsExactly(first);
        governance.saveApplicability(applicability(ApplicabilityDecision.SubjectType.REQUIRED_SOURCE, first,
                ApplicabilityDecision.Decision.APPLICABLE, 1, excluded.id(), NOW));
        assertThat(evidence.applicability(SyntheticPilotData.PROCESS_ID).excludedSourceIds()).isEmpty();
        assertThat(evidence.applicability(SyntheticPilotData.PROCESS_ID).excludedIndicatorIds()).containsExactly(second);
        assertThat(evidence.applicability(UUID.randomUUID()).excludedIndicatorIds()).isEmpty();
    }

    private ApplicabilityDecision applicability(ApplicabilityDecision.SubjectType type, UUID subject,
            ApplicabilityDecision.Decision decision, long revision, UUID predecessor, Instant at) {
        return new ApplicabilityDecision(null, SyntheticPilotData.PROCESS_ID, type, subject, decision,
                "Regla sintética", "Decisión sintética con fundamento documental.", "Base sintética",
                "fixture://decision", actor, at, predecessor, revision);
    }

    private void cover(UUID version, UUID source) {
        governance.saveCoverage(new SourceCoverage(null, version, source, "Cobertura sintética explícita.", actor, NOW));
    }

    private UUID seedVersion(UUID lineage, UUID process, int revision, UUID predecessor, String status) {
        if (revision == 1) jdbc.update("insert into evidence(id,process_id,owner_id,created_at,correlation_id,latest_revision)"
                + " values(?,?,?,?,?,1)", key(lineage), key(process), key(actor), Timestamp.from(NOW), key(UUID.randomUUID()));
        var id = UUID.randomUUID();
        jdbc.update("insert into evidence_version(id,evidence_id,revision_number,predecessor_id,status,title,"
                        + "description,provenance,covered_from,covered_to,author_id,submitted_at,created_at)"
                        + " values(?,?,?,?,?,?,?,?,?,?,?,?,?)", key(id), key(lineage), revision,
                predecessor == null ? null : key(predecessor), status, "Evidencia sintética",
                "Descripción de evidencia sintética.", "Procedencia sintética", "2026-01-01", "2026-06-30",
                key(actor), Timestamp.from(NOW), Timestamp.from(NOW));
        return id;
    }

    private byte[] key(UUID id) {
        return ByteBuffer.allocate(16).putLong(id.getMostSignificantBits()).putLong(id.getLeastSignificantBits()).array();
    }
}
