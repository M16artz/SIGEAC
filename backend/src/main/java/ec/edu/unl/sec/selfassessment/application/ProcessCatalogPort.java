package ec.edu.unl.sec.selfassessment.application;
import ec.edu.unl.sec.selfassessment.domain.ProcessState;import java.util.*;
public interface ProcessCatalogPort{Optional<ProcessView> process(UUID id);record ProcessView(UUID id,UUID careerId,UUID modelVersionId,int modelVersionNumber,ProcessState state,List<Indicator> indicators){}record Indicator(UUID id,String code,String name){}}
