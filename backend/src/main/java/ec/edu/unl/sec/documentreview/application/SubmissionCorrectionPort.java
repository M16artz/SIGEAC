package ec.edu.unl.sec.documentreview.application;

import java.util.UUID;

/** Creates the correction successor of a submitted evidence version (P10). */
public interface SubmissionCorrectionPort {

    /** Builds the successor draft and links it to the triggering review decision. */
    Successor correct(UUID correctedVersionId, UUID triggeringDecisionId);

    record Successor(UUID versionId, UUID evidenceId, int revisionNumber) {}
}
