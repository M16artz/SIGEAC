package ec.edu.unl.sec.academicevaluation.application;

import java.util.Set;
import java.util.UUID;

/** Bridge consumed by governance: which indicators support a consolidation and under which rule. */
public interface AcademicConsolidationPort {
    Set<UUID> evaluatedIndicatorIds(UUID processId);

    Set<UUID> pendingIndicatorIds(UUID processId);

    boolean allowsAggregation(UUID modelVersionId, UUID aggregationRuleId);
}
