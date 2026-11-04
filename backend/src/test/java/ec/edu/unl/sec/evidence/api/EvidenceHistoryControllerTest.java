package ec.edu.unl.sec.evidence.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.unl.sec.evidence.application.CreateEvidenceSuccessorService;
import ec.edu.unl.sec.evidence.application.EvidenceDraftService;
import ec.edu.unl.sec.evidence.application.EvidenceSourceService;
import ec.edu.unl.sec.evidence.domain.EvidenceVersion;
import ec.edu.unl.sec.evidence.domain.EvidenceVersionStatus;
import ec.edu.unl.sec.evidence.domain.VersionIndicator;
import ec.edu.unl.sec.identityaccess.application.SessionService;
import ec.edu.unl.sec.identityaccess.infrastructure.AccessProblemWriter;
import ec.edu.unl.sec.identityaccess.infrastructure.SessionAuthenticationFilter;
import ec.edu.unl.sec.shared.api.ApiProblemHandler;
import ec.edu.unl.sec.shared.infrastructure.CorrelationFilter;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class EvidenceHistoryControllerTest {
    private static final String CSRF = "synthetic-csrf-token-with-32-characters";
    private static final UUID SOURCE = UUID.fromString("00000000-0000-0000-0000-000000000901");
    private static final UUID SUCCESSOR = UUID.fromString("00000000-0000-0000-0000-000000000902");

    private CreateEvidenceSuccessorService successors;
    private EvidenceDraftService drafts;
    private EvidenceSourceService sources;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        successors = mock(CreateEvidenceSuccessorService.class);
        drafts = mock(EvidenceDraftService.class);
        sources = mock(EvidenceSourceService.class);
        var sessions = mock(SessionService.class);
        when(sessions.authenticateForRequest("synthetic-session")).thenReturn(Optional.of(
                new SessionService.AuthenticatedSession(UUID.randomUUID(), UUID.randomUUID(), "Responsable", false,
                        SessionService.sha256Hex(CSRF))));
        mvc = MockMvcBuilders.standaloneSetup(new EvidenceHistoryController(successors, drafts, sources))
                .setControllerAdvice(new ApiProblemHandler(), new EvidenceProblemHandler())
                .addFilters(new CorrelationFilter(), new SessionAuthenticationFilter(sessions,
                        new AccessProblemWriter(new ObjectMapper().findAndRegisterModules()))).build();
    }

    @Test
    void createsSuccessorWithLocationAndEtag() throws Exception {
        when(successors.createSuccessor(SOURCE)).thenReturn(new CreateEvidenceSuccessorService.SuccessorVersion(
                SUCCESSOR, UUID.randomUUID(), 2));
        when(drafts.getVersion(SUCCESSOR)).thenReturn(version());
        when(sources.snapshot(SUCCESSOR)).thenReturn(EvidenceSourceService.Sources.empty());

        mvc.perform(post("/evidence-versions/" + SOURCE + "/successor").cookie(session())
                        .header("X-CSRF-TOKEN", CSRF))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(header().exists("ETag"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.revisionNumber").value(2));
    }

    private static EvidenceVersion version() {
        return new EvidenceVersion(SUCCESSOR, UUID.randomUUID(), 2, SOURCE,
                EvidenceVersionStatus.DRAFT, "Informe", "Descripcion", "Procedencia",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 6, 30),
                UUID.randomUUID(), Instant.parse("2026-10-20T15:00:00Z"), null, 0,
                List.of(new VersionIndicator(SUCCESSOR, UUID.randomUUID(), "IND-1", "Indicador", 1)));
    }

    private static Cookie session() {
        return new Cookie("SEC_SESSION", "synthetic-session");
    }
}
