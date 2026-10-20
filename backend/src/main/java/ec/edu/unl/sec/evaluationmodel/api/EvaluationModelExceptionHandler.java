package ec.edu.unl.sec.evaluationmodel.api;

import ec.edu.unl.sec.evaluationmodel.application.EvaluationModelService;
import ec.edu.unl.sec.evaluationmodel.application.ModelProblemException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages="ec.edu.unl.sec.evaluationmodel.api")
public class EvaluationModelExceptionHandler {
    @ExceptionHandler(EvaluationModelService.PublicationRejected.class)
    ProblemDetail publication(EvaluationModelService.PublicationRejected e,HttpServletRequest request){ProblemDetail p=ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,e.getMessage());p.setType(URI.create("urn:sec-unl:problem:model-publication-invalid"));p.setTitle("Invalid model version");p.setInstance(URI.create(request.getRequestURI()));p.setProperty("code",e.code());p.setProperty("correlationId",String.valueOf(request.getAttribute("sec.correlationId")));java.util.Map<String,String> details=new java.util.LinkedHashMap<>();for(int i=0;i<e.issues().size();i++)details.put("issue"+i,e.issues().get(i));p.setProperty("details",details);return p;}
    @ExceptionHandler(ModelProblemException.class)
    ProblemDetail model(ModelProblemException e,HttpServletRequest request){HttpStatus status=switch(e.kind()){case UNAUTHORIZED->HttpStatus.UNAUTHORIZED;case FORBIDDEN->HttpStatus.FORBIDDEN;case NOT_FOUND->HttpStatus.NOT_FOUND;case CONFLICT->HttpStatus.CONFLICT;case PRECONDITION->HttpStatus.PRECONDITION_FAILED;case INVALID->HttpStatus.BAD_REQUEST;};ProblemDetail p=ProblemDetail.forStatusAndDetail(status,e.getMessage());p.setType(URI.create("urn:sec-unl:problem:"+e.code().toLowerCase().replace('_','-')));p.setTitle(status.getReasonPhrase());p.setInstance(URI.create(request.getRequestURI()));p.setProperty("code",e.code());p.setProperty("correlationId",String.valueOf(request.getAttribute("sec.correlationId")));p.setProperty("details",java.util.Map.of());return p;}
}
