package ec.edu.unl.sec.reporting.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Counts sources after authorization, applicability and current-submission projection. */
public final class DocumentaryProgressCalculator {
    public static final String FORMULA_VERSION = "SOURCE_COVERAGE_V1";

    public Progress calculate(List<Source> sources, List<Coverage> currentCoverages) {
        var distinctSources = new HashMap<UUID, Source>();
        for (var source : sources) {
            var previous = distinctSources.putIfAbsent(source.id(), source);
            if (previous != null && !previous.equals(source)) {
                throw new IllegalArgumentException("Conflicting source definitions");
            }
        }
        var bySource = new HashMap<UUID, Map<UUID, Boolean>>();
        for (var coverage : currentCoverages) {
            if (!distinctSources.containsKey(coverage.sourceId())) {
                throw new IllegalArgumentException("Coverage outside the supplied source scope");
            }
            var byEvidence = bySource.computeIfAbsent(coverage.sourceId(), ignored -> new HashMap<>());
            var previous = byEvidence.putIfAbsent(coverage.evidenceId(), coverage.accepted());
            if (previous != null && previous != coverage.accepted()) {
                throw new IllegalArgumentException("Conflicting current review outcomes");
            }
        }

        int configured = 0;
        int required = 0;
        int registered = 0;
        int accepted = 0;
        for (var source : distinctSources.values()) {
            if (!source.required()) continue;
            configured++;
            if (!source.applicable()) continue;
            required++;
            var evidence = bySource.getOrDefault(source.id(), Map.of());
            if (!evidence.isEmpty()) registered++;
            if (evidence.values().stream().filter(Boolean::booleanValue).count() >= source.cardinalityMin()) accepted++;
        }
        var status = configured == 0 ? Status.NOT_CONFIGURED
                : required == 0 ? Status.NOT_APPLICABLE : Status.CONFIGURED;
        return new Progress(status, required, registered, accepted, FORMULA_VERSION);
    }

    /** Applicability includes both indicator and source exclusions already authorized by the caller. */
    public record Source(UUID id, boolean required, boolean applicable, int cardinalityMin) {
        public Source {
            if (id == null || cardinalityMin < 1) throw new IllegalArgumentException("Invalid required source");
        }
    }

    /** One coverage of the current submitted version; accepted comes from its current per-source decision. */
    public record Coverage(UUID sourceId, UUID evidenceId, boolean accepted) {
        public Coverage {
            if (sourceId == null || evidenceId == null) throw new IllegalArgumentException("Coverage identifiers required");
        }
    }

    public enum Status { CONFIGURED, NOT_CONFIGURED, NOT_APPLICABLE }

    public record Progress(Status status, int requiredSourceTotal, int registeredSourceTotal,
            int acceptedSourceTotal, String formulaVersion) {
        public BigDecimal collectionPercent() {
            return percent(registeredSourceTotal);
        }

        public BigDecimal documentReviewPercent() {
            return percent(acceptedSourceTotal);
        }

        private BigDecimal percent(int numerator) {
            return requiredSourceTotal == 0 ? null : BigDecimal.valueOf(numerator)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(requiredSourceTotal), 2, RoundingMode.HALF_UP);
        }
    }
}
