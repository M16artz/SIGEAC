package ec.edu.unl.sec.documentreview.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Review result of one required source inside a documentary review decision.
 * Outcomes never duplicate evidence status; they project review decisions per
 * coverage so the pending documentary work stays explicit (AC-X06).
 */
public record ReviewCoverageOutcome(UUID requiredSourceId, Outcome outcome, String note) {
    public enum Outcome { ACCEPTED, OBSERVED, REJECTED }

    public ReviewCoverageOutcome {
        Objects.requireNonNull(requiredSourceId, "required source required");
        Objects.requireNonNull(outcome, "outcome required");
        note = text(note, outcome);
    }

    /**
     * Foundation is mandatory for observations and rejections; acceptance may
     * confirm the coverage with a short note (FR-022).
     */
    private static String text(String value, Outcome outcome) {
        if (value == null || value.isBlank()) {
            if (outcome == Outcome.ACCEPTED) return null;
            throw new IllegalArgumentException("outcome note required");
        }
        String stripped = value.strip();
        if (outcome != Outcome.ACCEPTED && stripped.length() < 10)
            throw new IllegalArgumentException("outcome note must contain at least 10 characters");
        if (stripped.length() > 4000)
            throw new IllegalArgumentException("outcome note too long");
        return stripped;
    }
}
