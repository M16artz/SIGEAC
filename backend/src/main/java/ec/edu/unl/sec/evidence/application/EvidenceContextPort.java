package ec.edu.unl.sec.evidence.application;
import java.time.Instant;import java.util.*;
public interface EvidenceContextPort{Optional<ProcessContext> process(UUID processId);Optional<ProcessContext> processForWrite(UUID processId);boolean hasCurrentResponsibleAssignment(UUID processId,UUID indicatorId,UUID actorId,Instant at);record ProcessContext(UUID id,UUID careerId,UUID fixedModelVersionId,int modelVersionNumber,String state,List<Indicator> indicators){public ProcessContext{indicators=List.copyOf(indicators);}}record Indicator(UUID id,String code,String title){}}
