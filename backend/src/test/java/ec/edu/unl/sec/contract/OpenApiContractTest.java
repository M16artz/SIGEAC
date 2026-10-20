package ec.edu.unl.sec.contract;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class OpenApiContractTest {
    private final Path contract = Path.of("..", "docs", "technical", "contracts", "openapi.yaml");

    @Test
    void declaresOpenApi31AndStableProblemDetails() throws IOException {
        String yaml = Files.readString(contract);
        assertAll(
                () -> assertTrue(yaml.startsWith("openapi: 3.1.0")),
                () -> assertTrue(yaml.contains("application/problem+json")),
                () -> assertTrue(yaml.contains("required: [type, title, status, code, correlationId]")),
                () -> assertTrue(yaml.contains("correlationId: {type: string, format: uuid}")),
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
}
