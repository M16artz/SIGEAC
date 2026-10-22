package ec.edu.unl.sec.selfassessment.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record ProcessActivity(UUID id, UUID processId, UUID indicatorId, UUID requiredSourceId,
        UUID assignmentId, String title, Instant dueAt, Status status, List<UUID> dependencyIds,
        String deadlineExceptionReason, UUID deadlineExceptionAuthorityAssignmentId,
        String deadlineExceptionLegalBasis, UUID createdBy, Instant createdAt, long revision) {
    public enum Status { PLANNED }

    public ProcessActivity {
        id = id == null ? UUID.randomUUID() : id;
        Objects.requireNonNull(processId, "process required");
        Objects.requireNonNull(indicatorId, "indicator required");
        Objects.requireNonNull(assignmentId, "assignment required");
        Objects.requireNonNull(createdBy, "creator required");
        title = text(title, 300);
        Objects.requireNonNull(dueAt, "due date required");
        status = status == null ? Status.PLANNED : status;
        dependencyIds = dependencyIds == null ? List.of() : List.copyOf(dependencyIds);
        if (dependencyIds.stream().anyMatch(id::equals))
            throw new IllegalArgumentException("activity cannot depend on itself");
        if (deadlineExceptionReason != null) deadlineExceptionReason = text(deadlineExceptionReason, 4000);
        if (deadlineExceptionLegalBasis != null) deadlineExceptionLegalBasis = text(deadlineExceptionLegalBasis, 4000);
        if (revision < 0) throw new IllegalArgumentException("invalid revision");
        createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    public boolean hasDeadlineException() {
        return deadlineExceptionReason != null && deadlineExceptionAuthorityAssignmentId != null
                && deadlineExceptionLegalBasis != null;
    }

    private static String text(String value, int max) {
        if (value == null || value.isBlank() || value.strip().length() > max)
            throw new IllegalArgumentException("invalid text");
        return value.strip();
    }
}
