package ec.edu.unl.sec.selfassessment.domain;

import java.util.Map;
import java.util.Set;

public final class ProcessStateMachine {
    private static final Map<ProcessState, Set<ProcessState>> ALLOWED = Map.of(
            ProcessState.DRAFT, Set.of(ProcessState.ACTIVE),
            ProcessState.PREPARATION, Set.of(ProcessState.ACTIVE),
            ProcessState.ACTIVE, Set.of(ProcessState.SUSPENDED, ProcessState.DOCUMENTARY_CLOSED),
            ProcessState.SUSPENDED, Set.of(ProcessState.ACTIVE),
            ProcessState.DOCUMENTARY_CLOSED, Set.of(ProcessState.CLOSING, ProcessState.REOPENED),
            ProcessState.CLOSING, Set.of(ProcessState.CLOSED, ProcessState.DOCUMENTARY_CLOSED),
            ProcessState.CLOSED, Set.of(ProcessState.REOPENED),
            ProcessState.REOPENED, Set.of(ProcessState.ACTIVE, ProcessState.DOCUMENTARY_CLOSED));

    private ProcessStateMachine() {}

    public static void require(ProcessState from, ProcessState to) {
        if (to == null || !ALLOWED.getOrDefault(from, Set.of()).contains(to))
            throw new IllegalStateException("INVALID_PROCESS_TRANSITION");
    }
}
