package ec.edu.unl.sec.selfassessment.domain;
import static org.assertj.core.api.Assertions.*;
import java.util.Set;
import org.junit.jupiter.api.Test;
class ProcessStateMachineTest {
    private static final Set<String> VALID = Set.of("DRAFT:ACTIVE", "PREPARATION:ACTIVE", "ACTIVE:SUSPENDED",
            "SUSPENDED:ACTIVE", "ACTIVE:DOCUMENTARY_CLOSED", "DOCUMENTARY_CLOSED:CLOSING",
            "DOCUMENTARY_CLOSED:REOPENED", "CLOSING:CLOSED", "CLOSING:DOCUMENTARY_CLOSED",
            "CLOSED:REOPENED", "REOPENED:ACTIVE", "REOPENED:DOCUMENTARY_CLOSED");
    @Test void validatesEveryPairAgainstTheDocumentedLifecycle() {
        for (var from : ProcessState.values()) for (var to : ProcessState.values()) {
            if (VALID.contains(from + ":" + to)) assertThatCode(() -> ProcessStateMachine.require(from,to)).doesNotThrowAnyException();
            else assertThatThrownBy(() -> ProcessStateMachine.require(from,to)).isInstanceOf(IllegalStateException.class);
        }
    }
}
