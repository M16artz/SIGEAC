package ec.edu.unl.sec.audit.infrastructure;

import ec.edu.unl.sec.audit.application.AuditEventCommand;
import ec.edu.unl.sec.audit.application.AuditPort;
import ec.edu.unl.sec.audit.application.AuditQueryPort;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(propagation = Propagation.MANDATORY)
public class JpaAuditAdapter implements AuditPort {
    private final AuditEventRepository repository;
    private final AuditQueryRepository queryRepository;

    public JpaAuditAdapter(AuditEventRepository repository, AuditQueryRepository queryRepository) {
        this.repository = repository;
        this.queryRepository = queryRepository;
    }

    @Override
    public UUID record(AuditEventCommand command) {
        return save(command);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UUID recordRejection(AuditEventCommand command) {
        return save(command);
    }

    private UUID save(AuditEventCommand command) {
        UUID id = UUID.randomUUID();
        repository.save(new AuditEventEntity(id, command));
        return id;
    }
}

interface AuditEventRepository extends JpaRepository<AuditEventEntity, UUID> {}

@Component
class AuditQueryRepository implements AuditQueryPort {
    private final JdbcTemplate jdbc;

    AuditQueryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<AuditEventView> search(
            List<UUID> careerIds,
            boolean includeUnscoped,
            String objectType,
            UUID objectId,
            int offset,
            int limit) {
        String filter = filter(careerIds, includeUnscoped, objectType, objectId);
        return jdbc.query(
                "select id,occurred_at,correlation_id,action,object_type,object_id,outcome,reason_code,scope_career_id,scope_process_id "
                        + "from audit_event "
                        + filter
                        + " order by occurred_at desc,id offset ? limit ?",
                (r, n) -> new AuditEventView(
                        r.getObject(1, UUID.class),
                        r.getTimestamp(2).toInstant(),
                        r.getObject(3, UUID.class),
                        r.getString(4),
                        r.getString(5),
                        r.getObject(6, UUID.class),
                        r.getString(7),
                        r.getString(8),
                        r.getObject(9, UUID.class),
                        r.getObject(10, UUID.class)),
                parameters(careerIds, objectType, objectId, offset, limit));
    }

    @Override
    public long count(List<UUID> careerIds, boolean includeUnscoped, String objectType, UUID objectId) {
        Long n = jdbc.queryForObject(
                "select count(*) from audit_event " + filter(careerIds, includeUnscoped, objectType, objectId),
                Long.class,
                parameters(careerIds, objectType, objectId));
        return n == null ? 0 : n;
    }

    private static String filter(
            List<UUID> careerIds, boolean includeUnscoped, String objectType, UUID objectId) {
        StringBuilder where = new StringBuilder(" where 1=1");
        if (!careerIds.isEmpty()) {
            where.append(" and (scope_career_id = any(?::uuid[])");
            if (includeUnscoped) where.append(" or scope_career_id is null");
            where.append(')');
        } else if (includeUnscoped) {
            where.append(" and scope_career_id is null");
        } else {
            where.append(" and 1=0");
        }
        if (objectType != null && !objectType.isBlank()) where.append(" and object_type=?");
        if (objectId != null) where.append(" and object_id=?");
        return where.toString();
    }

    private static Object[] parameters(List<UUID> careerIds, String objectType, UUID objectId, Object... tail) {
        java.util.ArrayList<Object> values = new java.util.ArrayList<>();
        if (!careerIds.isEmpty()) values.add(careerIds.toArray(UUID[]::new));
        if (objectType != null && !objectType.isBlank()) values.add(objectType);
        if (objectId != null) values.add(objectId);
        values.addAll(List.of(tail));
        return values.toArray();
    }
}

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
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata_json", nullable = false, columnDefinition = "jsonb") Map<String, Object> metadata;

    protected AuditEventEntity() {}

    AuditEventEntity(UUID id, AuditEventCommand command) {
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
        this.metadata = command.metadata();
    }
}
