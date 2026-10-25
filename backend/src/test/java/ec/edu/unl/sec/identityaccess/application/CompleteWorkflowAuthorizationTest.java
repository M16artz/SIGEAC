package ec.edu.unl.sec.identityaccess.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ec.edu.unl.sec.identityaccess.domain.AuthorizationScope;
import ec.edu.unl.sec.identityaccess.domain.OperationCode;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CompleteWorkflowAuthorizationTest {
    @Test
    void allowsScopedOperationsForP01ToP16() {
        var auth = mock(AuthorizationPort.class);
        UUID actor = UUID.randomUUID();
        UUID career = UUID.randomUUID();
        UUID process = UUID.randomUUID();
        var scope = new AuthorizationScope(career, process, null);
        when(auth.isAllowed(actor, OperationCode.CAREER_ADMIN, scope)).thenReturn(true);
        when(auth.isAllowed(actor, OperationCode.MODEL_APPLICABILITY_AUTHORIZE, scope)).thenReturn(true);
        when(auth.isAllowed(actor, OperationCode.PROCESS_AUTHORIZE, scope)).thenReturn(true);
        when(auth.isAllowed(actor, OperationCode.PROCESS_COORDINATE, scope)).thenReturn(true);
        when(auth.isAllowed(actor, OperationCode.PROCESS_MANAGE, scope)).thenReturn(true);
        when(auth.isAllowed(actor, OperationCode.EVIDENCE_REVIEW, scope)).thenReturn(true);
        when(auth.isAllowed(actor, OperationCode.DOCUMENTARY_SNAPSHOT_CREATE, scope)).thenReturn(true);
        when(auth.isAllowed(actor, OperationCode.PROCESS_DOCUMENTARY_CLOSE, scope)).thenReturn(true);
        assertThat(auth.isAllowed(actor, OperationCode.CAREER_ADMIN, scope)).isTrue();
        assertThat(auth.isAllowed(actor, OperationCode.MODEL_APPLICABILITY_AUTHORIZE, scope)).isTrue();
        assertThat(auth.isAllowed(actor, OperationCode.PROCESS_DOCUMENTARY_CLOSE, scope)).isTrue();
    }
}
