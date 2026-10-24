package ec.edu.unl.sec.reporting.api;

import ec.edu.unl.sec.reporting.application.ReportingProblemException;
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

@RestControllerAdvice(basePackages = "ec.edu.unl.sec.reporting.api")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ReportingProblemHandler {
    @ExceptionHandler(org.springframework.dao.ConcurrencyFailureException.class)
    ProblemDetail concurrentSnapshot(org.springframework.dao.ConcurrencyFailureException exception,HttpServletRequest request) {
        return handle(new ReportingProblemException(ReportingProblemException.Kind.PRECONDITION_FAILED,"STALE_PROCESS",
                "Hubo cambios concurrentes; recargue el proceso antes de continuar."),request);
    }
    @ExceptionHandler(ReportingProblemException.class)
    ProblemDetail handle(ReportingProblemException exception, HttpServletRequest request) {
        var status = switch (exception.kind()) {
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INVALID -> HttpStatus.UNPROCESSABLE_CONTENT;
            case CONFLICT -> HttpStatus.CONFLICT;
            case PRECONDITION_REQUIRED -> HttpStatus.PRECONDITION_REQUIRED;
            case PRECONDITION_FAILED -> HttpStatus.PRECONDITION_FAILED;
        };
        var problem = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
        problem.setTitle(status.getReasonPhrase());
        problem.setType(URI.create("urn:sec-unl:problem:" + exception.code().toLowerCase().replace('_', '-')));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", exception.code());
        var correlation = request.getAttribute(CorrelationFilter.CORRELATION_ATTRIBUTE);
        problem.setProperty("correlationId", correlation == null ? "unavailable" : correlation.toString());
        problem.setProperty("details", exception.details());
        return problem;
    }
}
