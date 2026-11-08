package ec.edu.unl.sec.shared.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;

class ApiResponseAdviceTest {
    MockMvc mvc;
    @BeforeEach void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new Endpoint())
                .setControllerAdvice(new ApiResponseAdvice(new ObjectMapper().findAndRegisterModules()), new ApiProblemHandler()).build();
    }
    @Test void wrapsSuccessAndKeepsNestedDomainDataAndHeaders() throws Exception {
        mvc.perform(get("/career")).andExpect(status().isCreated()).andExpect(header().string("ETag", "\"1\""))
                .andExpect(jsonPath("$.success").value(true)).andExpect(jsonPath("$.data.id").value("one"))
                .andExpect(jsonPath("$.data.data.name").value("Career")).andExpect(jsonPath("$.errors").isEmpty())
                .andExpect(jsonPath("$.meta").isMap());
    }
    @Test void movesOnlyPaginationToMeta() throws Exception {
        mvc.perform(get("/page")).andExpect(jsonPath("$.data.items[0]").value("one"))
                .andExpect(jsonPath("$.data.page").doesNotExist()).andExpect(jsonPath("$.meta.page").value(0))
                .andExpect(jsonPath("$.meta.totalElements").value(2));
    }
    @Test void boundsCollectionsAndKeepsTotalInMeta() throws Exception {
        mvc.perform(get("/list").queryParam("page", "1").queryParam("size", "1"))
                .andExpect(jsonPath("$.data.length()").value(1)).andExpect(jsonPath("$.data[0]").value("two"))
                .andExpect(jsonPath("$.meta.totalElements").value(3));
    }
    @Test void keepsDatesInIsoFormatInsidePages() throws Exception {
        mvc.perform(get("/dated-page"))
                .andExpect(jsonPath("$.data.items[0].validFrom").value("2026-11-08"))
                .andExpect(jsonPath("$.data.items[0].createdAt").value("2026-11-08T10:00:00Z"));
    }
    @Test void wrapsProblemDetailsWithTheirActualStatus() throws Exception {
        mvc.perform(get("/problem")).andExpect(status().isBadRequest()).andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.success").value(false)).andExpect(jsonPath("$.errors.status").value(400))
                .andExpect(jsonPath("$.data").isEmpty());
    }
    @Test void sanitizesFrameworkErrorsAndUsesTheSameErrorSchema() throws Exception {
        mvc.perform(get("/framework-error")).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false)).andExpect(jsonPath("$.errors.code").value("HTTP_500"))
                .andExpect(jsonPath("$.errors.instance").value("/framework-error"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("private-diagnostic"))));
    }
    @Test void leavesNoContentNotModifiedHeadBinaryAndOpenApiUnwrapped() throws Exception {
        mvc.perform(delete("/session")).andExpect(status().isNoContent()).andExpect(content().string(""));
        mvc.perform(get("/cached")).andExpect(status().isNotModified()).andExpect(content().string(""));
        mvc.perform(head("/career")).andExpect(content().string(""));
        mvc.perform(get("/binary")).andExpect(content().bytes(new byte[]{1, 2}));
        mvc.perform(get("/v3/api-docs")).andExpect(jsonPath("$.openapi").value("3.1.0")).andExpect(jsonPath("$.success").doesNotExist());
    }
    @RestController static class Endpoint {
        @GetMapping("/career") ResponseEntity<?> career() { return ResponseEntity.status(201).eTag("1").body(Map.of("id", "one", "data", Map.of("name", "Career"))); }
        @GetMapping("/page") Map<String, Object> page() { return Map.of("items", List.of("one"), "page", 0, "size", 1, "totalElements", 2); }
        @GetMapping("/list") List<String> list() { return List.of("one", "two", "three"); }
        @GetMapping("/dated-page") Map<String, Object> datedPage() { return Map.of("items", List.of(Map.of(
                "validFrom", java.time.LocalDate.parse("2026-11-08"), "createdAt", java.time.Instant.parse("2026-11-08T10:00:00Z"))), "page", 0, "size", 1); }
        @GetMapping("/problem") ProblemDetail problem() { return ProblemDetail.forStatusAndDetail(org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid"); }
        @GetMapping("/framework-error") ResponseEntity<?> frameworkError() { return ResponseEntity.status(500).body(Map.of("exception", "private-diagnostic")); }
        @DeleteMapping("/session") ResponseEntity<Void> logout() { return ResponseEntity.noContent().build(); }
        @GetMapping("/cached") ResponseEntity<Void> cached() { return ResponseEntity.status(304).build(); }
        @GetMapping(value = "/binary", produces = "application/octet-stream") byte[] binary() { return new byte[]{1, 2}; }
        @GetMapping("/v3/api-docs") Map<String, String> openapi() { return Map.of("openapi", "3.1.0"); }
    }
}
