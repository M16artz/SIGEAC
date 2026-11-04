package ec.edu.unl.sec.selfassessment.api;
import ec.edu.unl.sec.selfassessment.application.*;
import ec.edu.unl.sec.selfassessment.domain.*;
import ec.edu.unl.sec.shared.api.EntityTagSupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.net.URI;
import java.time.LocalDate;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
public class CareerController {
    private final CareerAdministrationService service;
    public CareerController(CareerAdministrationService service){this.service=service;}
    @GetMapping("/careers/access") public Map<String,Boolean> access(){return service.access();}
    @GetMapping("/careers") public CatalogPage<Career> careers(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size,@RequestParam(defaultValue="")String q,@RequestParam(required=false)Career.Status status,@RequestParam(required=false)UUID academicUnitId){return service.careers(page,size,q,status,academicUnitId);}
    @GetMapping("/academic-units") public CatalogPage<AcademicUnit> units(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size,@RequestParam(defaultValue="")String q){return service.units(page,size,q);}
    @GetMapping("/careers/{id}") public ResponseEntity<Career> career(@PathVariable UUID id){var value=service.getCareer(id);return ResponseEntity.ok().eTag(tag(value.revision())).body(value);}
    @GetMapping("/academic-units/{id}") public ResponseEntity<AcademicUnit> unit(@PathVariable UUID id){var value=service.getUnit(id);return ResponseEntity.ok().eTag(tag(value.revision())).body(value);}
    @GetMapping("/careers/{id}/history") public CatalogPage<Career> history(@PathVariable UUID id,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){return service.history(id,page,size);}
    @PostMapping("/careers") public ResponseEntity<Career> createCareer(@Valid @RequestBody CareerInput input,HttpServletRequest request){var value=service.createCareer(input.data(),SelfAssessmentController.correlation(request));return ResponseEntity.created(URI.create("/api/v1/careers/"+value.id())).eTag(tag(value.revision())).body(value);}
    @PostMapping("/academic-units") public ResponseEntity<AcademicUnit> createUnit(@Valid @RequestBody UnitInput input,HttpServletRequest request){var value=service.createUnit(input.data(),SelfAssessmentController.correlation(request));return ResponseEntity.created(URI.create("/api/v1/academic-units/"+value.id())).eTag(tag(value.revision())).body(value);}
    @PatchMapping("/careers/{id}") public ResponseEntity<Career> updateCareer(@PathVariable UUID id,@RequestHeader(value="If-Match",required=false)String match,@Valid @RequestBody CareerInput input,HttpServletRequest request){var value=service.updateCareer(id,EntityTagSupport.require(match),input.data(),SelfAssessmentController.correlation(request));return ResponseEntity.ok().eTag(tag(value.revision())).body(value);}
    @PatchMapping("/academic-units/{id}") public ResponseEntity<AcademicUnit> updateUnit(@PathVariable UUID id,@RequestHeader(value="If-Match",required=false)String match,@Valid @RequestBody UnitInput input,HttpServletRequest request){var value=service.updateUnit(id,EntityTagSupport.require(match),input.data(),SelfAssessmentController.correlation(request));return ResponseEntity.ok().eTag(tag(value.revision())).body(value);}
    @PostMapping("/careers/{id}/state-decisions") public ResponseEntity<Career> careerState(@PathVariable UUID id,@RequestHeader(value="If-Match",required=false)String match,@Valid @RequestBody StateChange input,HttpServletRequest request){var value=service.changeCareerState(id,EntityTagSupport.require(match),input.status(),input.reason(),SelfAssessmentController.correlation(request));return ResponseEntity.status(201).eTag(tag(value.revision())).body(value);}
    @PostMapping("/academic-units/{id}/state-decisions") public ResponseEntity<AcademicUnit> unitState(@PathVariable UUID id,@RequestHeader(value="If-Match",required=false)String match,@Valid @RequestBody StateChange input,HttpServletRequest request){var value=service.changeUnitState(id,EntityTagSupport.require(match),input.status(),input.reason(),SelfAssessmentController.correlation(request));return ResponseEntity.status(201).eTag(tag(value.revision())).body(value);}
    private static String tag(long revision){return EntityTagSupport.strong(revision);}
    public record CareerInput(@NotBlank @Size(max=100)String code,@NotBlank @Size(max=300)String name,@NotNull UUID academicUnitId,@NotNull LocalDate validFrom,LocalDate validUntil){
        Career.Data data(){return new Career.Data(code,name,academicUnitId,validFrom,validUntil);}
        @com.fasterxml.jackson.annotation.JsonAnySetter public void unknown(String name,Object value){throw new IllegalArgumentException("unknown catalog field");}
    }
    public record UnitInput(@NotBlank @Size(max=100)String code,@NotBlank @Size(max=300)String name){
        AcademicUnit.Data data(){return new AcademicUnit.Data(code,name);}
        @com.fasterxml.jackson.annotation.JsonAnySetter public void unknown(String name,Object value){throw new IllegalArgumentException("unknown catalog field");}
    }
    public record StateChange(@NotNull Career.Status status,@NotBlank @Size(min=10,max=4000)String reason){
        @com.fasterxml.jackson.annotation.JsonAnySetter public void unknown(String name,Object value){throw new IllegalArgumentException("unknown catalog field");}
    }
}
