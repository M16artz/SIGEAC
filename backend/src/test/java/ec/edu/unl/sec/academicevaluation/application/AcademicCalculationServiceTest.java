package ec.edu.unl.sec.academicevaluation.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import ec.edu.unl.sec.academicevaluation.domain.*;
import ec.edu.unl.sec.academicevaluation.domain.QuantitativeEvaluation.*;
import ec.edu.unl.sec.evaluationmodel.domain.*;
import ec.edu.unl.sec.selfassessment.application.DocumentaryProcessPort;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;

class AcademicCalculationServiceTest {
    final AcademicEvaluationServiceTest base=new AcademicEvaluationServiceTest();
    final Store store=new Store();
    QuantitativeContext context;VariableValueService variables;AcademicCalculationService calculations;
    ValuationRule rule;
    @BeforeEach void setup() {
        base.setup();
        var process=new DocumentaryProcessPort.Context(base.process,base.career,base.model,"DOCUMENTARY_CLOSED",7,List.of(base.indicator),LocalDate.of(2026,1,1),LocalDate.of(2026,12,31));
        when(base.processes.context(base.process)).thenReturn(Optional.of(process));when(base.processes.lock(base.process)).thenReturn(Optional.of(process));
        rule=new ValuationRule(null,base.indicator,ValuationRule.Type.QUANTITATIVE,"Método porcentual sintético","A / B * 100","ratio-percent","1",base.scale.id(),
                List.of(new ValuationVariable("A","Cantidad A","personas",ValuationVariable.DataType.INTEGER,true,"NON_NEGATIVE"),new ValuationVariable("B","Cantidad B","personas",ValuationVariable.DataType.INTEGER,true,"POSITIVE")),
                List.of(new ValuationThreshold(base.scale.options().getFirst().id(),java.math.BigDecimal.ZERO,null,true,false)),"REJECT",ValuationRule.Status.VALIDATED);
        base.model(rule,IndicatorDefinition.Nature.QUANTITATIVE);
        context=new QuantitativeContext(base.service,base.models,()->Set.of(RatioPercentStrategy.REFERENCE));
        variables=new VariableValueService(context,store,base.audit);calculations=new AcademicCalculationService(context,variables,store,base.audit);
    }
    VariableValueService.Command command(String a,String b,UUID predecessor) {
        return new VariableValueService.Command(LocalDate.of(2026,1,1),LocalDate.of(2026,6,30),Map.of(
                "A",new VariableValueService.ValueInput(a,"personas",List.of(base.version)),"B",new VariableValueService.ValueInput(b,"personas",List.of(base.version))),predecessor);
    }
    Inputs capture() {return variables.capture(base.process,base.indicator,0,command("3","4",null),UUID.randomUUID());}
    Verification verify(Inputs inputs) {return variables.verify(base.process,base.indicator,inputs.id(),inputs.revision(),"Verificación explícita de fuentes, cifras y período.",UUID.randomUUID());}
    Run calculate(Inputs inputs) {return calculations.calculate(base.process,base.indicator,inputs.id(),inputs.revision(),"Justificación académica del resultado.","Limitaciones sintéticas del piloto.",UUID.randomUUID());}
    void rejected(String code,Runnable action) {assertThatThrownBy(action::run).isInstanceOf(AcademicProblemException.class).extracting("code").isEqualTo(code);}
    @Test void capturesVerifiesAndStoresExactRuleInputsAndThreshold() {
        var input=capture();var verification=verify(input);var run=calculate(input);
        assertThat(run.result()).isEqualByComparingTo("75");assertThat(run.rule()).isEqualTo(rule);assertThat(run.inputs()).isEqualTo(input);
        assertThat(run.verification()).isEqualTo(verification);assertThat(run.threshold()).isEqualTo(rule.thresholds().getFirst());
        assertThat(calculations.view(base.process,base.indicator,0,20).items()).containsExactly(run);
        org.mockito.Mockito.verify(base.processes,never()).reserveSnapshot(any(),anyLong());org.mockito.Mockito.verify(base.processes,never()).completeClosure(any(),anyLong(),any(),any(),any());
    }
    @Test void rejectsUnverifiedInputsAndDuplicateCalculation() {
        var inputs=capture();rejected("VARIABLES_UNVERIFIED",()->calculate(inputs));assertThat(store.runs).isEmpty();
        verify(inputs);calculate(inputs);rejected("INPUTS_ALREADY_CALCULATED",()->calculate(inputs));assertThat(store.runs).hasSize(1);
    }
    @Test void correctionPreservesEarlierInputsVerificationAndResult() {
        var first=capture();verify(first);var run=calculate(first);
        var second=variables.capture(base.process,base.indicator,1,command("2","4",first.id()),UUID.randomUUID());
        assertThat(store.inputs).containsValue(first);assertThat(store.runs).containsValue(run);
        rejected("STALE_VARIABLE_SET",()->calculate(first));rejected("VARIABLES_UNVERIFIED",()->calculate(second));
        verify(second);assertThat(calculate(second).result()).isEqualByComparingTo("50");assertThat(run.result()).isEqualByComparingTo("75");
    }
    @Test void rejectsStaleOrForeignPredecessor() {
        capture();rejected("STALE_VARIABLE_SET",()->capture());
        rejected("STALE_VARIABLE_SET",()->variables.capture(base.process,base.indicator,1,command("2","4",UUID.randomUUID()),UUID.randomUUID()));
    }
    @Test void requiresCompleteVariablesAndTheirExactUnitsAndPeriod() {
        var command=command("3","4",null);
        rejected("MISSING_VARIABLES",()->variables.capture(base.process,base.indicator,0,new VariableValueService.Command(command.periodStart(),command.periodEnd(),Map.of("A",command.variables().get("A")),null),UUID.randomUUID()));
        rejected("INVALID_VARIABLE_PERIOD",()->variables.capture(base.process,base.indicator,0,new VariableValueService.Command(LocalDate.of(2025,1,1),command.periodEnd(),command.variables(),null),UUID.randomUUID()));
        var entries=new HashMap<>(command.variables());entries.put("B",new VariableValueService.ValueInput("4","otra unidad",List.of(base.version)));
        rejected("INVALID_VARIABLE",()->variables.capture(base.process,base.indicator,0,new VariableValueService.Command(command.periodStart(),command.periodEnd(),entries,null),UUID.randomUUID()));
        rejected("INVALID_VARIABLE_VALUE",()->variables.capture(base.process,base.indicator,0,command("3","0",null),UUID.randomUUID()));
    }
    @Test void supportRectificationInvalidatesVerificationAndCalculationWithoutDeletingHistory() {
        var inputs=capture();base.outcome("OBSERVED");rejected("INVALIDATED_VARIABLE_SUPPORTS",()->verify(inputs));
        base.outcome("ACCEPTED");verify(inputs);base.outcome("REJECTED");rejected("INVALIDATED_VARIABLE_SUPPORTS",()->calculate(inputs));
        assertThat(variables.get(base.process,base.indicator,inputs.id())).isEqualTo(inputs);
    }
    @Test void foreignScopeCannotCaptureVerifyOrRead() {
        when(base.auth.isAllowed(any(),any(),any())).thenReturn(false);rejected("RESOURCE_NOT_FOUND",()->capture());
        rejected("RESOURCE_NOT_FOUND",()->calculations.view(base.process,base.indicator,0,20));org.mockito.Mockito.verify(base.processes,never()).lock(any());
    }
    @Test void technicalAccountsCannotCaptureAndSuspendedProcessesCannotBeModified() {
        when(base.actors.currentActor()).thenReturn(Optional.of(new ec.edu.unl.sec.identityaccess.application.CurrentActorPort.Actor(base.actor,true)));
        rejected("EVALUATION_DENIED",()->capture());
        when(base.actors.currentActor()).thenReturn(Optional.of(new ec.edu.unl.sec.identityaccess.application.CurrentActorPort.Actor(base.actor,false)));
        base.state("SUSPENDED");rejected("ACADEMIC_PHASE_CLOSED",()->capture());
        assertThat(store.inputs).isEmpty();
    }
    @Test void missingRegistryAndUnsupportedRuleDoNotProduceResults() {
        var unavailable=new QuantitativeContext(base.service,base.models,Set::of);
        rejected("RULE_NOT_VALIDATED",()->unavailable.require(base.process,base.indicator,true));
        base.model(new ValuationRule(rule.id(),base.indicator,rule.ruleType(),rule.methodDefinition(),"B / A","ratio-percent","1",base.scale.id(),rule.variables(),rule.thresholds(),"REJECT",rule.status()),IndicatorDefinition.Nature.QUANTITATIVE);
        rejected("UNSUPPORTED_QUANTITATIVE_RULE",()->capture());assertThat(store.inputs).isEmpty();
    }
    @Test void duplicateVerificationAndEmptyReasonDoNotOverwriteVerification() {
        var input=capture();rejected("INVALID_VERIFICATION_REASON",()->variables.verify(base.process,base.indicator,input.id(),1,"",UUID.randomUUID()));
        var first=verify(input);rejected("VARIABLES_ALREADY_VERIFIED",()->verify(input));assertThat(store.verifications.get(input.id())).isEqualTo(first);
    }
    static class Store implements QuantitativeRepository {
        public List<Run> current(UUID p,UUID i){return runs.values().stream().filter(v->v.inputs().processId().equals(p)&&v.inputs().indicatorId().equals(i))
                .collect(java.util.stream.Collectors.toMap(Run::executedBy,v->v,(a,b)->a.executedAt().isAfter(b.executedAt())?a:b)).values().stream().toList();}
        final Map<UUID,Inputs> inputs=new LinkedHashMap<>();final Map<UUID,Verification> verifications=new HashMap<>();final Map<UUID,Run> runs=new LinkedHashMap<>();
        public Optional<Inputs> inputs(UUID id){return Optional.ofNullable(inputs.get(id));}
        public Optional<Inputs> latest(UUID p,UUID i,UUID a){return inputs.values().stream().filter(v->v.processId().equals(p)&&v.indicatorId().equals(i)&&v.capturedBy().equals(a)).max(Comparator.comparing(Inputs::revision));}
        public Optional<Verification> verification(UUID id){return Optional.ofNullable(verifications.get(id));}
        public Optional<Run> run(UUID id){return Optional.ofNullable(runs.get(id));}
        public List<Run> history(UUID p,UUID i,long offset,int limit){return runs.values().stream().filter(v->v.inputs().processId().equals(p)&&v.inputs().indicatorId().equals(i)).skip(offset).limit(limit).toList();}
        public void append(Inputs v){inputs.put(v.id(),v);}public void verify(Verification v){verifications.put(v.inputSetId(),v);}public void append(Run v){runs.put(v.inputs().id(),v);}
    }
}
