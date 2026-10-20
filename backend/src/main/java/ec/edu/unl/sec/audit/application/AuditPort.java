package ec.edu.unl.sec.audit.application;

import java.util.UUID;

public interface AuditPort {
    UUID record(AuditEventCommand command);
}
