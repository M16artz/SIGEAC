package ec.edu.unl.sec.evaluationmodel.application;

import ec.edu.unl.sec.evaluationmodel.domain.ValuationRule;
import java.util.Set;

/** Only strategies registered and validated by the backend may enable automation. */
public interface ValidatedStrategyPort {
    Set<ValuationRule.StrategyReference> validatedStrategies();
}
