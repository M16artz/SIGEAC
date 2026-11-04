package ec.edu.unl.sec.reporting.application;

import ec.edu.unl.sec.audit.application.AuditEventCommand;
import ec.edu.unl.sec.audit.application.AuditPort;
import ec.edu.unl.sec.evidence.application.EvidenceIntegrityPort;
import ec.edu.unl.sec.identityaccess.application.AuthorizationPort;
import ec.edu.unl.sec.identityaccess.application.CurrentActorPort;
import ec.edu.unl.sec.identityaccess.domain.AuthorizationScope;
import ec.edu.unl.sec.identityaccess.domain.OperationCode;
import ec.edu.unl.sec.reporting.domain.DocumentaryClosure;
import ec.edu.unl.sec.reporting.domain.DocumentarySnapshot;
import ec.edu.unl.sec.reporting.domain.DocumentaryProgressCalculator;
import ec.edu.unl.sec.selfassessment.application.DocumentaryProcessPort;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentaryClosureService {
    public static final String STRICT_POLICY="DOCUMENTARY_STRICT_V1";
    private final CurrentActorPort actors;
    private final AuthorizationPort auth;
    private final DocumentaryProcessPort processes;
    private final DocumentarySnapshotService snapshots;
    private final DocumentaryClosureRepository repository;
    private final DocumentarySnapshotCodec codec;
    private final EvidenceIntegrityPort integrity;
    private final AuditPort audit;
    public DocumentaryClosureService(CurrentActorPort actors,AuthorizationPort auth,DocumentaryProcessPort processes,
            DocumentarySnapshotService snapshots,DocumentaryClosureRepository repository,DocumentarySnapshotCodec codec,
            EvidenceIntegrityPort integrity,AuditPort audit) {
        this.actors=actors;this.auth=auth;this.processes=processes;this.snapshots=snapshots;
        this.repository=repository;this.codec=codec;this.integrity=integrity;this.audit=audit;
    }

    @Transactional
    public DocumentaryClosure close(UUID id,long expected,String policy,String reason,UUID correlation) {
        var actor=actor(OperationCode.PROCESS_DOCUMENTARY_CLOSE);
        if (!STRICT_POLICY.equals(policy)) throw problem(ReportingProblemException.Kind.INVALID,"UNSUPPORTED_CLOSURE_POLICY","Solo se admite la política documental estricta versionada.");
        if (expected<0 || reason==null || reason.strip().length()<10 || reason.strip().length()>2000)
            throw problem(ReportingProblemException.Kind.INVALID,"INVALID_CLOSURE","El cierre requiere motivo de 10 a 2000 caracteres y revisión válida.");
        authorize(actor,id,OperationCode.PROCESS_DOCUMENTARY_CLOSE);
        var process=processes.lock(id).orElseThrow(DocumentaryClosureService::hidden);
        if (process.revision()!=expected) throw stale();
        if (!"ACTIVE".equals(process.state())) throw problem(ReportingProblemException.Kind.CONFLICT,"PROCESS_NOT_ACTIVE","El cierre requiere un proceso activo.");
        var snapshot=snapshots.createForClosure(id,expected,reason,correlation);
        var pending=snapshot.content().sources().stream().filter(source -> source.required() && source.applicable())
                .filter(source -> source.coverages().stream().filter(coverage -> "ACCEPTED".equals(coverage.outcome()))
                        .map(DocumentarySnapshot.Coverage::evidenceId).distinct().count()<source.cardinalityMin())
                .map(source -> source.id().toString()).sorted().collect(Collectors.joining(","));
        if (snapshot.content().progress().status()==DocumentaryProgressCalculator.Status.NOT_CONFIGURED || !pending.isEmpty())
            throw new ReportingProblemException(ReportingProblemException.Kind.CONFLICT,"CLOSURE_BLOCKED",
                    "Hay fuentes obligatorias sin aceptación suficiente o falta configurar fuentes.",Map.of("sourceIds",pending));
        var versions=snapshot.content().sources().stream().flatMap(source -> source.coverages().stream())
                .map(DocumentarySnapshot.Coverage::versionId).collect(Collectors.toSet());
        var checked=integrity.verify(versions);
        if (!checked.issues().isEmpty()) throw new ReportingProblemException(ReportingProblemException.Kind.CONFLICT,"CLOSURE_BLOCKED",
                "Hay archivos referenciados que no están disponibles o no superan la verificación de integridad.",
                Map.of("fileIds",checked.issues().stream().map(issue -> issue.fileId().toString()).distinct().sorted().collect(Collectors.joining(","))));
        var closed=processes.completeClosure(id,snapshot.content().processRevision(),actor.identityId(),reason.strip(),correlation)
                .orElseThrow(DocumentaryClosureService::stale);
        var files=checked.files().stream().map(file -> new DocumentaryClosure.FileReference(file.versionId(),file.fileId(),file.sizeBytes(),file.sha256())).toList();
        var closure=codec.sealClosure(new DocumentaryClosure.Content(UUID.randomUUID(),id,process.careerId(),snapshot.content().id(),
                snapshot.sha256(),closed.state(),closed.revision(),policy,actor.identityId(),Instant.now(),reason,files));
        repository.save(closure);
        audit.record(new AuditEventCommand(closure.content().closedAt(),actor.identityId(),null,correlation,"PROCESS_DOCUMENTARY_CLOSED",
                "DOCUMENTARY_CLOSURE",closure.content().id(),process.careerId(),id,"SUCCESS",null,
                Map.of("snapshotId",snapshot.content().id(),"policyVersion",policy,"sha256",closure.sha256())));
        return closure;
    }

    @Transactional(readOnly=true)
    public DocumentaryClosure get(UUID id) {
        var actor=actor(OperationCode.PROGRESS_READ);
        var header=repository.header(id).orElseThrow(DocumentaryClosureService::hidden);
        var process=authorize(actor,header.processId(),OperationCode.PROGRESS_READ);
        if (!process.careerId().equals(header.careerId())) throw hidden();
        var closure=repository.find(id).orElseThrow(DocumentaryClosureService::hidden);
        if (!snapshots.get(closure.content().snapshotId()).sha256().equals(closure.content().snapshotSha256()))
            throw problem(ReportingProblemException.Kind.CONFLICT,"SNAPSHOT_INTEGRITY_FAILED","El cierre no coincide con su corte documental conservado.");
        return closure;
    }

    @Transactional(readOnly=true)
    public Page history(UUID processId,int page,int size) {
        var actor=actor(OperationCode.PROGRESS_READ);
        authorize(actor,processId,OperationCode.PROGRESS_READ);
        if (page<0 || size<1 || size>100) throw problem(ReportingProblemException.Kind.INVALID,"INVALID_PAGE","Paginación inválida.");
        var rows=repository.list(processId,(long)page*size,size+1);
        return new Page(rows.stream().limit(size).toList(),page,size,rows.size()>size);
    }

    private CurrentActorPort.Actor actor(OperationCode op) {
        var actor=actors.currentActor().orElseThrow(() -> problem(ReportingProblemException.Kind.UNAUTHORIZED,"UNAUTHORIZED","Se requiere una sesión vigente."));
        if (actor.technical() || !auth.hasOperation(actor.identityId(),op)) throw problem(ReportingProblemException.Kind.FORBIDDEN,"FORBIDDEN","La operación requiere una función documental vigente.");
        return actor;
    }
    private DocumentaryProcessPort.Context authorize(CurrentActorPort.Actor actor,UUID id,OperationCode op) {
        var context=processes.context(id).orElseThrow(DocumentaryClosureService::hidden);
        var scope=new AuthorizationScope(context.careerId(),id,null);
        if (!auth.isAllowed(actor.identityId(),op,scope) || !auth.isAllowed(actor.identityId(),OperationCode.PROGRESS_READ,scope)) throw hidden();
        return context;
    }
    private static ReportingProblemException problem(ReportingProblemException.Kind kind,String code,String message) { return new ReportingProblemException(kind,code,message); }
    private static ReportingProblemException hidden() { return problem(ReportingProblemException.Kind.NOT_FOUND,"RESOURCE_NOT_FOUND","No se encontró el recurso en el ámbito autorizado."); }
    private static ReportingProblemException stale() { return problem(ReportingProblemException.Kind.PRECONDITION_FAILED,"STALE_PROCESS","El proceso cambió; recargue antes de cerrar."); }
    public record Page(List<DocumentaryClosure.Header> items,int page,int size,boolean hasNext) {}
}
