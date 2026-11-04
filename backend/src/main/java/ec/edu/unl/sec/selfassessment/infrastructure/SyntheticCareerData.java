package ec.edu.unl.sec.selfassessment.infrastructure;
import ec.edu.unl.sec.selfassessment.application.CareerCatalogRepository;
import ec.edu.unl.sec.selfassessment.domain.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.UUID;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
@Component @Order(0)
@ConditionalOnProperty(prefix="sec",name="demo-data",havingValue="true")
public class SyntheticCareerData implements ApplicationRunner {
    public static final UUID UNIT=UUID.fromString("80000000-0000-0000-0000-000000000001");
    public static final UUID CAREER=UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final CareerCatalogRepository repository;
    public SyntheticCareerData(CareerCatalogRepository repository){this.repository=repository;}
    @Transactional public void run(ApplicationArguments args){
        UUID actor=UUID.nameUUIDFromBytes("sec-unl:admin".getBytes(StandardCharsets.UTF_8));
        if(repository.findUnit(UNIT).isEmpty())repository.saveUnit(new AcademicUnit(UNIT,new AcademicUnit.Data("UNIT-SYN","Unidad sintética piloto"),Career.Status.ACTIVE,1,actor,Instant.now()),-1,actor,"SYNTHETIC_FIXTURE");
        var data=new Career.Data("CAREER-SYN","Carrera sintética piloto",UNIT,LocalDate.of(2020,1,1),null);
        var old=repository.findCareer(CAREER);
        if(old.isEmpty())repository.saveCareer(new Career(CAREER,data,Career.Status.ACTIVE,1,actor,Instant.now(),false),-1,actor,"SYNTHETIC_FIXTURE");
        else if(old.get().classificationPending() && old.get().data().code().equals("LEGACY-"+CAREER.toString().toUpperCase(java.util.Locale.ROOT))){
            var previous=old.get();var replaced=new Career(CAREER,data,Career.Status.ACTIVE,previous.revision()+1,previous.createdBy(),previous.createdAt(),false);
            repository.saveCareer(replaced,previous.revision(),actor,"SYNTHETIC_FIXTURE_RECONCILIATION");
        }
    }
}
