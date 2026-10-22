package ec.edu.unl.sec.selfassessment.application;
import ec.edu.unl.sec.selfassessment.domain.*;
import java.util.*;
public interface CareerCatalogRepository {
    Optional<AcademicUnit> findUnit(UUID id);
    Optional<Career> findCareer(UUID id);
    CatalogPage<AcademicUnit> units(Set<UUID> careerIds,int page,int size,String query);
    CatalogPage<Career> careers(Set<UUID> careerIds,int page,int size,String query,Career.Status status,UUID unitId);
    boolean unitCodeExists(String code,UUID except);
    boolean careerCodeExists(String code,UUID except);
    boolean hasActiveCareers(UUID unitId);
    void saveUnit(AcademicUnit unit,long expected,UUID actor,String reason);
    void saveCareer(Career career,long expected,UUID actor,String reason);
    CatalogPage<Career> careerHistory(UUID id,int page,int size);
}
