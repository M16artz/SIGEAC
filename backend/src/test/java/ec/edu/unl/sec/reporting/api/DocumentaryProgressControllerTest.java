package ec.edu.unl.sec.reporting.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.unl.sec.identityaccess.application.SessionService;
import ec.edu.unl.sec.identityaccess.infrastructure.AccessProblemWriter;
import ec.edu.unl.sec.identityaccess.infrastructure.SessionAuthenticationFilter;
import ec.edu.unl.sec.reporting.application.DocumentaryProgressQueryService;
import ec.edu.unl.sec.reporting.application.ReportingProblemException;
import ec.edu.unl.sec.reporting.domain.DocumentaryProgressCalculator;
import ec.edu.unl.sec.shared.api.ApiProblemHandler;
import ec.edu.unl.sec.shared.infrastructure.CorrelationFilter;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class DocumentaryProgressControllerTest {
    private final UUID process = UUID.randomUUID();
    private final DocumentaryProgressQueryService query = mock(DocumentaryProgressQueryService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        var sessions = mock(SessionService.class);
        when(sessions.authenticateForRequest("synthetic-session")).thenReturn(Optional.of(
                new SessionService.AuthenticatedSession(UUID.randomUUID(), UUID.randomUUID(),
                        "Consultor sintético", false, SessionService.sha256Hex("synthetic-csrf"))));
        var mapper = new ObjectMapper().findAndRegisterModules();
        mvc = MockMvcBuilders.standaloneSetup(new DocumentaryProgressController(query))
                .setControllerAdvice(new ApiProblemHandler(), new ReportingProblemHandler())
                .addFilters(new CorrelationFilter(), new SessionAuthenticationFilter(sessions, new AccessProblemWriter(mapper)))
                .build();
    }

    @Test
    void returnsExactDocumentaryCountsWithExplicitScopeAndNoCaching() throws Exception {
        when(query.query(process, null, null, 0, 20)).thenReturn(view(
                new DocumentaryProgressCalculator.Progress(DocumentaryProgressCalculator.Status.CONFIGURED,
                        4, 3, 2, DocumentaryProgressCalculator.FORMULA_VERSION)));
        mvc.perform(get(path()).cookie(session())).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.requiredSourceTotal").value(4))
                .andExpect(jsonPath("$.registeredSourceTotal").value(3))
                .andExpect(jsonPath("$.acceptedSourceTotal").value(2))
                .andExpect(jsonPath("$.collectionPercent").value(75))
                .andExpect(jsonPath("$.documentReviewPercent").value(50))
                .andExpect(jsonPath("$.scopeDescription").value("Ámbito autorizado"))
                .andExpect(jsonPath("$.academicStatus").doesNotExist());
    }

    @Test
    void serializesZeroDenominatorPercentagesAsNull() throws Exception {
        when(query.query(process, null, null, 0, 20)).thenReturn(view(
                new DocumentaryProgressCalculator.Progress(DocumentaryProgressCalculator.Status.NOT_CONFIGURED,
                        0, 0, 0, DocumentaryProgressCalculator.FORMULA_VERSION)));
        mvc.perform(get(path()).cookie(session())).andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("NOT_CONFIGURED"))
                .andExpect(jsonPath("$.collectionPercent").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.documentReviewPercent").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void passesScopeFiltersAndPagination() throws Exception {
        var indicator = UUID.randomUUID();
        var source = UUID.randomUUID();
        when(query.query(process, indicator, source, 2, 5)).thenReturn(view(
                new DocumentaryProgressCalculator.Progress(DocumentaryProgressCalculator.Status.CONFIGURED, 1, 0, 0, "v1")));
        mvc.perform(get(path()).cookie(session()).param("indicatorId", indicator.toString())
                .param("requiredSourceId", source.toString()).param("page", "2").param("size", "5"))
                .andExpect(status().isOk());
        verify(query).query(process, indicator, source, 2, 5);
    }

    @Test
    void requiresSessionAndRejectsUnsupportedHistoryFilters() throws Exception {
        when(query.query(any(), any(), any(), anyInt(), anyInt())).thenThrow(new ReportingProblemException(
                ReportingProblemException.Kind.UNAUTHORIZED, "UNAUTHORIZED", "Se requiere una sesión vigente."));
        mvc.perform(get(path())).andExpect(status().isUnauthorized());
        clearInvocations(query);
        mvc.perform(get(path()).cookie(session()).param("cutoffAt", "2026-01-01T00:00:00Z"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_FILTER"));
        verifyNoInteractions(query);
    }

    @Test
    void returnsSafeScopedProblemWithCorrelation() throws Exception {
        when(query.query(any(), any(), any(), anyInt(), anyInt())).thenThrow(new ReportingProblemException(
                ReportingProblemException.Kind.NOT_FOUND, "RESOURCE_NOT_FOUND", "Recurso no encontrado en el ámbito."));
        mvc.perform(get(path()).cookie(session())).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.correlationId").isNotEmpty())
                .andExpect(jsonPath("$.details").isMap());
    }

    private DocumentaryProgressQueryService.View view(DocumentaryProgressCalculator.Progress progress) {
        return new DocumentaryProgressQueryService.View(process, UUID.randomUUID(), Instant.parse("2026-10-24T12:00:00Z"),
                "Ámbito autorizado", List.of(), progress, List.of(), 0, 20, 0);
    }

    private String path() { return "/processes/" + process + "/documentary-progress"; }
    @org.junit.jupiter.api.AfterEach
    void clearSecurityContext() { org.springframework.security.core.context.SecurityContextHolder.clearContext(); }
    private Cookie session() { return new Cookie(ec.edu.unl.sec.identityaccess.infrastructure.SessionCookie.NAME, "synthetic-session"); }
}
