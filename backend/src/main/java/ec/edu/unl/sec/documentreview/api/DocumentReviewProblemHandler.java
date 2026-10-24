package ec.edu.unl.sec.documentreview.api;

import ec.edu.unl.sec.documentreview.application.DocumentReviewProblemException;
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

/** Maps review/authorization problems of the documentary review surface. */
@RestControllerAdvice(basePackages = "ec.edu.unl.sec.documentreview.api")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DocumentReviewProblemHandler {
    @ExceptionHandler(DocumentReviewProblemException.class)
    ProblemDetail handleReview(DocumentReviewProblemException exception, HttpServletRequest request) {
        return problem(exception.kind(), exception.code(), exception.getMessage(), request);
    }

    @ExceptionHandler(EvidenceProblemException.class)
    ProblemDetail handleEvidence(EvidenceProblemException exception, HttpServletRequest request) {
        return problem(exception.kind(), exception.code(), exception.getMessage(), request);
    }

    private static ProblemDetail problem(
            DocumentReviewProblemException.Kind kind, String code, String message, HttpServletRequest request) {
        return problem(switch (kind) {
            case UNAUTHORIZED -> org.springframework.http.HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> org.springframework.http.HttpStatus.FORBIDDEN;
            case NOT_FOUND -> org.springframework.http.HttpStatus.NOT_FOUND;
            case CONFLICT -> org.springframework.http.HttpStatus.CONFLICT;
            case INVALID -> org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;
            case PRECONDITION_REQUIRED -> org.springframework.http.HttpStatus.PRECONDITION_REQUIRED;
            case PRECONDITION_FAILED -> org.springframework.http.HttpStatus.PRECONDITION_FAILED;
        }, code, message, request);
    }

    private static ProblemDetail problem(HttpStatus status, String code, String message, HttpServletRequest request) {
        var problem = ProblemDetail.forStatusAndDetail(status, message);
        problem.setType(URI.create("urn:sec-unl:problem:" + code.toLowerCase().replace('_', '-')));
        problem.setTitle(status.getReasonPhrase());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", code);
        Object correlation = request.getAttribute(CorrelationFilter.CORRELATION_ATTRIBUTE);
        problem.setProperty("correlationId", correlation == null ? "unavailable" : correlation.toString());
        problem.setProperty("details", Map.of());
        return problem;
    }

    private static ProblemDetail problem(
            EvidenceProblemException.Kind kind, String code, String message, HttpServletRequest request) {
        return problem(switch (kind) {
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
            case INVALID -> HttpStatus.UNPROCESSABLE_CONTENT;
            case PRECONDITION_REQUIRED -> HttpStatus.PRECONDITION_REQUIRED;
            case PRECONDITION_FAILED -> HttpStatus.PRECONDITION_FAILED;
            case PAYLOAD_TOO_LARGE -> HttpStatus.PAYLOAD_TOO_LARGE;
            case STORAGE_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
        }, code, message, request);
    }
}
