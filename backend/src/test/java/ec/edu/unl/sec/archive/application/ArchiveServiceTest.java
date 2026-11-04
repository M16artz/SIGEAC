package ec.edu.unl.sec.archive.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import ec.edu.unl.sec.archive.domain.ArchiveRecord;
import ec.edu.unl.sec.archive.domain.IntegralClosure;
import ec.edu.unl.sec.audit.application.AuditPort;
import ec.edu.unl.sec.identityaccess.application.AuthorizationPort;
import ec.edu.unl.sec.identityaccess.application.CurrentActorPort;
import ec.edu.unl.sec.identityaccess.application.InstitutionalAuthorityPort;
import ec.edu.unl.sec.selfassessment.application.DocumentaryProcessPort;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ArchiveServiceTest {
    private final UUID process = UUID.randomUUID();
    private final UUID career = UUID.randomUUID();
    private final UUID model = UUID.randomUUID();
    private final UUID actor = UUID.randomUUID();
    private final UUID closureId = UUID.randomUUID();
    private final CurrentActorPort actors = mock(CurrentActorPort.class);
    private final AuthorizationPort auth = mock(AuthorizationPort.class);
    private final InstitutionalAuthorityPort authority = mock(InstitutionalAuthorityPort.class);
    private final DocumentaryProcessPort processes = mock(DocumentaryProcessPort.class);
    private final IntegralClosureRepository repository = mock(IntegralClosureRepository.class);
    private final AuditPort audit = mock(AuditPort.class);
    private final ArchiveService service = new ArchiveService(actors, auth, authority, processes, repository, audit);
    private final UUID authorityAssignment = UUID.randomUUID();
    private final String payload = "{\"closureType\":\"INTEGRAL_CLOSURE\"}";
    private final ArchiveService.Provenance provenance = new ArchiveService.Provenance(authorityAssignment,
            "Ley Orgánica de Educación Superior", "Acta de cierre integral 2026", "Cierre integral autorizado.");

    @BeforeEach
    void setup() {
        when(actors.currentActor()).thenReturn(Optional.of(new CurrentActorPort.Actor(actor, false)));
        when(auth.hasOperation(actor, ec.edu.unl.sec.identityaccess.domain.OperationCode.ARCHIVE_WRITE))
                .thenReturn(true);
        when(auth.isAllowed(any(), any(), any())).thenReturn(true);
        when(authority.isAuthorizedAssignment(any(), any(), any(), any())).thenReturn(true);
        state("CLOSED");
        when(repository.findForProcess(process, closureId)).thenReturn(Optional.of(closure(sha(payload))));
        when(repository.archiveForClosure(closureId)).thenReturn(Optional.empty());
    }

    @Test
    void archivesSealedSnapshotWithVerifiedIntegrityAndAudit() {
        var record = service.archive(process, new ArchiveService.ArchiveCommand(closureId, "RETENTION_7Y",
                provenance), UUID.randomUUID());

        assertThat(record.integrityStatus()).isEqualTo(ArchiveRecord.VERIFIED);
        assertThat(record.integrityHash()).isEqualTo(sha(payload));
        assertThat(record.retentionPolicyVersion()).isEqualTo("RETENTION_7Y");
        verify(repository).appendArchive(record);
        verify(audit).record(any());
    }

    @Test
    void rejectsCorruptedSnapshotBeforeWritingArchive() {
        when(repository.findForProcess(process, closureId)).thenReturn(Optional.of(closure(
                "0000000000000000000000000000000000000000000000000000000000000000")));

        assertThatThrownBy(() -> service.archive(process, new ArchiveService.ArchiveCommand(closureId,
                "RETENTION_7Y", provenance), UUID.randomUUID()))
                .isInstanceOf(ArchiveProblemException.class).extracting("code")
                .isEqualTo("ARCHIVE_INTEGRITY_FAILED");
        verify(repository, never()).appendArchive(any());
    }

    @Test
    void rejectsArchiveWhenProcessIsNotClosed() {
        state("DOCUMENTARY_CLOSED");

        assertThatThrownBy(() -> service.archive(process, new ArchiveService.ArchiveCommand(closureId,
                "RETENTION_7Y", provenance), UUID.randomUUID()))
                .isInstanceOf(ArchiveProblemException.class).extracting("code")
                .isEqualTo("ARCHIVE_STATE_INVALID");
        verify(repository, never()).appendArchive(any());
    }

    @Test
    void rejectsDuplicatedArchiveOfTheSameSnapshot() {
        when(repository.archiveForClosure(closureId)).thenReturn(Optional.of(new ArchiveRecord(UUID.randomUUID(),
                process, closureId, "RETENTION_7Y", actor, Instant.now(), sha(payload), ArchiveRecord.VERIFIED)));

        assertThatThrownBy(() -> service.archive(process, new ArchiveService.ArchiveCommand(closureId,
                "RETENTION_7Y", provenance), UUID.randomUUID()))
                .isInstanceOf(ArchiveProblemException.class).extracting("code")
                .isEqualTo("ARCHIVE_ALREADY_EXISTS");
        verify(repository, never()).appendArchive(any());
    }

    @Test
    void requiresInstitutionalAuthorityForArchive() {
        when(authority.isAuthorizedAssignment(any(), any(), any(), any())).thenReturn(false);

        assertThatThrownBy(() -> service.archive(process, new ArchiveService.ArchiveCommand(closureId,
                "RETENTION_7Y", provenance), UUID.randomUUID()))
                .isInstanceOf(ArchiveProblemException.class).extracting("code")
                .isEqualTo("ARCHIVE_AUTHORITY_DENIED");
    }

    private void state(String state) {
        when(processes.lock(process)).thenReturn(Optional.of(new DocumentaryProcessPort.Context(process, career,
                model, state, 7, List.of())));
    }

    private IntegralClosure closure(String digest) {
        return new IntegralClosure(closureId, process, UUID.randomUUID(), "INTEGRAL_STRICT_V1", payload, digest,
                List.of(), List.of(), actor, Instant.parse("2026-05-01T00:00:00Z"));
    }

    private static String sha(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
