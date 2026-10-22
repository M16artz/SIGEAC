package ec.edu.unl.sec.selfassessment.application;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import ec.edu.unl.sec.identityaccess.application.*;
import ec.edu.unl.sec.selfassessment.domain.*;
import ec.edu.unl.sec.selfassessment.support.InMemorySelfAssessmentRepository;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
class ProcessTransitionServiceTest {
    @Test void preventsActivationAndClosureByGenericTransitionButAllowsSuspension() {
        var repo=new InMemorySelfAssessmentRepository(); UUID actor=UUID.randomUUID();
        var process=new SelfAssessmentProcess(null,UUID.randomUUID(),"P-1","Proceso",LocalDate.now(),LocalDate.now().plusDays(1),UUID.randomUUID(),1,ProcessState.PREPARATION,0,actor,Instant.now(),List.of());
        repo.save(process,-1);
        var auth=mock(AuthorizationPort.class); when(auth.isAllowed(any(),any(),any())).thenReturn(true);
        var service=new ProcessTransitionService(repo,()->Optional.of(new CurrentActorPort.Actor(actor,false)),auth,mock(SelfAssessmentAuditService.class));
        assertThatThrownBy(()->service.transition(process.id(),0,ProcessState.ACTIVE,"Inicio sin autorización",UUID.randomUUID()))
                .isInstanceOf(ProcessProblemException.class).extracting("code").isEqualTo("ACTIVATION_REQUIRED");
        var active=new SelfAssessmentProcess(process.id(),process.careerId(),process.code(),process.name(),process.periodStart(),process.periodEnd(),process.modelVersionId(),1,ProcessState.ACTIVE,1,actor,Instant.now(),List.of());
        repo.processes.put(active.id(),active);
        for(var target:List.of(ProcessState.DOCUMENTARY_CLOSED,ProcessState.CLOSING,ProcessState.CLOSED))
            assertThatThrownBy(()->service.transition(active.id(),1,target,"Cierre sin validaciones",UUID.randomUUID()))
                    .isInstanceOf(ProcessProblemException.class).extracting("code").isEqualTo("CLOSURE_POLICY_REQUIRED");
        var suspended=service.transition(active.id(),1,ProcessState.SUSPENDED,"Suspensión justificada",UUID.randomUUID());
        assertThat(suspended.state()).isEqualTo(ProcessState.SUSPENDED);
        assertThat(service.transition(active.id(),2,ProcessState.ACTIVE,"Reanudación justificada",UUID.randomUUID()).revision()).isEqualTo(3);
    }
}
