package ec.edu.unl.sec.evidence.application;

import ec.edu.unl.sec.evidence.domain.ExternalReference;
import ec.edu.unl.sec.evidence.domain.VersionFile;
import ec.edu.unl.sec.filestorage.application.FileAssetRepository;
import ec.edu.unl.sec.filestorage.domain.FileAsset;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EvidenceSourceService {
    private final EvidenceDraftService drafts;
    private final EvidenceSourceRepository repository;
    private final FileAssetRepository assets;

    public EvidenceSourceService(EvidenceDraftService drafts, EvidenceSourceRepository repository, FileAssetRepository assets) {
        this.drafts = drafts;
        this.repository = repository;
        this.assets = assets;
    }

    @Transactional(readOnly = true)
    public Sources snapshot(UUID versionId) {
        drafts.getVersion(versionId);
        return new Sources(repository.files(versionId).stream().map(file -> new SourceFile(file,
                assets.findById(file.fileId()).orElseThrow(EvidenceDraftService::notFound).status())).toList(),
                repository.references(versionId));
    }

    @Transactional
    public Changed<ExternalReference> addReference(UUID versionId, String ifMatch, String uri, String description) {
        var version = drafts.requireEditableVersion(versionId, ifMatch);
        advance(versionId, version.lockRevision());
        var reference = new ExternalReference(UUID.randomUUID(), versionId, uri, description, repository.nextReferencePosition(versionId));
        repository.addReference(reference);
        return new Changed<>(reference, Math.incrementExact(version.lockRevision()));
    }

    @Transactional
    public Changed<ExternalReference> updateReference(UUID versionId, UUID referenceId, String ifMatch, String uri, String description) {
        var version = drafts.requireEditableVersion(versionId, ifMatch);
        var previous = reference(versionId, referenceId);
        var updated = new ExternalReference(previous.id(), versionId, uri, description, previous.position());
        advance(versionId, version.lockRevision());
        repository.updateReference(updated);
        return new Changed<>(updated, Math.incrementExact(version.lockRevision()));
    }

    @Transactional
    public long removeReference(UUID versionId, UUID referenceId, String ifMatch) {
        var version = drafts.requireEditableVersion(versionId, ifMatch);
        reference(versionId, referenceId);
        advance(versionId, version.lockRevision());
        repository.removeReference(versionId, referenceId);
        return Math.incrementExact(version.lockRevision());
    }

    /** Attaches an already promoted asset to a draft without re-reading its bytes. */
    @Transactional
    public Changed<SourceFile> attachFile(UUID versionId, UUID fileId, long expectedRevision) {
        var asset = assets.findById(fileId).orElseThrow(EvidenceDraftService::notFound);
        if (asset.status() != FileAsset.Status.AVAILABLE) {
            throw new EvidenceProblemException(EvidenceProblemException.Kind.CONFLICT,
                    "FILE_NOT_AVAILABLE", "El archivo aún no está disponible.");
        }
        advance(versionId, expectedRevision);
        var snapshot = VersionFile.from(versionId, asset, repository.nextFilePosition(versionId));
        repository.addFile(snapshot);
        return new Changed<>(new SourceFile(snapshot, asset.status()), Math.incrementExact(expectedRevision));
    }

    @Transactional
    public long removeFile(UUID versionId, UUID fileId, String ifMatch) {
        var version = drafts.requireEditableVersion(versionId, ifMatch);
        repository.files(versionId).stream().filter(file -> file.fileId().equals(fileId))
                .findFirst().orElseThrow(EvidenceDraftService::notFound);
        advance(versionId, version.lockRevision());
        repository.removeFile(versionId, fileId);
        return Math.incrementExact(version.lockRevision());
    }

    private ExternalReference reference(UUID versionId, UUID referenceId) {
        return repository.references(versionId).stream().filter(reference -> reference.id().equals(referenceId))
                .findFirst().orElseThrow(EvidenceDraftService::notFound);
    }

    private void advance(UUID versionId, long expectedRevision) {
        if (!repository.advanceDraftRevision(versionId, expectedRevision)) {
            throw new EvidenceProblemException(EvidenceProblemException.Kind.PRECONDITION_FAILED,
                    "STALE_DRAFT", "El borrador cambió; recargue antes de continuar.");
        }
    }

    public record Changed<T>(T value, long revision) {}
    public record SourceFile(VersionFile snapshot, FileAsset.Status status) {}
    public record Sources(List<SourceFile> files, List<ExternalReference> externalReferences) {
        public Sources { files = List.copyOf(files); externalReferences = List.copyOf(externalReferences); }
        public static Sources empty() { return new Sources(List.of(), List.of()); }
    }
}
