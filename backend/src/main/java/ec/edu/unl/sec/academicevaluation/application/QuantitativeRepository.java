package ec.edu.unl.sec.academicevaluation.application;
import ec.edu.unl.sec.academicevaluation.domain.QuantitativeEvaluation.*;
import java.util.*;

public interface QuantitativeRepository {
    Optional<Inputs> inputs(UUID id);
    Optional<Inputs> latest(UUID processId,UUID indicatorId,UUID capturedBy);
    Optional<Verification> verification(UUID inputSetId);
    Optional<Run> run(UUID inputSetId);
    List<Run> history(UUID processId,UUID indicatorId,long offset,int limit);
    List<Run> current(UUID processId,UUID indicatorId);
    void append(Inputs inputs);
    void verify(Verification verification);
    void append(Run run);
}
