package ec.edu.unl.sec.selfassessment.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record SelfAssessmentProcess(UUID id, UUID careerId, String code, String name,
        LocalDate periodStart, LocalDate periodEnd, UUID modelVersionId, int modelVersionNumber,
        ProcessState state, long revision, UUID createdBy, Instant createdAt,
        List<IndicatorSnapshot> indicators, ProcessAuthorization authorization) {
    public SelfAssessmentProcess {
        id = id == null ? UUID.randomUUID() : id;
        Objects.requireNonNull(careerId, "career required");
        Objects.requireNonNull(modelVersionId, "model version required");
        Objects.requireNonNull(createdBy, "creator required");
        code = text(code, 100);
        name = text(name, 300);
        if (periodStart == null || periodEnd == null || periodStart.isAfter(periodEnd))
            throw new IllegalArgumentException("invalid period");
        if (revision < 0 || modelVersionNumber < 1) throw new IllegalArgumentException("invalid revision");
        state = state == null ? ProcessState.PREPARATION : state;
        createdAt = createdAt == null ? Instant.now() : createdAt;
        indicators = indicators == null ? List.of() : List.copyOf(indicators);
    }

    public SelfAssessmentProcess(UUID id, UUID careerId, String code, String name,
            LocalDate start, LocalDate end, UUID model, int modelNumber, ProcessState state,
            long revision, UUID creator, Instant created, List<IndicatorSnapshot> indicators) {
        this(id, careerId, code, name, start, end, model, modelNumber, state, revision, creator, created, indicators, null);
    }

    public boolean inPreparation() { return state == ProcessState.PREPARATION || state == ProcessState.DRAFT; }

    public SelfAssessmentProcess authorize(ProcessAuthorization decision) {
        if (!inPreparation() || authorization != null) throw new IllegalStateException("AUTHORIZATION_ALREADY_FIXED");
        return new SelfAssessmentProcess(id, careerId, code, name, periodStart, periodEnd, modelVersionId,
                modelVersionNumber, state, revision + 1, createdBy, createdAt, indicators, Objects.requireNonNull(decision));
    }

    public SelfAssessmentProcess activate(List<IndicatorSnapshot> snapshot) {
        if (!inPreparation()) throw new IllegalStateException("INVALID_PROCESS_TRANSITION");
        if (authorization == null) throw new IllegalStateException("AUTHORIZATION_MISSING");
        if (snapshot.isEmpty()) throw new IllegalStateException("MODEL_HAS_NO_INDICATORS");
        return with(ProcessState.ACTIVE, revision + 1, snapshot);
    }

    public SelfAssessmentProcess advanceRevision() { return with(state, revision + 1, indicators); }

    public SelfAssessmentProcess transition(ProcessState target) {
        if (inPreparation() && target == ProcessState.ACTIVE) throw new IllegalStateException("ACTIVATION_REQUIRED");
        ProcessStateMachine.require(state, target);
        return with(target, revision + 1, indicators);
    }

    private SelfAssessmentProcess with(ProcessState target, long nextRevision, List<IndicatorSnapshot> snapshot) {
        return new SelfAssessmentProcess(id, careerId, code, name, periodStart, periodEnd, modelVersionId,
                modelVersionNumber, target, nextRevision, createdBy, createdAt, snapshot, authorization);
    }

    private static String text(String value, int max) {
        if (value == null || value.isBlank() || value.strip().length() > max) throw new IllegalArgumentException("invalid text");
        return value.strip();
    }

    public record IndicatorSnapshot(UUID id, String code, String name) {
        public IndicatorSnapshot { Objects.requireNonNull(id); code = text(code, 100); name = text(name, 300); }
    }
}
