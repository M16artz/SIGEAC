package ec.edu.unl.sec.reporting.infrastructure;

import ec.edu.unl.sec.reporting.application.DocumentaryClosureRepository;
import ec.edu.unl.sec.reporting.application.DocumentarySnapshotCodec;
import ec.edu.unl.sec.reporting.domain.DocumentaryClosure;
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
public class JdbcDocumentaryClosureRepository implements DocumentaryClosureRepository {
    private static final String HEADER="id,process_id,career_id,snapshot_id,process_revision,policy_version,closed_by,closed_at,reason,sha256";
    private final JdbcTemplate jdbc;
    private final DocumentarySnapshotCodec codec;
    public JdbcDocumentaryClosureRepository(JdbcTemplate jdbc,DocumentarySnapshotCodec codec) {this.jdbc=jdbc;this.codec=codec;}
    @Override
    public void save(DocumentaryClosure closure) {
        var header=closure.header();
        if (!codec.readClosure(closure.payloadJson(),closure.sha256()).header().equals(header)) throw JacksonDocumentarySnapshotCodec.integrity();
        jdbc.update("insert into documentary_closure("+HEADER+",payload_json) values(?,?,?,?,?,?,?,?,?,?,?)",
                header.id(),header.processId(),header.careerId(),header.snapshotId(),header.processRevision(),header.policyVersion(),
                header.closedBy(),Timestamp.from(header.closedAt()),header.reason(),header.sha256(),closure.payloadJson());
    }
    @Override
    public Optional<DocumentaryClosure.Header> header(UUID id) {
        return jdbc.query("select "+HEADER+" from documentary_closure where id=?",(row,index)->header(row),id).stream().findFirst();
    }
    @Override
    public Optional<DocumentaryClosure> find(UUID id) {
        return jdbc.query("select "+HEADER+",payload_json from documentary_closure where id=?",(row,index)-> {
            var header=header(row);
            var closure=codec.readClosure(row.getString(11),header.sha256());
            if (!closure.header().equals(header)) throw JacksonDocumentarySnapshotCodec.integrity();
            return closure;
        },id).stream().findFirst();
    }
    @Override
    public List<DocumentaryClosure.Header> list(UUID processId,long offset,int limit) {
        return jdbc.query("select "+HEADER+" from documentary_closure where process_id=? order by closed_at desc,id desc limit ? offset ?",
                (row,index)->header(row),processId,limit,offset);
    }
    private static DocumentaryClosure.Header header(ResultSet row) throws SQLException {
        return new DocumentaryClosure.Header(JdbcValueSupport.uuid(row,1),JdbcValueSupport.uuid(row,2),JdbcValueSupport.uuid(row,3),
                JdbcValueSupport.uuid(row,4),row.getLong(5),row.getString(6),JdbcValueSupport.uuid(row,7),
                JdbcValueSupport.instant(row,8),row.getString(9),row.getString(10));
    }
}
