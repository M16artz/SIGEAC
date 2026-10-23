package ec.edu.unl.sec.evidence.api;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.unl.sec.evidence.application.EvidenceProblemException;
import ec.edu.unl.sec.evidence.application.EvidenceSourceService;
import ec.edu.unl.sec.evidence.domain.ExternalReference;
import ec.edu.unl.sec.identityaccess.application.SessionService;
import ec.edu.unl.sec.identityaccess.infrastructure.AccessProblemWriter;
import ec.edu.unl.sec.identityaccess.infrastructure.SessionAuthenticationFilter;
import ec.edu.unl.sec.shared.api.ApiProblemHandler;
import ec.edu.unl.sec.shared.infrastructure.CorrelationFilter;
import jakarta.servlet.http.Cookie;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class EvidenceSourceControllerTest {
    private static final String CSRF = "synthetic-csrf-token-with-32-characters";
    private final UUID version = UUID.randomUUID(), referenceId = UUID.randomUUID();
    private final String path = "/evidence-versions/" + version;
    private EvidenceSourceService service;
    private MockMvc mvc;

    @BeforeEach void setup() {
        service = mock(EvidenceSourceService.class);
        var sessions = mock(SessionService.class);
        when(sessions.authenticateForRequest("synthetic-session")).thenReturn(Optional.of(
                new SessionService.AuthenticatedSession(UUID.randomUUID(), UUID.randomUUID(), "Responsable", false,
                        SessionService.sha256Hex(CSRF))));
        mvc = MockMvcBuilders.standaloneSetup(new EvidenceSourceController(service))
                .setControllerAdvice(new ApiProblemHandler(), new EvidenceProblemHandler())
                .addFilters(new CorrelationFilter(), new SessionAuthenticationFilter(sessions,
                        new AccessProblemWriter(new ObjectMapper().findAndRegisterModules()))).build();
    }

    @Test void addsAndUpdatesOnlyPublicReferenceFieldsWithStrongEtags() throws Exception {
        var reference = new ExternalReference(referenceId, version, "urn:synthetic:document", "Declaracion", 2);
        when(service.addReference(version, "\"0\"", reference.uri(), reference.description()))
                .thenReturn(new EvidenceSourceService.Changed<>(reference, 1));
        when(service.updateReference(version, referenceId, "\"1\"", reference.uri(), reference.description()))
                .thenReturn(new EvidenceSourceService.Changed<>(reference, 2));
        String body = "{\"uri\":\"urn:synthetic:document\",\"description\":\"Declaracion\"}";
        mvc.perform(post(path + "/external-references").cookie(session()).header("X-CSRF-TOKEN", CSRF)
                        .header("If-Match", "\"0\"").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(header().string("ETag", "\"1\""))
                .andExpect(header().string("Location", "/api/v1" + path + "/external-references/" + referenceId))
                .andExpect(jsonPath("$.id").value(referenceId.toString()))
                .andExpect(jsonPath("$.uri").value(reference.uri())).andExpect(jsonPath("$.position").doesNotExist())
                .andExpect(jsonPath("$.versionId").doesNotExist());
        mvc.perform(patch(path + "/external-references/" + referenceId).cookie(session()).header("X-CSRF-TOKEN", CSRF)
                        .header("If-Match", "\"1\"").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(header().string("ETag", "\"2\""));
    }

    @Test void removalsReturnTheNewEtagAndCsrfBlocksWritesBeforeTheService() throws Exception {
        when(service.removeReference(version, referenceId, "\"2\"")).thenReturn(3L);
        mvc.perform(delete(path + "/external-references/" + referenceId).cookie(session())
                        .header("X-CSRF-TOKEN", CSRF).header("If-Match", "\"2\""))
                .andExpect(status().isNoContent()).andExpect(header().string("ETag", "\"3\""));
        UUID file = UUID.randomUUID();
        when(service.removeFile(version, file, "\"3\"")).thenReturn(4L);
        mvc.perform(delete(path + "/files/" + file).cookie(session()).header("X-CSRF-TOKEN", CSRF).header("If-Match", "\"3\""))
                .andExpect(status().isNoContent()).andExpect(header().string("ETag", "\"4\""));
        clearInvocations(service);
        mvc.perform(delete(path + "/files/" + file).cookie(session())).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test void mapsMissingAndStalePreconditionsWithoutDisclosingSources() throws Exception {
        when(service.removeReference(version, referenceId, null)).thenThrow(new EvidenceProblemException(
                EvidenceProblemException.Kind.PRECONDITION_REQUIRED, "PRECONDITION_REQUIRED", "Se requiere If-Match."));
        when(service.removeReference(version, referenceId, "\"9\"")).thenThrow(new EvidenceProblemException(
                EvidenceProblemException.Kind.PRECONDITION_FAILED, "STALE_DRAFT", "Recargue el borrador."));
        mvc.perform(delete(path + "/external-references/" + referenceId).cookie(session()).header("X-CSRF-TOKEN", CSRF))
                .andExpect(status().is(428)).andExpect(jsonPath("$.code").value("PRECONDITION_REQUIRED"));
        mvc.perform(delete(path + "/external-references/" + referenceId).cookie(session()).header("X-CSRF-TOKEN", CSRF).header("If-Match", "\"9\""))
                .andExpect(status().isPreconditionFailed()).andExpect(jsonPath("$.code").value("STALE_DRAFT"));
    }

    private static Cookie session() { return new Cookie("SEC_SESSION", "synthetic-session"); }
}
