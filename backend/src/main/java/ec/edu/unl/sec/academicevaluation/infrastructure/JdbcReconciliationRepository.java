package ec.edu.unl.sec.academicevaluation.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.unl.sec.academicevaluation.application.ReconciliationRepository;
import ec.edu.unl.sec.academicevaluation.domain.AcademicReconciliation.*;
import ec.edu.unl.sec.shared.infrastructure.JdbcValueSupport;
import java.sql.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcReconciliationRepository implements ReconciliationRepository {
    private final JdbcTemplate jdbc;private final ObjectMapper json;
    public JdbcReconciliationRepository(JdbcTemplate jdbc,ObjectMapper json){this.jdbc=jdbc;this.json=json;}
    @Override public void append(Policy p){jdbc.update("insert into academic_reconciliation_policy(id,process_id,indicator_id,revision,predecessor_id,configured_by,authority_assignment_id,configured_at,payload_json) values(?,?,?,?,?,?,?,?,?)",
            p.id(),p.processId(),p.indicatorId(),p.revision(),p.predecessorId(),p.configuredBy(),p.authorityAssignmentId(),Timestamp.from(p.configuredAt()),encode(p));}
    @Override public void append(Disagreement d){jdbc.update("insert into academic_disagreement_record(id,process_id,indicator_id,revision,predecessor_id,opened_by,opened_at,payload_json) values(?,?,?,?,?,?,?,?)",
            d.id(),d.processId(),d.indicatorId(),d.revision(),d.predecessorId(),d.openedBy(),Timestamp.from(d.openedAt()),encode(d));}
    @Override public void append(Resolution r){jdbc.update("insert into academic_reconciliation_resolution(id,process_id,indicator_id,disagreement_id,policy_id,resolved_by,authority_assignment_id,resolved_at,payload_json) values(?,?,?,?,?,?,?,?,?)",
            r.id(),r.processId(),r.indicatorId(),r.disagreementId(),r.policy().id(),r.resolvedBy(),r.authorityAssignmentId(),Timestamp.from(r.resolvedAt()),encode(r));}
    @Override public Optional<Policy> policy(UUID p,UUID i){return jdbc.query("select * from academic_reconciliation_policy where process_id=? and indicator_id=? order by revision desc limit 1",(r,n)->policy(r),p,i).stream().findFirst();}
    @Override public Optional<Disagreement> latest(UUID p,UUID i){return jdbc.query("select * from academic_disagreement_record where process_id=? and indicator_id=? order by revision desc limit 1",(r,n)->disagreement(r),p,i).stream().findFirst();}
    @Override public List<Disagreement> history(UUID p,UUID i,long offset,int limit){return jdbc.query("select * from academic_disagreement_record where process_id=? and indicator_id=? order by revision desc limit ? offset ?",(r,n)->disagreement(r),p,i,limit,offset);}
    @Override public Optional<Resolution> resolution(UUID id){return jdbc.query("select * from academic_reconciliation_resolution where disagreement_id=?",(r,n)->{
        var v=decode(r,Resolution.class);check(v.id().equals(uuid(r,"id")) && v.processId().equals(uuid(r,"process_id")) && v.indicatorId().equals(uuid(r,"indicator_id"))
                && v.disagreementId().equals(uuid(r,"disagreement_id")) && v.policy().id().equals(uuid(r,"policy_id"))
                && v.resolvedBy().equals(uuid(r,"resolved_by")) && v.authorityAssignmentId().equals(uuid(r,"authority_assignment_id")));return v;
    },id).stream().findFirst();}
    private Policy policy(ResultSet r)throws SQLException{
        var v=decode(r,Policy.class);check(v.id().equals(uuid(r,"id")) && v.processId().equals(uuid(r,"process_id")) && v.indicatorId().equals(uuid(r,"indicator_id"))
                && v.revision()==r.getLong("revision") && Objects.equals(v.predecessorId(),uuid(r,"predecessor_id"))
                && v.configuredBy().equals(uuid(r,"configured_by")) && v.authorityAssignmentId().equals(uuid(r,"authority_assignment_id")));return v;
    }
    private Disagreement disagreement(ResultSet r)throws SQLException{
        var v=decode(r,Disagreement.class);check(v.id().equals(uuid(r,"id")) && v.processId().equals(uuid(r,"process_id")) && v.indicatorId().equals(uuid(r,"indicator_id"))
                && v.revision()==r.getLong("revision") && Objects.equals(v.predecessorId(),uuid(r,"predecessor_id")) && v.openedBy().equals(uuid(r,"opened_by")));return v;
    }
    private static UUID uuid(ResultSet r,String name)throws SQLException{return JdbcValueSupport.uuid(r,r.findColumn(name));}
    private static void check(boolean value){if(!value)throw new IllegalStateException("Reconciliation metadata mismatch");}
    private String encode(Object value){try{return json.writeValueAsString(value);}catch(JsonProcessingException ex){throw new IllegalStateException("Cannot serialize reconciliation",ex);}}
    private <T>T decode(ResultSet r,Class<T> type)throws SQLException{try{return json.readValue(r.getString("payload_json"),type);}catch(JsonProcessingException ex){throw new IllegalStateException("Cannot read reconciliation",ex);}}
}
