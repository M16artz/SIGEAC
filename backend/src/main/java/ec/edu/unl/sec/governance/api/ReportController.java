package ec.edu.unl.sec.governance.api;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import ec.edu.unl.sec.audit.application.AuditEventCommand;
import ec.edu.unl.sec.audit.application.FailureAuditPort;
import ec.edu.unl.sec.governance.application.FindingService;
import ec.edu.unl.sec.governance.application.GovernanceProblemException;
import ec.edu.unl.sec.governance.application.ReportWorkflowService;
import ec.edu.unl.sec.governance.domain.ApprovalDecision;
import ec.edu.unl.sec.governance.domain.Consolidation;
import ec.edu.unl.sec.governance.domain.Finding;
import ec.edu.unl.sec.governance.domain.ObservationReply;
import ec.edu.unl.sec.governance.domain.ReportObservation;
import ec.edu.unl.sec.governance.domain.ReportSocialization;
import ec.edu.unl.sec.governance.domain.ReportVersion;
import ec.edu.unl.sec.identityaccess.application.CurrentActorPort;
import ec.edu.unl.sec.shared.api.EntityTagSupport;
import ec.edu.unl.sec.shared.infrastructure.CorrelationFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
public class ReportController {
    private final FindingService findings;
    private final ReportWorkflowService reports;
    private final FailureAuditPort failures;
    private final CurrentActorPort actors;

    public ReportController(FindingService findings, ReportWorkflowService reports,
            FailureAuditPort failures, CurrentActorPort actors) {
        this.findings = findings;
        this.reports = reports;
        this.failures = failures;
        this.actors = actors;
    }

    @GetMapping("/processes/{processId}/findings")
    public ResponseEntity<FindingPage> view(@PathVariable UUID processId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        var result = findings.findings(processId, (long) page * size, size);
        var items = result.items().stream()
                .map(f -> new FindingResponse(f.id(), f.version(), f.createdAt(), f.createdBy(), f.data())).toList();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .eTag(EntityTagSupport.strong(result.totalElements()))
                .body(new FindingPage(items, page, size, result.totalElements()));
    }

    @PostMapping("/processes/{processId}/findings")
    public ResponseEntity<FindingResponse> register(@PathVariable UUID processId,
            @RequestHeader(name = "If-Match", required = false) String etag,
            @Valid @RequestBody FindRequest command, HttpServletRequest request) {
        UUID correlation = correlation(request);
        Finding result;
        try {
            EntityTagSupport.require(etag);
            result = findings.register(processId, new FindingService.RegisterCommand(command.type(),
                    command.title(), command.description(), command.indicatorIds(),
                    orEmpty(command.evaluationIds()), orEmpty(command.evidenceVersionIds())), correlation);
        } catch (GovernanceProblemException rejected) {
            rejection("FINDING_REGISTRATION_REJECTED", processId, correlation, rejected.code());
            throw rejected;
        }
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
                .eTag(EntityTagSupport.strong(result.version()))
                .body(new FindingResponse(result.id(), result.version(), result.createdAt(), result.createdBy(),
                        result.data()));
    }

    @PostMapping("/processes/{processId}/consolidations")
    public ResponseEntity<ConsolidationResponse> consolidate(@PathVariable UUID processId,
            @RequestHeader(name = "If-Match", required = false) String etag,
            @Valid @RequestBody ConsolidationRequest command, HttpServletRequest request) {
        UUID correlation = correlation(request);
        Consolidation result;
        try {
            result = findings.consolidate(processId, EntityTagSupport.require(etag),
                    new FindingService.ConsolidateCommand(command.findingIds(), command.aggregationRuleId(),
                            command.reason()), correlation);
        } catch (GovernanceProblemException rejected) {
            rejection("REPORT_CONSOLIDATION_REJECTED", processId, correlation, rejected.code());
            throw rejected;
        } catch (ConcurrencyFailureException stale) {
            rejection("REPORT_CONSOLIDATION_REJECTED", processId, correlation, "STALE_WORKFLOW");
            throw stale;
        }
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
                .eTag(EntityTagSupport.strong(result.revision()))
                .body(new ConsolidationResponse(result.id(), result.revision(), result.createdAt(), result.createdBy(),
                        result.data()));
    }

