package ec.edu.unl.sec.contract;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class OpenApiContractTest {
    private final Path contract = Path.of("..", "docs", "technical", "contracts", "openapi.yaml");
    private final Path evidenceContract = Path.of("..", "specs", "004-submit-evidence", "contracts", "openapi.yaml");

    @Test
    void declaresOpenApi31AndStableProblemDetails() throws IOException {
        String yaml = Files.readString(contract);
        assertAll(
                () -> assertTrue(yaml.startsWith("openapi: 3.1.0")),
                () -> assertTrue(yaml.contains("application/problem+json")),
                () -> assertTrue(yaml.contains("required: [type, title, status, detail, instance, code, correlationId, details]")),
                () -> assertTrue(yaml.contains("correlationId: {type: string, minLength: 8, maxLength: 64}")),
                () -> assertTrue(yaml.contains("sessionCookie:")));
    }

    @Test
    void exposesAllMvpCapabilityPaths() throws IOException {
        String yaml = Files.readString(contract);
        assertAll(
                () -> assertTrue(yaml.contains("/sessions:")),
                () -> assertTrue(yaml.contains("/models:")),
                () -> assertTrue(yaml.contains("/processes:")),
                () -> assertTrue(yaml.contains("/evidences:")),
                () -> assertTrue(yaml.contains("/document-review-inbox:")),
                () -> assertTrue(yaml.contains("/academic-evaluation-inbox:")),
                () -> assertTrue(yaml.contains("/progress:")));
    }

    @Test
    void exposesIdentityAccessCapabilityPaths() throws IOException {
        String yaml = Files.readString(contract);
        assertAll(
                () -> assertTrue(yaml.contains("/users:")),
                () -> assertTrue(yaml.contains("/users/{userId}:")),
                () -> assertTrue(yaml.contains("/users/{userId}/assignments:")),
                () -> assertTrue(yaml.contains("/conflict-exceptions:")),
                () -> assertTrue(yaml.contains("/audit-events:")));
    }

    @Test
    void exposesGovernanceFindingAndReportPaths() throws IOException {
        String yaml = Files.readString(contract);
        assertAll(
                () -> assertTrue(yaml.contains("Reports")),
                () -> assertTrue(yaml.contains("/processes/{processId}/findings:")),
                () -> assertTrue(yaml.contains("/processes/{processId}/consolidations:")),
                () -> assertTrue(yaml.contains("/processes/{processId}/report-versions:")),
                () -> assertTrue(yaml.contains("/report-versions/{reportVersionId}:")),
                () -> assertTrue(yaml.contains("/report-versions/{reportVersionId}/socializations:")),
                () -> assertTrue(yaml.contains("/report-versions/{reportVersionId}/observations:")),
                () -> assertTrue(yaml.contains("/report-observations/{observationId}/responses:")),
                () -> assertTrue(yaml.contains("/report-versions/{reportVersionId}/approval-decisions:")));
    }

    @Test
    void exposesClosureAndArchivePaths() throws IOException {
        String yaml = Files.readString(contract);
        assertAll(
                () -> assertTrue(yaml.contains("/processes/{processId}/closure-preview:")),
                () -> assertTrue(yaml.contains("/processes/{processId}/close:")),
                () -> assertTrue(yaml.contains("/processes/{processId}/archive:")),
                () -> assertTrue(yaml.contains("/processes/{processId}/closures:")));
    }

    @Test
    void evidenceContractRequiresConcurrencyIntegrityAndCompleteProblems() throws IOException {
        String yaml = Files.readString(evidenceContract);
        assertAll(
                () -> assertTrue(yaml.contains("/evidences:")),
                () -> assertTrue(yaml.contains("/evidence-versions/{versionId}/files:")),
                () -> assertTrue(yaml.contains("/evidence-versions/{versionId}/files/{fileId}/content:")),
                () -> assertTrue(yaml.contains("/evidence-versions/{versionId}/external-references:")),
                () -> assertTrue(yaml.contains("/evidence-versions/{versionId}/submit:")),
                () -> assertTrue(yaml.contains("/evidence-versions/{versionId}/successor:")),
                () -> assertTrue(yaml.contains("If-Match")),
                () -> assertTrue(yaml.contains("'412'")),
                () -> assertTrue(yaml.contains("'428'")),
                () -> assertTrue(yaml.contains("declaredMediaType")),
                () -> assertTrue(yaml.contains("detectedMediaType")),
                () -> assertTrue(yaml.contains("required: [type, title, status, detail, instance, code, correlationId, details]")));
    }
}
