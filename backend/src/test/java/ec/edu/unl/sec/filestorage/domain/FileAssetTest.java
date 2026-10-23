package ec.edu.unl.sec.filestorage.domain;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FileAssetTest {
    @Test void preservesIdentityAndMetadataAcrossOperationalStateChanges() {
        var staged = asset("synthetic.pdf", 0, "a".repeat(64));
        var available = staged.withStatus(FileAsset.Status.AVAILABLE);
        assertThat(available.id()).isEqualTo(staged.id());
        assertThat(available.uploadId()).isEqualTo(staged.uploadId());
        assertThat(available.sha256()).isEqualTo(staged.sha256());
        assertThat(available.validationPolicyId()).isEqualTo(staged.validationPolicyId());
        assertThat(available.declaredMediaType()).isEqualTo("application/octet-stream");
        assertThat(available.detectedMediaType()).isEqualTo("application/pdf");
        assertThat(available.withStatus(FileAsset.Status.MISSING).withStatus(FileAsset.Status.AVAILABLE)).isEqualTo(available);
        assertThat(available.withStatus(FileAsset.Status.QUARANTINED).status()).isEqualTo(FileAsset.Status.QUARANTINED);
        assertThatThrownBy(() -> available.withStatus(FileAsset.Status.STAGED)).isInstanceOf(IllegalStateException.class);
    }

    @Test void validatesNamesSizesHashesAndRecoveryKeys() {
        asset("synthetic.pdf", 26_214_400, "0".repeat(64));
        for (String name : new String[]{"", "../file", "C:\\file", "bad\r\nname", ".", "..", "a".repeat(501)}) {
            assertThatThrownBy(() -> asset(name, 1, "a".repeat(64))).isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> asset("file", -1, "a".repeat(64))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> asset("file", 26_214_401, "a".repeat(64))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> asset("file", 1, "A".repeat(64))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> asset("file", 1, "ab")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FileAsset(UUID.randomUUID(), UUID.randomUUID(), "../outside", "part",
                "file", "text/plain", "text/plain", 1, "a".repeat(64), UUID.randomUUID(),
                FileAsset.Status.STAGED, Instant.now())).isInstanceOf(IllegalArgumentException.class);
    }

    private static FileAsset asset(String name, long size, String hash) {
        UUID upload = UUID.randomUUID();
        return new FileAsset(UUID.randomUUID(), upload, upload + ".bin", upload + ".part", name,
                "application/octet-stream", "application/pdf", size, hash, UUID.randomUUID(),
                FileAsset.Status.STAGED, Instant.parse("2026-10-22T00:00:00Z"));
    }
}
