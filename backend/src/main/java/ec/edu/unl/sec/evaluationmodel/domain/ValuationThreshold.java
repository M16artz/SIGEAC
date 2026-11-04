package ec.edu.unl.sec.evaluationmodel.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record ValuationThreshold(UUID ratingOptionId, BigDecimal lowerBound, BigDecimal upperBound,
        boolean lowerInclusive, boolean upperInclusive) {
    public ValuationThreshold {
        if (ratingOptionId == null) throw new IllegalArgumentException("rating option required");
        if (lowerBound != null && upperBound != null) {
            int order = lowerBound.compareTo(upperBound);
            if (order > 0 || order == 0 && !(lowerInclusive && upperInclusive)) {
                throw new IllegalArgumentException("empty or reversed threshold");
            }
        }
    }

    public boolean overlaps(ValuationThreshold other) {
        return !endsBefore(this, other) && !endsBefore(other, this);
    }

    private static boolean endsBefore(ValuationThreshold left, ValuationThreshold right) {
        if (left.upperBound == null || right.lowerBound == null) return false;
        int order = left.upperBound.compareTo(right.lowerBound);
        return order < 0 || order == 0 && !(left.upperInclusive && right.lowerInclusive);
    }
}
