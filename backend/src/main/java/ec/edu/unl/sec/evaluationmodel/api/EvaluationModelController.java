package ec.edu.unl.sec.evaluationmodel.api;

import ec.edu.unl.sec.evaluationmodel.application.EvaluationModelService;
import ec.edu.unl.sec.evaluationmodel.application.ModelComparisonService;
import ec.edu.unl.sec.evaluationmodel.application.ModelSuccessionService;
import ec.edu.unl.sec.evaluationmodel.domain.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping
public class EvaluationModelController {
    private final EvaluationModelService service;private final ModelSuccessionService succession;private final ModelComparisonService comparison;
    public EvaluationModelController(EvaluationModelService service,ModelSuccessionService succession,ModelComparisonService comparison){this.service=service;this.succession=succession;this.comparison=comparison;}
    @GetMapping("/models") public List<EvaluationModel> models(){return service.listModels();}
    @PostMapping("/models") public ResponseEntity<EvaluationModel> create(@Valid @RequestBody ModelCommand body,HttpServletRequest request){var created=service.createModel(body.name(),body.description(),correlation(request));return ResponseEntity.created(URI.create("/api/v1/models/"+created.id())).body(created);}
    @GetMapping("/models/{modelId}/versions") public List<ModelVersion> versions(@PathVariable UUID modelId){return service.listVersions(modelId);}
    @PostMapping("/models/{modelId}/versions") public ResponseEntity<ModelVersion> createVersion(@PathVariable UUID modelId,@Valid @RequestBody VersionDraft body,HttpServletRequest request){var created=service.createDraft(modelId,body.source(),body.criteria(),body.scale(),body.formula(),correlation(request));return ResponseEntity.created(URI.create("/api/v1/model-versions/"+created.id())).eTag(etag(created)).body(created);}
    @GetMapping("/model-versions/{id}") public ResponseEntity<ModelVersion> version(@PathVariable UUID id,HttpServletRequest request){var value=service.get(id,correlation(request));return ResponseEntity.ok().eTag(etag(value)).body(value);}
    @PutMapping("/model-versions/{id}") public ResponseEntity<ModelVersion> replace(@PathVariable UUID id,@RequestHeader("If-Match") String match,@Valid @RequestBody VersionDraft body,HttpServletRequest request){var value=service.replace(id,revision(match),body.source(),body.criteria(),body.scale(),body.formula(),correlation(request));return ResponseEntity.ok().eTag(etag(value)).body(value);}
    @PostMapping("/model-versions/{id}/publish") public ResponseEntity<ModelVersion> publish(@PathVariable UUID id,@RequestHeader("If-Match") String match,@Valid @RequestBody PublishCommand body,HttpServletRequest request){var value=service.publish(id,revision(match),body.approverId(),correlation(request));return ResponseEntity.ok().eTag(etag(value)).body(value);}
    @PostMapping("/model-versions/{id}/successors") public ResponseEntity<ModelVersion> successor(@PathVariable UUID id,HttpServletRequest request){var value=succession.create(id,correlation(request));return ResponseEntity.created(URI.create("/api/v1/model-versions/"+value.id())).eTag(etag(value)).body(value);}
    @GetMapping("/model-versions/compare") public ModelComparisonService.Comparison compare(@RequestParam("from") UUID from,@RequestParam("to") UUID to,HttpServletRequest request){return comparison.compare(from,to,correlation(request));}
    private static String etag(ModelVersion v){return "\""+v.revision()+"\"";} private static long revision(String value){try{return Long.parseLong(value.replace("\"","").strip());}catch(Exception e){throw new IllegalArgumentException("If-Match inválido");}}
    private static UUID correlation(HttpServletRequest request){Object value=request.getAttribute("sec.correlationId");if(value!=null)try{return UUID.fromString(value.toString());}catch(Exception ignored){}return UUID.randomUUID();}
    public record ModelCommand(@NotBlank @Size(max=200) String name,@Size(max=2000) String description){}
    public record VersionDraft(@NotNull @Size(max=2000) String source,@NotNull List<ModelNode> criteria,RatingScale scale,ProgressFormula formula){}
    public record PublishCommand(@NotNull UUID approverId){}
}
