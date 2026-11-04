package ec.edu.unl.sec.evidence.api;

import ec.edu.unl.sec.evidence.application.EvidenceSourceService;
import java.util.UUID;

public record FileMetadataView(UUID id, String originalName, String declaredMediaType, String detectedMediaType,
        long sizeBytes, String sha256, String status) {
    static FileMetadataView from(EvidenceSourceService.SourceFile source) {
        var file = source.snapshot();
        return new FileMetadataView(file.fileId(), file.originalName(), file.declaredMediaType(),
                file.detectedMediaType(), file.sizeBytes(), file.sha256(), source.status().name());
    }
}
