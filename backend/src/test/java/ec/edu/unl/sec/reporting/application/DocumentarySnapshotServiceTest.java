package ec.edu.unl.sec.reporting.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import ec.edu.unl.sec.audit.application.AuditPort;
import ec.edu.unl.sec.evidence.application.EvidenceProgressPort;
import ec.edu.unl.sec.identityaccess.application.AuthorizationPort;
import ec.edu.unl.sec.identityaccess.application.CurrentActorPort;
import ec.edu.unl.sec.identityaccess.domain.AuthorizationScope;
import ec.edu.unl.sec.identityaccess.domain.OperationCode;
import ec.edu.unl.sec.reporting.domain.DocumentaryProgressCalculator;
import ec.edu.unl.sec.reporting.domain.DocumentarySnapshot;
import ec.edu.unl.sec.selfassessment.application.DocumentaryProcessPort;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DocumentarySnapshotServiceTest {
    private final UUID actor=UUID.randomUUID(), process=UUID.randomUUID(), career=UUID.randomUUID(), model=UUID.randomUUID();
    private final UUID indicator=UUID.randomUUID(), source=UUID.randomUUID(), version=UUID.randomUUID();
    private final CurrentActorPort actors=mock(CurrentActorPort.class);
    private final AuthorizationPort authorization=mock(AuthorizationPort.class);
    private final DocumentaryProcessPort processes=mock(DocumentaryProcessPort.class);
    private final DocumentaryProgressQueryService progress=mock(DocumentaryProgressQueryService.class);
    private final EvidenceProgressPort evidence=mock(EvidenceProgressPort.class);
    private final DocumentarySnapshotRepository repository=mock(DocumentarySnapshotRepository.class);
    private final DocumentarySnapshotCodec codec=mock(DocumentarySnapshotCodec.class);
    private final AuditPort audit=mock(AuditPort.class);
    private DocumentarySnapshotService service;

    @BeforeEach
    void setUp() {
        service=new DocumentarySnapshotService(actors, authorization, processes, progress, evidence, repository, codec, audit);
        when(actors.currentActor()).thenReturn(Optional.of(new CurrentActorPort.Actor(actor,false)));
        when(authorization.hasOperation(eq(actor),any())).thenReturn(true);
        when(authorization.isAllowed(eq(actor),any(),any())).thenReturn(true);
        when(processes.context(process)).thenReturn(Optional.of(new DocumentaryProcessPort.Context(
                process,career,model,"ACTIVE",7,List.of(indicator))));
        when(processes.reserveSnapshot(process,7)).thenReturn(true);
        when(progress.snapshotView(process)).thenReturn(new DocumentaryProgressQueryService.View(process,model,
                Instant.parse("2026-10-24T12:00:00Z"),"Ámbito completo",List.of(indicator),
                new DocumentaryProgressCalculator.Progress(DocumentaryProgressCalculator.Status.CONFIGURED,4,3,2,"SOURCE_COVERAGE_V1"),
                List.of(new DocumentaryProgressQueryService.SourceView(source,indicator,"F1","Fuente pendiente",true,true,1,
                        List.of(new DocumentaryProgressQueryService.CoverageView(UUID.randomUUID(),version,null,"PENDING_REVIEW")))),0,100,1));
        when(evidence.currentApplicabilityDecisions(process)).thenReturn(List.of());
        when(codec.seal(any())).thenAnswer(call -> new DocumentarySnapshot(call.getArgument(0),"{}","a".repeat(64)));
    }

    @Test
    void preservesPendingSourcesWithoutClosingProcessOrRequiringAcademicModules() {
        var result=service.create(process,7,"Corte documental sintético con pendientes.",UUID.randomUUID());
        assertThat(result.content().processRevision()).isEqualTo(8);
        assertThat(result.content().progress().registeredSourceTotal()).isEqualTo(3);
        assertThat(result.content().sources().getFirst().coverages().getFirst().outcome()).isEqualTo("PENDING_REVIEW");
        verify(processes).reserveSnapshot(process,7);
        verify(repository).save(result);
        verify(audit).record(any());
    }

    @Test
    void storesOnlyApplicabilityReferencesOfTheCapturedModelScope() {
        var known=UUID.randomUUID();
        when(evidence.currentApplicabilityDecisions(process)).thenReturn(List.of(
                new EvidenceProgressPort.ApplicabilityReference(known,"REQUIRED_SOURCE",source,"APPLICABLE",2),
                new EvidenceProgressPort.ApplicabilityReference(UUID.randomUUID(),"REQUIRED_SOURCE",UUID.randomUUID(),"NOT_APPLICABLE",1)));
        var result=service.create(process,7,"Corte documental con referencias exactas.",UUID.randomUUID());
        assertThat(result.content().applicability()).extracting(DocumentarySnapshot.Applicability::id).containsExactly(known);
    }

    @Test
    void staleRevisionDoesNotSaveOrAuditASnapshot() {
        problem(() -> service.create(process,6,"Corte documental con revisión obsoleta.",UUID.randomUUID()),ReportingProblemException.Kind.PRECONDITION_FAILED);
        verifyNoInteractions(progress,repository,codec,audit);
        when(processes.reserveSnapshot(process,7)).thenReturn(false);
        problem(() -> service.create(process,7,"Corte documental con revisión en conflicto.",UUID.randomUUID()),ReportingProblemException.Kind.PRECONDITION_FAILED);
        verifyNoInteractions(repository,codec,audit);
    }

    @Test
    void technicalMissingOrUnauthorizedActorsCannotCreateCuts() {
        when(actors.currentActor()).thenReturn(Optional.empty());
        problem(() -> service.create(process,7,"Corte documental sintético.",UUID.randomUUID()),ReportingProblemException.Kind.UNAUTHORIZED);
        when(actors.currentActor()).thenReturn(Optional.of(new CurrentActorPort.Actor(actor,true)));
        problem(() -> service.create(process,7,"Corte documental sintético.",UUID.randomUUID()),ReportingProblemException.Kind.FORBIDDEN);
        when(actors.currentActor()).thenReturn(Optional.of(new CurrentActorPort.Actor(actor,false)));
        when(authorization.hasOperation(actor,OperationCode.DOCUMENTARY_SNAPSHOT_CREATE)).thenReturn(false);
        problem(() -> service.create(process,7,"Corte documental sintético.",UUID.randomUUID()),ReportingProblemException.Kind.FORBIDDEN);
        verifyNoInteractions(processes,progress,repository);
    }

    @Test
    void requiresFullProcessReadScopeBeforeCapturingAnyTotals() {
        when(authorization.isAllowed(actor,OperationCode.PROGRESS_READ,new AuthorizationScope(career,process,null))).thenReturn(false);
        problem(() -> service.create(process,7,"Corte documental sintético.",UUID.randomUUID()),ReportingProblemException.Kind.NOT_FOUND);
        verifyNoInteractions(progress,evidence,repository);
    }

    @Test
    void rejectsNonActiveProcessesAndInvalidReason() {
        when(processes.context(process)).thenReturn(Optional.of(new DocumentaryProcessPort.Context(process,career,model,"SUSPENDED",7,List.of(indicator))));
        problem(() -> service.create(process,7,"Corte documental suspendido.",UUID.randomUUID()),ReportingProblemException.Kind.CONFLICT);
        problem(() -> service.create(process,7,"breve",UUID.randomUUID()),ReportingProblemException.Kind.INVALID);
        verifyNoInteractions(progress,repository,audit);
    }

    @Test
    void storedHistoryIsReturnedWithoutRecalculatingCurrentProgress() {
        var stored=service.create(process,7,"Corte histórico reproducible.",UUID.randomUUID());
        when(repository.header(stored.content().id())).thenReturn(Optional.of(stored.header()));
        when(repository.find(stored.content().id())).thenReturn(Optional.of(stored));
        clearInvocations(progress,evidence);
        assertThat(service.get(stored.content().id())).isEqualTo(stored);
        verifyNoInteractions(progress,evidence);
    }

    @Test
    void deniesHistoricalContentBeforeLoadingThePayloadAfterScopeRevocation() {
        var stored=service.create(process,7,"Corte histórico para comprobar el ámbito.",UUID.randomUUID());
        when(repository.header(stored.content().id())).thenReturn(Optional.of(stored.header()));
        when(authorization.isAllowed(actor,OperationCode.PROGRESS_READ,new AuthorizationScope(career,process,null))).thenReturn(false);
        problem(() -> service.get(stored.content().id()),ReportingProblemException.Kind.NOT_FOUND);
        verify(repository,never()).find(any());
    }

    private void problem(Runnable action,ReportingProblemException.Kind kind) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(ReportingProblemException.class,
                exception -> assertThat(exception.kind()).isEqualTo(kind));
    }
}
