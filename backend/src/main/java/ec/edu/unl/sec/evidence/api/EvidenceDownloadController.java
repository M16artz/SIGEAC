package ec.edu.unl.sec.evidence.api;

import ec.edu.unl.sec.evidence.application.EvidenceDownloadService;
import java.util.Base64;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EvidenceDownloadController {
    private final EvidenceDownloadService downloads;

    public EvidenceDownloadController(EvidenceDownloadService downloads) {
        this.downloads = downloads;
    }

    @GetMapping("/evidence-versions/{versionId}/files/{fileId}/content")
    public ResponseEntity<byte[]> download(@PathVariable UUID versionId, @PathVariable UUID fileId) {
        var download = downloads.prepare(versionId, fileId);
        return ResponseEntity.ok()
                .contentType(mediaType(download.mediaType()))
                .contentLength(download.sizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(download.filename(), java.nio.charset.StandardCharsets.UTF_8).build().toString())
                .header("Digest", "sha-256=" + Base64.getEncoder().encodeToString(
                        java.util.HexFormat.of().parseHex(download.sha256())))
                .header("X-Content-SHA256", download.sha256())
                .body(download.content());
    }

    private static MediaType mediaType(String detected) {
        try {
            return MediaType.parseMediaType(detected);
        } catch (IllegalArgumentException invalid) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
