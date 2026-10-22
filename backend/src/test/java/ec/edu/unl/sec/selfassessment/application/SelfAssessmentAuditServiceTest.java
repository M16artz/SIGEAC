package ec.edu.unl.sec.selfassessment.application;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import ec.edu.unl.sec.audit.application.*;
import ec.edu.unl.sec.selfassessment.domain.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
class SelfAssessmentAuditServiceTest {
    @Test void processAuditContainsTheCareerAndProcessScopeWithoutDecisionPayload() {
        UUID actor=UUID.randomUUID();
        var process=new SelfAssessmentProcess(null,UUID.randomUUID(),"P","Proceso",LocalDate.now(),LocalDate.now(),UUID.randomUUID(),1,ProcessState.PREPARATION,0,actor,Instant.now(),List.of());
        var audit=mock(AuditPort.class);
        new SelfAssessmentAuditService(audit).record(actor,null,"PROCESS_CREATED",process,"SUCCESS",null);
        var captured=ArgumentCaptor.forClass(AuditEventCommand.class); verify(audit).record(captured.capture());
        assertThat(captured.getValue().scopeCareerId()).isEqualTo(process.careerId());
        assertThat(captured.getValue().scopeProcessId()).isEqualTo(process.id());
        assertThat(captured.getValue().metadata()).isEmpty();
    }
}
