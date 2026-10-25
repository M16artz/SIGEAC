package ec.edu.unl.sec.academicevaluation.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Another evaluator's judgement is never superseded. */
public record ManualAcademicEvaluation(UUID id, UUID processId, UUID indicatorId, UUID modelVersionId,
        UUID ruleId, UUID scaleId, UUID ratingOptionId, String ratingLabel, String method,
        String limitations, String justification, List<Support> supports, UUID evaluatorId,
        Instant evaluatedAt, long revision, UUID predecessorId) {
    public ManualAcademicEvaluation {
        if (id == null || processId == null || indicatorId == null || modelVersionId == null || ruleId == null
                || scaleId == null || ratingOptionId == null || evaluatorId == null || evaluatedAt == null || revision < 1)
            throw new IllegalArgumentException("Evaluation identity required");
        ratingLabel = text(ratingLabel, 1, 200);
        method = text(method, 1, 20000);
        limitations = text(limitations, 10, 4000);
        justification = text(justification, 10, 4000);
        supports = List.copyOf(supports);
        if (supports.isEmpty() || supports.size() > 500 || supports.stream().distinct().count() != supports.size())
            throw new IllegalArgumentException("Distinct documentary supports required");
        evaluatedAt = evaluatedAt.truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
        if ((revision == 1) != (predecessorId == null)) throw new IllegalArgumentException("Invalid predecessor");
    }
    private static String text(String value, int min, int max) {
        if (value == null || value.strip().length() < min || value.strip().length() > max)
            throw new IllegalArgumentException("Invalid evaluation text");
        return value.strip();
    }
    public record Support(UUID evidenceVersionId, UUID requiredSourceId, UUID reviewDecisionId, String title) {
        public Support(UUID evidenceVersionId, UUID requiredSourceId, UUID reviewDecisionId) {
            this(evidenceVersionId, requiredSourceId, reviewDecisionId, evidenceVersionId.toString());
        }
        public Support {
            if (evidenceVersionId == null || requiredSourceId == null || reviewDecisionId == null)
                throw new IllegalArgumentException("Support references required");
            title = text(title, 1, 300);
        }
    }
}
