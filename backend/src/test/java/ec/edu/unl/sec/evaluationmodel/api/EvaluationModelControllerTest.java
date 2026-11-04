package ec.edu.unl.sec.evaluationmodel.api;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import ec.edu.unl.sec.evaluationmodel.application.*;
import ec.edu.unl.sec.evaluationmodel.domain.ModelCareerApplicability;
import ec.edu.unl.sec.evaluationmodel.domain.ModelConfiguration;
import ec.edu.unl.sec.evaluationmodel.domain.NormativeReference;
import ec.edu.unl.sec.evaluationmodel.support.ModelFixtures;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class EvaluationModelControllerTest {
    private EvaluationModelService service;
    private ModelApplicabilityService applicability;
    private ModelSuccessionService succession;
    private MockMvc mvc;

    @BeforeEach void setup() {
        service = mock(EvaluationModelService.class);
        applicability = mock(ModelApplicabilityService.class);
        succession = mock(ModelSuccessionService.class);
        mvc = MockMvcBuilders.standaloneSetup(new EvaluationModelController(service, succession,
                mock(ModelComparisonService.class), applicability)).setControllerAdvice(new EvaluationModelExceptionHandler())
                .addFilters(new ec.edu.unl.sec.shared.infrastructure.CorrelationFilter()).build();
    }

    @Test void modelErrorsRetainTheRequestCorrelationAndItsAuditIdentifier() throws Exception {
        UUID id = UUID.randomUUID(), correlation = UUID.randomUUID();
        when(service.get(eq(id), eq(correlation))).thenThrow(new ModelProblemException(
                ModelProblemException.Kind.NOT_FOUND, "MODEL_VERSION_NOT_FOUND", "Recurso no visible"));
        mvc.perform(get("/model-versions/" + id).header("X-Correlation-ID", correlation.toString()))
                .andExpect(status().isNotFound()).andExpect(header().string("X-Correlation-ID", correlation.toString()))
                .andExpect(jsonPath("$.correlationId").value(correlation.toString()));
        verify(service).get(id, correlation);
    }

    @Test void configuredHistoryAndSuccessorPreserveTheFullConfiguration() throws Exception {
        var fixture = ec.edu.unl.sec.evaluationmodel.support.ConfiguredModelFixtures.first();
        var published = ModelFixtures.draft().replaceConfiguration(fixture.source(), fixture.nodes(), fixture.configuration())
                .publish(ModelFixtures.APPROVER, Instant.now());
        var next = published.successor(2, ModelFixtures.AUTHOR, Instant.now());
        when(service.listVersions(published.modelId())).thenReturn(List.of(published, next));
        when(succession.createConfigured(eq(published.id()), any())).thenReturn(next);
        mvc.perform(get("/models/" + published.modelId() + "/versions/configured"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].configuration.indicators.length()").value(2))
                .andExpect(jsonPath("$[1].predecessorId").value(published.id().toString()));
        mvc.perform(post("/model-versions/" + published.id() + "/successors/configured"))
                .andExpect(status().isCreated()).andExpect(header().string("ETag", "\"0\""))
                .andExpect(header().string("Location", "/api/v1/model-versions/" + next.id() + "/configured"))
                .andExpect(jsonPath("$.configuration.indicators.length()").value(2));
        verify(succession, never()).create(any(), any());
    }

    @Test void getsVersionWithRevisionEtag() throws Exception {
        var draft = ModelFixtures.draft();
        when(service.get(eq(draft.id()), any())).thenReturn(draft);
        mvc.perform(get("/model-versions/" + draft.id())).andExpect(status().isOk()).andExpect(header().string("ETag", "\"0\""));
    }

    @Test void replacesDraftUsingIfMatch() throws Exception {
        var draft = ModelFixtures.draft();
        when(service.replace(eq(draft.id()), eq(0L), anyString(), anyList(), any(), any(), any())).thenReturn(draft);
        String body = "{\"source\":\"Synthetic\",\"criteria\":[],\"scale\":null,\"formula\":null}";
        mvc.perform(put("/model-versions/" + draft.id()).header("If-Match", "\"0\"").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        verify(service).replace(eq(draft.id()), eq(0L), eq("Synthetic"), anyList(), isNull(), isNull(), any());
    }

    @Test void createsConfiguredVersionAndReturnsPerIndicatorConfiguration() throws Exception {
        var draft = ModelFixtures.draft();
        var configured = new ec.edu.unl.sec.evaluationmodel.domain.ModelVersion(null, draft.modelId(), 1,
                ec.edu.unl.sec.evaluationmodel.domain.ModelVersionStatus.DRAFT, "Synthetic", draft.authorId(), null,
                null, 0, Instant.now(), null, List.of(), null, null,
                new ModelConfiguration(List.of(), List.of(), List.of()));
        when(service.createDraft(eq(draft.modelId()), anyString(), anyList(), any(ModelConfiguration.class), any()))
                .thenReturn(configured);
        String body = """
                {"source":"Synthetic","nodes":[],"configuration":{"indicators":[],"scales":[],"rules":[]}}
                """;
        mvc.perform(post("/models/" + draft.modelId() + "/versions/configured")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.configuration.indicators").isArray());
        verify(service).createDraft(eq(draft.modelId()), eq("Synthetic"), anyList(), any(ModelConfiguration.class), any());
    }

    @Test void recordsApplicabilityDecisionUsingStrongIfMatch() throws Exception {
        UUID versionId = UUID.randomUUID();
        UUID careerId = UUID.randomUUID();
        var decision = new ModelCareerApplicability(null, versionId, careerId, ModelCareerApplicability.Status.APPLICABLE,
                LocalDate.parse("2026-10-01"), null, new ModelCareerApplicability.Decision("Direccion", "CL-011",
                "fixture://decision", "Prueba"), null, 1, ModelFixtures.APPROVER, Instant.parse("2026-10-22T00:00:00Z"));
        when(applicability.decide(eq(versionId), any(ModelApplicabilityService.ApplicabilityInput.class), eq(0L), any()))
                .thenReturn(decision);
        String body = """
                {"careerId":"%s","decision":"APPLICABLE","validFrom":"2026-10-01","validUntil":null,
                 "provenance":{"authorityFunction":"Direccion","legalBasis":"CL-011",
                 "documentReference":"fixture://decision","reason":"Prueba"}}
                """.formatted(careerId);
        mvc.perform(post("/model-versions/" + versionId + "/applicability-decisions").header("If-Match", "\"0\"")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("APPLICABLE"));
    }

    @Test void registersPendingNormativeReference() throws Exception {
        var reference = new NormativeReference(null, "Referencia sintetica", null, null,
                "fixture://normative/pending", null, null, null,
                NormativeReference.VerificationStatus.PENDING_VALIDATION, null, null);
        when(applicability.registerReference(any(NormativeReference.class), any())).thenReturn(reference);
        String body = """
                {"title":"Referencia sintetica","documentReference":"fixture://normative/pending"}
                """;
        mvc.perform(post("/normative-references").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.verificationStatus").value("PENDING_VALIDATION"));
        verify(applicability).registerReference(argThat(value -> !value.verified()
                && value.documentReference().equals("fixture://normative/pending")), any());
    }

    @Test void listsNormativeReferencesPerVersion() throws Exception {
        UUID versionId = UUID.randomUUID();
        var reference = new NormativeReference(null, "Referencia sintetica", null, null,
                "fixture://normative/pending", null, null, null,
                NormativeReference.VerificationStatus.PENDING_VALIDATION, null, null);
        when(applicability.references(versionId)).thenReturn(List.of(reference));
        mvc.perform(get("/model-versions/" + versionId + "/normative-references"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].verificationStatus").value("PENDING_VALIDATION"));
    }

    @Test void keepsTheExactV1NodeShape() throws Exception {
        var draft = ModelFixtures.draft();
        when(service.get(eq(draft.id()), any())).thenReturn(draft);
        mvc.perform(get("/model-versions/" + draft.id())).andExpect(status().isOk())
                .andExpect(jsonPath("$.criteria[0].nodeType").doesNotExist())
                .andExpect(jsonPath("$.criteria[0].children[0].children[0].code").value("IND-1"));
    }

    @Test void doesNotSilentlyProjectTypedContentIntoV1() throws Exception {
        var old = ModelFixtures.draft();
        var node = new ec.edu.unl.sec.evaluationmodel.domain.ModelNode(null, "C-1", "Criterio", "", 1, List.of(), "CRITERION");
        var typed = old.replaceDraft(old.source(), List.of(node), old.scale(), old.formula());
        when(service.get(eq(typed.id()), any())).thenReturn(typed);
        mvc.perform(get("/model-versions/" + typed.id())).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MODEL_VERSION_REQUIRES_V2"));
    }

    @Test void mapsLegacyDraftNodesWithoutChangingTheirIdentity() throws Exception {
        var old = ModelFixtures.draft();
        when(service.createDraft(any(), anyString(), anyList(), any(), any(), any())).thenReturn(old);
        String body = """
                {"source":"Synthetic","criteria":[{"id":"10000000-0000-0000-0000-000000000010","code":"C-1","name":"Criterio","description":"","position":1,"children":[]}],"scale":null,"formula":null}
                """;
        mvc.perform(post("/models/" + old.modelId() + "/versions").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        verify(service).createDraft(eq(old.modelId()), eq("Synthetic"), argThat(nodes -> nodes.size() == 1
                && nodes.getFirst().id().toString().equals("10000000-0000-0000-0000-000000000010")
                && nodes.getFirst().nodeType() == null), isNull(), isNull(), any());
    }

    @Test void rejectsTypedNodeInputOnV1BeforeCallingTheService() throws Exception {
        String body = """
                {"source":"Synthetic","criteria":[{"code":"C-1","name":"Criterio","description":"","position":1,"children":[],"nodeType":"CRITERION"}],"scale":null,"formula":null}
                """;
        mvc.perform(post("/models/" + UUID.randomUUID() + "/versions").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