    @GetMapping("/processes/{processId}/report-versions")
    public ResponseEntity<VersionPage> listVersions(@PathVariable UUID processId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        var result = reports.listVersions(processId, (long) page * size, size);
        var items = result.items().stream().map(v -> new Item(v.id(), reports.processingRevision(v.id()),
                v.createdAt(), v.createdBy(), v.data(), null)).toList();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .eTag(EntityTagSupport.strong(result.revision()))
                .body(new VersionPage(items, page, size, result.totalElements()));
    }

    @PostMapping("/processes/{processId}/report-versions")
    public ResponseEntity<VersionView> issue(@PathVariable UUID processId,
            @RequestHeader(name = "If-Match", required = false) String etag,
            @Valid @RequestBody IssueRequest command, HttpServletRequest request) {
        UUID correlation = correlation(request);
        ReportWorkflowService.ReportVersionDetail detail;
        try {
            var version = reports.create(processId, EntityTagSupport.require(etag),
                    new ReportWorkflowService.ReportCommand(command.kind(), command.consolidationId(),
                            command.templateReference(), command.templateValidated(), command.content(),
                            command.predecessorVersionId()), correlation);
            detail = reports.viewVersion(processId, version.id());
        } catch (GovernanceProblemException rejected) {
            rejection("REPORT_EMISSION_REJECTED", processId, correlation, rejected.code());
            throw rejected;
        } catch (ConcurrencyFailureException stale) {
            rejection("REPORT_EMISSION_REJECTED", processId, correlation, "STALE_WORKFLOW");
            throw stale;
        }
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
                .eTag(EntityTagSupport.strong(detail.revision())).body(view(detail));
    }

    @GetMapping("/report-versions/{reportVersionId}")
    public ResponseEntity<VersionView> viewVersion(@PathVariable UUID reportVersionId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .eTag(EntityTagSupport.strong(reports.viewVersion(reportVersionId).revision()))
                .body(view(reports.viewVersion(reportVersionId)));
    }

    @PostMapping("/report-versions/{reportVersionId}/socializations")
    public ResponseEntity<Item> socialize(@PathVariable UUID reportVersionId,
            @RequestHeader(name = "If-Match", required = false) String etag,
            @Valid @RequestBody SocializationRequest command, HttpServletRequest request) {
        UUID correlation = correlation(request);
        long expected = EntityTagSupport.require(etag);
        ReportSocialization result;
        try {
            result = reports.socialize(reportVersionId, expected, new ReportWorkflowService.SocializationCommand(
                    command.occurredAt(), command.participantAssignmentIds(), command.recordReference()), correlation);
        } catch (GovernanceProblemException rejected) {
            rejection("REPORT_SOCIALIZATION_REJECTED", reportVersionId, correlation, rejected.code());
            throw rejected;
        } catch (ConcurrencyFailureException stale) {
            rejection("REPORT_SOCIALIZATION_REJECTED", reportVersionId, correlation, "STALE_WORKFLOW");
            throw stale;
        }
        return eventResult(result.id(), expected, new SocializationData(command.occurredAt(),
                command.participantAssignmentIds(), command.recordReference()));
    }

    @PostMapping("/report-versions/{reportVersionId}/observations")
    public ResponseEntity<Item> observe(@PathVariable UUID reportVersionId,
            @RequestHeader(name = "If-Match", required = false) String etag,
            @Valid @RequestBody ObservationRequest command, HttpServletRequest request) {
        UUID correlation = correlation(request);
        long expected = EntityTagSupport.require(etag);
        ReportObservation result;
        try {
            result = reports.observe(reportVersionId, expected, new ReportWorkflowService.ObservationCommand(
                    command.sectionReference(), command.text()), correlation);
        } catch (GovernanceProblemException rejected) {
            rejection("REPORT_OBSERVATION_REJECTED", reportVersionId, correlation, rejected.code());
            throw rejected;
        } catch (ConcurrencyFailureException stale) {
            rejection("REPORT_OBSERVATION_REJECTED", reportVersionId, correlation, "STALE_WORKFLOW");
            throw stale;
        }
        return eventResult(result.id(), expected, new ObservationData(command.sectionReference(), command.text()));
    }

