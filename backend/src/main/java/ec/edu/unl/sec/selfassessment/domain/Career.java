package ec.edu.unl.sec.selfassessment.domain;
import java.time.*;
import java.util.*;
public record Career(UUID id, Data data, Status status, long revision, UUID createdBy, Instant createdAt, boolean classificationPending) {
    public enum Status { ACTIVE, DISABLED }
    public Career {
        if(id==null || data==null || status==null || revision<1 || createdBy==null || createdAt==null) throw new IllegalArgumentException("invalid career");
        createdAt=createdAt.truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
    }
    public record Data(String code,String name,UUID academicUnitId,LocalDate validFrom,LocalDate validUntil) {
        public Data {
            code=Career.code(code); name=text(name,300);
            if(academicUnitId==null || validFrom==null || (validUntil!=null && validUntil.isBefore(validFrom))) throw new IllegalArgumentException("invalid career validity or unit");
        }
    }
    public Career replace(Data value) {return new Career(id,value,status,revision+1,createdBy,createdAt,false);}
    public Career state(Status value) {return new Career(id,data,value,revision+1,createdBy,createdAt,classificationPending);}
    public boolean activeOn(LocalDate date) {return status==Status.ACTIVE && !classificationPending && !date.isBefore(data.validFrom()) && (data.validUntil()==null || !date.isAfter(data.validUntil()));}
    static String code(String value) {return text(value,100).replaceAll("\\s+"," ").toUpperCase(Locale.ROOT);}
    static String text(String value,int max) {
        if(value==null || value.isBlank() || value.strip().length()>max) throw new IllegalArgumentException("invalid catalog text");
        return value.strip();
    }
}
