package ec.edu.unl.sec.shared.api;
import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
class ApiProblemHandlerTest {
    @Test void invalidRequestDoesNotExposeTheUnderlyingMessage() {
        var request=new MockHttpServletRequest("POST","/processes");
        var problem=new ApiProblemHandler().handleInvalidRequest(new IllegalArgumentException("secret internal payload"),request);
        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(problem.getDetail()).doesNotContain("secret");
    }
}
