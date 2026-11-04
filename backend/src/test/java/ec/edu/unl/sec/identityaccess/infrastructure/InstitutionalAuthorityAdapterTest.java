package ec.edu.unl.sec.identityaccess.infrastructure;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import ec.edu.unl.sec.identityaccess.application.IdentityAccessRepository;
import ec.edu.unl.sec.identityaccess.domain.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
class InstitutionalAuthorityAdapterTest {
    @Test void requiresAnActiveOwnAssignmentWithTheOperationAndRequestedScope() {
        var repository=mock(IdentityAccessRepository.class); UUID actor=UUID.randomUUID(), function=UUID.randomUUID(), career=UUID.randomUUID(), id=UUID.randomUUID();
        var scope=new AuthorizationScope(career,UUID.randomUUID(),null);
        var assignment=new FunctionalAssignment(id,actor,function,new AuthorizationScope(career,null,null),Instant.now().minusSeconds(60),Instant.now().plusSeconds(60),actor,null,0);
        when(repository.findUser(actor)).thenReturn(Optional.of(new IdentityAccessRepository.UserRecord(actor,"authority","Authority",false,true,0)));
        when(repository.activeAssignments(eq(actor),any())).thenReturn(List.of(assignment));
        var adapter=new InstitutionalAuthorityAdapter(repository);
        assertThat(adapter.isAuthorizedAssignment(id,actor,OperationCode.PROCESS_AUTHORIZE,scope)).isFalse();
        when(repository.functionAllows(function,OperationCode.PROCESS_AUTHORIZE)).thenReturn(true);
        assertThat(adapter.isAuthorizedAssignment(id,actor,OperationCode.PROCESS_AUTHORIZE,scope)).isTrue();
        assertThat(adapter.isAuthorizedAssignment(UUID.randomUUID(),actor,OperationCode.PROCESS_AUTHORIZE,scope)).isFalse();
        assertThat(adapter.isAuthorizedAssignment(id,actor,OperationCode.PROCESS_AUTHORIZE,new AuthorizationScope(UUID.randomUUID(),null,null))).isFalse();
        when(repository.hasUnexcusedConflict(eq(actor),eq(function),eq(scope),any())).thenReturn(true);
        assertThat(adapter.isAuthorizedAssignment(id,actor,OperationCode.PROCESS_AUTHORIZE,scope)).isFalse();
        when(repository.findUser(actor)).thenReturn(Optional.of(new IdentityAccessRepository.UserRecord(actor,"authority","Authority",false,false,1)));
        assertThat(adapter.isAuthorizedAssignment(id,actor,OperationCode.PROCESS_AUTHORIZE,scope)).isFalse();
    }
}
