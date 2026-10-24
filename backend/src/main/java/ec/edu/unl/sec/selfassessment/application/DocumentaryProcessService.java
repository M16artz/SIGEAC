package ec.edu.unl.sec.selfassessment.application;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentaryProcessService implements DocumentaryProcessPort {
    private final SelfAssessmentRepository repository;
    public DocumentaryProcessService(SelfAssessmentRepository repository) { this.repository = repository; }

    @Override
    @Transactional(readOnly = true)
    public Optional<Context> context(UUID processId) {
        return repository.find(processId).map(process -> new Context(process.id(), process.careerId(),
                process.modelVersionId(), process.state().name(), process.revision(),
                process.indicators().stream().map(indicator -> indicator.id()).toList()));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<Context> lock(UUID processId) {
        return repository.findForUpdate(processId).map(process -> new Context(process.id(),process.careerId(),
                process.modelVersionId(),process.state().name(),process.revision(),
                process.indicators().stream().map(indicator -> indicator.id()).toList()));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<Context> completeClosure(UUID id,long expectedRevision,UUID actor,String reason,UUID correlation) {
        var process=repository.findForUpdate(id).orElse(null);
        if (process==null || process.revision()!=expectedRevision || !"ACTIVE".equals(process.state().name())) return Optional.empty();
        var closed=process.transition(ec.edu.unl.sec.selfassessment.domain.ProcessState.DOCUMENTARY_CLOSED);
        repository.save(closed,expectedRevision);
        repository.recordTransition(id,process.state(),closed.state(),actor,reason,correlation,closed.revision());
        return context(id);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean reserveSnapshot(UUID processId, long expectedRevision) {
        var process = repository.find(processId).orElse(null);
        if (process == null || process.revision() != expectedRevision || !"ACTIVE".equals(process.state().name())) return false;
        try {
            repository.save(process.advanceRevision(), expectedRevision);
            return true;
        } catch (ProcessProblemException exception) {
            if ("STALE_PROCESS".equals(exception.code())) return false;
            throw exception;
        }
    }
}
