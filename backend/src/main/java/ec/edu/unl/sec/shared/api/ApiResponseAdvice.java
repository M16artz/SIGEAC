package ec.edu.unl.sec.shared.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice
public class ApiResponseAdvice implements ResponseBodyAdvice<Object> {
    private final ObjectMapper mapper;

    public ApiResponseAdvice(ObjectMapper mapper) {
        // Keep ISO dates on the wire without changing the mapper used for persisted snapshots.
        this.mapper = mapper.copy().disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Override
    public boolean supports(MethodParameter method, Class<? extends HttpMessageConverter<?>> converter) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter method, MediaType contentType,
            Class<? extends HttpMessageConverter<?>> converter, ServerHttpRequest request, ServerHttpResponse response) {
        String path = request.getURI().getPath();
        int status = response instanceof ServletServerHttpResponse servlet
                ? servlet.getServletResponse().getStatus() : 200;
        if (request.getMethod() == org.springframework.http.HttpMethod.HEAD || status == 204 || status == 304) return null;
        // Protocol documents, operational probes and binary downloads have their own media contracts.
        if (path.contains("/v3/api-docs") || path.contains("/swagger-ui") || path.contains("/actuator/")
                || body instanceof byte[] || body instanceof Resource || body instanceof ApiResponse) return body;
        if (!MediaType.APPLICATION_JSON.isCompatibleWith(contentType)
                && !MediaType.APPLICATION_PROBLEM_JSON.isCompatibleWith(contentType)) return body;

        ApiResponse result;
        if (body instanceof ProblemDetail problem) {
            response.setStatusCode(org.springframework.http.HttpStatusCode.valueOf(problem.getStatus()));
            result = ApiResponse.failure(problem.getDetail(), problem);
        } else if (status >= 400) {
            // Includes framework /error responses without exposing diagnostic internals.
            var error = new LinkedHashMap<String, Object>();
            error.put("status", status);
            error.put("code", "HTTP_" + status);
            String message = HttpStatus.resolve(status) == null ? "Error HTTP." : HttpStatus.valueOf(status).getReasonPhrase();
            error.put("type", "urn:sec-unl:problem:http-" + status);
            error.put("title", message);
            error.put("detail", message);
            error.put("instance", request.getURI().getPath());
            error.put("details", Map.of());
            error.put("correlationId", java.util.Optional.ofNullable(response.getHeaders().getFirst("X-Correlation-ID")).orElse("unavailable"));
            result = ApiResponse.failure(message, error);
        } else {
            JsonNode value = mapper.valueToTree(body);
            Map<String, Object> meta = new LinkedHashMap<>();
            Object data = body;
            if (value != null && value.isArray()) {
                var query = org.springframework.web.util.UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams();
                int page = Integer.parseInt(query.getFirst("page") == null ? "0" : query.getFirst("page"));
                int size = Integer.parseInt(query.getFirst("size") == null ? "100" : query.getFirst("size"));
                if (page < 0 || page > Integer.MAX_VALUE / 100 || size < 1 || size > 100) throw new IllegalArgumentException("Invalid pagination");
                int start = (int) Math.min((long) value.size(), (long) page * size);
                var items = mapper.createArrayNode();
                for (int index = start; index < Math.min(value.size(), start + size); index++) items.add(value.get(index));
                data = mapper.convertValue(items, Object.class);
                meta.put("page", page); meta.put("size", size); meta.put("totalElements", value.size());
                meta.put("totalPages", (value.size() + size - 1) / size);
            }
            if (value != null && value.isObject() && value.has("page") && (value.has("items") || value.has("size"))) {
                var remaining = ((com.fasterxml.jackson.databind.node.ObjectNode) value).deepCopy();
                var pagination = java.util.Set.of("page", "size", "total", "totalElements", "totalPages", "hasNext");
                value.fields().forEachRemaining(field -> {
                    if (pagination.contains(field.getKey())) {
                        meta.put(field.getKey(), mapper.convertValue(field.getValue(), Object.class));
                        remaining.remove(field.getKey());
                    }
                });
                data = mapper.convertValue(remaining, Object.class);
            }
            result = new ApiResponse(true, "Operación realizada correctamente.", data, null, meta);
        }
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().remove("Content-Length");
        response.getHeaders().setCacheControl("no-store");
        if (StringHttpMessageConverter.class.isAssignableFrom(converter)) {
            try { return mapper.writeValueAsString(result); }
            catch (JsonProcessingException exception) { throw new IllegalStateException("Cannot serialize API response", exception); }
        }
        return result;
    }
}
