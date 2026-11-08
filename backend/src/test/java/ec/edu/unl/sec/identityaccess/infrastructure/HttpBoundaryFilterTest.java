package ec.edu.unl.sec.identityaccess.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class HttpBoundaryFilterTest {
    final ObjectMapper mapper = new ObjectMapper();
    @Test void rejectsPlainHttpBeforeHandlingCredentials() throws Exception {
        var filter = new HttpBoundaryFilter(new AccessProblemWriter(mapper), true, 2, 2, Clock.systemUTC());
        var response = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("POST", "/sessions"), response, (q, s) -> { throw new AssertionError("must not execute"); });
        assertThat(response.getStatus()).isEqualTo(426);
        assertThat(mapper.readTree(response.getContentAsString()).at("/errors/code").asText()).isEqualTo("HTTPS_REQUIRED");
    }
    @Test void enforcesRateLimitAndRecoversAfterWindowWithoutTrustingForwardedIp() throws Exception {
        Clock clock = mock(Clock.class); when(clock.millis()).thenReturn(1_000L);
        var filter = new HttpBoundaryFilter(new AccessProblemWriter(mapper), false, 1, 2, clock);
        var first = new MockHttpServletResponse(); filter.doFilter(new MockHttpServletRequest(), first, (q, s) -> {});
        var request = new MockHttpServletRequest(); request.addHeader("X-Forwarded-For", "different-client");
        var limited = new MockHttpServletResponse(); filter.doFilter(request, limited, (q, s) -> { throw new AssertionError(); });
        assertThat(limited.getStatus()).isEqualTo(429); assertThat(limited.getHeader("Retry-After")).isEqualTo("60");
        assertThat(mapper.readTree(limited.getContentAsString()).path("success").asBoolean()).isFalse();
        when(clock.millis()).thenReturn(61_001L);
        var recovered = new MockHttpServletResponse(); filter.doFilter(new MockHttpServletRequest(), recovered, (q, s) -> {});
        assertThat(recovered.getStatus()).isEqualTo(200);
    }
    @Test void rejectsQuerySecretsAndInvalidPagination() throws Exception {
        var filter = new HttpBoundaryFilter(new AccessProblemWriter(mapper), false, 10, 10, Clock.systemUTC());
        for (String query : new String[]{"access_token=PRIVATE_TEST_VALUE", "pass%77ord=PRIVATE_TEST_VALUE", "size=101", "page=-1", "size=abc"}) {
            var request = new MockHttpServletRequest(); request.setQueryString(query);
            var response = new MockHttpServletResponse();
            filter.doFilter(request, response, (q, s) -> { throw new AssertionError(); });
            assertThat(response.getStatus()).isEqualTo(400);
            assertThat(response.getContentAsString()).doesNotContain("PRIVATE_TEST_VALUE");
        }
    }
    @Test void boundsClientRegistry() throws Exception {
        var filter = new HttpBoundaryFilter(new AccessProblemWriter(mapper), false, 10, 1, Clock.systemUTC());
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (q, s) -> {});
        var request = new MockHttpServletRequest(); request.setRemoteAddr("192.0.2.2");
        var response = new MockHttpServletResponse(); filter.doFilter(request, response, (q, s) -> { throw new AssertionError(); });
        assertThat(response.getStatus()).isEqualTo(429);
    }
}
