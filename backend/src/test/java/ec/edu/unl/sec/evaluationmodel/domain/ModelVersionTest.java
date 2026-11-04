package ec.edu.unl.sec.evaluationmodel.domain;

import static org.assertj.core.api.Assertions.*;
import ec.edu.unl.sec.evaluationmodel.support.ModelFixtures;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ModelVersionTest {
    @Test void normalizesCodesAndIncrementsDraftRevision(){var v=ModelFixtures.draft();assertThat(v.criteria().getFirst().children().getFirst().children().getFirst().code()).isEqualTo("IND-1");assertThat(v.replaceDraft(v.source(),v.criteria(),v.scale(),v.formula()).revision()).isEqualTo(1);}
    @Test void publishedVersionRejectsReplacement(){var v=ModelFixtures.draft().publish(ModelFixtures.APPROVER,Instant.now());assertThatThrownBy(()->v.replaceDraft(v.source(),v.criteria(),v.scale(),v.formula())).isInstanceOf(IllegalStateException.class);}
}
