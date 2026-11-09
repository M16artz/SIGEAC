package ec.edu.unl.sec.identityaccess.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AccessProblemWriterTest {
    @Test void challengesUnauthorizedResponsesWithoutReplacingBearerChallenges() throws Exception {
        var writer = new AccessProblemWriter(new ObjectMapper());
        var request = new MockHttpServletRequest("GET","/processes");
        var response = new MockHttpServletResponse();
        writer.write(request,response,HttpStatus.UNAUTHORIZED,"AUTHENTICATION_REQUIRED","Authentication required");
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getHeader("WWW-Authenticate")).isEqualTo("Session realm=\"sec-unl\"");
        assertThat(response.getContentAsString()).contains("AUTHENTICATION_REQUIRED");
        var bearer = new MockHttpServletResponse();
        bearer.setHeader("WWW-Authenticate","Bearer");
        writer.write(request,bearer,HttpStatus.UNAUTHORIZED,"INVALID_BEARER_TOKEN","Authentication required");
        assertThat(bearer.getHeader("WWW-Authenticate")).isEqualTo("Bearer");
        var forbidden = new MockHttpServletResponse();
        writer.write(request,forbidden,HttpStatus.FORBIDDEN,"ACCESS_DENIED","Access denied");
        assertThat(forbidden.getHeader("WWW-Authenticate")).isNull();
    }
}
