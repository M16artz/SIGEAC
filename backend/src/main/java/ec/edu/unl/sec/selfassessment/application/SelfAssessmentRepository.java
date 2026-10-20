package ec.edu.unl.sec.selfassessment.application;
import ec.edu.unl.sec.selfassessment.domain.*;import java.util.*;
public interface SelfAssessmentRepository{void save(SelfAssessmentProcess process,long expectedRevision);Optional<SelfAssessmentProcess> find(UUID id);List<SelfAssessmentProcess> list();void saveAssignment(ProcessAssignment assignment);List<ProcessAssignment> assignments(UUID processId);void recordTransition(UUID processId,ProcessState from,ProcessState to,UUID actor,String reason,UUID correlation,long revision);}
