package ec.edu.unl.sec.selfassessment.domain;
import static org.assertj.core.api.Assertions.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
class FixedModelSnapshotTest {
    @Test void activationFixesExactModelIndicatorsAndAuthorization() {
        UUID model=UUID.randomUUID(), indicator=UUID.randomUUID(), actor=UUID.randomUUID();
        var preparation=new SelfAssessmentProcess(null,UUID.randomUUID(),"P-1","Proceso",LocalDate.now(),LocalDate.now().plusDays(1),model,1,ProcessState.PREPARATION,0,actor,Instant.now(),List.of());
        var decision=new ProcessAuthorization(null,UUID.randomUUID(),"Designación sintética","fixture://autorizacion","Autorización sintética",actor,Instant.now());
        var active=preparation.authorize(decision).activate(List.of(new SelfAssessmentProcess.IndicatorSnapshot(indicator,"I-1","Indicador")));
        assertThat(active.modelVersionId()).isEqualTo(model);
        assertThat(active.modelVersionNumber()).isEqualTo(1);
        assertThat(active.authorization()).isEqualTo(decision);
        assertThat(active.indicators()).extracting(SelfAssessmentProcess.IndicatorSnapshot::id).containsExactly(indicator);
        assertThat(active.revision()).isEqualTo(2);
        assertThatThrownBy(()->preparation.transition(ProcessState.ACTIVE)).isInstanceOf(IllegalStateException.class);
    }
}
