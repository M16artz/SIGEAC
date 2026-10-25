package ec.edu.unl.sec.academicevaluation.application;

import ec.edu.unl.sec.academicevaluation.domain.*;
import ec.edu.unl.sec.academicevaluation.domain.QuantitativeEvaluation.*;
import ec.edu.unl.sec.audit.application.*;
import java.util.*;
import java.time.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VariableValueService {
    private final QuantitativeContext context;
    private final QuantitativeRepository repository;
    private final AuditPort audit;
    public VariableValueService(QuantitativeContext context,QuantitativeRepository repository,AuditPort audit) {
        this.context=context;this.repository=repository;this.audit=audit;
    }
    @Transactional(readOnly=true)
    public Inputs get(UUID processId,UUID indicatorId,UUID id) {
        context.require(processId,indicatorId,false);
        return repository.inputs(id).filter(i->i.processId().equals(processId) && i.indicatorId().equals(indicatorId))
                .orElseThrow(()->new AcademicProblemException(AcademicProblemException.Kind.NOT_FOUND,"INPUT_SET_NOT_FOUND","No se encontró el conjunto en este indicador."));
    }
    @Transactional
    public Inputs capture(UUID processId,UUID indicatorId,long expected,Command command,UUID correlation) {
        var c=context.require(processId,indicatorId,true);var actor=c.scope().actorId();
        var latest=repository.latest(processId,indicatorId,actor);
        if(expected!=latest.map(Inputs::revision).orElse(0L) || !Objects.equals(command.predecessorId(),latest.map(Inputs::id).orElse(null))) throw QuantitativeContext.stale();
        var period=c.scope().process();
        if(command.periodStart()==null || command.periodEnd()==null || command.periodStart().isAfter(command.periodEnd())
                || period.periodStart()==null || period.periodEnd()==null || command.periodStart().isBefore(period.periodStart()) || command.periodEnd().isAfter(period.periodEnd()))
            throw QuantitativeContext.invalid("INVALID_VARIABLE_PERIOD","El período debe estar dentro del período del proceso.");
        if(command.variables()==null || !command.variables().keySet().equals(Set.of("A","B")))
            throw QuantitativeContext.invalid("MISSING_VARIABLES","Se requieren todas las variables declaradas, sin variables adicionales.");
        var values=new ArrayList<Variable>();
        for(var definition:c.rule().variables()) {
            var input=command.variables().get(definition.code());
            if(input==null || !definition.unit().equals(input.unit()) || input.evidenceVersionIds()==null || input.evidenceVersionIds().isEmpty()
                    || input.evidenceVersionIds().size()>100 || input.evidenceVersionIds().stream().anyMatch(Objects::isNull)
                    || input.evidenceVersionIds().stream().distinct().count()!=input.evidenceVersionIds().size())
                throw QuantitativeContext.invalid("INVALID_VARIABLE","Revise unidad y sustentos de cada variable.");
            var supports=c.supports().stream().filter(s->input.evidenceVersionIds().contains(s.evidenceVersionId())).toList();
            if(supports.stream().map(ManualAcademicEvaluation.Support::evidenceVersionId).distinct().count()!=input.evidenceVersionIds().size())
                throw QuantitativeContext.invalid("INVALID_SUPPORTS","Una variable referencia sustentos no aceptados o ajenos al indicador.");
            try { values.add(new Variable(definition.code(),RatioPercentStrategy.value(input.value(),definition),input.unit(),supports)); }
            catch(IllegalArgumentException ex) { throw QuantitativeContext.invalid("INVALID_VARIABLE_VALUE","Use valores no negativos de hasta 12 enteros y 6 decimales; respete el tipo y denominador positivo."); }
        }
        var result=new Inputs(UUID.randomUUID(),processId,indicatorId,period.modelVersionId(),c.rule().id(),actor,Instant.now(),expected+1,
                command.predecessorId(),command.periodStart(),command.periodEnd(),values);
        repository.append(result);audit(c,result.id(),"ACADEMIC_VARIABLES_CAPTURED",correlation);
        return result;
    }
    @Transactional
    public Verification verify(UUID processId,UUID indicatorId,UUID id,long expected,String reason,UUID correlation) {
        var c=context.require(processId,indicatorId,true);var inputs=requireCurrent(c,processId,indicatorId,id,expected);
        if(repository.verification(id).isPresent()) throw new AcademicProblemException(AcademicProblemException.Kind.CONFLICT,"VARIABLES_ALREADY_VERIFIED","Este conjunto ya está verificado; capture otra revisión para corregirlo.");
        Verification verification;
        try { verification=new Verification(inputs.id(),c.scope().actorId(),Instant.now(),reason); }
        catch(IllegalArgumentException ex) { throw QuantitativeContext.invalid("INVALID_VERIFICATION_REASON","La verificación exige un fundamento de 10 a 4000 caracteres."); }
        repository.verify(verification);audit(c,id,"ACADEMIC_VARIABLES_VERIFIED",correlation);return verification;
    }
    Inputs requireCurrent(QuantitativeContext.Context c,UUID processId,UUID indicatorId,UUID id,long expected) {
        var inputs=repository.inputs(id).filter(i->i.processId().equals(processId) && i.indicatorId().equals(indicatorId))
                .orElseThrow(()->new AcademicProblemException(AcademicProblemException.Kind.NOT_FOUND,"INPUT_SET_NOT_FOUND","No se encontró el conjunto de variables en este indicador."));
        if(inputs.revision()!=expected || !repository.latest(processId,indicatorId,inputs.capturedBy()).map(Inputs::id).orElseThrow().equals(id)) throw QuantitativeContext.stale();
        if(!inputs.modelVersionId().equals(c.scope().process().modelVersionId()) || !inputs.ruleId().equals(c.rule().id())
                || !c.supports().containsAll(inputs.variables().stream().flatMap(v->v.supports().stream()).toList()))
            throw QuantitativeContext.invalid("INVALIDATED_VARIABLE_SUPPORTS","Los sustentos o la regla cambiaron; capture y verifique nuevas variables.");
        return inputs;
    }
    private void audit(QuantitativeContext.Context c,UUID id,String action,UUID correlation) {
        audit.record(new AuditEventCommand(Instant.now(),c.scope().actorId(),null,correlation,action,"ACADEMIC_INPUT_SET",id,
                c.scope().process().careerId(),c.scope().process().id(),"SUCCESS",null,Map.of()));
    }
    public record ValueInput(String value,String unit,List<UUID> evidenceVersionIds) {}
    public record Command(LocalDate periodStart,LocalDate periodEnd,Map<String,ValueInput> variables,UUID predecessorId) {}
}
