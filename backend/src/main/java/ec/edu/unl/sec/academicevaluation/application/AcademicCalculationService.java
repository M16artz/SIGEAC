package ec.edu.unl.sec.academicevaluation.application;

import ec.edu.unl.sec.academicevaluation.domain.*;
import ec.edu.unl.sec.academicevaluation.domain.QuantitativeEvaluation.*;
import ec.edu.unl.sec.evaluationmodel.domain.*;
import ec.edu.unl.sec.audit.application.*;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AcademicCalculationService {
    private final QuantitativeContext context;
    private final VariableValueService variables;
    private final QuantitativeRepository repository;
    private final AuditPort audit;
    public AcademicCalculationService(QuantitativeContext context,VariableValueService variables,QuantitativeRepository repository,AuditPort audit) {
        this.context=context;this.variables=variables;this.repository=repository;this.audit=audit;
    }
    @Transactional(readOnly=true)
    public View view(UUID processId,UUID indicatorId,int page,int size) {
        if(page<0 || size<1 || size>100) throw QuantitativeContext.invalid("INVALID_PAGE","Página o tamaño inválidos.");
        var c=context.require(processId,indicatorId,false);
        var own=repository.latest(processId,indicatorId,c.scope().actorId()).orElse(null);
        var history=repository.history(processId,indicatorId,(long)page*size,size+1);
        return new View(processId,indicatorId,c.scope().process().state(),c.scope().process().periodStart(),c.scope().process().periodEnd(),
                c.rule(),c.scale(),c.supports(),own,own==null?null:repository.verification(own.id()).orElse(null),
                history.stream().limit(size).toList(),page,size,history.size()>size);
    }
    @Transactional
    public Run calculate(UUID processId,UUID indicatorId,UUID inputSetId,long expected,String justification,String limitations,UUID correlation) {
        var c=context.require(processId,indicatorId,true);
        var inputs=variables.requireCurrent(c,processId,indicatorId,inputSetId,expected);
        var verification=repository.verification(inputSetId).orElseThrow(()->QuantitativeContext.invalid("VARIABLES_UNVERIFIED","Verifique explícitamente todas las variables antes de calcular."));
        if(repository.run(inputSetId).isPresent()) throw new AcademicProblemException(AcademicProblemException.Kind.CONFLICT,"INPUTS_ALREADY_CALCULATED","El conjunto ya tiene un cálculo conservado.");
        Run run;
        try {
            var result=RatioPercentStrategy.calculate(c.rule(),c.scale(),inputs.variables().stream().collect(Collectors.toMap(Variable::code,Variable::value)));
            run=new Run(UUID.randomUUID(),inputs,verification,c.rule(),c.scale(),result.value(),result.threshold(),result.option().id(),result.option().label(),
                    "DECIMAL128_HALF_EVEN_34_DIGITS",c.scope().actorId(),Instant.now(),justification,limitations);
        } catch(IllegalArgumentException ex) { throw QuantitativeContext.invalid("CALCULATION_INVALID","Revise umbrales, justificación y limitaciones; no se generó resultado."); }
        repository.append(run);
        audit.record(new AuditEventCommand(run.executedAt(),c.scope().actorId(),null,correlation,"ACADEMIC_CALCULATION_RECORDED","ACADEMIC_CALCULATION",run.id(),
                c.scope().process().careerId(),processId,"SUCCESS",null,Map.of("strategy",RatioPercentStrategy.REFERENCE.key(),"version",RatioPercentStrategy.REFERENCE.version())));
        return run;
    }
    public record View(UUID processId,UUID indicatorId,String processState,LocalDate periodStart,LocalDate periodEnd,ValuationRule rule,RatingScale scale,
            List<ManualAcademicEvaluation.Support> acceptedSupports,Inputs ownInputs,Verification verification,List<Run> items,int page,int size,boolean hasNext) {}
}
