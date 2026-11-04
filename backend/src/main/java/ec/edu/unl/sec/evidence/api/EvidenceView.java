package ec.edu.unl.sec.evidence.api;

import ec.edu.unl.sec.evidence.application.EvidenceDraftService;
import ec.edu.unl.sec.evidence.application.EvidenceSourceService;
import ec.edu.unl.sec.evidence.domain.EvidenceVersion;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record EvidenceView(
        UUID id,
        UUID processId,
        UUID ownerId,
        Instant createdAt,
        int latestRevisionNumber,
        Version latestVersion) {

    static EvidenceView from(EvidenceDraftService.EvidenceDetails details) {
        return from(details, EvidenceSourceService.Sources.empty());
    }

    static EvidenceView from(EvidenceDraftService.EvidenceDetails details, EvidenceSourceService.Sources sources) {
        var evidence = details.evidence();
        return new EvidenceView(
                evidence.id(),
                evidence.processId(),
                evidence.ownerId(),
                evidence.createdAt(),
                evidence.latestRevision(),
                version(details.latestVersion(), sources));
    }

    static Version version(EvidenceVersion value) {
        return version(value, EvidenceSourceService.Sources.empty());
    }

    static Version version(EvidenceVersion value, EvidenceSourceService.Sources sources) {
        return new Version(
                value.id(),
                value.evidenceId(),
                value.revisionNumber(),
                value.predecessorId(),
                value.status().name(),
                value.title(),
                value.description(),
                value.provenance(),
                value.coveredFrom(),
                value.coveredTo(),
                value.indicators().stream().map(indicator -> indicator.indicatorId()).toList(),
                value.authorId(),
                value.createdAt(),
                null,
                value.submittedAt(),
                value.lockRevision(),
                sources.files().stream().map(FileMetadataView::from).toList(),
                sources.externalReferences().stream().map(ExternalReferenceView::from).toList());
    }

    public record Version(
            UUID id,
            UUID evidenceId,
            int revisionNumber,
            UUID predecessorId,
            String status,
            String title,
            String description,
            String provenance,
            LocalDate coveredFrom,
            LocalDate coveredTo,
            List<UUID> indicatorIds,
            UUID createdBy,
            Instant createdAt,
            UUID submittedBy,
            Instant submittedAt,
            long revision,
            List<FileMetadataView> files,
            List<ExternalReferenceView> externalReferences) {}

    public record Page<T>(List<T> items, PageMetadata page) {}

    public record PageMetadata(int page, int size, long totalElements, int totalPages) {}
}
