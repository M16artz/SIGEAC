package ec.edu.unl.sec.improvement.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * A measurable improvement action derived from a weakness. The head carries the current state and
 * revision; every revision, progress entry and decision is appended separately and never edited.
 */
public record ImprovementAction(UUID id, UUID processId, Data data, State state, UUID createdBy,
        Instant createdAt, long revision) {
    public ImprovementAction {
        if (id == null || processId == null || data == null || state == null || createdBy == null
                || createdAt == null || revision < 1)
            throw new IllegalArgumentException("improvement action identity is required");
        createdAt = createdAt.truncatedTo(ChronoUnit.MILLIS);
    }

    public enum State { PROPOSED, APPROVED, IN_PROGRESS, PENDING_VERIFICATION, VERIFIED }

    public enum Transition { APPROVE, RETURN, START, SUBMIT_VERIFICATION, VERIFY, REQUEST_CORRECTION }

    public record Data(UUID planId, List<UUID> findingIds, String title, String description,
            UUID responsibleAssignmentId, Instant dueAt, String target, String verificationMethod) {
        public Data {
            Objects.requireNonNull(planId, "planId");
            Objects.requireNonNull(responsibleAssignmentId, "responsibleAssignmentId");
            Objects.requireNonNull(dueAt, "dueAt");
            if (findingIds == null || findingIds.isEmpty() || findingIds.stream().anyMatch(Objects::isNull))
                throw new IllegalArgumentException("at least one finding is required");
            if (findingIds.stream().distinct().count() != findingIds.size())
                throw new IllegalArgumentException("distinct findings are required");
            findingIds = List.copyOf(findingIds);
            dueAt = dueAt.truncatedTo(ChronoUnit.MILLIS);
            title = text(title, 1, 300, "action title is required");
            description = text(description, 10, 4000, "action description out of bounds");
            target = text(target, 1, 4000, "measurable target is required");
            verificationMethod = text(verificationMethod, 1, 4000, "verification method is required");
        }
    }

    public boolean overdue(Instant now) {
        Objects.requireNonNull(now, "now");
        return state != State.VERIFIED && data.dueAt().isBefore(now);
    }

    /** Pure lifecycle step; the service maps illegal transitions to a conflict. */
    public State advance(Transition transition) {
        Objects.requireNonNull(transition, "transition");
        return switch (transition) {
            case APPROVE -> require(state == State.PROPOSED, transition, State.APPROVED);
            case RETURN -> require(state != State.VERIFIED, transition, State.PROPOSED);
            case START -> require(state == State.APPROVED, transition, State.IN_PROGRESS);
            case SUBMIT_VERIFICATION -> require(state == State.IN_PROGRESS, transition, State.PENDING_VERIFICATION);
            case VERIFY -> require(state == State.PENDING_VERIFICATION, transition, State.VERIFIED);
            case REQUEST_CORRECTION -> require(state == State.PENDING_VERIFICATION, transition, State.IN_PROGRESS);
        };
    }

    private State require(boolean allowed, Transition transition, State next) {
        if (!allowed) throw new IllegalStateException("transition " + transition + " is not allowed from " + state);
        return next;
    }

    private static String text(String value, int min, int max, String message) {
        if (value == null) throw new IllegalArgumentException(message);
        String stripped = value.strip();
        if (stripped.length() < min || stripped.length() > max) throw new IllegalArgumentException(message);
        return stripped;
    }
}
