package ec.edu.unl.sec.evaluationmodel.domain;

import static org.assertj.core.api.Assertions.assertThat;
import ec.edu.unl.sec.evaluationmodel.support.ModelFixtures;
import java.util.List;
import org.junit.jupiter.api.Test;

class PublicationValidatorTest {
    @Test void acceptsCompleteVersionWithIndependentApprover(){assertThat(PublicationValidator.validate(ModelFixtures.draft(),ModelFixtures.APPROVER,"sec.progress/v1")).isEmpty();}
    @Test void reportsAllMissingConditions(){var v=new ModelVersion(null,ModelFixtures.MODEL,1,ModelVersionStatus.DRAFT,"",ModelFixtures.AUTHOR,null,null,0,null,null,List.of(),null,null);assertThat(PublicationValidator.validate(v,ModelFixtures.AUTHOR,"sec.progress/v1")).contains("SOURCE_REQUIRED","APPROVER_MUST_DIFFER_FROM_AUTHOR","CRITERION_REQUIRED","SCALE_REQUIRED","FORMULA_REQUIRED");}
}
