package ec.edu.unl.sec.support;

import static org.assertj.core.api.Assertions.assertThat;
import ec.edu.unl.sec.evaluationmodel.domain.PublicationValidator;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CompleteWorkflowPerformanceDataTest {
    @Test void publishesFiveHundredDistinctSyntheticIndicatorsWithExplicitSources() {
        var author = UUID.nameUUIDFromBytes("sec-unl:admin".getBytes(StandardCharsets.UTF_8));
        var model = CompleteWorkflowPerformanceData.model(author, Instant.parse("2026-01-01T00:00:00Z"));
        assertThat(PublicationValidator.validate(model, model.approverId(), "sec.progress/v1")).isEmpty();
        assertThat(model.configuration().indicators()).hasSize(500);
        assertThat(model.configuration().indicators().stream().map(i -> i.nodeId()).distinct().count()).isEqualTo(500);
        assertThat(model.configuration().indicators().stream().flatMap(i -> i.sources().stream()).count()).isEqualTo(500);
    }
}
