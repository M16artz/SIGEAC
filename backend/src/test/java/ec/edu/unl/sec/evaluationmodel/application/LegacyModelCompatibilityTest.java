package ec.edu.unl.sec.evaluationmodel.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import ec.edu.unl.sec.evaluationmodel.domain.*;
import ec.edu.unl.sec.evaluationmodel.support.*;
import ec.edu.unl.sec.identityaccess.application.*;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LegacyModelCompatibilityTest {
    @Test void v1CannotReplaceOrPublishTypedDraftsOrCreateTypedDrafts() {
        var repo = new InMemoryEvaluationModelRepository();
        var old = ModelFixtures.draft();
        var typed = old.replaceDraft(old.source(), List.of(new ModelNode(null, "C-1", "Criterio", "", 1,
                List.of(new ModelNode(null, "IND-1", "Indicador", "", 1, List.of(), "INDICATOR")), "CRITERION")), old.scale(), old.formula());
        repo.saveModel(new EvaluationModel(old.modelId(), "Sintético", "", old.authorId(), Instant.now()));
        repo.saveVersion(typed, -1);
        CurrentActorPort actors = () -> Optional.of(new CurrentActorPort.Actor(ModelFixtures.APPROVER, false));
        var auth = mock(AuthorizationPort.class);
        when(auth.hasOperation(any(), any())).thenReturn(true);
        var audit = mock(EvaluationModelAuditService.class);
        var service = new EvaluationModelService(repo, actors, auth, new EvaluationModelProperties(null, null), audit);
        assertRequiresV2(() -> service.replace(typed.id(), typed.revision(), old.source(), old.criteria(), old.scale(), old.formula(), UUID.randomUUID()));
        assertRequiresV2(() -> service.publish(typed.id(), typed.revision(), ModelFixtures.APPROVER, UUID.randomUUID()));
        assertRequiresV2(() -> service.createDraft(old.modelId(), old.source(), typed.criteria(), old.scale(), old.formula(), UUID.randomUUID()));
        assertThat(repo.findVersion(typed.id())).contains(typed);
        assertThat(repo.listVersions(old.modelId())).containsExactly(typed);
        verifyNoInteractions(audit);
    }

    @Test void v1CannotCreateALossySuccessorFromTypedPublishedContent() {
        var old = ModelFixtures.draft();
        var typed = old.replaceDraft(old.source(), List.of(new ModelNode(null, "C-1", "Criterio", "", 1,
                List.of(), "CRITERION")), old.scale(), old.formula()).publish(ModelFixtures.APPROVER, Instant.now());
        var repo = new InMemoryEvaluationModelRepository();
        repo.saveVersion(typed, -1);
        CurrentActorPort actors = () -> Optional.of(new CurrentActorPort.Actor(ModelFixtures.AUTHOR, false));
        var auth = mock(AuthorizationPort.class);
        when(auth.hasOperation(any(), any())).thenReturn(true);
        var audit = mock(EvaluationModelAuditService.class);
        var service = new ModelSuccessionService(repo, actors, auth, audit);
        assertRequiresV2(() -> service.create(typed.id(), UUID.randomUUID()));
        assertThat(repo.listVersions(old.modelId())).containsExactly(typed);
        verifyNoInteractions(audit);
    }

    private static void assertRequiresV2(Runnable command) {
        assertThatThrownBy(command::run).isInstanceOf(ModelProblemException.class)
                .extracting("code").isEqualTo("MODEL_VERSION_REQUIRES_V2");
    }
}
