package ec.edu.unl.sec.evaluationmodel.application;

import static org.assertj.core.api.Assertions.*;import static org.mockito.ArgumentMatchers.any;import static org.mockito.Mockito.*;
import ec.edu.unl.sec.audit.application.*;import ec.edu.unl.sec.evaluationmodel.support.ModelFixtures;import java.util.*;import org.junit.jupiter.api.Test;
class EvaluationModelAuditTest {
 @Test void removesSensitiveMetadataAndUsesAppendOnlyPort(){AuditPort port=mock(AuditPort.class);when(port.record(any())).thenReturn(UUID.randomUUID());var service=new EvaluationModelAuditService(port);service.record(ModelFixtures.AUTHOR,UUID.randomUUID(),"MODEL_CREATED","EVALUATION_MODEL",ModelFixtures.MODEL,Map.of("token","unsafe","source","synthetic"));var captor=org.mockito.ArgumentCaptor.forClass(AuditEventCommand.class);verify(port).record(captor.capture());assertThat(captor.getValue().metadata()).containsEntry("source","synthetic").doesNotContainKey("token");}
}
