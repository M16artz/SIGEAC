package ec.edu.unl.sec.selfassessment.application;
import java.time.Instant;import java.util.UUID;
public interface ProcessAssignmentCatalogPort{boolean hasCurrentAssignment(UUID processId,UUID indicatorId,UUID identityId,String function,Instant at);}
