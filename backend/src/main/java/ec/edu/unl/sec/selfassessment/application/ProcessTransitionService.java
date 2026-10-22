package ec.edu.unl.sec.selfassessment.application;

import ec.edu.unl.sec.identityaccess.application.AuthorizationPort;
import ec.edu.unl.sec.identityaccess.application.CurrentActorPort;
import ec.edu.unl.sec.identityaccess.domain.AuthorizationScope;
import ec.edu.unl.sec.identityaccess.domain.OperationCode;
import ec.edu.unl.sec.selfassessment.domain.ProcessState;
import ec.edu.unl.sec.selfassessment.domain.SelfAssessmentProcess;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessTransitionService {
    private final SelfAssessmentRepository repository;
    private final CurrentActorPort actors;
    private final AuthorizationPort auth;
    private final SelfAssessmentAuditService audit;

    public ProcessTransitionService(SelfAssessmentRepository repository, CurrentActorPort actors,
            AuthorizationPort auth, SelfAssessmentAuditService audit) {
        this.repository = repository; this.actors = actors; this.auth = auth; this.audit = audit;
    }

    @Transactional
    public SelfAssessmentProcess transition(UUID id, long expected, ProcessState target, String reason, UUID correlation) {
        var actor = actors.currentActor().orElseThrow(() -> SelfAssessmentService.problem(
                ProcessProblemException.Kind.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "Se requiere autenticación."));
        var process = repository.find(id).orElseThrow(ProcessTransitionService::hidden);
        var scope = new AuthorizationScope(process.careerId(), process.id(), null);
        if (actor.technical() || !auth.isAllowed(actor.identityId(), OperationCode.PROCESS_COORDINATE, scope)) throw hidden();
        SelfAssessmentService.check(process, expected);
        if (target == null) throw SelfAssessmentService.problem(ProcessProblemException.Kind.INVALID,
                "INVALID_PROCESS_TRANSITION", "Se requiere el estado de destino.");
        if (target == ProcessState.CLOSING || target == ProcessState.CLOSED || target == ProcessState.DOCUMENTARY_CLOSED)
            throw SelfAssessmentService.problem(ProcessProblemException.Kind.CONFLICT, "CLOSURE_POLICY_REQUIRED",
                    "El cierre requiere validar la política y conservar su instantánea mediante el flujo de cierre.");
        if (reason == null || reason.strip().length() < 10 || reason.strip().length() > 2000)
            throw SelfAssessmentService.problem(ProcessProblemException.Kind.INVALID, "TRANSITION_REASON_REQUIRED",
                    "La transición requiere un motivo de 10 a 2000 caracteres.");
        if (process.inPreparation() && target == ProcessState.ACTIVE)
            throw SelfAssessmentService.problem(ProcessProblemException.Kind.CONFLICT, "ACTIVATION_REQUIRED",
                    "Utilice el flujo de activación con autorización institucional.");
        if (target == ProcessState.REOPENED && !auth.isAllowed(actor.identityId(), OperationCode.PROCESS_AUTHORIZE, scope))
            throw SelfAssessmentService.problem(ProcessProblemException.Kind.FORBIDDEN, "REOPEN_AUTHORITY_REQUIRED",
                    "La reapertura requiere competencia autorizadora explícita.");
        try {
            var changed = process.transition(target);
            repository.save(changed, expected);
            repository.recordTransition(id, process.state(), target, actor.identityId(), reason.strip(), correlation, changed.revision());
            audit.record(actor.identityId(), correlation, "PROCESS_TRANSITIONED", changed, "SUCCESS", null);
            return changed;
        } catch (IllegalStateException exception) {
            throw SelfAssessmentService.problem(ProcessProblemException.Kind.CONFLICT, "INVALID_PROCESS_TRANSITION",
                    "La transición solicitada no está permitida.");
        }
    }

    private static ProcessProblemException hidden() {
        return SelfAssessmentService.problem(ProcessProblemException.Kind.NOT_FOUND, "PROCESS_NOT_FOUND",
                "El proceso no existe o no está visible.");
    }
}
