package ec.edu.unl.sec.academicevaluation.api;

import ec.edu.unl.sec.academicevaluation.application.AcademicProblemException;
import ec.edu.unl.sec.shared.infrastructure.CorrelationFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice(basePackages="ec.edu.unl.sec.academicevaluation.api")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AcademicProblemHandler {
    @ExceptionHandler(org.springframework.dao.ConcurrencyFailureException.class)
    ProblemDetail stale(Exception exception, HttpServletRequest request) {
        return handle(new AcademicProblemException(AcademicProblemException.Kind.PRECONDITION_FAILED,
                "STALE_EVALUATION", "Hubo cambios concurrentes; recargue antes de continuar."), request);
    }
    @ExceptionHandler(AcademicProblemException.class)
    ProblemDetail handle(AcademicProblemException exception, HttpServletRequest request) {
        var status=switch(exception.kind()) {
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INVALID -> HttpStatus.UNPROCESSABLE_CONTENT;
            case CONFLICT -> HttpStatus.CONFLICT;
            case PRECONDITION_FAILED -> HttpStatus.PRECONDITION_FAILED;
        };
        var problem=ProblemDetail.forStatusAndDetail(status, exception.getMessage());
        problem.setTitle(status.getReasonPhrase());
        problem.setType(URI.create("urn:sec-unl:problem:"+exception.code().toLowerCase().replace('_','-')));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code",exception.code());
        problem.setProperty("correlationId",String.valueOf(request.getAttribute(CorrelationFilter.CORRELATION_ATTRIBUTE)));
        problem.setProperty("details",Map.of());
        return problem;
    }
}
