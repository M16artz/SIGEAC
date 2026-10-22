package ec.edu.unl.sec.shared.api;
import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;
class EntityTagValidationTest {
    @Test void rejectsWeakUnquotedNegativeCompositeAndOverflowTags() {
        for (var tag : new String[]{"7","\"-1\"","W/\"7\"","*","\"1\", \"2\"","\"99999999999999999999999\""})
            assertThatThrownBy(()->EntityTagSupport.require(tag)).isInstanceOf(DomainProblemException.class);
    }
}
