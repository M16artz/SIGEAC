package ec.edu.unl.sec.selfassessment.application;

import ec.edu.unl.sec.identityaccess.application.AuthorizationPort;
import ec.edu.unl.sec.identityaccess.application.CurrentActorPort;
import ec.edu.unl.sec.identityaccess.application.InstitutionalAuthorityPort;
import ec.edu.unl.sec.identityaccess.domain.AuthorizationScope;
import ec.edu.unl.sec.identityaccess.domain.OperationCode;
import ec.edu.unl.sec.selfassessment.domain.ProcessAuthorization;
import ec.edu.unl.sec.selfassessment.domain.ProcessState;
import ec.edu.unl.sec.selfassessment.domain.SelfAssessmentProcess;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessTransitionService {
    private final SelfAssessmentRepository repository;
    private final CurrentActorPort actors;
    private final AuthorizationPort auth;
    private final InstitutionalAuthorityPort authority;
    private final SelfAssessmentAuditService audit;

    @Autowired
    public ProcessTransitionService(SelfAssessmentRepository repository, CurrentActorPort actors,
            AuthorizationPort auth, InstitutionalAuthorityPort authority, SelfAssessmentAuditService audit) {
        this.repository = repository; this.actors = actors; this.auth = auth;
        this.authority = authority; this.audit = audit;
    }

    public ProcessTransitionService(SelfAssessmentRepository repository, CurrentActorPort actors,
            AuthorizationPort auth, SelfAssessmentAuditService audit) {
        this(repository, actors, auth, null, audit);
    }

    @Transactional
    public SelfAssessmentProcess transition(UUID id, long expected, ProcessState target, String reason, UUID correlation) {
        return transition(id, expected, target, null, reason, correlation);
    }

    @Transactional
    public SelfAssessmentProcess transition(UUID id, long expected, ProcessState target, Decision provenance,
            String reason, UUID correlation) {
        var actor = actors.currentActor().orElseThrow(() -> SelfAssessmentService.problem(
                ProcessProblemException.Kind.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "Se requiere autenticacion."));
        var process = repository.find(id).orElseThrow(ProcessTransitionService::hidden);
        var scope = new AuthorizationScope(process.careerId(), process.id(), null);
        if (actor.technical() || !auth.isAllowed(actor.identityId(), OperationCode.PROCESS_COORDINATE, scope)) throw hidden();
        SelfAssessmentService.check(process, expected);
        if (target == null) throw SelfAssessmentService.problem(ProcessProblemException.Kind.INVALID,
                "INVALID_PROCESS_TRANSITION", "Se requiere el estado de destino.");
        if (target == ProcessState.CLOSING || target == ProcessState.CLOSED || target == ProcessState.DOCUMENTARY_CLOSED)
            throw SelfAssessmentService.problem(ProcessProblemException.Kind.CONFLICT, "CLOSURE_POLICY_REQUIRED",
                    "El cierre requiere validar la politica y conservar su instantanea mediante el flujo de cierre.");
        if (reason == null || reason.strip().length() < 10 || reason.strip().length() > 2000)
            throw SelfAssessmentService.problem(ProcessProblemException.Kind.INVALID, "TRANSITION_REASON_REQUIRED",
                    "La transicion requiere un motivo de 10 a 2000 caracteres.");
        if (process.inPreparation() && target == ProcessState.ACTIVE)
            throw SelfAssessmentService.problem(ProcessProblemException.Kind.CONFLICT, "ACTIVATION_REQUIRED",
                    "Utilice el flujo de activacion con autorizacion institucional.");
        ProcessAuthorization decision = null;
        if (requiresAuthority(process.state(), target)) {
            if (provenance == null || provenance.authorityAssignmentId() == null)
                throw SelfAssessmentService.problem(ProcessProblemException.Kind.FORBIDDEN, "AUTHORITY_DECISION_REQUIRED",
                        "La decision requiere una asignacion autorizadora vigente, fundamento y constancia.");
            if (authority == null || !authority.isAuthorizedAssignment(provenance.authorityAssignmentId(),
                    actor.identityId(), OperationCode.PROCESS_AUTHORIZE, scope))
                throw SelfAssessmentService.problem(ProcessProblemException.Kind.FORBIDDEN, "PROCESS_AUTHORITY_DENIED",
                        "La asignacion indicada no autoriza esta decision.");
            decision = new ProcessAuthorization(null, provenance.authorityAssignmentId(), provenance.legalBasis(),
                    provenance.documentReference(), reason, actor.identityId(), Instant.now());
        }
        try {
            var changed = process.transition(target);
            repository.save(changed, expected);
            repository.recordTransition(id, process.state(), target, actor.identityId(), reason.strip(), correlation,
                    changed.revision(), decision);
            audit.record(actor.identityId(), correlation, "PROCESS_TRANSITIONED", changed, "SUCCESS", null);
            return changed;
        } catch (IllegalStateException exception) {
            throw SelfAssessmentService.problem(ProcessProblemException.Kind.CONFLICT, "INVALID_PROCESS_TRANSITION",
                    "La transicion solicitada no esta permitida.");
        }
    }

    private static boolean requiresAuthority(ProcessState from, ProcessState to) {
        if (to == ProcessState.REOPENED || to == ProcessState.SUSPENDED) return true;
        return to == ProcessState.ACTIVE && (from == ProcessState.SUSPENDED || from == ProcessState.REOPENED);
    }

    private static ProcessProblemException hidden() {
        return SelfAssessmentService.problem(ProcessProblemException.Kind.NOT_FOUND, "PROCESS_NOT_FOUND",
                "El proceso no existe o no esta visible.");
    }

    public record Decision(UUID authorityAssignmentId, String legalBasis, String documentReference) {
        public Decision {
            if (authorityAssignmentId == null || legalBasis == null || legalBasis.isBlank()
                    || documentReference == null || documentReference.isBlank())
                throw new IllegalArgumentException("authority decision provenance required");
        }
    }
}
