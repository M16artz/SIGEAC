package ec.edu.unl.sec.selfassessment.infrastructure;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.unl.sec.selfassessment.application.*;
import ec.edu.unl.sec.selfassessment.domain.*;
import ec.edu.unl.sec.shared.infrastructure.JdbcValueSupport;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
@Repository
public class JdbcCareerCatalogRepository implements CareerCatalogRepository {
    private final JdbcTemplate jdbc;private final ObjectMapper json;private final boolean postgres;
    public JdbcCareerCatalogRepository(JdbcTemplate jdbc,ObjectMapper json){this.jdbc=jdbc;this.json=json;this.postgres=Boolean.TRUE.equals(jdbc.execute((ConnectionCallback<Boolean>)c->c.getMetaData().getDatabaseProductName().contains("PostgreSQL")));}
    public Optional<AcademicUnit> findUnit(UUID id){return jdbc.query("select * from academic_unit where id=?",this::unit,id).stream().findFirst();}
    public Optional<Career> findCareer(UUID id){return jdbc.query("select * from career where id=?",this::career,id).stream().findFirst();}
    public CatalogPage<Career> careers(Set<UUID> ids,int page,int size,String query,Career.Status status,UUID unitId){
        var args=new ArrayList<Object>();String where=scope("c.id",ids,args)+search("c",query,args);
        if(status!=null){where+=" and c.status=?";args.add(status.name());}if(unitId!=null){where+=" and c.academic_unit_id=?";args.add(unitId);}
        long total=jdbc.queryForObject("select count(*) from career c where "+where,Long.class,args.toArray());
        args.add(size);args.add(page*size);var items=jdbc.query("select c.* from career c where "+where+" order by c.code,c.id limit ? offset ?",this::career,args.toArray());
        return new CatalogPage<>(items,page,size,total);
    }
    public CatalogPage<AcademicUnit> units(Set<UUID> ids,int page,int size,String query){
        var args=new ArrayList<Object>();String where=ids==null?"1=1":"exists(select 1 from career c where c.academic_unit_id=u.id and "+scope("c.id",ids,args)+")";
        where+=search("u",query,args);long total=jdbc.queryForObject("select count(*) from academic_unit u where "+where,Long.class,args.toArray());
        args.add(size);args.add(page*size);return new CatalogPage<>(jdbc.query("select u.* from academic_unit u where "+where+" order by u.code,u.id limit ? offset ?",this::unit,args.toArray()),page,size,total);
    }
    public boolean unitCodeExists(String code,UUID except){return codeExists("academic_unit",code,except);}
    public boolean careerCodeExists(String code,UUID except){return codeExists("career",code,except);}
    private boolean codeExists(String table,String code,UUID except){return jdbc.queryForObject("select count(*) from "+table+" where code=?"+(except==null?"":" and id<>?"),Long.class,except==null?new Object[]{code}:new Object[]{code,except})>0;}
    public boolean hasActiveCareers(UUID unitId){return jdbc.queryForObject("select count(*) from career where academic_unit_id=? and status='ACTIVE'",Long.class,unitId)>0;}
    @Transactional public void saveUnit(AcademicUnit unit,long expected,UUID actor,String reason){
        try {
            if(expected<0)jdbc.update("insert into academic_unit(id,code,name,status,revision,created_by,created_at) values(?,?,?,?,?,?,?)",unit.id(),unit.data().code(),unit.data().name(),unit.status().name(),unit.revision(),unit.createdBy(),Timestamp.from(unit.createdAt()));
            else check(jdbc.update("update academic_unit set code=?,name=?,status=?,revision=? where id=? and revision=?",unit.data().code(),unit.data().name(),unit.status().name(),unit.revision(),unit.id(),expected));
            history("academic_unit_history","unit_id",unit.id(),unit.revision(),unit,actor,reason);
        }catch(DataAccessException error){throw conflict(error);}
    }
    @Transactional public void saveCareer(Career value,long expected,UUID actor,String reason){
        try {
            if(expected<0)jdbc.update("insert into career(id,academic_unit_id,code,name,status,valid_from,valid_until,classification_pending,revision,created_by,created_at) values(?,?,?,?,?,?,?,?,?,?,?)",value.id(),value.data().academicUnitId(),value.data().code(),value.data().name(),value.status().name(),value.data().validFrom(),value.data().validUntil(),value.classificationPending(),value.revision(),value.createdBy(),Timestamp.from(value.createdAt()));
            else check(jdbc.update("update career set academic_unit_id=?,code=?,name=?,status=?,valid_from=?,valid_until=?,classification_pending=?,revision=? where id=? and revision=?",value.data().academicUnitId(),value.data().code(),value.data().name(),value.status().name(),value.data().validFrom(),value.data().validUntil(),value.classificationPending(),value.revision(),value.id(),expected));
            history("career_history","career_id",value.id(),value.revision(),value,actor,reason);
        }catch(DataAccessException error){throw conflict(error);}
    }
    public CatalogPage<Career> careerHistory(UUID id,int page,int size){var items=jdbc.query("select "+(postgres?"snapshot_json::text":"snapshot_json")+" from career_history where career_id=? order by revision limit ? offset ?",(r,n)->decode(r.getString(1)),id,size,page*size);return new CatalogPage<>(items,page,size,jdbc.queryForObject("select count(*) from career_history where career_id=?",Long.class,id));}
    private void history(String table,String key,UUID id,long revision,Object snapshot,UUID actor,String reason){jdbc.update("insert into "+table+"("+key+",revision,snapshot_json,changed_by,changed_at,reason) values(?,?,"+(postgres?"?::jsonb":"?")+",?,?,?)",id,revision,encode(snapshot),actor,Timestamp.from(Instant.now()),reason);}
    private String encode(Object value){try{return json.writeValueAsString(value);}catch(JsonProcessingException error){throw new IllegalStateException("CATALOG_SERIALIZATION_FAILED",error);}}
    private Career decode(String value){try{return json.readValue(value,Career.class);}catch(JsonProcessingException error){throw new IllegalStateException("CATALOG_SERIALIZATION_FAILED",error);}}
    private AcademicUnit unit(ResultSet r,int row)throws SQLException{return new AcademicUnit(uuid(r,"id"),new AcademicUnit.Data(r.getString("code"),r.getString("name")),Career.Status.valueOf(r.getString("status")),r.getLong("revision"),uuid(r,"created_by"),instant(r,"created_at"));}
    private Career career(ResultSet r,int row)throws SQLException{return new Career(uuid(r,"id"),new Career.Data(r.getString("code"),r.getString("name"),uuid(r,"academic_unit_id"),LocalDate.parse(r.getString("valid_from")),r.getString("valid_until")==null?null:LocalDate.parse(r.getString("valid_until"))),Career.Status.valueOf(r.getString("status")),r.getLong("revision"),uuid(r,"created_by"),instant(r,"created_at"),r.getBoolean("classification_pending"));}
    private static UUID uuid(ResultSet r,String name)throws SQLException{return JdbcValueSupport.uuid(r,r.findColumn(name));}
    private static Instant instant(ResultSet r,String name)throws SQLException{return JdbcValueSupport.instant(r,r.findColumn(name));}
    private static String scope(String column,Set<UUID> ids,List<Object> args){if(ids==null)return "1=1";if(ids.isEmpty())return "1=0";args.addAll(ids);return column+" in ("+String.join(",",Collections.nCopies(ids.size(),"?"))+")";}
    private static String search(String alias,String query,List<Object> args){if(query==null||query.isBlank())return "";String value="%"+query.toUpperCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_")+"%";args.add(value);args.add(value);return " and (upper("+alias+".code) like ? escape '!' or upper("+alias+".name) like ? escape '!')";}
    private static void check(int changed){if(changed!=1)throw new CareerCatalogException(412,"STALE_REVISION","El recurso cambió; recargue antes de continuar.");}
    private static RuntimeException conflict(DataAccessException error){
        for(Throwable cause=error;cause!=null;cause=cause.getCause())if(cause instanceof SQLException sql && ("23505".equals(sql.getSQLState()) || (sql.getErrorCode()==19 && sql.getMessage().contains("UNIQUE"))))return new CareerCatalogException(409,"DUPLICATE_CODE","El código ya está registrado.");
        for(Throwable cause=error;cause!=null;cause=cause.getCause())if(cause instanceof SQLException sql && ("23514".equals(sql.getSQLState()) || "23503".equals(sql.getSQLState())))return new CareerCatalogException(409,"CATALOG_STATE_CONFLICT","El estado o las relaciones del catálogo cambiaron; recargue antes de continuar.");
        return error;
    }
}
