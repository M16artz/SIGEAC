package ec.edu.unl.sec.filestorage.infrastructure;

import ec.edu.unl.sec.filestorage.application.FileAssetRepository;
import ec.edu.unl.sec.filestorage.domain.FileAsset;
import ec.edu.unl.sec.shared.infrastructure.JdbcValueSupport;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JpaFileAssetRepository implements FileAssetRepository {
    private static final String COLUMNS = "id,upload_id,storage_key,staging_key,original_name,declared_media_type,"
            + "detected_media_type,size_bytes,sha256,validation_policy_id,status,created_at";
    private final JdbcTemplate jdbc;

    public JpaFileAssetRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override public void create(FileAsset asset) {
        jdbc.update("insert into file_asset(" + COLUMNS + ") values(?,?,?,?,?,?,?,?,?,?,?,?)",
                asset.id(), asset.uploadId(), asset.storageKey(), asset.stagingKey(), asset.originalName(),
                asset.declaredMediaType(), asset.detectedMediaType(), asset.sizeBytes(), asset.sha256(),
                asset.validationPolicyId(), asset.status().name(), Timestamp.from(asset.createdAt()));
    }

    @Override public Optional<FileAsset> findById(UUID id) {
        return jdbc.query("select " + COLUMNS + " from file_asset where id=?", (row, number) -> map(row), id).stream().findFirst();
    }

    @Override public Optional<FileAsset> findByUploadId(UUID uploadId) {
        return jdbc.query("select " + COLUMNS + " from file_asset where upload_id=?", (row, number) -> map(row), uploadId).stream().findFirst();
    }

    @Override public boolean changeStatus(UUID id, FileAsset.Status expected, FileAsset.Status next) {
        FileAsset.requireTransition(expected, next);
        return jdbc.update("update file_asset set status=? where id=? and status=?", next.name(), id, expected.name()) == 1;
    }

    private static FileAsset map(ResultSet row) throws SQLException {
        return new FileAsset(JdbcValueSupport.uuid(row, 1), JdbcValueSupport.uuid(row, 2), row.getString(3),
                row.getString(4), row.getString(5), row.getString(6), row.getString(7), row.getLong(8),
                row.getString(9), JdbcValueSupport.uuid(row, 10), FileAsset.Status.valueOf(row.getString(11)),
                JdbcValueSupport.instant(row, 12));
    }
}
