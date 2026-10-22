package ec.edu.unl.sec.shared.api;

import ec.edu.unl.sec.shared.infrastructure.CorrelationFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiProblemHandler {
    @ExceptionHandler(DomainProblemException.class)
    ProblemDetail handleDomainProblem(DomainProblemException exception, HttpServletRequest request) {
        return problem(exception.status(), exception.code(), exception.getMessage(), request, Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> details = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            details.putIfAbsent(error.getField(), safeValidationMessage(error));
        }
        return problem(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "La solicitud contiene datos inválidos.", request, details);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail handleAccessDenied(AccessDeniedException exception, HttpServletRequest request) {
        return problem(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "La operación no está permitida en el ámbito actual.", request, Map.of());
    }

    @ExceptionHandler({org.springframework.http.converter.HttpMessageNotReadableException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
            org.springframework.web.bind.MissingServletRequestParameterException.class,
            IllegalArgumentException.class})
    ProblemDetail handleInvalidRequest(Exception exception, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "La solicitud contiene datos inválidos.", request, Map.of());
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception exception, HttpServletRequest request) {
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "No fue posible completar la operación.", request, Map.of());
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    ProblemDetail handleUnsupportedMethod(Exception exception,HttpServletRequest request) {
        return problem(HttpStatus.METHOD_NOT_ALLOWED,"METHOD_NOT_ALLOWED","La operación no está disponible para este recurso.",request,Map.of());
    }

    private ProblemDetail problem(HttpStatus status, String code, String detail, HttpServletRequest request, Map<String, String> details) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("urn:sec-unl:problem:" + code.toLowerCase().replace('_', '-')));
        problem.setTitle(status.getReasonPhrase());
        problem.setProperty("code", code);
        problem.setProperty("correlationId", correlationId(request));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("details", details);
        return problem;
    }

    private String correlationId(HttpServletRequest request) {
        Object value = request.getAttribute(CorrelationFilter.CORRELATION_ATTRIBUTE);
        return value == null ? "unavailable" : value.toString();
    }

    private String safeValidationMessage(FieldError error) {
        String message = error.getDefaultMessage();
        return message == null || message.isBlank() ? "valor inválido" : message;
    }
}
