package ec.edu.unl.sec.evidence.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import ec.edu.unl.sec.evidence.domain.*;
import ec.edu.unl.sec.filestorage.application.FileAssetRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EvidenceSourceServiceTest {
    private final EvidenceDraftService drafts = mock(EvidenceDraftService.class);
    private final EvidenceSourceRepository repository = mock(EvidenceSourceRepository.class);
    private final FileAssetRepository assets = mock(FileAssetRepository.class);
    private final UUID versionId = UUID.randomUUID();
    private EvidenceSourceService service;

    @BeforeEach void setup() {
        service = new EvidenceSourceService(drafts, repository, assets);
        var version = new EvidenceVersion(versionId, UUID.randomUUID(), 1, null, EvidenceVersionStatus.DRAFT,
                "Titulo", "Descripcion", "Procedencia", LocalDate.now(), LocalDate.now(), UUID.randomUUID(),
                Instant.now(), null, 0, List.of(new VersionIndicator(versionId, UUID.randomUUID(), "I", "Indicador", 1)));
        when(drafts.requireEditableVersion(versionId, "\"0\"")).thenReturn(version);
        when(repository.advanceDraftRevision(versionId, 0)).thenReturn(true);
        when(repository.nextReferencePosition(versionId)).thenReturn(4);
    }

    @Test void addsAReferenceAfterAuthorizationAndAdvancesTheEtag() {
        var changed = service.addReference(versionId, "\"0\"", "https://synthetic.invalid/a", "Declaracion");
        assertThat(changed.revision()).isEqualTo(1);
        assertThat(changed.value().position()).isEqualTo(4);
        var ordered = inOrder(drafts, repository);
        ordered.verify(drafts).requireEditableVersion(versionId, "\"0\"");
        ordered.verify(repository).advanceDraftRevision(versionId, 0);
        ordered.verify(repository).nextReferencePosition(versionId);
        ordered.verify(repository).addReference(changed.value());
    }

    @Test void preservesReferenceIdentityAndPositionOnEditAndHidesForeignIds() {
        var reference = new ExternalReference(UUID.randomUUID(), versionId, "urn:synthetic:old", "Anterior", 2);
        when(repository.references(versionId)).thenReturn(List.of(reference));
        var changed = service.updateReference(versionId, reference.id(), "\"0\"", "urn:synthetic:new", "Actualizada");
        assertThat(changed.value().id()).isEqualTo(reference.id());
        assertThat(changed.value().position()).isEqualTo(2);
        verify(repository).updateReference(changed.value());
        assertThatThrownBy(() -> service.removeReference(versionId, UUID.randomUUID(), "\"0\""))
                .isInstanceOf(EvidenceProblemException.class).hasMessageContaining("recurso");
    }

    @Test void rejectsStaleWritesWithoutChangingAnySource() {
        when(repository.advanceDraftRevision(versionId, 0)).thenReturn(false);
        assertThatThrownBy(() -> service.addReference(versionId, "\"0\"", "urn:synthetic:a", "Declaracion"))
                .isInstanceOf(EvidenceProblemException.class).extracting("code").isEqualTo("STALE_DRAFT");
        verify(repository, never()).addReference(any());
    }

    @Test void hiddenVersionsDoNotReadOrModifySourceCollections() {
        when(drafts.getVersion(versionId)).thenThrow(EvidenceDraftService.notFound());
        when(drafts.requireEditableVersion(versionId, "\"0\"")).thenThrow(EvidenceDraftService.notFound());
        assertThatThrownBy(() -> service.snapshot(versionId)).isInstanceOf(EvidenceProblemException.class);
        assertThatThrownBy(() -> service.removeFile(versionId, UUID.randomUUID(), "\"0\""))
                .isInstanceOf(EvidenceProblemException.class);
        verifyNoInteractions(repository, assets);
    }
}
