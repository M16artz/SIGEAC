package ec.edu.unl.sec.identityaccess.infrastructure;

import ec.edu.unl.sec.shared.infrastructure.CorrelationFilter;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class AccessProblemWriter {
    private final ObjectMapper objectMapper;

    public AccessProblemWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(
            jakarta.servlet.http.HttpServletRequest request,
            HttpServletResponse response,
            HttpStatus status,
            String code,
            String detail)
            throws IOException {
        response.setStatus(status.value());
        if (status == HttpStatus.UNAUTHORIZED && !response.containsHeader("WWW-Authenticate")) {
            response.setHeader("WWW-Authenticate", "Session realm=\"sec-unl\"");
        }
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Cache-Control", "no-store");
        var body = objectMapper.createObjectNode();
        body.put("type", URI.create("urn:sec-unl:problem:" + code.toLowerCase().replace('_', '-')).toString());
        body.put("title", status.getReasonPhrase());
        body.put("status", status.value());
        body.put("detail", detail);
        body.put("instance", request.getRequestURI());
        body.put("code", code);
        Object correlation = request.getAttribute(CorrelationFilter.CORRELATION_ATTRIBUTE);
        body.put("correlationId", correlation == null ? "unavailable" : correlation.toString());
        body.putObject("details");
        objectMapper.writeValue(response.getOutputStream(),
                ec.edu.unl.sec.shared.api.ApiResponse.failure(detail, body));
    }
}
