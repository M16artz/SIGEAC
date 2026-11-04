package ec.edu.unl.sec.selfassessment.infrastructure;

import ec.edu.unl.sec.selfassessment.application.ProcessAssignmentCatalogPort;
import ec.edu.unl.sec.selfassessment.application.SelfAssessmentRepository;
import ec.edu.unl.sec.selfassessment.domain.ProcessAssignment;
import ec.edu.unl.sec.identityaccess.application.FunctionalAssignmentPolicyPort;
import ec.edu.unl.sec.identityaccess.domain.AuthorizationScope;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ProcessAssignmentCatalogAdapter implements ProcessAssignmentCatalogPort {
    private final SelfAssessmentRepository repository;
    private final FunctionalAssignmentPolicyPort policy;

    public ProcessAssignmentCatalogAdapter(SelfAssessmentRepository repository, FunctionalAssignmentPolicyPort policy) {
        this.repository = repository; this.policy = policy;
    }

    public boolean hasCurrentAssignment(UUID process, UUID indicator, UUID identity, String function, Instant at) {
        return repository.assignments(process).stream()
                .anyMatch(a -> a.indicatorId().equals(indicator) && a.function().name().equals(function)
                        && isCurrentParticipant(a, identity, at));
    }

    @Override
    public boolean isCurrentParticipant(ProcessAssignment assignment, UUID identity, Instant at) {
        if (assignment.revokedAt() != null || at.isBefore(assignment.validFrom()) || !at.isBefore(assignment.validTo()))
            return false;
        if (assignment.assignedTo(identity)) return true;
        if (assignment.teamId() == null) return false;
        var team = repository.findTeam(assignment.teamId()).orElse(null);
        if (team == null || !team.processId().equals(assignment.processId()) || !team.activeAt(at)) return false;
        var process = repository.find(assignment.processId()).orElse(null);
        if (process == null) return false;
        var scope = new AuthorizationScope(process.careerId(), process.id(), assignment.indicatorId());
        return repository.memberships(team.id()).stream()
                .filter(member -> member.userId().equals(identity) && member.activeAt(at))
                .anyMatch(member -> policy.evaluateMembership(identity, member.roleAssignmentId(), scope,
                        at, at.plusNanos(1)).allowed());
    }
}
