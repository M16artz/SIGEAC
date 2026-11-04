package ec.edu.unl.sec.reporting.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import ec.edu.unl.sec.documentreview.application.DocumentReviewProgressPort;
import ec.edu.unl.sec.evidence.application.EvidenceContextPort;
import ec.edu.unl.sec.evidence.application.EvidenceProgressPort;
import ec.edu.unl.sec.evidence.application.RequiredSourceCatalogPort;
import ec.edu.unl.sec.identityaccess.application.AuthorizationPort;
import ec.edu.unl.sec.identityaccess.application.CurrentActorPort;
import ec.edu.unl.sec.identityaccess.domain.AuthorizationScope;
import ec.edu.unl.sec.identityaccess.domain.OperationCode;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DocumentaryProgressQueryTest {
    private final UUID actor = UUID.randomUUID(), process = UUID.randomUUID(), career = UUID.randomUUID();
    private final UUID model = UUID.randomUUID(), visible = UUID.randomUUID(), hidden = UUID.randomUUID();
    private final UUID first = UUID.randomUUID(), second = UUID.randomUUID(), foreign = UUID.randomUUID();
    private final UUID evidenceId = UUID.randomUUID(), version = UUID.randomUUID(), decision = UUID.randomUUID();
    private final CurrentActorPort actors = mock(CurrentActorPort.class);
    private final AuthorizationPort authorization = mock(AuthorizationPort.class);
    private final EvidenceContextPort context = mock(EvidenceContextPort.class);
    private final RequiredSourceCatalogPort catalog = mock(RequiredSourceCatalogPort.class);
    private final EvidenceProgressPort evidence = mock(EvidenceProgressPort.class);
    private final DocumentReviewProgressPort reviews = mock(DocumentReviewProgressPort.class);
    private DocumentaryProgressQueryService service;

    @BeforeEach
    void setUp() {
        service = new DocumentaryProgressQueryService(actors, authorization, context, catalog, evidence, reviews);
        when(actors.currentActor()).thenReturn(Optional.of(new CurrentActorPort.Actor(actor, false)));
        when(authorization.hasOperation(actor, OperationCode.PROGRESS_READ)).thenReturn(true);
        when(context.process(process)).thenReturn(Optional.of(new EvidenceContextPort.ProcessContext(
                process, career, model, 1, "ACTIVE", List.of(
                        new EvidenceContextPort.Indicator(visible, "I1", "Visible"),
                        new EvidenceContextPort.Indicator(hidden, "I2", "Oculto")))));
        when(authorization.isAllowed(actor, OperationCode.PROGRESS_READ,
                new AuthorizationScope(career, process, visible))).thenReturn(true);
        when(catalog.model(model)).thenReturn(Optional.of(new RequiredSourceCatalogPort.ModelSources(model,
                List.of(source(first, visible, "F1"), source(second, visible, "F2"), source(foreign, hidden, "F3")))));
        when(evidence.applicability(process)).thenReturn(new EvidenceProgressPort.Applicability(Set.of(), Set.of()));
        when(evidence.currentCoverages(eq(process), any())).thenReturn(List.of(
                new EvidenceProgressPort.CurrentCoverage(evidenceId, version, first)));
        when(reviews.latestOutcomes(any())).thenReturn(List.of(
                new DocumentReviewProgressPort.Outcome(version, first, decision, "ACCEPTED")));
    }

    @Test
    void appliesIndicatorScopeBeforeCountingAndReadingEvidence() {
        var result = query(null, null, 0, 20);
        assertThat(result.indicatorIds()).containsExactly(visible);
        assertThat(result.progress().requiredSourceTotal()).isEqualTo(2);
        assertThat(result.progress().registeredSourceTotal()).isEqualTo(1);
        assertThat(result.progress().documentReviewPercent()).isEqualByComparingTo("50.00");
        assertThat(result.sources()).extracting(DocumentaryProgressQueryService.SourceView::id)
                .containsExactly(first, second);
        assertThat(result.sources().getFirst().coverages().getFirst().decisionId()).isEqualTo(decision);
        verify(evidence).currentCoverages(process, Set.of(first, second));
        verify(reviews).latestOutcomes(Set.of(version));
    }

    @Test
    void unauthorizedAndUnknownFiltersReturnTheSameNotFoundWithoutReadingEvidence() {
        for (var filter : List.of(hidden, UUID.randomUUID())) {
            assertProblem(() -> query(filter, null, 0, 20), ReportingProblemException.Kind.NOT_FOUND);
        }
        for (var filter : List.of(foreign, UUID.randomUUID())) {
            assertProblem(() -> query(null, filter, 0, 20), ReportingProblemException.Kind.NOT_FOUND);
        }
        verifyNoInteractions(evidence, reviews);
    }

    @Test
    void sourceFilterChangesBothDenominatorAndDetail() {
        when(evidence.currentCoverages(process, Set.of(first))).thenReturn(List.of(
                new EvidenceProgressPort.CurrentCoverage(evidenceId, version, first)));
        var result = query(visible, first, 0, 20);
        assertThat(result.progress().requiredSourceTotal()).isEqualTo(1);
        assertThat(result.progress().documentReviewPercent()).isEqualByComparingTo("100.00");
        assertThat(result.sources()).hasSize(1);
    }

    @Test
    void paginationDoesNotChangeScopeTotals() {
        var result = query(null, null, 1, 1);
        assertThat(result.totalSources()).isEqualTo(2);
        assertThat(result.sources()).extracting(DocumentaryProgressQueryService.SourceView::id).containsExactly(second);
        assertThat(result.progress().requiredSourceTotal()).isEqualTo(2);
    }

    @Test
    void excludesIndicatorAndSourceUsingCurrentApplicability() {
        when(evidence.applicability(process)).thenReturn(new EvidenceProgressPort.Applicability(Set.of(visible), Set.of()));
        var result = query(null, null, 0, 20);
        assertThat(result.progress().status().name()).isEqualTo("NOT_APPLICABLE");
        assertThat(result.sources()).allSatisfy(source -> assertThat(source.applicable()).isFalse());
        when(evidence.applicability(process)).thenReturn(new EvidenceProgressPort.Applicability(Set.of(), Set.of(first)));
        result = query(null, null, 0, 20);
        assertThat(result.progress().requiredSourceTotal()).isEqualTo(1);
        assertThat(result.progress().acceptedSourceTotal()).isZero();
    }

    @Test
    void missingDecisionIsPendingAndDoesNotInheritAcceptance() {
        when(reviews.latestOutcomes(any())).thenReturn(List.of());
        var result = query(null, null, 0, 20);
        assertThat(result.progress().registeredSourceTotal()).isEqualTo(1);
        assertThat(result.progress().acceptedSourceTotal()).isZero();
        assertThat(result.sources().getFirst().coverages().getFirst().outcome()).isEqualTo("PENDING_REVIEW");
    }

    @Test
    void rejectsAbsentOrTechnicalActorsBeforeReadingProcess() {
        when(actors.currentActor()).thenReturn(Optional.empty());
        assertProblem(() -> query(null, null, 0, 20), ReportingProblemException.Kind.UNAUTHORIZED);
        when(actors.currentActor()).thenReturn(Optional.of(new CurrentActorPort.Actor(actor, true)));
        assertProblem(() -> query(null, null, 0, 20), ReportingProblemException.Kind.FORBIDDEN);
        verifyNoInteractions(context, catalog, evidence, reviews);
    }

    @Test
    void rejectsMissingCapabilityOrWrongProcessScope() {
        when(authorization.hasOperation(actor, OperationCode.PROGRESS_READ)).thenReturn(false);
        assertProblem(() -> query(null, null, 0, 20), ReportingProblemException.Kind.FORBIDDEN);
        verifyNoInteractions(context, catalog, evidence, reviews);
        when(authorization.hasOperation(actor, OperationCode.PROGRESS_READ)).thenReturn(true);
        when(authorization.isAllowed(eq(actor), eq(OperationCode.PROGRESS_READ), any())).thenReturn(false);
        assertProblem(() -> query(null, null, 0, 20), ReportingProblemException.Kind.NOT_FOUND);
        verifyNoInteractions(catalog, evidence, reviews);
    }

    @Test
    void capabilityRevocationIsRecheckedOnEveryRequest() {
        query(null, null, 0, 20);
        when(authorization.hasOperation(actor, OperationCode.PROGRESS_READ)).thenReturn(false);
        clearInvocations(context, catalog, evidence, reviews);
        assertProblem(() -> query(null, null, 0, 20), ReportingProblemException.Kind.FORBIDDEN);
        verifyNoInteractions(context, catalog, evidence, reviews);
    }

    @Test
    void rejectsInvalidPagination() {
        assertProblem(() -> query(null, null, -1, 20), ReportingProblemException.Kind.INVALID);
        assertProblem(() -> query(null, null, 0, 0), ReportingProblemException.Kind.INVALID);
        assertProblem(() -> query(null, null, 0, 101), ReportingProblemException.Kind.INVALID);
    }

    private DocumentaryProgressQueryService.View query(UUID indicator, UUID source, int page, int size) {
        return service.query(process, indicator, source, page, size);
    }

    private RequiredSourceCatalogPort.Entry source(UUID id, UUID indicator, String code) {
        return new RequiredSourceCatalogPort.Entry(id, indicator, "I", code, code, "FILE", true, 1, null);
    }

    private void assertProblem(Runnable action, ReportingProblemException.Kind kind) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(ReportingProblemException.class,
                problem -> assertThat(problem.kind()).isEqualTo(kind));
    }
}
