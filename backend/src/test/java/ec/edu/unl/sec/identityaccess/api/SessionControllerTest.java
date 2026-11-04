package ec.edu.unl.sec.identityaccess.api;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ec.edu.unl.sec.identityaccess.application.IdentityAdministrationService;
import ec.edu.unl.sec.identityaccess.application.SessionService;
import ec.edu.unl.sec.identityaccess.support.TestSupport;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class SessionControllerTest {
    private SessionService sessions;
    private IdentityAdministrationService administration;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        sessions = mock(SessionService.class);
        administration = mock(IdentityAdministrationService.class);
        mvc = MockMvcBuilders.standaloneSetup(
                        new SessionController(sessions, administration, TestSupport.properties()))
                .setControllerAdvice(new IdentityAccessExceptionHandler())
                .build();
    }

    @Test
    void createsOpaqueSessionAndReturnsCsrfToken() throws Exception {
        Instant expires = Instant.parse("2026-01-01T00:30:00Z");
        when(sessions.login(eq("admin"), eq(TestSupport.ADMIN_PASSWORD), any(), any()))
                .thenReturn(new SessionService.CreatedSession("opaque-token", "csrf-token", expires, TestSupport.ADMIN));

        mvc.perform(post("/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"" + TestSupport.ADMIN_PASSWORD + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Set-Cookie", containsString("SEC_SESSION=opaque-token")))
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
                .andExpect(jsonPath("$.userId").value(TestSupport.ADMIN.toString()))
                .andExpect(jsonPath("$.csrfToken").value("csrf-token"));
    }

    @Test
    void invalidCredentialsDoNotRevealWhetherTheIdentityExists() throws Exception {
        when(sessions.login(any(), any(), any(), any()))
                .thenThrow(new SessionService.AuthenticationRejected("AUTHENTICATION_FAILED"));

        String existing = mvc.perform(post("/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"Synthetic-Wrong-2026\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"))
                .andExpect(jsonPath("$.correlationId").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String missing = mvc.perform(post("/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"inexistente\",\"password\":\"Synthetic-Wrong-2026\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String normalized = existing.replaceAll("\"correlationId\":\"[^\"]+\"", "\"correlationId\":\"correlation\"");
        String normalizedMissing =
                missing.replaceAll("\"correlationId\":\"[^\"]+\"", "\"correlationId\":\"correlation\"");
        org.assertj.core.api.Assertions.assertThat(normalized).isEqualTo(normalizedMissing);
    }

    @Test
    void returnsCurrentIdentityForAuthenticatedPrincipal() throws Exception {
        when(administration.currentIdentity(TestSupport.USER))
                .thenReturn(new IdentityAdministrationService.CurrentIdentity(
                        TestSupport.USER, "Responsable Sintetico", false, List.of()));

        mvc.perform(get("/me").with(principal(TestSupport.USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TestSupport.USER.toString()))
                .andExpect(jsonPath("$.displayName").value("Responsable Sintetico"))
                .andExpect(jsonPath("$.assignments").isArray());
    }

    @Test
    void revokesCurrentSessionAndClearsTheCookie() throws Exception {
        mvc.perform(delete("/sessions")
                        .cookie(new Cookie("SEC_SESSION", "opaque-token"))
                        .with(principal(TestSupport.USER)))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Set-Cookie", containsString("SEC_SESSION=")))
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));

        verify(sessions).logout(eq("opaque-token"), eq(TestSupport.USER), any(UUID.class));
    }

    @Test
    void validatesLoginPayload() throws Exception {
        mvc.perform(post("/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ab\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.username").exists())
                .andExpect(jsonPath("$.details.password").exists());
    }

    private static RequestPostProcessor principal(UUID identityId) {
        return request -> {
            request.setUserPrincipal(new UsernamePasswordAuthenticationToken(identityId, null, List.of()));
            return request;
        };
    }
}
