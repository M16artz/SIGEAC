package ec.edu.unl.sec.selfassessment.application;
import ec.edu.unl.sec.selfassessment.domain.ProcessAssignment;
import java.time.Instant;
import java.util.UUID;
public interface ProcessAssignmentCatalogPort{
    boolean hasCurrentAssignment(UUID processId,UUID indicatorId,UUID identityId,String function,Instant at);
    boolean isCurrentParticipant(ProcessAssignment assignment, UUID identityId, Instant at);
}
