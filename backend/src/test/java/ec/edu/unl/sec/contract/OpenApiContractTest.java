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
        var document = read(contract);
        var envelope = map(map(map(document.get("components")).get("schemas")).get("ApiEnvelope"));
        var problem = map(map(map(document.get("components")).get("schemas")).get("Problem"));
        var correlation = map(map(problem.get("properties")).get("correlationId"));
        assertAll(
                () -> org.junit.jupiter.api.Assertions.assertEquals("3.1.0", document.get("openapi")),
                () -> assertTrue(((java.util.List<?>) envelope.get("required")).containsAll(java.util.List.of("success", "message", "data", "errors", "meta"))),
                () -> assertTrue(((java.util.List<?>) problem.get("required")).containsAll(java.util.List.of("type", "title", "status", "detail", "instance", "code", "correlationId", "details"))),
                () -> org.junit.jupiter.api.Assertions.assertEquals(8, correlation.get("minLength")),
                () -> org.junit.jupiter.api.Assertions.assertEquals(64, correlation.get("maxLength")),
                () -> assertTrue(map(map(document.get("components")).get("securitySchemes")).containsKey("sessionCookie")));
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
                () -> assertTrue(((java.util.List<?>) problem(evidenceContract).get("required"))
                        .containsAll(java.util.List.of("type", "title", "status", "detail", "instance", "code", "correlationId", "details"))));
    }

    private java.util.Map<?, ?> read(Path path) throws IOException {
        return new org.yaml.snakeyaml.Yaml(new org.yaml.snakeyaml.constructor.SafeConstructor(new org.yaml.snakeyaml.LoaderOptions()))
                .load(Files.readString(path));
    }
    private java.util.Map<?, ?> map(Object value) { return (java.util.Map<?, ?>) value; }
    private java.util.Map<?, ?> problem(Path file) throws IOException {
        var schema = map(map(map(read(file).get("components")).get("schemas")).get("Problem"));
        if (schema.containsKey("$ref")) {
            String relative = schema.get("$ref").toString().split("#")[0];
            return problem(file.getParent().resolve(relative).normalize());
        }
        return schema;
    }
}
