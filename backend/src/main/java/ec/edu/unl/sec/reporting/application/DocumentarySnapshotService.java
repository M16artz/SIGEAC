package ec.edu.unl.sec.reporting.application;

import ec.edu.unl.sec.audit.application.AuditEventCommand;
import ec.edu.unl.sec.audit.application.AuditPort;
import ec.edu.unl.sec.evidence.application.EvidenceProgressPort;
import ec.edu.unl.sec.identityaccess.application.AuthorizationPort;
import ec.edu.unl.sec.identityaccess.application.CurrentActorPort;
import ec.edu.unl.sec.identityaccess.domain.AuthorizationScope;
import ec.edu.unl.sec.identityaccess.domain.OperationCode;
import ec.edu.unl.sec.reporting.domain.DocumentarySnapshot;
import ec.edu.unl.sec.selfassessment.application.DocumentaryProcessPort;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentarySnapshotService {
    private final CurrentActorPort actors;
    private final AuthorizationPort authorization;
    private final DocumentaryProcessPort processes;
    private final DocumentaryProgressQueryService progress;
    private final EvidenceProgressPort evidence;
    private final DocumentarySnapshotRepository repository;
    private final DocumentarySnapshotCodec codec;
    private final AuditPort audit;

    public DocumentarySnapshotService(CurrentActorPort actors, AuthorizationPort authorization,
            DocumentaryProcessPort processes, DocumentaryProgressQueryService progress, EvidenceProgressPort evidence,
            DocumentarySnapshotRepository repository, DocumentarySnapshotCodec codec, AuditPort audit) {
        this.actors=actors; this.authorization=authorization; this.processes=processes; this.progress=progress;
        this.evidence=evidence; this.repository=repository; this.codec=codec; this.audit=audit;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public DocumentarySnapshot create(UUID processId, long expectedRevision, String reason, UUID correlationId) {
        return createFor(processId,expectedRevision,reason,correlationId,OperationCode.DOCUMENTARY_SNAPSHOT_CREATE);
    }

    @Transactional(propagation=org.springframework.transaction.annotation.Propagation.MANDATORY)
    public DocumentarySnapshot createForClosure(UUID processId,long expectedRevision,String reason,UUID correlationId) {
        return createFor(processId,expectedRevision,reason,correlationId,OperationCode.PROCESS_DOCUMENTARY_CLOSE);
    }

    private DocumentarySnapshot createFor(UUID processId,long expectedRevision,String reason,UUID correlationId,OperationCode operation) {
        var actor=actor(operation);
        if (expectedRevision<0 || reason==null || reason.strip().length()<10 || reason.strip().length()>4000)
            throw problem(ReportingProblemException.Kind.INVALID,"INVALID_SNAPSHOT","El corte requiere un motivo de 10 a 4000 caracteres y una revisión válida.");
        var process=authorizedProcess(actor,processId,operation);
        if (!"ACTIVE".equals(process.state())) throw problem(ReportingProblemException.Kind.CONFLICT,
                "PROCESS_NOT_ACTIVE","El corte documental se crea en un proceso activo.");
        if (process.revision()!=expectedRevision) throw stale();
        var view=progress.snapshotView(processId);
        if (!Set.copyOf(view.indicatorIds()).equals(Set.copyOf(process.indicatorIds()))) throw hidden();
        var sources=view.sources().stream().sorted(Comparator.comparing(DocumentaryProgressQueryService.SourceView::id))
                .map(source -> new DocumentarySnapshot.Source(source.id(),source.indicatorId(),source.code(),source.name(),
                        source.required(),source.applicable(),source.cardinalityMin(),source.coverages().stream()
                                .sorted(Comparator.comparing(DocumentaryProgressQueryService.CoverageView::evidenceId)
                                        .thenComparing(DocumentaryProgressQueryService.CoverageView::versionId))
                                .map(coverage -> new DocumentarySnapshot.Coverage(coverage.evidenceId(),coverage.versionId(),
                                        coverage.decisionId(),coverage.outcome())).toList())).toList();
        var sourceIds=sources.stream().map(DocumentarySnapshot.Source::id).collect(Collectors.toSet());
        var decisions=evidence.currentApplicabilityDecisions(processId).stream()
                .filter(decision -> "INDICATOR".equals(decision.subjectType())
                        ? process.indicatorIds().contains(decision.subjectId()) : sourceIds.contains(decision.subjectId()))
                .sorted(Comparator.comparing(EvidenceProgressPort.ApplicabilityReference::id))
                .map(decision -> new DocumentarySnapshot.Applicability(decision.id(),decision.subjectType(),
                        decision.subjectId(),decision.decision(),decision.revision())).toList();
        if (!processes.reserveSnapshot(processId,expectedRevision)) throw stale();
        var content=new DocumentarySnapshot.Content(UUID.randomUUID(),processId,process.careerId(),process.modelVersionId(),
                process.state(),expectedRevision+1,actor.identityId(),Instant.now(),reason,process.indicatorIds().stream().sorted().toList(),
                view.progress(),sources,decisions);
        var snapshot=codec.seal(content);
        repository.save(snapshot);
        audit.record(new AuditEventCommand(content.createdAt(),actor.identityId(),null,correlationId,
                "DOCUMENTARY_SNAPSHOT_CREATED","DOCUMENTARY_SNAPSHOT",content.id(),process.careerId(),processId,
                "SUCCESS",null,Map.of("sha256",snapshot.sha256(),"processRevision",content.processRevision())));
        return snapshot;
    }

    @Transactional(readOnly = true)
    public DocumentarySnapshot get(UUID id) {
        var actor=actor(OperationCode.PROGRESS_READ);
        var header=repository.header(id).orElseThrow(DocumentarySnapshotService::hidden);
        var process=authorizedProcess(actor,header.processId(),OperationCode.PROGRESS_READ);
        if (!process.careerId().equals(header.careerId())) throw hidden();
        return repository.find(id).orElseThrow(DocumentarySnapshotService::hidden);
    }

    @Transactional(readOnly = true)
    public Page history(UUID processId,int page,int size) {
        var actor=actor(OperationCode.PROGRESS_READ);
        authorizedProcess(actor,processId,OperationCode.PROGRESS_READ);
        if (page<0 || size<1 || size>100) throw problem(ReportingProblemException.Kind.INVALID,"INVALID_PAGE","Paginación inválida.");
        var items=repository.list(processId,(long)page*size,size+1);
        return new Page(items.stream().limit(size).toList(),page,size,items.size()>size);
    }

    private CurrentActorPort.Actor actor(OperationCode operation) {
        var actor=actors.currentActor().orElseThrow(() -> problem(ReportingProblemException.Kind.UNAUTHORIZED,
                "UNAUTHORIZED","Se requiere una sesión vigente."));
        if (actor.technical() || !authorization.hasOperation(actor.identityId(),operation))
            throw problem(ReportingProblemException.Kind.FORBIDDEN,"FORBIDDEN","La operación requiere una función documental vigente.");
        return actor;
    }

    private DocumentaryProcessPort.Context authorizedProcess(CurrentActorPort.Actor actor,UUID id,OperationCode operation) {
        var process=processes.context(id).orElseThrow(DocumentarySnapshotService::hidden);
        var scope=new AuthorizationScope(process.careerId(),id,null);
        if (!authorization.isAllowed(actor.identityId(),operation,scope)
                || !authorization.isAllowed(actor.identityId(),OperationCode.PROGRESS_READ,scope)) throw hidden();
        return process;
    }

    private static ReportingProblemException hidden() {
        return problem(ReportingProblemException.Kind.NOT_FOUND,"RESOURCE_NOT_FOUND","No se encontró el recurso en el ámbito autorizado.");
    }
    private static ReportingProblemException stale() {
        return problem(ReportingProblemException.Kind.PRECONDITION_FAILED,"STALE_PROCESS","El proceso cambió; recargue antes de guardar el corte.");
    }
    private static ReportingProblemException problem(ReportingProblemException.Kind kind,String code,String detail) {
        return new ReportingProblemException(kind,code,detail);
    }
    public record Page(List<DocumentarySnapshot.Header> items,int page,int size,boolean hasNext) {}
}
