package ec.edu.unl.sec.audit.application;
import java.util.UUID;
public interface FailureAuditPort{UUID recordAfterRollback(AuditEventCommand command);}