    @PostMapping("/report-observations/{observationId}/responses")
    public ResponseEntity<Item> reply(@PathVariable UUID observationId,
            @RequestHeader(name = "If-Match", required = false) String etag,
            @Valid @RequestBody ReplyRequest command, HttpServletRequest request) {
        UUID correlation = correlation(request);
        long expected = EntityTagSupport.require(etag);
        ObservationReply result;
        try {
            result = reports.reply(observationId, expected, new ReportWorkflowService.ReplyCommand(
                    command.response(), command.correctedVersionId()), correlation);
        } catch (GovernanceProblemException rejected) {
            rejection("REPORT_OBSERVATION_REPLY_REJECTED", null, correlation, rejected.code());
            throw rejected;
        } catch (ConcurrencyFailureException stale) {
            rejection("REPORT_OBSERVATION_REPLY_REJECTED", null, correlation, "STALE_WORKFLOW");
            throw stale;
        }
        return eventResult(result.id(), expected, new ReplyData(command.response(), command.correctedVersionId()));
    }

    @PostMapping("/report-versions/{reportVersionId}/approval-decisions")
    public ResponseEntity<Item> decide(@PathVariable UUID reportVersionId,
            @RequestHeader(name = "If-Match", required = false) String etag,
            @Valid @RequestBody ApprovalRequest command, HttpServletRequest request) {
        UUID correlation = correlation(request);
        long expected = EntityTagSupport.require(etag);
        ApprovalDecision result;
        try {
            result = reports.decide(reportVersionId, expected, new ReportWorkflowService.ApprovalDecisionCommand(
                    command.decision(), new ReportWorkflowService.Provenance(
                            command.provenance().authorityAssignmentId(), command.provenance().legalBasis(),
                            command.provenance().documentReference(), command.provenance().reason())), correlation);
        } catch (GovernanceProblemException rejected) {
            rejection("REPORT_DECISION_REJECTED", reportVersionId, correlation, rejected.code());
            throw rejected;
        } catch (ConcurrencyFailureException stale) {
            rejection("REPORT_DECISION_REJECTED", reportVersionId, correlation, "STALE_WORKFLOW");
            throw stale;
        }
        return eventResult(result.id(), expected, new ApprovalData(command.decision(),
                new ApprovalData.Provenance(command.provenance().authorityAssignmentId(),
                        command.provenance().legalBasis(), command.provenance().documentReference(),
                        command.provenance().reason())));
    }

    private VersionView view(ReportWorkflowService.ReportVersionDetail detail) {
        var version = detail.version();
        return new VersionView(version.id(), detail.revision(), version.createdAt(), version.createdBy(),
                version.data(), detail.state(), detail.socializations(), detail.observations(),
                detail.replies(), detail.decisions());
    }

    private ResponseEntity<Item> eventResult(UUID id, long expectedRevision, Object data) {
        var body = new Item(id, expectedRevision + 1, null, actor().orElse(null), data, null);
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
                .eTag(EntityTagSupport.strong(expectedRevision + 1)).body(body);
    }

    private java.util.Optional<UUID> actor() {
        return actors.currentActor()
                .map(ec.edu.unl.sec.identityaccess.application.CurrentActorPort.Actor::identityId);
    }

    private void rejection(String action, UUID objectId, UUID correlation, String code) {
        actors.currentActor().ifPresent(actor -> failures.recordAfterRollback(new AuditEventCommand(
                Instant.now(), actor.technical() ? null : actor.identityId(),
                actor.technical() ? actor.identityId() : null, correlation, action, "ReportVersion", objectId, null,
                null, "REJECTED", code, java.util.Map.of())));
    }

