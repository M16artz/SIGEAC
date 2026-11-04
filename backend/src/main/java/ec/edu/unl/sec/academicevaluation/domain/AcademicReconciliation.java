package ec.edu.unl.sec.academicevaluation.domain;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.util.*;

public final class AcademicReconciliation {
    public static final String METHOD="SELECT_EXISTING_JUDGEMENT_V1";
    private AcademicReconciliation() {}
    public record Judgement(String kind,UUID id,UUID authorId,UUID modelVersionId,UUID ruleId,UUID scaleId,
            UUID ratingOptionId,String ratingLabel,String result,String method,String justification,String limitations,
            Instant recordedAt,List<ManualAcademicEvaluation.Support> supports,List<String> flags) {
        public Judgement {supports=List.copyOf(supports);flags=List.copyOf(flags);}
    }
    public record Policy(UUID id,UUID processId,UUID indicatorId,long revision,UUID predecessorId,String procedureReference,
            String method,String legalBasis,String documentReference,UUID authorityAssignmentId,UUID configuredBy,Instant configuredAt) {
        public Policy {
            Objects.requireNonNull(id);Objects.requireNonNull(processId);Objects.requireNonNull(indicatorId);
            Objects.requireNonNull(authorityAssignmentId);Objects.requireNonNull(configuredBy);
            if(revision<1 || (revision==1)!=(predecessorId==null) || !METHOD.equals(method)) throw new IllegalArgumentException("Invalid reconciliation policy");
            procedureReference=text(procedureReference,3,300);legalBasis=text(legalBasis,10,4000);documentReference=text(documentReference,3,1000);
            configuredAt=at(configuredAt);
        }
    }
    public record Disagreement(UUID id,UUID processId,UUID indicatorId,long revision,UUID predecessorId,String fingerprint,
            List<Judgement> judgements,String reason,UUID openedBy,Instant openedAt) {
        public Disagreement {
            Objects.requireNonNull(id);Objects.requireNonNull(processId);Objects.requireNonNull(indicatorId);Objects.requireNonNull(openedBy);
            judgements=List.copyOf(judgements);reason=text(reason,10,4000);openedAt=at(openedAt);
            if(revision<1 || (revision==1)!=(predecessorId==null) || judgements.size()<2
                    || judgements.stream().map(Judgement::authorId).distinct().count()<2
                    || judgements.stream().map(Judgement::id).distinct().count()!=judgements.size()
                    || !AcademicReconciliation.fingerprint(judgements).equals(fingerprint)) throw new IllegalArgumentException("Distinct judgements from two evaluators required");
        }
    }
    public record Resolution(UUID id,UUID processId,UUID indicatorId,UUID disagreementId,Policy policy,Judgement selectedJudgement,
            List<UUID> participants,String reason,UUID authorityAssignmentId,UUID resolvedBy,Instant resolvedAt,String fingerprint) {
        public Resolution {
            Objects.requireNonNull(id);Objects.requireNonNull(processId);Objects.requireNonNull(indicatorId);Objects.requireNonNull(disagreementId);
            Objects.requireNonNull(policy);Objects.requireNonNull(selectedJudgement);Objects.requireNonNull(authorityAssignmentId);Objects.requireNonNull(resolvedBy);
            participants=List.copyOf(participants);reason=text(reason,10,4000);resolvedAt=at(resolvedAt);
            if(!policy.processId().equals(processId) || !policy.indicatorId().equals(indicatorId) || !selectedJudgement.flags().isEmpty()
                    || participants.isEmpty() || participants.stream().anyMatch(Objects::isNull)
                    || participants.stream().distinct().count()!=participants.size() || !participants.contains(resolvedBy)
                    || fingerprint==null || !fingerprint.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("Invalid reconciliation resolution");
        }
    }
    public static String fingerprint(List<Judgement> judgements) {
        var value=judgements.stream().map(j->j.kind()+":"+j.id()).sorted().collect(java.util.stream.Collectors.joining("\n"));
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}
        catch(NoSuchAlgorithmException ex){throw new IllegalStateException(ex);}
    }
    private static String text(String value,int min,int max) {
        if(value==null || value.strip().length()<min || value.strip().length()>max) throw new IllegalArgumentException("Invalid reconciliation text");return value.strip();
    }
    private static Instant at(Instant value){return Objects.requireNonNull(value).truncatedTo(java.time.temporal.ChronoUnit.MILLIS);}
}
