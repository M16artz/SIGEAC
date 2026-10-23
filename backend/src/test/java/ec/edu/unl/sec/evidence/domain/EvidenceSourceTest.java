package ec.edu.unl.sec.evidence.domain;

import static org.assertj.core.api.Assertions.*;

import ec.edu.unl.sec.filestorage.domain.FileAsset;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EvidenceSourceTest {
    @Test void validatesAbsoluteBoundedUrisWithoutDereferencingThem() {
        var reference = new ExternalReference(UUID.randomUUID(), UUID.randomUUID(),
                "https://synthetic.invalid/document", "  Referencia declarada  ", 2);
        assertThat(reference.description()).isEqualTo("Referencia declarada");
        assertThat(reference.position()).isEqualTo(2);
        new ExternalReference(UUID.randomUUID(), reference.versionId(), "urn:synthetic:document", "Declaracion", 1);
        for (String uri : new String[]{"relative/path", "", "http://", "https://synthetic.invalid/" + "a".repeat(2048)}) {
            assertThatThrownBy(() -> new ExternalReference(UUID.randomUUID(), reference.versionId(), uri, "Descripcion", 1))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> new ExternalReference(UUID.randomUUID(), reference.versionId(), reference.uri(), " ", 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ExternalReference(UUID.randomUUID(), reference.versionId(), reference.uri(), "x".repeat(1001), 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void copiesAvailableAssetMetadataIntoAnIndependentOrderedSnapshot() {
        UUID upload = UUID.randomUUID();
        var asset = new FileAsset(UUID.randomUUID(), upload, upload + ".bin", upload + ".part", "synthetic.txt",
                "application/octet-stream", "text/plain", 0, "a".repeat(64), UUID.randomUUID(), FileAsset.Status.AVAILABLE, Instant.now());
        var snapshot = VersionFile.from(UUID.randomUUID(), asset, 3);
        assertThat(snapshot.fileId()).isEqualTo(asset.id());
        assertThat(snapshot.originalName()).isEqualTo(asset.originalName());
        assertThat(snapshot.declaredMediaType()).isNotEqualTo(snapshot.detectedMediaType());
        assertThat(snapshot.sha256()).isEqualTo(asset.sha256());
        assertThat(snapshot.position()).isEqualTo(3);
        assertThatThrownBy(() -> VersionFile.from(snapshot.versionId(), asset.withStatus(FileAsset.Status.MISSING), 1))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> VersionFile.from(snapshot.versionId(), asset, 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
