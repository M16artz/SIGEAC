package ec.edu.unl.sec.evaluationmodel.application;

import static org.assertj.core.api.Assertions.*;import static org.mockito.Mockito.*;
import ec.edu.unl.sec.evaluationmodel.support.*;import ec.edu.unl.sec.identityaccess.application.*;import java.time.Instant;import java.util.Optional;import org.junit.jupiter.api.Test;
class ModelSuccessionServiceTest {
 @Test void createsRelatedDraftWithoutChangingPublishedPredecessor(){var repo=new InMemoryEvaluationModelRepository();var published=ModelFixtures.draft().publish(ModelFixtures.APPROVER,Instant.now());repo.saveVersion(published,-1);CurrentActorPort actors=()->Optional.of(new CurrentActorPort.Actor(ModelFixtures.AUTHOR,false));AuthorizationPort auth=mock(AuthorizationPort.class);when(auth.hasOperation(any(),any())).thenReturn(true);var audit=mock(EvaluationModelAuditService.class);var next=new ModelSuccessionService(repo,actors,auth,audit).create(published.id(),java.util.UUID.randomUUID());assertThat(next.predecessorId()).isEqualTo(published.id());assertThat(next.status()).isEqualTo(ec.edu.unl.sec.evaluationmodel.domain.ModelVersionStatus.DRAFT);assertThat(repo.findVersion(published.id()).orElseThrow()).isEqualTo(published);}
}
