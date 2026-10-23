package ec.edu.unl.sec.selfassessment.support;

import ec.edu.unl.sec.selfassessment.application.SelfAssessmentRepository;
import ec.edu.unl.sec.selfassessment.application.ProcessProblemException;
import ec.edu.unl.sec.selfassessment.domain.ProcessActivity;
import ec.edu.unl.sec.selfassessment.domain.ProcessAssignment;
import ec.edu.unl.sec.selfassessment.domain.ProcessAuthorization;
import ec.edu.unl.sec.selfassessment.domain.ProcessState;
import ec.edu.unl.sec.selfassessment.domain.ProcessTeam;
import ec.edu.unl.sec.selfassessment.domain.SelfAssessmentProcess;
import ec.edu.unl.sec.selfassessment.domain.TeamMembership;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InMemorySelfAssessmentRepository implements SelfAssessmentRepository {
    public final Map<UUID, SelfAssessmentProcess> processes = new LinkedHashMap<>();
    public final Map<UUID, ProcessTeam> teams = new LinkedHashMap<>();
    public final Map<UUID, TeamMembership> memberships = new LinkedHashMap<>();
    public final Map<UUID, ProcessActivity> activities = new LinkedHashMap<>();
    public final List<ProcessAssignment> assigned = new ArrayList<>();
    public final List<ProcessAuthorization> authorizations = new ArrayList<>();

    public void save(SelfAssessmentProcess process, long expected) {
        var old = processes.get(process.id());
        if (expected >= 0 && (old == null || old.revision() != expected))
            throw new ProcessProblemException(ProcessProblemException.Kind.PRECONDITION, "STALE_PROCESS", "Stale process");
        processes.put(process.id(), process);
    }

    public Optional<SelfAssessmentProcess> find(UUID id) { return Optional.ofNullable(processes.get(id)); }
    public List<SelfAssessmentProcess> list() { return List.copyOf(processes.values()); }
    public void saveAssignment(ProcessAssignment assignment) { assigned.add(assignment); }
    public List<ProcessAssignment> assignments(UUID id) {
        return assigned.stream().filter(a -> a.processId().equals(id)).toList();
    }

    public void saveTeam(ProcessTeam team) { teams.put(team.id(), team); }
    public Optional<ProcessTeam> findTeam(UUID id) { return Optional.ofNullable(teams.get(id)); }
    public List<ProcessTeam> teams(UUID processId) {
        return teams.values().stream().filter(t -> t.processId().equals(processId)).toList();
    }

    public void saveMembership(TeamMembership membership) { memberships.put(membership.id(), membership); }
    public List<TeamMembership> memberships(UUID teamId) {
        return memberships.values().stream().filter(m -> m.teamId().equals(teamId)).toList();
    }

    public void saveActivity(ProcessActivity activity, long expected) {
        var old = activities.get(activity.id());
        if (expected >= 0 && (old == null || old.revision() != expected))
            throw new ProcessProblemException(ProcessProblemException.Kind.PRECONDITION, "STALE_ACTIVITY", "Stale activity");
        activities.put(activity.id(), activity);
    }
    public Optional<ProcessActivity> findActivity(UUID id) { return Optional.ofNullable(activities.get(id)); }
    public List<ProcessActivity> activities(UUID processId) {
        return activities.values().stream().filter(a -> a.processId().equals(processId)).toList();
    }

    public void saveAuthorization(UUID processId, ProcessAuthorization authorization) {
        authorizations.add(authorization);
    }

    public void recordTransition(UUID processId, ProcessState from, ProcessState to, UUID actor, String reason,
            UUID correlation, long revision, ProcessAuthorization decision) {
        if (decision != null) authorizations.add(decision);
    }
}
