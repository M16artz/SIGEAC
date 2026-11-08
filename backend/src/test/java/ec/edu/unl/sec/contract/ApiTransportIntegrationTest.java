package ec.edu.unl.sec.contract;

import static org.assertj.core.api.Assertions.assertThat;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("sqlite-test")
class ApiTransportIntegrationTest {
    private static final String DB = "transport-" + UUID.randomUUID();
    @org.junit.jupiter.api.io.TempDir static java.nio.file.Path files;
    @Value("${local.server.port}") int port;
    final ObjectMapper mapper = new ObjectMapper();

    @DynamicPropertySource static void data(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:file:" + DB + "?mode=memory&cache=shared");
        registry.add("sec.files.root", () -> files.toString());
        registry.add("sec.http.allowed-origins", () -> "https://client.example");
    }

    @Test void verifiesRealSecurityValidationPaginationPersistenceAndNoContent() throws Exception {
        HttpClient admin = client();
        check(call(admin, "GET", "/me", null, null), 401, false);
        check(call(admin, "POST", "/sessions", Map.of("username", "ab", "password", "short"), null), 400, false);
        var session = check(call(admin, "POST", "/sessions", Map.of("username", "admin", "password", "Synthetic-Admin-2026"), null), 201, true);
        String csrf = session.at("/data/csrfToken").asText();
        assertThat(csrf).isNotBlank();
        String code = "TRANSPORT-" + UUID.randomUUID();
        var unit = check(call(admin, "POST", "/academic-units", Map.of("code", code, "name", "Synthetic unit"), csrf), 201, true);
        Map<String, Object> input = Map.of("code", code, "name", "Synthetic career", "academicUnitId", unit.at("/data/id").asText(), "validFrom", "2020-01-01");
        check(call(admin, "POST", "/careers", input, null), 403, false);
        var createdResponse = call(admin, "POST", "/careers", input, csrf);
        var created = check(createdResponse, 201, true);
        assertThat(createdResponse.headers().firstValue("ETag")).contains("\"1\"");
        assertThat(created.at("/data/data/code").asText()).isEqualTo(code.toUpperCase(java.util.Locale.ROOT));
        String path = "/careers/" + created.at("/data/id").asText();
        var read = check(call(admin, "GET", path, null, null), 200, true);
        assertThat(read.at("/data/id").asText()).isEqualTo(created.at("/data/id").asText());
        check(call(admin, "POST", "/careers", input, csrf), 409, false);
        var invalid = check(call(admin, "POST", "/careers", Map.of("code", code), csrf), 400, false);
        assertThat(invalid.at("/errors/details/name").asText()).isNotBlank();
        var page = check(call(admin, "GET", "/careers?page=0&size=1", null, null), 200, true);
        assertThat(page.at("/data/items").size()).isEqualTo(1);
        assertThat(page.at("/data/page").isMissingNode()).isTrue();
        assertThat(page.at("/meta/size").asInt()).isEqualTo(1);
        check(call(admin, "GET", "/careers?page=-1", null, null), 400, false);
        check(call(admin, "GET", "/careers/not-a-uuid", null, null), 400, false);
        check(call(admin, "GET", "/careers/" + UUID.randomUUID(), null, null), 404, false);
        check(call(admin, "GET", "/nonexistent-route", null, null), 404, false);
        var logout = call(admin, "DELETE", "/sessions", null, csrf);
        assertThat(logout.statusCode()).isEqualTo(204); assertThat(logout.body()).isEmpty();
        check(call(admin, "GET", "/me", null, null), 401, false);

        HttpClient reader = client();
        var login = check(call(reader, "POST", "/sessions", Map.of("username", "responsable", "password", "Synthetic-User-2026"), null), 201, true);
        check(call(reader, "POST", "/careers", input, login.at("/data/csrfToken").asText()), 403, false);
        check(call(reader, "GET", path, null, null), 404, false);
    }

    @Test void keepsProtocolDocumentsRawAndReturnsJsonForRejectedCors() throws Exception {
        var client = client();
        var schemaResponse = call(client, "GET", "/v3/api-docs", null, null);
        assertThat(schemaResponse.statusCode()).isEqualTo(200);
        JsonNode schema = mapper.readTree(schemaResponse.body());
        assertThat(schema.has("openapi")).isTrue(); assertThat(schema.has("success")).isFalse();
        var careerSchema = schema.at("/paths/~1careers/post/responses/201/content/application~1json/schema");
        assertThat(careerSchema.at("/properties/success").isMissingNode()).isFalse();
        var denied = client.send(HttpRequest.newBuilder(uri("/careers")).header("Origin", "https://untrusted.example")
                .GET().build(), HttpResponse.BodyHandlers.ofString());
        var body = check(denied, 403, false);
        assertThat(body.at("/errors/code").asText()).isEqualTo("CORS_DENIED");
        var allowed = client.send(HttpRequest.newBuilder(uri("/careers")).header("Origin", "https://client.example")
                .header("Access-Control-Request-Method", "GET").method("OPTIONS", HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(allowed.statusCode()).isEqualTo(200);
        assertThat(allowed.headers().firstValue("Access-Control-Allow-Origin")).contains("https://client.example");
    }

    private HttpClient client() { return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build(); }
    private URI uri(String path) { return URI.create("http://localhost:" + port + "/api/v1" + path); }
    private HttpResponse<String> call(HttpClient client, String method, String path, Object body, String csrf) throws Exception {
        var request = HttpRequest.newBuilder(uri(path)).timeout(Duration.ofSeconds(15)).header("Accept", "application/json");
        if (csrf != null) request.header("X-CSRF-TOKEN", csrf);
        if (body != null) request.header("Content-Type", "application/json");
        request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)));
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
    private JsonNode check(HttpResponse<String> response, int status, boolean success) throws Exception {
        assertThat(response.statusCode()).isEqualTo(status);
        assertThat(response.headers().firstValue("Content-Type").orElse("")).contains("application/json");
        var body = mapper.readTree(response.body());
        assertThat(body.properties()).extracting(Map.Entry::getKey).containsExactlyInAnyOrder("success", "message", "data", "errors", "meta");
        assertThat(body.path("success").asBoolean()).isEqualTo(success);
        assertThat(body.path("message").asText()).isNotBlank();
        assertThat(body.path("meta").isObject()).isTrue();
        assertThat(body.path(success ? "errors" : "data").isNull()).isTrue();
        return body;
    }
}
