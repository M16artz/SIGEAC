package ec.edu.unl.sec.identityaccess.api;

import ec.edu.unl.sec.identityaccess.application.SessionService;
import ec.edu.unl.sec.identityaccess.domain.IdentityAccessDeniedException;
import ec.edu.unl.sec.identityaccess.domain.IdentityConflictException;
import ec.edu.unl.sec.identityaccess.domain.IdentityNotFoundException;
import ec.edu.unl.sec.identityaccess.domain.StaleIdentityRevisionException;
import ec.edu.unl.sec.shared.infrastructure.CorrelationFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "ec.edu.unl.sec.identityaccess.api")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class IdentityAccessExceptionHandler {
    @ExceptionHandler(SessionService.AuthenticationRejected.class)
    ProblemDetail authenticationRejected(
            SessionService.AuthenticationRejected exception, HttpServletRequest request) {
        boolean limited = "AUTHENTICATION_LIMITED".equals(exception.code());
        return problem(
                limited ? HttpStatus.TOO_MANY_REQUESTS : HttpStatus.UNAUTHORIZED,
                exception.code(),
                limited
                        ? "Se aplico una limitacion temporal. Intente nuevamente mas tarde."
                        : "Las credenciales no fueron aceptadas.",
                request,
                Map.of());
    }

    @ExceptionHandler(IdentityAccessDeniedException.class)
    ProblemDetail accessDenied(IdentityAccessDeniedException exception, HttpServletRequest request) {
        return problem(
                HttpStatus.FORBIDDEN,
                exception.code(),
                "La operacion no esta permitida en el ambito actual.",
                request,
                Map.of());
    }

    @ExceptionHandler(IdentityNotFoundException.class)
    ProblemDetail notFound(IdentityNotFoundException exception, HttpServletRequest request) {
        return problem(
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                "El recurso no existe o no es visible en el ambito actual.",
                request,
                Map.of());
    }

    @ExceptionHandler(IdentityConflictException.class)
    ProblemDetail conflict(IdentityConflictException exception, HttpServletRequest request) {
        Map<String, Object> details = new LinkedHashMap<>();
        if (exception.policyId() != null) details.put("policyId", exception.policyId().toString());
        return problem(HttpStatus.CONFLICT, exception.code(), exception.getMessage(), request, details);
    }

    @ExceptionHandler(StaleIdentityRevisionException.class)
    ProblemDetail stale(StaleIdentityRevisionException exception, HttpServletRequest request) {
        return problem(
                HttpStatus.PRECONDITION_FAILED,
                "STALE_REVISION",
                "La revision enviada esta desactualizada.",
                request,
                Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail invalid(MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> details = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            details.putIfAbsent(
                    error.getField(),
                    error.getDefaultMessage() == null ? "valor invalido" : error.getDefaultMessage());
        }
        return problem(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "La solicitud contiene datos invalidos.", request, details);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail invalidArgument(IllegalArgumentException exception, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", exception.getMessage(), request, Map.of());
    }

    private ProblemDetail problem(
            HttpStatus status,
            String code,
            String detail,
            HttpServletRequest request,
            Map<String, ?> details) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("urn:sec-unl:problem:" + code.toLowerCase().replace('_', '-')));
        problem.setTitle(status.getReasonPhrase());
        problem.setProperty("code", code);
        problem.setProperty("correlationId", correlation(request));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("details", details);
        return problem;
    }

    private static String correlation(HttpServletRequest request) {
        Object value = request.getAttribute(CorrelationFilter.CORRELATION_ATTRIBUTE);
        return value == null ? "unavailable" : value.toString();
    }
}
