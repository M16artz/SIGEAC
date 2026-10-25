package ec.edu.unl.sec.academicevaluation.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.unl.sec.academicevaluation.application.AcademicConsolidationPort;
import ec.edu.unl.sec.selfassessment.application.DocumentaryProcessPort;
import ec.edu.unl.sec.shared.infrastructure.JdbcValueSupport;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Bridges academic calculation progress for P13 consolidations: an indicator is evaluated when
 * the last calculation per author still exists, and aggregation requires a rule validated for
 * the model version that sustains the process iteration.
 */
@Component
public class AcademicConsolidationAdapter implements AcademicConsolidationPort {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final DocumentaryProcessPort processes;

    public AcademicConsolidationAdapter(JdbcTemplate jdbc, ObjectMapper json, DocumentaryProcessPort processes) {
        this.jdbc = jdbc;
        this.json = json;
        this.processes = processes;
    }

    @Override
    public Set<UUID> evaluatedIndicatorIds(UUID processId) {
        return new HashSet<>(jdbc.query("select indicator_id from quantitative_calculation where process_id=?"
                + " group by indicator_id", (r, i) -> uuid(r, 1), processId));
    }

    @Override
    public Set<UUID> pendingIndicatorIds(UUID processId) {
        var evaluated = evaluatedIndicatorIds(processId);
        var declared = processes.context(processId).map(DocumentaryProcessPort.Context::indicatorIds)
                .orElse(List.of());
        return declared.stream().filter(indicator -> !evaluated.contains(indicator))
                .collect(java.util.stream.Collectors.toSet());
    }

    @Override
    public boolean allowsAggregation(UUID modelVersionId, UUID aggregationRuleId) {
        var total = jdbc.queryForObject("select count(*) from valuation_rule where id=? and version_id=?"
                + " and status='VALIDATED'", Long.class, aggregationRuleId, modelVersionId);
        return total != null && total > 0;
    }

    private static UUID uuid(ResultSet r, int column) {
        try {
            return JdbcValueSupport.uuid(r, column);
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot read academic consolidation metadata", e);
        }
    }
}
