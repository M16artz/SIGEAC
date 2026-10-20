package ec.edu.unl.sec.evaluationmodel.domain;

import static org.assertj.core.api.Assertions.assertThat;
import ec.edu.unl.sec.evaluationmodel.support.ModelFixtures;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FormulaValidatorTest {
    @Test void acceptsAllowlistedDeterministicDefinition(){assertThat(FormulaValidator.validate(ModelFixtures.formula(),"sec.progress/v1",Set.of("IND-1"))).isEmpty();}
    @Test void rejectsUnknownReferencesAndMissingPolicies(){var f=new ProgressFormula("F","other",java.util.List.of(),java.util.List.of("MISSING"),"","",java.util.List.of(),java.util.Map.of());assertThat(FormulaValidator.validate(f,"sec.progress/v1",Set.of("IND-1"))).contains("FORMULA_SCHEMA_UNSUPPORTED","FORMULA_RESULTS_REQUIRED","FORMULA_ZERO_POLICY_REQUIRED","FORMULA_UNKNOWN_INPUT:MISSING");}
}
