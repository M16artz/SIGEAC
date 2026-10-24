package ec.edu.unl.sec.evidence.application;

import ec.edu.unl.sec.filestorage.application.FileAssetRepository;
import ec.edu.unl.sec.filestorage.application.FileStoragePort;
import ec.edu.unl.sec.filestorage.domain.FileAsset;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EvidenceIntegrityService implements EvidenceIntegrityPort {
    private final EvidenceSourceRepository sources;
    private final FileAssetRepository assets;
    private final FileStoragePort storage;
    public EvidenceIntegrityService(EvidenceSourceRepository sources,FileAssetRepository assets,FileStoragePort storage) {
        this.sources=sources;this.assets=assets;this.storage=storage;
    }

    @Override
    @Transactional(readOnly=true)
    public Report verify(Set<UUID> versionIds) {
        var files=new ArrayList<FileReference>();
        var issues=new ArrayList<Issue>();
        for (var versionId:versionIds.stream().sorted().toList()) {
            for (var file:sources.files(versionId)) {
                var asset=assets.findById(file.fileId()).orElse(null);
                if (asset==null || asset.status()!=FileAsset.Status.AVAILABLE) {
                    issues.add(new Issue(versionId,file.fileId(),"FILE_NOT_AVAILABLE"));
                    continue;
                }
                try {
                    var actual=storage.verify(asset.storageKey());
                    if (actual.sizeBytes()!=file.sizeBytes() || actual.sizeBytes()!=asset.sizeBytes()
                            || !actual.sha256().equals(file.sha256()) || !actual.sha256().equals(asset.sha256())) {
                        issues.add(new Issue(versionId,file.fileId(),"FILE_INTEGRITY_FAILED"));
                    } else files.add(new FileReference(versionId,file.fileId(),file.sizeBytes(),file.sha256()));
                } catch (IOException missing) {
                    issues.add(new Issue(versionId,file.fileId(),"FILE_INTEGRITY_FAILED"));
                }
            }
        }
        return new Report(files,issues);
    }
}
