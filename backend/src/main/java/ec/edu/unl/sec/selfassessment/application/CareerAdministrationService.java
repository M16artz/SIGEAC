package ec.edu.unl.sec.selfassessment.application;
import ec.edu.unl.sec.audit.application.*;
import ec.edu.unl.sec.identityaccess.application.*;
import ec.edu.unl.sec.identityaccess.domain.*;
import ec.edu.unl.sec.selfassessment.domain.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class CareerAdministrationService {
    private final CareerCatalogRepository repository; private final CurrentActorPort actors;
    private final AuthorizationPort authorization; private final AuditPort audit;
    public CareerAdministrationService(CareerCatalogRepository repository,CurrentActorPort actors,AuthorizationPort authorization,AuditPort audit) {
        this.repository=repository;this.actors=actors;this.authorization=authorization;this.audit=audit;
    }
    @Transactional(readOnly=true) public CatalogPage<Career> careers(int page,int size,String query,Career.Status status,UUID unitId) {
        var actor=current();page(page,size); return repository.careers(visibleIds(actor),page,size,query(query),status,unitId);
    }
    @Transactional(readOnly=true) public Map<String,Boolean> access() {var actor=current();visibleIds(actor);return Map.of("canManage",admin(actor));}
    @Transactional(readOnly=true) public CatalogPage<AcademicUnit> units(int page,int size,String query) {
        var actor=current();page(page,size);return repository.units(visibleIds(actor),page,size,query(query));
    }
    @Transactional(readOnly=true) public Career getCareer(UUID id) {var actor=current();return career(id,actor);}
    @Transactional(readOnly=true) public CatalogPage<Career> history(UUID id,int page,int size) {var actor=current();career(id,actor);page(page,size);return repository.careerHistory(id,page,size);}
    @Transactional(readOnly=true) public AcademicUnit getUnit(UUID id) {
        var actor=current();var unit=repository.findUnit(id).orElseThrow(CareerAdministrationService::hidden);
        if(!admin(actor)) {
            var visible=repository.careers(visibleIds(actor),0,1,"",null,id);
            if(visible.totalElements()==0) throw hidden();
        }
        return unit;
    }
    @Transactional public AcademicUnit createUnit(AcademicUnit.Data data,UUID correlation) {
        var actor=requireAdmin();if(repository.unitCodeExists(data.code(),null))throw duplicate();
        var unit=new AcademicUnit(UUID.randomUUID(),data,Career.Status.ACTIVE,1,actor.identityId(),Instant.now());
        repository.saveUnit(unit,-1,actor.identityId(),"UNIT_CREATED");record(actor,correlation,"UNIT_CREATED",unit.id(),null);return unit;
    }
    @Transactional public AcademicUnit updateUnit(UUID id,long expected,AcademicUnit.Data data,UUID correlation) {
        var actor=requireAdmin();var old=repository.findUnit(id).orElseThrow(CareerAdministrationService::hidden);revision(old.revision(),expected);
        if(repository.unitCodeExists(data.code(),id))throw duplicate();var unit=old.replace(data);
        repository.saveUnit(unit,expected,actor.identityId(),"UNIT_UPDATED");record(actor,correlation,"UNIT_UPDATED",id,null);return unit;
    }
    @Transactional public AcademicUnit changeUnitState(UUID id,long expected,Career.Status status,String reason,UUID correlation) {
        var actor=requireAdmin();var old=repository.findUnit(id).orElseThrow(CareerAdministrationService::hidden);revision(old.revision(),expected);reason(reason);
        if(status==null || old.status()==status)throw error(409,"STATE_UNCHANGED","No existe un cambio de estado.");
        if(status==Career.Status.DISABLED && repository.hasActiveCareers(id))throw error(409,"UNIT_HAS_ACTIVE_CAREERS","Desactive o reasigne las carreras activas antes de desactivar la unidad.");
        var unit=old.state(status);repository.saveUnit(unit,expected,actor.identityId(),reason.strip());record(actor,correlation,"UNIT_STATE_CHANGED",id,null);return unit;
    }
    @Transactional public Career createCareer(Career.Data data,UUID correlation) {
        var actor=requireAdmin();activeUnit(data.academicUnitId());if(repository.careerCodeExists(data.code(),null))throw duplicate();
        var value=new Career(UUID.randomUUID(),data,Career.Status.ACTIVE,1,actor.identityId(),Instant.now(),false);
        repository.saveCareer(value,-1,actor.identityId(),"CAREER_CREATED");record(actor,correlation,"CAREER_CREATED",value.id(),value.id());return value;
    }
    @Transactional public Career updateCareer(UUID id,long expected,Career.Data data,UUID correlation) {
        var actor=requireAdmin();var old=repository.findCareer(id).orElseThrow(CareerAdministrationService::hidden);revision(old.revision(),expected);activeUnit(data.academicUnitId());
        if(repository.careerCodeExists(data.code(),id))throw duplicate();var value=old.replace(data);
        repository.saveCareer(value,expected,actor.identityId(),"CAREER_UPDATED");record(actor,correlation,"CAREER_UPDATED",id,id);return value;
    }
    @Transactional public Career changeCareerState(UUID id,long expected,Career.Status status,String reason,UUID correlation) {
        var actor=requireAdmin();var old=repository.findCareer(id).orElseThrow(CareerAdministrationService::hidden);revision(old.revision(),expected);reason(reason);
        if(status==null || old.status()==status)throw error(409,"STATE_UNCHANGED","No existe un cambio de estado.");
        if(status==Career.Status.ACTIVE){activeUnit(old.data().academicUnitId());if(old.classificationPending())throw error(409,"CAREER_UNCLASSIFIED","Complete los datos de la carrera antes de activarla.");}
        var value=old.state(status);repository.saveCareer(value,expected,actor.identityId(),reason.strip());record(actor,correlation,"CAREER_STATE_CHANGED",id,id);return value;
    }
    @Transactional(readOnly=true) public void requireActiveCareer(UUID id,LocalDate start,LocalDate end) {
        var value=repository.findCareer(id).orElseThrow(CareerAdministrationService::hidden);
        if(!value.activeOn(LocalDate.now(ZoneOffset.UTC)))throw error(409,"CAREER_INACTIVE","La carrera no está activa o requiere clasificación.");
        activeUnit(value.data().academicUnitId());
        if(start==null || end==null || start.isAfter(end) || start.isBefore(value.data().validFrom()) || (value.data().validUntil()!=null && end.isAfter(value.data().validUntil())))
            throw error(422,"CAREER_PERIOD_MISMATCH","El período debe pertenecer a la vigencia de la carrera.");
    }
    private Career career(UUID id,CurrentActorPort.Actor actor) {
        if(!admin(actor) && !authorization.isAllowed(actor.identityId(),OperationCode.CAREER_READ,new AuthorizationScope(id,null,null)))throw hidden();
        return repository.findCareer(id).orElseThrow(CareerAdministrationService::hidden);
    }
    private Set<UUID> visibleIds(CurrentActorPort.Actor actor) {
        if(admin(actor))return null;
        if(!authorization.hasOperation(actor.identityId(),OperationCode.CAREER_READ))throw error(403,"CAPABILITY_DENIED","No dispone de acceso al catálogo.");
        return authorization.authorizedCareerIds(actor.identityId(),OperationCode.CAREER_READ);
    }
    private boolean admin(CurrentActorPort.Actor actor){return authorization.hasOperation(actor.identityId(),OperationCode.CAREER_ADMIN);}
    private CurrentActorPort.Actor current(){var actor=actors.currentActor().orElseThrow(()->error(401,"AUTHENTICATION_REQUIRED","Se requiere autenticación."));if(actor.technical())throw error(403,"CAPABILITY_DENIED","Una cuenta técnica no administra el catálogo.");return actor;}
    private CurrentActorPort.Actor requireAdmin(){var actor=current();if(!admin(actor))throw error(403,"CAPABILITY_DENIED","Se requiere administración del catálogo.");return actor;}
    private void activeUnit(UUID id){if(repository.findUnit(id).filter(u->u.status()==Career.Status.ACTIVE).isEmpty())throw error(422,"UNIT_UNAVAILABLE","La unidad no existe o no está activa.");}
    private static void revision(long actual,long expected){if(actual!=expected)throw error(412,"STALE_REVISION","El recurso cambió; recargue antes de continuar.");}
    private static void reason(String reason){if(reason==null || reason.strip().length()<10 || reason.strip().length()>4000)throw error(422,"REASON_REQUIRED","Indique un motivo de 10 a 4000 caracteres.");}
    private static void page(int page,int size){if(page<0 || page>Integer.MAX_VALUE/100 || size<1 || size>100)throw error(400,"INVALID_PAGE","Paginación fuera de rango.");}
    private static String query(String query){if(query!=null && query.length()>300)throw error(400,"INVALID_QUERY","La búsqueda admite hasta 300 caracteres.");return query==null?"":query.strip();}
    private void record(CurrentActorPort.Actor actor,UUID correlation,String action,UUID id,UUID career){audit.record(new AuditEventCommand(Instant.now(),actor.identityId(),null,correlation==null?UUID.randomUUID():correlation,action,career==null?"ACADEMIC_UNIT":"CAREER",id,career,null,"SUCCESS",null,Map.of()));}
    private static CareerCatalogException duplicate(){return error(409,"DUPLICATE_CODE","El código ya está registrado.");}
    private static CareerCatalogException hidden(){return error(404,"RESOURCE_NOT_FOUND","El recurso no existe o no está visible.");}
    private static CareerCatalogException error(int status,String code,String message){return new CareerCatalogException(status,code,message);}
}
