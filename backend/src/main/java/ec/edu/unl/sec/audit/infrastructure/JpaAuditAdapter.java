package ec.edu.unl.sec.audit.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.unl.sec.audit.application.AuditEventCommand;
import ec.edu.unl.sec.audit.application.AuditPort;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(propagation = Propagation.MANDATORY)
public class JpaAuditAdapter implements AuditPort {
    private final AuditEventRepository repository;
    private final ObjectMapper objectMapper;

    public JpaAuditAdapter(AuditEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public UUID record(AuditEventCommand command) {
        UUID id = UUID.randomUUID();
        repository.save(new AuditEventEntity(id, command, serialize(command)));
        return id;
    }

    private String serialize(AuditEventCommand command) {
        try {
            return objectMapper.writeValueAsString(command.metadata());
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Audit metadata must be serializable", exception);
        }
    }
}

interface AuditEventRepository extends JpaRepository<AuditEventEntity, UUID> {}

@Entity
@Table(name = "audit_event")
class AuditEventEntity {
    @Id UUID id;
    @Column(name = "occurred_at", nullable = false) Instant occurredAt;
    @Column(name = "actor_user_id") UUID actorUserId;
    @Column(name = "technical_account_id") UUID technicalAccountId;
    @Column(name = "correlation_id", nullable = false) UUID correlationId;
    @Column(nullable = false, length = 100) String action;
    @Column(name = "object_type", nullable = false, length = 100) String objectType;
    @Column(name = "object_id", nullable = false) UUID objectId;
    @Column(name = "scope_career_id") UUID scopeCareerId;
    @Column(name = "scope_process_id") UUID scopeProcessId;
    @Column(nullable = false, length = 30) String outcome;
    @Column(name = "reason_code", length = 100) String reasonCode;
    @Column(name = "metadata_json", nullable = false, columnDefinition = "jsonb") String metadataJson;

    protected AuditEventEntity() {}

    AuditEventEntity(UUID id, AuditEventCommand command, String metadataJson) {
        this.id = id;
        this.occurredAt = command.occurredAt();
        this.actorUserId = command.actorUserId();
        this.technicalAccountId = command.technicalAccountId();
        this.correlationId = command.correlationId();
        this.action = command.action();
        this.objectType = command.objectType();
        this.objectId = command.objectId();
        this.scopeCareerId = command.scopeCareerId();
        this.scopeProcessId = command.scopeProcessId();
        this.outcome = command.outcome();
        this.reasonCode = command.reasonCode();
        this.metadataJson = metadataJson;
    }
}