    private UUID correlation(HttpServletRequest request) {
        var raw = request.getAttribute(CorrelationFilter.CORRELATION_ATTRIBUTE);
        try {
            return UUID.fromString(String.valueOf(raw));
        } catch (IllegalArgumentException ex) {
            return UUID.randomUUID();
        }
    }

    private <T> List<T> orEmpty(List<T> values) { return values == null ? List.of() : values; }

    public record FindingPage(List<FindingResponse> items, int page, int size, long totalElements) {}

    public record FindingResponse(UUID id, long revision, Instant createdAt, UUID createdBy, Finding.Data data) {}

    public record ConsolidationResponse(UUID id, long revision, Instant createdAt, UUID createdBy,
            Consolidation.Data data) {}

    public record VersionPage(List<Item> items, int page, int size, long totalElements) {}

    public record Item(UUID id, long revision, Instant createdAt, UUID createdBy, Object data, String state) {}

    public record VersionView(UUID id, long revision, Instant createdAt, UUID createdBy, ReportVersion.Data data,
            String state, List<ReportSocialization> socializations, List<ReportObservation> observations,
            List<ObservationReply> replies, List<ApprovalDecision> decisions) {}

    public record FindRequest(@NotBlank @Size(max = 300) String type,
            @NotBlank @Size(min = 1, max = 300) String title,
            @NotBlank @Size(min = 10, max = 4000) String description,
            @NotEmpty @Size(max = 500) List<UUID> indicatorIds, List<UUID> evaluationIds,
            List<UUID> evidenceVersionIds) {
        @JsonAnySetter public void unknown(String key, Object value) { throw new IllegalArgumentException("Campo no admitido"); }
    }

    public record ConsolidationRequest(@NotEmpty @Size(max = 1000) List<UUID> findingIds, UUID aggregationRuleId,
            @NotBlank @Size(min = 10, max = 4000) String reason) {
        @JsonAnySetter public void unknown(String key, Object value) { throw new IllegalArgumentException("Campo no admitido"); }
    }

    public record IssueRequest(@NotBlank String kind, @NotNull UUID consolidationId,
            @NotBlank @Size(min = 1, max = 500) String templateReference, @NotNull Boolean templateValidated,
            @NotBlank @Size(min = 10, max = 200000) String content, UUID predecessorVersionId) {
        @JsonAnySetter public void unknown(String key, Object value) { throw new IllegalArgumentException("Campo no admitido"); }
    }

    public record SocializationRequest(@NotNull Instant occurredAt,
            @NotEmpty @Size(max = 500) List<UUID> participantAssignmentIds,
            @NotBlank @Size(min = 1, max = 500) String recordReference) {
        @JsonAnySetter public void unknown(String key, Object value) { throw new IllegalArgumentException("Campo no admitido"); }
    }

    public record ObservationRequest(@NotBlank @Size(min = 10, max = 4000) String text,
            @NotBlank @Size(min = 1, max = 500) String sectionReference) {
        @JsonAnySetter public void unknown(String key, Object value) { throw new IllegalArgumentException("Campo no admitido"); }
    }

    public record ReplyRequest(@NotBlank @Size(min = 10, max = 4000) String response, UUID correctedVersionId) {
        @JsonAnySetter public void unknown(String key, Object value) { throw new IllegalArgumentException("Campo no admitido"); }
    }

    public record ApprovalRequest(@NotBlank String decision, @Valid Provenance provenance) {
        @JsonAnySetter public void unknown(String key, Object value) { throw new IllegalArgumentException("Campo no admitido"); }
        public record Provenance(@NotNull UUID authorityAssignmentId, @NotBlank @Size(max = 4000) String legalBasis,
                @NotBlank @Size(max = 2000) String documentReference,
                @NotBlank @Size(min = 10, max = 4000) String reason) {}
    }

    public record SocializationData(Instant occurredAt, List<UUID> participantAssignmentIds, String recordReference) {}

    public record ObservationData(String sectionReference, String text) {}

    public record ReplyData(String response, UUID correctedVersionId) {}

    public record ApprovalData(String decision, Provenance provenance) {
        public record Provenance(UUID authorityAssignmentId, String legalBasis, String documentReference,
                String reason) {}
    }
}
