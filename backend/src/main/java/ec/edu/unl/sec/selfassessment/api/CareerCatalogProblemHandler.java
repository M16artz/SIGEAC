package ec.edu.unl.sec.selfassessment.api;
import ec.edu.unl.sec.selfassessment.application.CareerCatalogException;
import ec.edu.unl.sec.shared.api.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
@RestControllerAdvice(basePackages="ec.edu.unl.sec.selfassessment.api")
public class CareerCatalogProblemHandler {
    @ExceptionHandler(CareerCatalogException.class) public ProblemDetail handle(CareerCatalogException error,HttpServletRequest request) {
        var result=ProblemDetail.forStatusAndDetail(HttpStatus.valueOf(error.status()),error.getMessage());
        result.setType(java.net.URI.create("urn:sec-unl:problem:"+error.code().toLowerCase().replace('_','-')));
        result.setTitle(HttpStatus.valueOf(error.status()).getReasonPhrase());result.setInstance(java.net.URI.create(request.getRequestURI()));
        result.setProperty("code",error.code());result.setProperty("details",java.util.Map.of());
        result.setProperty("correlationId",String.valueOf(request.getAttribute("sec.correlationId")));return result;
    }
}
