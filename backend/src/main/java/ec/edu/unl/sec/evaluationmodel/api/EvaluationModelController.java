package ec.edu.unl.sec.evaluationmodel.api;

import ec.edu.unl.sec.evaluationmodel.application.EvaluationModelService;
import ec.edu.unl.sec.evaluationmodel.application.ModelApplicabilityService;
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
    private final ModelApplicabilityService applicability;
    public EvaluationModelController(EvaluationModelService service,ModelSuccessionService succession,ModelComparisonService comparison,ModelApplicabilityService applicability){this.service=service;this.succession=succession;this.comparison=comparison;this.applicability=applicability;}
    @GetMapping("/models") public List<EvaluationModel> models(){return service.listModels();}
    @PostMapping("/models") public ResponseEntity<EvaluationModel> create(@Valid @RequestBody ModelCommand body,HttpServletRequest request){var created=service.createModel(body.name(),body.description(),correlation(request));return ResponseEntity.created(URI.create("/api/v1/models/"+created.id())).body(created);}
    @GetMapping("/models/{modelId}/versions") public List<LegacyModelVersion> versions(@PathVariable UUID modelId){return service.listVersions(modelId).stream().map(LegacyModelVersion::from).toList();}
    @GetMapping("/models/{modelId}/versions/configured") public List<ModelVersion> configuredVersions(@PathVariable UUID modelId){return service.listVersions(modelId);}
    @PostMapping("/models/{modelId}/versions") public ResponseEntity<LegacyModelVersion> createVersion(@PathVariable UUID modelId,@Valid @RequestBody VersionDraft body,HttpServletRequest request){var created=service.createDraft(modelId,body.source(),body.nodes(),body.scale(),body.formula(),correlation(request));return ResponseEntity.created(URI.create("/api/v1/model-versions/"+created.id())).eTag(etag(created)).body(LegacyModelVersion.from(created));}
    @PostMapping("/models/{modelId}/versions/configured") public ResponseEntity<ModelVersion> createConfigured(@PathVariable UUID modelId,@Valid @RequestBody ConfiguredDraft body,HttpServletRequest request){var created=service.createDraft(modelId,body.source(),body.nodes(),body.configuration(),correlation(request));return ResponseEntity.created(URI.create("/api/v1/model-versions/"+created.id())).eTag(etag(created)).body(created);}
    @GetMapping("/model-versions/{id}") public ResponseEntity<LegacyModelVersion> version(@PathVariable UUID id,HttpServletRequest request){var value=service.get(id,correlation(request));return ResponseEntity.ok().eTag(etag(value)).body(LegacyModelVersion.from(value));}
    @GetMapping("/model-versions/{id}/configured") public ResponseEntity<ModelVersion> configured(@PathVariable UUID id,HttpServletRequest request){var value=service.get(id,correlation(request));return ResponseEntity.ok().eTag(etag(value)).body(value);}
    @PutMapping("/model-versions/{id}") public ResponseEntity<LegacyModelVersion> replace(@PathVariable UUID id,@RequestHeader(value="If-Match",required=false) String match,@Valid @RequestBody VersionDraft body,HttpServletRequest request){var value=service.replace(id,revision(match),body.source(),body.nodes(),body.scale(),body.formula(),correlation(request));return ResponseEntity.ok().eTag(etag(value)).body(LegacyModelVersion.from(value));}
    @PutMapping("/model-versions/{id}/configured") public ResponseEntity<ModelVersion> replaceConfigured(@PathVariable UUID id,@RequestHeader(value="If-Match",required=false) String match,@Valid @RequestBody ConfiguredDraft body,HttpServletRequest request){var value=service.replaceConfigured(id,revision(match),body.source(),body.nodes(),body.configuration(),correlation(request));return ResponseEntity.ok().eTag(etag(value)).body(value);}
    @PostMapping("/model-versions/{id}/publish") public ResponseEntity<LegacyModelVersion> publish(@PathVariable UUID id,@RequestHeader(value="If-Match",required=false) String match,@Valid @RequestBody PublishCommand body,HttpServletRequest request){var value=service.publish(id,revision(match),body.approverId(),correlation(request));return ResponseEntity.ok().eTag(etag(value)).body(LegacyModelVersion.from(value));}
    @PostMapping("/model-versions/{id}/publish-configured") public ResponseEntity<ModelVersion> publishConfigured(@PathVariable UUID id,@RequestHeader(value="If-Match",required=false) String match,@Valid @RequestBody PublishCommand body,HttpServletRequest request){var value=service.publishConfigured(id,revision(match),body.approverId(),correlation(request));return ResponseEntity.ok().eTag(etag(value)).body(value);}
    @PostMapping("/model-versions/{id}/successors") public ResponseEntity<LegacyModelVersion> successor(@PathVariable UUID id,HttpServletRequest request){var value=succession.create(id,correlation(request));return ResponseEntity.created(URI.create("/api/v1/model-versions/"+value.id())).eTag(etag(value)).body(LegacyModelVersion.from(value));}
    @PostMapping("/model-versions/{id}/successors/configured") public ResponseEntity<ModelVersion> configuredSuccessor(@PathVariable UUID id,HttpServletRequest request){var value=succession.createConfigured(id,correlation(request));return ResponseEntity.created(URI.create("/api/v1/model-versions/"+value.id()+"/configured")).eTag(etag(value)).body(value);}
    @GetMapping("/model-versions/compare") public ModelComparisonService.Comparison compare(@RequestParam("from") UUID from,@RequestParam("to") UUID to,HttpServletRequest request){return comparison.compare(from,to,correlation(request));}
    @PostMapping("/normative-references") public ResponseEntity<NormativeReference> registerReference(@Valid @RequestBody NormativeReference reference,HttpServletRequest request){var value=applicability.registerReference(reference,correlation(request));return ResponseEntity.created(URI.create("/api/v1/normative-references/"+value.id())).body(value);}
    @GetMapping("/model-versions/{id}/normative-references") public List<NormativeReference> references(@PathVariable UUID id){return applicability.references(id);}
    @PostMapping("/model-versions/{id}/applicability-decisions") public ResponseEntity<ModelCareerApplicability> decide(@PathVariable UUID id,@RequestHeader(value="If-Match",required=false) String match,@Valid @RequestBody ModelApplicabilityService.ApplicabilityInput body,HttpServletRequest request){var value=applicability.decide(id,body,revision(match),correlation(request));return ResponseEntity.created(URI.create("/api/v1/model-versions/"+id+"/applicability-decisions")).body(value);}
    @GetMapping("/model-versions/{id}/applicability-decisions") public List<ModelCareerApplicability> applicability(@PathVariable UUID id){return applicability.history(id);}
    private static String etag(ModelVersion v){return ec.edu.unl.sec.shared.api.EntityTagSupport.strong(v.revision());} private static long revision(String value){return ec.edu.unl.sec.shared.api.EntityTagSupport.require(value);}
    private static UUID correlation(HttpServletRequest request){Object value=request.getAttribute(ec.edu.unl.sec.shared.infrastructure.CorrelationFilter.CORRELATION_ATTRIBUTE);if(value!=null)try{return UUID.fromString(value.toString());}catch(Exception ignored){}return UUID.randomUUID();}
    public record ModelCommand(@NotBlank @Size(max=200) String name,@Size(max=2000) String description){}
    public record VersionDraft(@NotNull @Size(max=2000) String source,@NotNull List<LegacyModelNode> criteria,RatingScale scale,ProgressFormula formula){
        List<ModelNode> nodes(){return criteria.stream().map(LegacyModelNode::toDomain).toList();}
    }
    public record ConfiguredDraft(@NotNull @Size(max=2000) String source,@NotNull List<ModelNode> nodes,@NotNull ModelConfiguration configuration){}
    public record PublishCommand(@NotNull UUID approverId){}
}
