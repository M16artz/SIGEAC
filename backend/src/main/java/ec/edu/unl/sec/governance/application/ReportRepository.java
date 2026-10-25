package ec.edu.unl.sec.governance.application;

import ec.edu.unl.sec.governance.domain.ApprovalDecision;
import ec.edu.unl.sec.governance.domain.ObservationReply;
import ec.edu.unl.sec.governance.domain.ReportObservation;
import ec.edu.unl.sec.governance.domain.ReportSocialization;
import ec.edu.unl.sec.governance.domain.ReportVersion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistence contract for the report version chain and its append-only workflow events. */
public interface ReportRepository {
    void appendVersion(ReportVersion version);

    Optional<ReportVersion> latest(UUID processId);

    List<ReportVersion> versions(UUID processId, long offset, long limit);

    long totalVersions(UUID processId);

    Optional<ReportVersion> findVersion(UUID versionId);

    boolean hasApprovedFinal(UUID processId);

    record Processing(String state, long revision) {}

    Optional<Processing> processing(UUID versionId);

    boolean advanceProcessing(UUID versionId, long expectedRevision, String nextState);

    void appendObservation(ReportObservation observation);

    void appendSocialization(ReportSocialization socialization);

    void appendReply(ObservationReply reply);

    void appendDecision(ApprovalDecision decision);

    Optional<ReportObservation> findObservation(UUID observationId);

    List<ReportObservation> observations(UUID versionId);

    List<ReportSocialization> socializations(UUID versionId);

    List<ObservationReply> repliesForVersion(UUID versionId);

    List<ApprovalDecision> decisions(UUID versionId);
}
