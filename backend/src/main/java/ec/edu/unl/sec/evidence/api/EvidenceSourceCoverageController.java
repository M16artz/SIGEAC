package ec.edu.unl.sec.evidence.api;

import ec.edu.unl.sec.evidence.application.ApplicabilityDecisionService;
import ec.edu.unl.sec.evidence.application.EvidenceSourceCoverageService;
import ec.edu.unl.sec.evidence.application.EvidenceSourceService;
import ec.edu.unl.sec.evidence.application.SubmitEvidenceService;
import ec.edu.unl.sec.evidence.domain.ApplicabilityDecision;
import ec.edu.unl.sec.evidence.domain.SourceCoverage;
import ec.edu.unl.sec.shared.api.EntityTagSupport;
import ec.edu.unl.sec.shared.infrastructure.CorrelationFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EvidenceSourceCoverageController {
    private final EvidenceSourceCoverageService coverage;
    private final ApplicabilityDecisionService applicability;
    private final SubmitEvidenceService submission;
    private final EvidenceSourceService sources;

    public EvidenceSourceCoverageController(EvidenceSourceCoverageService coverage,
            ApplicabilityDecisionService applicability, SubmitEvidenceService submission, EvidenceSourceService sources) {
        this.coverage = coverage;
        this.applicability = applicability;
        this.submission = submission;
        this.sources = sources;
    }

    @GetMapping("/evidence-versions/{versionId}/required-sources")
    public ResponseEntity<List<EvidenceSourceCoverageService.RequiredSourceState>> requiredSources(
            @PathVariable UUID versionId) {
        var sources = coverage.requiredSources(versionId);
        return ResponseEntity.ok().body(sources);
    }

    @GetMapping("/evidence-versions/{versionId}/source-coverages")
    public ResponseEntity<List<CoverageView>> coverages(@PathVariable UUID versionId) {
        return ResponseEntity.ok().body(coverage.coverages(versionId).stream().map(CoverageView::from).toList());
    }

    @PostMapping("/evidence-versions/{versionId}/source-coverages")
    public ResponseEntity<CoverageView> declare(
            @PathVariable UUID versionId,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody CoverageCommand request) {
        var declared = coverage.declare(versionId, ifMatch, request.requiredSourceId(), request.coverageNote());
        return ResponseEntity.created(URI.create("/api/v1/evidence-versions/" + versionId + "/source-coverages"))
                .eTag(EntityTagSupport.strong(declared.versionRevision()))
                .body(CoverageView.from(declared.value()));
    }

    @DeleteMapping("/evidence-versions/{versionId}/source-coverages/{coverageId}")
    public ResponseEntity<Void> remove(
            @PathVariable UUID versionId,
            @PathVariable UUID coverageId,
            @RequestHeader(name = "If-Match", required = false) String ifMatch) {
        coverage.remove(versionId, coverageId, ifMatch);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/evidence-versions/{versionId}/submit")
    public ResponseEntity<EvidenceView.Version> submit(
            @PathVariable UUID versionId,
            @RequestHeader(name = "If-Match", required = false) String ifMatch) {
        var version = submission.submit(versionId, ifMatch);
        return ResponseEntity.ok()
                .eTag(EntityTagSupport.strong(version.lockRevision()))
                .body(EvidenceView.version(version, sources.snapshot(versionId)));
    }

    @GetMapping("/processes/{processId}/applicability-decisions")
    public ResponseEntity<List<ExclusionView>> applicabilityHistory(@PathVariable UUID processId) {
        return ResponseEntity.ok().body(applicability.history(processId).stream()
                .map(ExclusionView::from).toList());
    }

    @PostMapping("/processes/{processId}/applicability-decisions")
    public ResponseEntity<ExclusionView> decide(
            @PathVariable UUID processId,
            @RequestHeader(name = "If-Match", required = false) String ifMatch,
            @Valid @RequestBody ExclusionCommand request) {
        var decided = applicability.decide(processId, new ApplicabilityDecisionService.ApplicabilityInput(
                request.subjectType(), request.subjectId(), request.decision(), request.ruleReference(),
                revisionOf(ifMatch), new ApplicabilityDecisionService.DecisionInput(
                        decision(request.provenance().authorityAssignmentId(),
                                "AUTHORITY_PROVENANCE_INCOMPLETE", "La procedencia requiere asignación autorizadora."),
                        required(request.provenance().legalBasis(), "AUTHORITY_PROVENANCE_INCOMPLETE",
                                "La procedencia requiere fundamento normativo."),
                        required(request.provenance().documentReference(), "AUTHORITY_PROVENANCE_INCOMPLETE",
                                "La procedencia requiere referencia documental."),
                        reason(request.provenance().reason(), "AUTHORITY_PROVENANCE_INCOMPLETE",
                                "La procedencia requiere justificación de al menos diez caracteres."))));
        return ResponseEntity.created(URI.create("/api/v1/processes/" + processId + "/applicability-decisions"))
                .eTag(EntityTagSupport.strong(decided.revision()))
                .body(ExclusionView.from(decided));
    }

    private long revisionOf(String ifMatch) {
        if (ifMatch == null || ifMatch.isBlank()) {
            throw new ec.edu.unl.sec.evidence.application.EvidenceProblemException(
                    ec.edu.unl.sec.evidence.application.EvidenceProblemException.Kind.PRECONDITION_REQUIRED,
                    "PRECONDITION_REQUIRED",
                    "Se requiere If-Match.");
        }
        if (!ifMatch.matches("\\\"[0-9]+\\\"")) {
            throw new ec.edu.unl.sec.evidence.application.EvidenceProblemException(
                    ec.edu.unl.sec.evidence.application.EvidenceProblemException.Kind.INVALID,
                    "INVALID_ETAG",
                    "If-Match no contiene una revisión válida.");
        }
        return Long.parseLong(ifMatch.substring(1, ifMatch.length() - 1));
    }

    private static UUID decision(UUID value, String code, String message) {
        if (value == null) {
            throw new ec.edu.unl.sec.evidence.application.EvidenceProblemException(
                    ec.edu.unl.sec.evidence.application.EvidenceProblemException.Kind.INVALID, code, message);
        }
        return value;
    }

    private static String required(String value, String code, String message) {
        if (value == null || value.isBlank()) {
            throw new ec.edu.unl.sec.evidence.application.EvidenceProblemException(
                    ec.edu.unl.sec.evidence.application.EvidenceProblemException.Kind.INVALID, code, message);
        }
        return value.strip();
    }

    private static String reason(String value, String code, String message) {
        var checked = required(value, code, message);
        if (checked.length() < 10) {
            throw new ec.edu.unl.sec.evidence.application.EvidenceProblemException(
                    ec.edu.unl.sec.evidence.application.EvidenceProblemException.Kind.INVALID, code, message);
        }
        return checked;
    }

    public record CoverageCommand(@NotNull UUID requiredSourceId,
            @NotBlank @Size(max = 4000) String coverageNote) {}

    public record ExclusionCommand(@NotNull ApplicabilityDecision.SubjectType subjectType,
            @NotNull UUID subjectId, @NotNull ApplicabilityDecision.Decision decision,
            @NotBlank @Size(max = 2000) String ruleReference,
            @Valid @NotNull Provenance provenance) {
        public record Provenance(UUID authorityAssignmentId, String legalBasis, String documentReference,
                String reason) {}
    }

    public record CoverageView(UUID id, UUID versionId, UUID requiredSourceId, String coverageNote,
            UUID declaredBy, Instant declaredAt) {
        static CoverageView from(SourceCoverage value) {
            return new CoverageView(value.id(), value.versionId(), value.requiredSourceId(), value.coverageNote(),
                    value.declaredBy(), value.declaredAt());
        }
    }

    public record ExclusionView(UUID id, UUID processId, String subjectType, UUID subjectId, String decision,
            String ruleReference, String justification, String legalBasis, String documentReference,
            UUID authorizedBy, Instant decidedAt, UUID predecessorId, long revision) {
        static ExclusionView from(ApplicabilityDecision value) {
            return new ExclusionView(value.id(), value.processId(), value.subjectType().name(),
                    value.subjectId(), value.decision().name(), value.ruleReference(), value.justification(),
                    value.legalBasis(), value.documentReference(), value.authorizedBy(), value.decidedAt(),
                    value.predecessorId(), value.revision());
        }
    }

    private static UUID correlation(HttpServletRequest request) {
        Object value = request.getAttribute(CorrelationFilter.CORRELATION_ATTRIBUTE);
        if (value == null) return UUID.randomUUID();
        try {
            return UUID.fromString(value.toString());
        } catch (IllegalArgumentException ignored) {
            return UUID.nameUUIDFromBytes(value.toString().getBytes(StandardCharsets.UTF_8));
        }
    }
}
