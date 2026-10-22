package ec.edu.unl.sec.selfassessment.domain;
import java.time.Instant;
import java.util.UUID;
public record AcademicUnit(UUID id, Data data, Career.Status status, long revision, UUID createdBy, Instant createdAt) {
    public AcademicUnit {
        if(id==null || data==null || status==null || revision<1 || createdBy==null || createdAt==null) throw new IllegalArgumentException("invalid unit");
        createdAt=createdAt.truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
    }
    public record Data(String code,String name) {
        public Data { code=Career.code(code); name=Career.text(name,300); }
    }
    public AcademicUnit replace(Data value) { return new AcademicUnit(id,value,status,revision+1,createdBy,createdAt); }
    public AcademicUnit state(Career.Status value) { return new AcademicUnit(id,data,value,revision+1,createdBy,createdAt); }
}
