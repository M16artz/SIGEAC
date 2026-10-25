package ec.edu.unl.sec.archive.application;

import ec.edu.unl.sec.archive.domain.ArchiveRecord;
import ec.edu.unl.sec.archive.domain.IntegralClosure;
import ec.edu.unl.sec.audit.application.AuditEventCommand;
import ec.edu.unl.sec.audit.application.AuditPort;
import ec.edu.unl.sec.identityaccess.application.AuthorizationPort;
import ec.edu.unl.sec.identityaccess.application.CurrentActorPort;
import ec.edu.unl.sec.identityaccess.application.InstitutionalAuthorityPort;
import ec.edu.unl.sec.identityaccess.domain.AuthorizationScope;
import ec.edu.unl.sec.identityaccess.domain.OperationCode;
import ec.edu.unl.sec.selfassessment.application.DocumentaryProcessPort;
import ec.edu.unl.sec.shared.domain.DomainClock;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** P16 archive: seals an existing closure snapshot and verifies its digest before archiving. */
@Service
public class ArchiveService {
    private static final String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";

    private final CurrentActorPort actors;
    private final AuthorizationPort auth;
    private final InstitutionalAuthorityPort authority;
    private final DocumentaryProcessPort processes;
    private final IntegralClosureRepository repository;
    private final AuditPort audit;
    private final DomainClock clock = DomainClock.systemUtc();

    public ArchiveService(CurrentActorPort actors, AuthorizationPort auth, InstitutionalAuthorityPort authority,
            DocumentaryProcessPort processes, IntegralClosureRepository repository, AuditPort audit) {
        this.actors = Objects.requireNonNull(actors, "actors");
        this.auth = Objects.requireNonNull(auth, "auth");
        this.authority = Objects.requireNonNull(authority, "authority");
        this.processes = Objects.requireNonNull(processes, "processes");
        this.repository = Objects.requireNonNull(repository, "repository");
        this.audit = Objects.requireNonNull(audit, "audit");
    }

    public record Provenance(UUID authorityAssignmentId, String legalBasis, String documentReference,
            String reason) {}

    public record ArchiveCommand(UUID closureSnapshotId, String retentionPolicyVersion, Provenance provenance) {}

    @Transactional
    public ArchiveRecord archive(UUID processId, ArchiveCommand command, UUID correlationId) {
        var actor = actor(OperationCode.ARCHIVE_WRITE);
        if (command == null || command.closureSnapshotId() == null || command.provenance() == null
                || command.retentionPolicyVersion() == null || command.retentionPolicyVersion().isBlank()
                || command.retentionPolicyVersion().length() > 100)
            throw problem(ArchiveProblemException.Kind.INVALID, "ARCHIVE_INVALID",
                    "El archivo requiere una instantánea de cierre, política de retención y procedencia institucional.");
        var context = processes.lock(processId)
                .orElseThrow(() -> problem(ArchiveProblemException.Kind.NOT_FOUND, RESOURCE_NOT_FOUND,
                        "El proceso indicado no existe."));
        var scope = new AuthorizationScope(context.careerId(), context.id(), null);
        if (!auth.isAllowed(actor, OperationCode.ARCHIVE_WRITE, scope)) throw hidden();
        if (!"CLOSED".equals(context.state()))
            throw problem(ArchiveProblemException.Kind.CONFLICT, "ARCHIVE_STATE_INVALID",
                    "Solo un proceso cerrado integralmente puede archivarse.");
        var closure = repository.findForProcess(processId, command.closureSnapshotId())
                .orElseThrow(() -> problem(ArchiveProblemException.Kind.NOT_FOUND, RESOURCE_NOT_FOUND,
                        "La instantánea de cierre indicada no existe en este proceso."));
        provenance(actor, scope, command.provenance());
        verifyIntegrity(closure);
        if (repository.archiveForClosure(closure.id()).isPresent())
            throw problem(ArchiveProblemException.Kind.CONFLICT, "ARCHIVE_ALREADY_EXISTS",
                    "La instantánea de cierre ya se encuentra archivada.");
        var now = clock.now();
        var record = new ArchiveRecord(UUID.randomUUID(), processId, closure.id(),
                command.retentionPolicyVersion(), actor, now, closure.sha256(), ArchiveRecord.VERIFIED);
        try {
            repository.appendArchive(record);
        } catch (org.springframework.dao.DataIntegrityViolationException duplicate) {
            throw problem(ArchiveProblemException.Kind.CONFLICT, "ARCHIVE_ALREADY_EXISTS",
                    "La instantánea de cierre ya se encuentra archivada.");
        }
        audit.record(new AuditEventCommand(now, actor, null, correlationId, "PROCESS_ARCHIVED",
                "ARCHIVE_RECORD", record.id(), context.careerId(), processId, "SUCCESS", null,
                Map.of("closureSnapshotId", closure.id().toString(),
                        "retentionPolicyVersion", record.retentionPolicyVersion(),
                        "integrityStatus", record.integrityStatus())));
        return record;
    }

    private void verifyIntegrity(IntegralClosure closure) {
        String digest;
        try {
            digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(closure.payloadJson().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
        if (!digest.equals(closure.sha256()))
            throw problem(ArchiveProblemException.Kind.CONFLICT, "ARCHIVE_INTEGRITY_FAILED",
                    "La instantánea de cierre no supera la verificación de integridad.");
    }

    private void provenance(UUID actor, AuthorizationScope scope, Provenance provenance) {
        if (provenance.authorityAssignmentId() == null
                || !authority.isAuthorizedAssignment(provenance.authorityAssignmentId(), actor,
                        OperationCode.ARCHIVE_WRITE, scope))
            throw problem(ArchiveProblemException.Kind.FORBIDDEN, "ARCHIVE_AUTHORITY_DENIED",
                    "La asignación indicada no autoriza el archivo del proceso.");
        try {
            new ec.edu.unl.sec.selfassessment.domain.ProcessAuthorization(null,
                    provenance.authorityAssignmentId(), provenance.legalBasis(), provenance.documentReference(),
                    provenance.reason(), actor, clock.now());
        } catch (IllegalArgumentException invalid) {
            throw problem(ArchiveProblemException.Kind.INVALID, "ARCHIVE_INVALID",
                    "La procedencia del archivo está incompleta.");
        }
    }

    private UUID actor(OperationCode operation) {
        var actor = actors.currentActor().filter(candidate -> !candidate.technical())
                .orElseThrow(() -> problem(ArchiveProblemException.Kind.FORBIDDEN, "ARCHIVE_ACTOR_REQUIRED",
                        "Solo sesiones de usuario activas pueden operar el cierre y archivo."));
        if (!auth.hasOperation(actor.identityId(), operation))
            throw problem(ArchiveProblemException.Kind.FORBIDDEN, "ARCHIVE_CAPABILITY_REQUIRED",
                    "La operación requiere una capacidad de cierre o archivo vigente.");
        return actor.identityId();
    }

    private ArchiveProblemException hidden() {
        return problem(ArchiveProblemException.Kind.NOT_FOUND, RESOURCE_NOT_FOUND,
                "El recurso solicitado no existe.");
    }

    private ArchiveProblemException problem(ArchiveProblemException.Kind kind, String code, String message) {
        return new ArchiveProblemException(kind, code, message);
    }
}
