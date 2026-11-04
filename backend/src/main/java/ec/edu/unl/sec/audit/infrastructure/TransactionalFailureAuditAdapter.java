package ec.edu.unl.sec.audit.infrastructure;
import ec.edu.unl.sec.audit.application.*;import java.util.UUID;import org.springframework.stereotype.Component;
@Component public class TransactionalFailureAuditAdapter implements FailureAuditPort{private final AuditPort audit;public TransactionalFailureAuditAdapter(AuditPort audit){this.audit=audit;}public UUID recordAfterRollback(AuditEventCommand command){return audit.recordRejection(command);}}
