package ec.edu.unl.sec.shared.api;
import static org.assertj.core.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
class ApiProblemHandlerTest {
    @Test void oversizedMultipartReturnsASafePayloadTooLargeProblem() {
        var request=new MockHttpServletRequest("POST","/evidence-versions/synthetic/files");
        request.setAttribute(ec.edu.unl.sec.shared.infrastructure.CorrelationFilter.CORRELATION_ATTRIBUTE,"synthetic-correlation");
        var problem=new ApiProblemHandler().handleOversizedUpload(
                new org.springframework.web.multipart.MaxUploadSizeExceededException(26214400),request);
        assertThat(problem.getStatus()).isEqualTo(413);
        assertThat(problem.getProperties()).containsEntry("code","FILE_TOO_LARGE")
                .containsEntry("correlationId","synthetic-correlation");
    }
    @Test void invalidRequestDoesNotExposeTheUnderlyingMessage() {
        var request=new MockHttpServletRequest("POST","/processes");
        var problem=new ApiProblemHandler().handleInvalidRequest(new IllegalArgumentException("secret internal payload"),request);
        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(problem.getDetail()).doesNotContain("secret");
    }
}
