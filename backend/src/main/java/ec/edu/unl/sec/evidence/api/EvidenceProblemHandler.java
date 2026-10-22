package ec.edu.unl.sec.evidence.api;

import ec.edu.unl.sec.evidence.application.EvidenceProblemException;
import ec.edu.unl.sec.shared.infrastructure.CorrelationFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "ec.edu.unl.sec.evidence.api")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class EvidenceProblemHandler {
    @ExceptionHandler(EvidenceProblemException.class)
    ProblemDetail handle(EvidenceProblemException exception, HttpServletRequest request) {
        HttpStatus status = switch (exception.kind()) {
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
            case INVALID -> HttpStatus.UNPROCESSABLE_CONTENT;
            case PRECONDITION_REQUIRED -> HttpStatus.PRECONDITION_REQUIRED;
            case PRECONDITION_FAILED -> HttpStatus.PRECONDITION_FAILED;
        };
        var problem = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
        problem.setType(URI.create("urn:sec-unl:problem:" + exception.code().toLowerCase().replace('_', '-')));
        problem.setTitle(status.getReasonPhrase());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", exception.code());
        Object correlation = request.getAttribute(CorrelationFilter.CORRELATION_ATTRIBUTE);
        problem.setProperty("correlationId", correlation == null ? "unavailable" : correlation.toString());
        problem.setProperty("details", Map.of());
        return problem;
    }
}
