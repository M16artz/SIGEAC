package ec.edu.unl.sec.reporting.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import ec.edu.unl.sec.audit.application.AuditPort;
import ec.edu.unl.sec.evidence.application.EvidenceIntegrityPort;
import ec.edu.unl.sec.identityaccess.application.AuthorizationPort;
import ec.edu.unl.sec.identityaccess.application.CurrentActorPort;
import ec.edu.unl.sec.identityaccess.domain.OperationCode;
import ec.edu.unl.sec.reporting.domain.DocumentaryClosure;
import ec.edu.unl.sec.reporting.domain.DocumentarySnapshot;
import ec.edu.unl.sec.reporting.domain.DocumentaryProgressCalculator;
import ec.edu.unl.sec.selfassessment.application.DocumentaryProcessPort;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DocumentaryClosureServiceTest {
    private final UUID actor=UUID.randomUUID(),process=UUID.randomUUID(),career=UUID.randomUUID(),model=UUID.randomUUID();
    private final UUID indicator=UUID.randomUUID(),source=UUID.randomUUID(),version=UUID.randomUUID();
    private final CurrentActorPort actors=mock(CurrentActorPort.class);
    private final AuthorizationPort auth=mock(AuthorizationPort.class);
    private final DocumentaryProcessPort processes=mock(DocumentaryProcessPort.class);
    private final DocumentarySnapshotService snapshots=mock(DocumentarySnapshotService.class);
    private final DocumentaryClosureRepository repository=mock(DocumentaryClosureRepository.class);
    private final DocumentarySnapshotCodec codec=mock(DocumentarySnapshotCodec.class);
    private final EvidenceIntegrityPort integrity=mock(EvidenceIntegrityPort.class);
    private final AuditPort audit=mock(AuditPort.class);
    private DocumentaryClosureService service;

    @BeforeEach
    void setUp() {
        service=new DocumentaryClosureService(actors,auth,processes,snapshots,repository,codec,integrity,audit);
        when(actors.currentActor()).thenReturn(Optional.of(new CurrentActorPort.Actor(actor,false)));
        when(auth.hasOperation(eq(actor),any())).thenReturn(true);
        when(auth.isAllowed(eq(actor),any(),any())).thenReturn(true);
        var context=new DocumentaryProcessPort.Context(process,career,model,"ACTIVE",7,List.of(indicator));
        when(processes.context(process)).thenReturn(Optional.of(context));
        when(processes.lock(process)).thenReturn(Optional.of(context));
        when(processes.completeClosure(eq(process),eq(8L),eq(actor),anyString(),any())).thenReturn(Optional.of(
                new DocumentaryProcessPort.Context(process,career,model,"DOCUMENTARY_CLOSED",9,List.of(indicator))));
        when(snapshots.createForClosure(eq(process),eq(7L),anyString(),any())).thenReturn(cut("ACCEPTED",1));
        when(integrity.verify(any())).thenReturn(new EvidenceIntegrityPort.Report(List.of(),List.of()));
        when(codec.sealClosure(any())).thenAnswer(call -> new DocumentaryClosure(call.getArgument(0),"{}","b".repeat(64)));
    }

    @Test
    void closesWithAcceptedSourcesAndNoAcademicDependencies() {
        var result=close("DOCUMENTARY_STRICT_V1");
        assertThat(result.content().state()).isEqualTo("DOCUMENTARY_CLOSED");
        assertThat(result.content().processRevision()).isEqualTo(9);
        var order=inOrder(processes,snapshots,integrity,repository,audit);
        order.verify(processes).context(process);
        order.verify(processes).lock(process);
        order.verify(snapshots).createForClosure(eq(process),eq(7L),anyString(),any());
        order.verify(integrity).verify(Set.of(version));
        order.verify(processes).completeClosure(eq(process),eq(8L),eq(actor),anyString(),any());
        order.verify(repository).save(result);
        order.verify(audit).record(any());
    }

    @Test
    void pendingObservedAndRejectedSourcesBlockStrictClosure() {
        for (String state:List.of("PENDING_REVIEW","OBSERVED","REJECTED")) {
            when(snapshots.createForClosure(eq(process),eq(7L),anyString(),any())).thenReturn(cut(state,0));
            assertThatThrownBy(() -> close("DOCUMENTARY_STRICT_V1")).isInstanceOfSatisfying(ReportingProblemException.class,
                    problem -> {assertThat(problem.code()).isEqualTo("CLOSURE_BLOCKED");assertThat(problem.details().get("sourceIds")).contains(source.toString());});
        }
        verify(processes,never()).completeClosure(any(),anyLong(),any(),anyString(),any());
        verifyNoInteractions(repository,audit);
    }

    @Test
    void missingOrCorruptFilesPreventTransitionAndClosureRecord() {
        var file=UUID.randomUUID();
        when(integrity.verify(any())).thenReturn(new EvidenceIntegrityPort.Report(List.of(),List.of(
                new EvidenceIntegrityPort.Issue(version,file,"FILE_INTEGRITY_FAILED"))));
        assertThatThrownBy(() -> close("DOCUMENTARY_STRICT_V1")).isInstanceOfSatisfying(ReportingProblemException.class,
                problem -> assertThat(problem.details().get("fileIds")).contains(file.toString()));
        verify(processes,never()).completeClosure(any(),anyLong(),any(),anyString(),any());
        verifyNoInteractions(repository,audit);
    }

    @Test
    void unsupportedPolicyIsRejectedBeforeReservingAnything() {
        assertThatThrownBy(() -> close("ALLOW_ANYTHING")).isInstanceOfSatisfying(ReportingProblemException.class,
                problem -> assertThat(problem.code()).isEqualTo("UNSUPPORTED_CLOSURE_POLICY"));
        verifyNoInteractions(processes,snapshots,repository);
    }

    @Test
    void rejectsTechnicalActorsAndMissingCapability() {
        when(actors.currentActor()).thenReturn(Optional.of(new CurrentActorPort.Actor(actor,true)));
        assertThatThrownBy(() -> close("DOCUMENTARY_STRICT_V1")).isInstanceOf(ReportingProblemException.class);
        when(actors.currentActor()).thenReturn(Optional.of(new CurrentActorPort.Actor(actor,false)));
        when(auth.hasOperation(actor,OperationCode.PROCESS_DOCUMENTARY_CLOSE)).thenReturn(false);
        assertThatThrownBy(() -> close("DOCUMENTARY_STRICT_V1")).isInstanceOf(ReportingProblemException.class);
        verifyNoInteractions(processes,snapshots,repository);
    }

    @Test
    void staleRevisionAndAlreadyClosedProcessCannotCreateAnotherClosure() {
        when(processes.lock(process)).thenReturn(Optional.of(new DocumentaryProcessPort.Context(process,career,model,"ACTIVE",8,List.of(indicator))));
        assertThatThrownBy(() -> close("DOCUMENTARY_STRICT_V1")).isInstanceOfSatisfying(ReportingProblemException.class,
                problem -> assertThat(problem.kind()).isEqualTo(ReportingProblemException.Kind.PRECONDITION_FAILED));
        when(processes.lock(process)).thenReturn(Optional.of(new DocumentaryProcessPort.Context(process,career,model,"DOCUMENTARY_CLOSED",7,List.of(indicator))));
        assertThatThrownBy(() -> close("DOCUMENTARY_STRICT_V1")).isInstanceOfSatisfying(ReportingProblemException.class,
                problem -> assertThat(problem.code()).isEqualTo("PROCESS_NOT_ACTIVE"));
        verifyNoInteractions(snapshots,repository,audit);
    }

    private DocumentaryClosure close(String policy) {
        return service.close(process,7,policy,"Cierre documental sintético según política estricta.",UUID.randomUUID());
    }
    @Test
    void authorisedExclusionOfAllRequiredSourcesCanCloseWithoutInventingPercentages() {
        var original=cut("ACCEPTED",1).content();
        var excluded=new DocumentarySnapshot.Source(source,indicator,"F1","Fuente excluida",true,false,1,List.of());
        when(snapshots.createForClosure(eq(process),eq(7L),anyString(),any())).thenReturn(replace(original,
                new DocumentaryProgressCalculator.Progress(DocumentaryProgressCalculator.Status.NOT_APPLICABLE,0,0,0,"SOURCE_COVERAGE_V1"),List.of(excluded)));
        assertThat(close("DOCUMENTARY_STRICT_V1").content().state()).isEqualTo("DOCUMENTARY_CLOSED");
    }
    @Test
    void optionalPendingSourcesDoNotBecomeMandatoryClosureRequirements() {
        var original=cut("ACCEPTED",1).content();
        var optional=new DocumentarySnapshot.Source(UUID.randomUUID(),indicator,"OPT","Fuente opcional",false,true,1,List.of());
        when(snapshots.createForClosure(eq(process),eq(7L),anyString(),any())).thenReturn(replace(original,original.progress(),
                List.of(original.sources().getFirst(),optional)));
        assertThat(close("DOCUMENTARY_STRICT_V1").content().state()).isEqualTo("DOCUMENTARY_CLOSED");
    }
    @Test
    void repeatedEvidenceDoesNotSatisfyCardinalityTwo() {
        var original=cut("ACCEPTED",1).content();
        var coverage=original.sources().getFirst().coverages().getFirst();
        var repeated=new DocumentarySnapshot.Source(source,indicator,"F1","Fuente de cardinalidad dos",true,true,2,
                List.of(coverage,new DocumentarySnapshot.Coverage(coverage.evidenceId(),UUID.randomUUID(),UUID.randomUUID(),"ACCEPTED")));
        when(snapshots.createForClosure(eq(process),eq(7L),anyString(),any())).thenReturn(replace(original,
                new DocumentaryProgressCalculator.Progress(DocumentaryProgressCalculator.Status.CONFIGURED,1,1,0,"SOURCE_COVERAGE_V1"),List.of(repeated)));
        assertThatThrownBy(() -> close("DOCUMENTARY_STRICT_V1")).isInstanceOfSatisfying(ReportingProblemException.class,
                problem -> assertThat(problem.code()).isEqualTo("CLOSURE_BLOCKED"));
    }
    private DocumentarySnapshot replace(DocumentarySnapshot.Content content,DocumentaryProgressCalculator.Progress progress,List<DocumentarySnapshot.Source> sources) {
        return new DocumentarySnapshot(new DocumentarySnapshot.Content(content.id(),process,career,model,"ACTIVE",8,actor,
                content.createdAt(),content.reason(),List.of(indicator),progress,sources,List.of()),"{}","a".repeat(64));
    }
    private DocumentarySnapshot cut(String outcome,int accepted) {
        return new DocumentarySnapshot(new DocumentarySnapshot.Content(UUID.randomUUID(),process,career,model,"ACTIVE",8,actor,
                Instant.parse("2026-10-24T12:00:00Z"),"Corte anterior a la transición documental.",List.of(indicator),
                new DocumentaryProgressCalculator.Progress(DocumentaryProgressCalculator.Status.CONFIGURED,1,1,accepted,"SOURCE_COVERAGE_V1"),
                List.of(new DocumentarySnapshot.Source(source,indicator,"F1","Fuente requerida",true,true,1,List.of(
                        new DocumentarySnapshot.Coverage(UUID.randomUUID(),version,"PENDING_REVIEW".equals(outcome)?null:UUID.randomUUID(),outcome)))),List.of()),"{}","a".repeat(64));
    }
}
