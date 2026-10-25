package ec.edu.unl.sec.archive.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Versioned integral closure policy. Strict closure requires the documentary phase to be closed,
 * an approved final report and no overdue actions unless an authorized exception waives them.
 */
public record IntegralClosurePolicy(String code, boolean strict, boolean requireApprovedFinalReport,
        boolean blockOverdueActions) {
    public static final String INTEGRAL_STRICT_V1 = "INTEGRAL_STRICT_V1";
    public static final IntegralClosurePolicy STRICT =
            new IntegralClosurePolicy(INTEGRAL_STRICT_V1, true, true, true);

    public IntegralClosurePolicy {
        Objects.requireNonNull(code, "code");
        if (code.isBlank()) throw new IllegalArgumentException("policy code is required");
    }

    public static IntegralClosurePolicy of(String code) {
        if (!INTEGRAL_STRICT_V1.equals(code)) {
            throw new IllegalArgumentException("unsupported integral closure policy");
        }
        return STRICT;
    }

    public record ActionSnapshot(UUID id, String title, String state, Instant dueAt) {
        public ActionSnapshot {
            Objects.requireNonNull(id, "action id");
            Objects.requireNonNull(state, "action state");
        }

        public boolean overdue(Instant now) {
            return !"VERIFIED".equals(state) && dueAt != null && dueAt.isBefore(now);
        }
    }

    public record Evidence(String processState, UUID documentaryClosureId, String finalReportState,
            List<ActionSnapshot> actions, Instant now, Set<String> exceptedCodes) {
        public Evidence {
            actions = actions == null ? List.of() : List.copyOf(actions);
            exceptedCodes = exceptedCodes == null ? Set.of() : Set.copyOf(exceptedCodes);
            Objects.requireNonNull(now, "now");
        }
    }

    public record Blocker(String code, String message, UUID subjectId) {
        public Blocker {
            Objects.requireNonNull(code, "code");
            Objects.requireNonNull(message, "message");
        }
    }

    public record Evaluation(List<Blocker> blockers, List<String> warnings, List<UUID> pendingIds) {
        public Evaluation {
            blockers = List.copyOf(blockers);
            warnings = List.copyOf(warnings);
            pendingIds = List.copyOf(pendingIds);
        }

        public boolean eligible() {
            return blockers.isEmpty();
        }
    }

    public Evaluation evaluate(Evidence evidence) {
        Objects.requireNonNull(evidence, "evidence");
        var blockers = new ArrayList<Blocker>();
        var warnings = new ArrayList<String>();
        if (!"DOCUMENTARY_CLOSED".equals(evidence.processState())) {
            blockers.add(new Blocker("PROCESS_NOT_DOCUMENTARY_CLOSED",
                    "El cierre integral requiere un proceso en cierre documental.", null));
        }
        if (evidence.documentaryClosureId() == null) {
            blockers.add(new Blocker("DOCUMENTARY_CLOSURE_MISSING",
                    "Falta el cierre documental con su instantánea conservada.", null));
        }
        if (requireApprovedFinalReport) {
            if (evidence.finalReportState() == null) {
                blockers.add(new Blocker("FINAL_REPORT_MISSING",
                        "Falta una versión de informe final emitida.", null));
            } else if (!"APPROVED".equals(evidence.finalReportState())) {
                blockers.add(new Blocker("FINAL_REPORT_NOT_APPROVED",
                        "El informe final aún no está aprobado.", null));
            }
        }
        var overdue = evidence.actions().stream().filter(action -> action.overdue(evidence.now())).toList();
        boolean excepted = !evidence.exceptedCodes().isEmpty();
        if (blockOverdueActions && !excepted) {
            for (var action : overdue) {
                blockers.add(new Blocker("OVERDUE_ACTION",
                        "Acción vencida sin verificación: " + action.title(), action.id()));
            }
        } else if (excepted && !overdue.isEmpty()) {
            warnings.add("Se aceptaron excepciones autorizadas para acciones vencidas sin verificación.");
        }
        boolean open = evidence.actions().stream().anyMatch(action -> !"VERIFIED".equals(action.state()));
        if (open && overdue.isEmpty()) {
            warnings.add("Las acciones abiertas continúan su seguimiento después del cierre integral.");
        }
        var pending = new LinkedHashSet<UUID>();
        for (var blocker : blockers) {
            if (blocker.subjectId() != null) pending.add(blocker.subjectId());
        }
        return new Evaluation(blockers, warnings, List.copyOf(pending));
    }
}
