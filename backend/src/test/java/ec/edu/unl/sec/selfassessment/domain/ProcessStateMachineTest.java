package ec.edu.unl.sec.selfassessment.domain;
import static org.assertj.core.api.Assertions.*;import java.util.*;import org.junit.jupiter.api.Test;
class ProcessStateMachineTest{
 @Test void acceptsEveryDeclaredTransition(){Map<ProcessState,ProcessState> valid=Map.of(ProcessState.DRAFT,ProcessState.ACTIVE,ProcessState.ACTIVE,ProcessState.CLOSING,ProcessState.CLOSING,ProcessState.CLOSED,ProcessState.CLOSED,ProcessState.REOPENED,ProcessState.REOPENED,ProcessState.ACTIVE);valid.forEach((from,to)->assertThatCode(()->ProcessStateMachine.require(from,to)).doesNotThrowAnyException());}
 @Test void rejectsEveryOtherTransition(){Set<String> valid=Set.of("DRAFT:ACTIVE","ACTIVE:CLOSING","CLOSING:CLOSED","CLOSED:REOPENED","REOPENED:ACTIVE");for(var from:ProcessState.values())for(var to:ProcessState.values())if(!valid.contains(from+":"+to))assertThatThrownBy(()->ProcessStateMachine.require(from,to)).isInstanceOf(IllegalStateException.class);}
}
