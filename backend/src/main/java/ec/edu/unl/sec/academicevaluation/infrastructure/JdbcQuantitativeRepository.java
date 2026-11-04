package ec.edu.unl.sec.academicevaluation.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.unl.sec.academicevaluation.application.QuantitativeRepository;
import ec.edu.unl.sec.academicevaluation.domain.QuantitativeEvaluation.*;
import ec.edu.unl.sec.shared.infrastructure.JdbcValueSupport;
import java.sql.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcQuantitativeRepository implements QuantitativeRepository {
    private final JdbcTemplate jdbc;private final ObjectMapper json;
    public JdbcQuantitativeRepository(JdbcTemplate jdbc,ObjectMapper json) {this.jdbc=jdbc;this.json=json;}
    @Override public void append(Inputs v) {
        jdbc.update("insert into quantitative_input_set(id,process_id,indicator_id,captured_by,revision,predecessor_id,captured_at,payload_json) values(?,?,?,?,?,?,?,?)",
                v.id(),v.processId(),v.indicatorId(),v.capturedBy(),v.revision(),v.predecessorId(),Timestamp.from(v.capturedAt()),encode(v));
    }
    @Override public void verify(Verification v) {
        jdbc.update("insert into quantitative_verification(input_set_id,verified_by,verified_at,payload_json) values(?,?,?,?)",
                v.inputSetId(),v.verifiedBy(),Timestamp.from(v.verifiedAt()),encode(v));
    }
    @Override public void append(Run v) {
        jdbc.update("insert into quantitative_calculation(id,input_set_id,process_id,indicator_id,executed_by,executed_at,payload_json) values(?,?,?,?,?,?,?)",
                v.id(),v.inputs().id(),v.inputs().processId(),v.inputs().indicatorId(),v.executedBy(),Timestamp.from(v.executedAt()),encode(v));
    }
    @Override public Optional<Inputs> inputs(UUID id) {return jdbc.query("select * from quantitative_input_set where id=?",(r,n)->inputs(r),id).stream().findFirst();}
    @Override public Optional<Inputs> latest(UUID p,UUID i,UUID a) {return jdbc.query("select * from quantitative_input_set where process_id=? and indicator_id=? and captured_by=? order by revision desc limit 1",
            (r,n)->inputs(r),p,i,a).stream().findFirst();}
    @Override public Optional<Verification> verification(UUID id) {
        return jdbc.query("select * from quantitative_verification where input_set_id=?",(r,n)-> {
            var v=decode(r,Verification.class);
            check(v.inputSetId().equals(uuid(r,"input_set_id")) && v.verifiedBy().equals(uuid(r,"verified_by")));return v;
        },id).stream().findFirst();
    }
    @Override public Optional<Run> run(UUID id) {return jdbc.query("select * from quantitative_calculation where input_set_id=?",(r,n)->run(r),id).stream().findFirst();}
    @Override public List<Run> history(UUID p,UUID i,long offset,int limit) {
        return jdbc.query("select * from quantitative_calculation where process_id=? and indicator_id=? order by executed_at desc,id desc limit ? offset ?",(r,n)->run(r),p,i,limit,offset);
    }
    @Override public List<Run> current(UUID p,UUID i) {
        return jdbc.query("select v.* from quantitative_calculation v where v.process_id=? and v.indicator_id=?"
                +" and not exists(select 1 from quantitative_calculation newer where newer.process_id=v.process_id"
                +" and newer.indicator_id=v.indicator_id and newer.executed_by=v.executed_by"
                +" and (newer.executed_at>v.executed_at or(newer.executed_at=v.executed_at and newer.id>v.id))) order by v.executed_by",
                (r,n)->run(r),p,i);
    }
    private Inputs inputs(ResultSet r) throws SQLException {
        var v=decode(r,Inputs.class);
        check(v.id().equals(uuid(r,"id")) && v.processId().equals(uuid(r,"process_id")) && v.indicatorId().equals(uuid(r,"indicator_id"))
                && v.capturedBy().equals(uuid(r,"captured_by")) && v.revision()==r.getLong("revision") && Objects.equals(v.predecessorId(),uuid(r,"predecessor_id")));
        return v;
    }
    private Run run(ResultSet r) throws SQLException {
        var v=decode(r,Run.class);
        check(v.id().equals(uuid(r,"id")) && v.inputs().id().equals(uuid(r,"input_set_id")) && v.inputs().processId().equals(uuid(r,"process_id"))
                && v.inputs().indicatorId().equals(uuid(r,"indicator_id")) && v.executedBy().equals(uuid(r,"executed_by")));return v;
    }
    private static UUID uuid(ResultSet r,String name) throws SQLException {return JdbcValueSupport.uuid(r,r.findColumn(name));}
    private static void check(boolean valid) {if(!valid) throw new IllegalStateException("Quantitative metadata mismatch");}
    private String encode(Object v) {try{return json.writeValueAsString(v);}catch(JsonProcessingException ex){throw new IllegalStateException("Cannot serialize quantitative record",ex);}}
    private <T>T decode(ResultSet r,Class<T> type) throws SQLException {try{return json.readValue(r.getString("payload_json"),type);}catch(JsonProcessingException ex){throw new IllegalStateException("Cannot read quantitative record",ex);}}
}
