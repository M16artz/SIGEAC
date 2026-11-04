package ec.edu.unl.sec.evidence.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import ec.edu.unl.sec.evidence.domain.VersionFile;
import ec.edu.unl.sec.filestorage.application.FileAssetRepository;
import ec.edu.unl.sec.filestorage.application.FileStoragePort;
import ec.edu.unl.sec.filestorage.domain.FileAsset;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EvidenceIntegrityServiceTest {
    private final UUID version=UUID.randomUUID(),file=UUID.randomUUID();
    private final EvidenceSourceRepository sources=mock(EvidenceSourceRepository.class);
    private final FileAssetRepository assets=mock(FileAssetRepository.class);
    private final FileStoragePort storage=mock(FileStoragePort.class);
    private final EvidenceIntegrityService service=new EvidenceIntegrityService(sources,assets,storage);
    private FileAsset asset;
    @BeforeEach
    void setUp() {
        asset=new FileAsset(file,UUID.randomUUID(),"synthetic.bin",null,"synthetic.txt","text/plain","text/plain",7,
                "a".repeat(64),UUID.randomUUID(),FileAsset.Status.AVAILABLE,Instant.now());
        when(sources.files(version)).thenReturn(List.of(VersionFile.from(version,asset,1)));
        when(assets.findById(file)).thenReturn(Optional.of(asset));
    }
    @Test
    void verifiesBytesAgainstBothImmutableSnapshotAndAssetMetadata() throws Exception {
        when(storage.verify("synthetic.bin")).thenReturn(new FileStoragePort.Verification(7,"a".repeat(64)));
        var result=service.verify(Set.of(version));
        assertThat(result.issues()).isEmpty();
        assertThat(result.files()).containsExactly(new EvidenceIntegrityPort.FileReference(version,file,7,"a".repeat(64)));
    }
    @Test
    void changedBytesAndMissingFilesAreExplicitBlockers() throws Exception {
        when(storage.verify("synthetic.bin")).thenReturn(new FileStoragePort.Verification(7,"b".repeat(64)));
        assertThat(service.verify(Set.of(version)).issues()).extracting(EvidenceIntegrityPort.Issue::code).containsExactly("FILE_INTEGRITY_FAILED");
        when(storage.verify("synthetic.bin")).thenThrow(new IOException("synthetic missing file"));
        assertThat(service.verify(Set.of(version)).issues()).extracting(EvidenceIntegrityPort.Issue::code).containsExactly("FILE_INTEGRITY_FAILED");
    }
    @Test
    void missingAssetAndEmptyFileSetsDoNotInventAvailability() {
        when(assets.findById(file)).thenReturn(Optional.empty());
        assertThat(service.verify(Set.of(version)).issues()).extracting(EvidenceIntegrityPort.Issue::code).containsExactly("FILE_NOT_AVAILABLE");
        assertThat(service.verify(Set.of()).files()).isEmpty();
        verifyNoInteractions(storage);
    }
}
