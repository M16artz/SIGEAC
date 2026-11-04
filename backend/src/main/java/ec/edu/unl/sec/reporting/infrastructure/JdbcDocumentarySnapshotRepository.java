package ec.edu.unl.sec.reporting.infrastructure;

import ec.edu.unl.sec.reporting.application.DocumentarySnapshotCodec;
import ec.edu.unl.sec.reporting.application.DocumentarySnapshotRepository;
import ec.edu.unl.sec.reporting.domain.DocumentaryProgressCalculator;
import ec.edu.unl.sec.reporting.domain.DocumentarySnapshot;
import ec.edu.unl.sec.shared.infrastructure.JdbcValueSupport;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcDocumentarySnapshotRepository implements DocumentarySnapshotRepository {
    private static final String HEADER="id,process_id,career_id,model_version_id,process_revision,created_by,created_at,reason,"
            + "formula_version,progress_state,required_source_total,registered_source_total,accepted_source_total,sha256";
    private final JdbcTemplate jdbc;
    private final DocumentarySnapshotCodec codec;
    public JdbcDocumentarySnapshotRepository(JdbcTemplate jdbc,DocumentarySnapshotCodec codec) { this.jdbc=jdbc; this.codec=codec; }

    @Override
    public void save(DocumentarySnapshot snapshot) {
        var header=snapshot.header();
        var verified=codec.read(snapshot.payloadJson(),snapshot.sha256());
        if (!verified.header().equals(header)) throw JacksonDocumentarySnapshotCodec.integrity();
        jdbc.update("insert into documentary_snapshot("+HEADER+",payload_json) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                header.id(),header.processId(),header.careerId(),header.modelVersionId(),header.processRevision(),
                header.createdBy(),Timestamp.from(header.createdAt()),header.reason(),header.progress().formulaVersion(),
                header.progress().status().name(),header.progress().requiredSourceTotal(),header.progress().registeredSourceTotal(),
                header.progress().acceptedSourceTotal(),header.sha256(),snapshot.payloadJson());
    }

    @Override
    public Optional<DocumentarySnapshot.Header> header(UUID id) {
        return jdbc.query("select "+HEADER+" from documentary_snapshot where id=?",(row,index)->header(row),id).stream().findFirst();
    }

    @Override
    public Optional<DocumentarySnapshot> find(UUID id) {
        return jdbc.query("select "+HEADER+",payload_json from documentary_snapshot where id=?",(row,index)-> {
            var header=header(row);
            var snapshot=codec.read(row.getString(15),header.sha256());
            if (!header.equals(snapshot.header())) throw JacksonDocumentarySnapshotCodec.integrity();
            return snapshot;
        },id).stream().findFirst();
    }

    @Override
    public List<DocumentarySnapshot.Header> list(UUID processId,long offset,int limit) {
        return jdbc.query("select "+HEADER+" from documentary_snapshot where process_id=? order by created_at desc,id desc limit ? offset ?",
                (row,index)->header(row),processId,limit,offset);
    }

    private static DocumentarySnapshot.Header header(ResultSet row) throws SQLException {
        return new DocumentarySnapshot.Header(JdbcValueSupport.uuid(row,1),JdbcValueSupport.uuid(row,2),JdbcValueSupport.uuid(row,3),
                JdbcValueSupport.uuid(row,4),row.getLong(5),JdbcValueSupport.uuid(row,6),JdbcValueSupport.instant(row,7),row.getString(8),
                new DocumentaryProgressCalculator.Progress(DocumentaryProgressCalculator.Status.valueOf(row.getString(10)),
                        row.getInt(11),row.getInt(12),row.getInt(13),row.getString(9)),row.getString(14));
    }
}
