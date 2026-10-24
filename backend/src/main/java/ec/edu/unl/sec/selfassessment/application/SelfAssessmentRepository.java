package ec.edu.unl.sec.selfassessment.application;

import ec.edu.unl.sec.selfassessment.domain.ProcessActivity;
import ec.edu.unl.sec.selfassessment.domain.ProcessAssignment;
import ec.edu.unl.sec.selfassessment.domain.ProcessAuthorization;
import ec.edu.unl.sec.selfassessment.domain.ProcessState;
import ec.edu.unl.sec.selfassessment.domain.ProcessTeam;
import ec.edu.unl.sec.selfassessment.domain.SelfAssessmentProcess;
import ec.edu.unl.sec.selfassessment.domain.TeamMembership;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SelfAssessmentRepository {
    void save(SelfAssessmentProcess process, long expectedRevision);
    Optional<SelfAssessmentProcess> find(UUID id);
    default Optional<SelfAssessmentProcess> findForUpdate(UUID id) { throw new UnsupportedOperationException("Process locking required"); }
    List<SelfAssessmentProcess> list();
    void saveAssignment(ProcessAssignment assignment);
    List<ProcessAssignment> assignments(UUID processId);
    void saveTeam(ProcessTeam team);
    Optional<ProcessTeam> findTeam(UUID id);
    List<ProcessTeam> teams(UUID processId);
    void saveMembership(TeamMembership membership);
    List<TeamMembership> memberships(UUID teamId);
    void saveActivity(ProcessActivity activity, long expectedRevision);
    Optional<ProcessActivity> findActivity(UUID id);
    List<ProcessActivity> activities(UUID processId);
    void saveAuthorization(UUID processId, ProcessAuthorization authorization);
    void recordTransition(UUID processId, ProcessState from, ProcessState to, UUID actor, String reason,
            UUID correlation, long revision, ProcessAuthorization decision);

    default void recordTransition(UUID processId, ProcessState from, ProcessState to, UUID actor, String reason,
            UUID correlation, long revision) {
        recordTransition(processId, from, to, actor, reason, correlation, revision, null);
    }
}
