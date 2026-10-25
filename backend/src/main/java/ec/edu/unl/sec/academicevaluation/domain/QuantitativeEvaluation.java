package ec.edu.unl.sec.academicevaluation.domain;

import ec.edu.unl.sec.evaluationmodel.domain.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public final class QuantitativeEvaluation {
    private QuantitativeEvaluation() {}
    public record Inputs(UUID id,UUID processId,UUID indicatorId,UUID modelVersionId,UUID ruleId,UUID capturedBy,
            Instant capturedAt,long revision,UUID predecessorId,LocalDate periodStart,LocalDate periodEnd,List<Variable> variables) {
        public Inputs {
            Objects.requireNonNull(id);Objects.requireNonNull(processId);Objects.requireNonNull(indicatorId);
            Objects.requireNonNull(modelVersionId);Objects.requireNonNull(ruleId);Objects.requireNonNull(capturedBy);
            capturedAt=Objects.requireNonNull(capturedAt).truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
            if (revision<1 || (revision==1)!=(predecessorId==null) || periodStart==null || periodEnd==null || periodStart.isAfter(periodEnd)) throw new IllegalArgumentException("Invalid input revision or period");
            variables=List.copyOf(variables);
            if (variables.size()!=2 || variables.stream().map(Variable::code).distinct().count()!=2) throw new IllegalArgumentException("Two distinct variables required");
        }
    }
    public record Variable(String code,@com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) BigDecimal value,String unit,List<ManualAcademicEvaluation.Support> supports) {
        public Variable {
            if (!Set.of("A","B").contains(code) || value==null || value.signum()<0 || unit==null || unit.isBlank()) throw new IllegalArgumentException("Invalid variable");
            supports=List.copyOf(supports);
            if (supports.isEmpty() || supports.size()>500 || supports.stream().distinct().count()!=supports.size()) throw new IllegalArgumentException("Distinct supports required");
        }
    }
    public record Verification(UUID inputSetId,UUID verifiedBy,Instant verifiedAt,String reason) {
        public Verification { Objects.requireNonNull(inputSetId);Objects.requireNonNull(verifiedBy);
            verifiedAt=Objects.requireNonNull(verifiedAt).truncatedTo(java.time.temporal.ChronoUnit.MILLIS);reason=text(reason); }
    }
    public record Run(UUID id,Inputs inputs,Verification verification,ValuationRule rule,RatingScale scale,
            @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING) BigDecimal result,ValuationThreshold threshold,UUID ratingOptionId,String ratingLabel,
            String arithmetic,UUID executedBy,Instant executedAt,String justification,String limitations) {
        public Run {
            Objects.requireNonNull(id);Objects.requireNonNull(inputs);Objects.requireNonNull(verification);
            Objects.requireNonNull(rule);Objects.requireNonNull(scale);Objects.requireNonNull(result);Objects.requireNonNull(threshold);
            Objects.requireNonNull(ratingOptionId);Objects.requireNonNull(executedBy);
            if (!verification.inputSetId().equals(inputs.id()) || !rule.id().equals(inputs.ruleId()) || !threshold.ratingOptionId().equals(ratingOptionId))
                throw new IllegalArgumentException("Run references mismatch");
            executedAt=Objects.requireNonNull(executedAt).truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
            justification=text(justification);limitations=text(limitations);
        }
    }
    private static String text(String value) {
        if(value==null || value.strip().length()<10 || value.strip().length()>4000) throw new IllegalArgumentException("Text must contain 10 to 4000 characters");
        return value.strip();
    }
}
