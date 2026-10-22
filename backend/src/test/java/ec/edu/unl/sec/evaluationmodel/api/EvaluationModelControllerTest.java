package ec.edu.unl.sec.evaluationmodel.api;

import static org.mockito.ArgumentMatchers.*;import static org.mockito.Mockito.*;import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import ec.edu.unl.sec.evaluationmodel.application.*;import ec.edu.unl.sec.evaluationmodel.support.ModelFixtures;import java.util.UUID;import org.junit.jupiter.api.BeforeEach;import org.junit.jupiter.api.Test;import org.springframework.http.MediaType;import org.springframework.test.web.servlet.MockMvc;import org.springframework.test.web.servlet.setup.MockMvcBuilders;
class EvaluationModelControllerTest {
 private EvaluationModelService service;private MockMvc mvc;
 @BeforeEach void setup(){service=mock(EvaluationModelService.class);mvc=MockMvcBuilders.standaloneSetup(new EvaluationModelController(service,mock(ModelSuccessionService.class),mock(ModelComparisonService.class))).setControllerAdvice(new EvaluationModelExceptionHandler()).build();}
 @Test void getsVersionWithRevisionEtag() throws Exception{var draft=ModelFixtures.draft();when(service.get(eq(draft.id()),any())).thenReturn(draft);mvc.perform(get("/model-versions/"+draft.id())).andExpect(status().isOk()).andExpect(header().string("ETag","\"0\""));}
 @Test void replacesDraftUsingIfMatch() throws Exception{var draft=ModelFixtures.draft();when(service.replace(eq(draft.id()),eq(0L),anyString(),anyList(),any(),any(),any())).thenReturn(draft);String body="{\"source\":\"Synthetic\",\"criteria\":[],\"scale\":null,\"formula\":null}";mvc.perform(put("/model-versions/"+draft.id()).header("If-Match","\"0\"").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk());verify(service).replace(eq(draft.id()),eq(0L),eq("Synthetic"),anyList(),isNull(),isNull(),any());}
 @Test void keepsTheExactV1NodeShape() throws Exception {
  var draft=ModelFixtures.draft();when(service.get(eq(draft.id()),any())).thenReturn(draft);
  mvc.perform(get("/model-versions/"+draft.id())).andExpect(status().isOk())
   .andExpect(jsonPath("$.criteria[0].nodeType").doesNotExist())
   .andExpect(jsonPath("$.criteria[0].children[0].children[0].code").value("IND-1"));
 }
 @Test void doesNotSilentlyProjectTypedContentIntoV1() throws Exception {
  var old=ModelFixtures.draft();var node=new ec.edu.unl.sec.evaluationmodel.domain.ModelNode(null,"C-1","Criterio","",1,java.util.List.of(),"CRITERION");
  var typed=old.replaceDraft(old.source(),java.util.List.of(node),old.scale(),old.formula());
  when(service.get(eq(typed.id()),any())).thenReturn(typed);
  mvc.perform(get("/model-versions/"+typed.id())).andExpect(status().isConflict())
   .andExpect(jsonPath("$.code").value("MODEL_VERSION_REQUIRES_V2"));
 }
 @Test void mapsLegacyDraftNodesWithoutChangingTheirIdentity() throws Exception {
  var old=ModelFixtures.draft();when(service.createDraft(any(),anyString(),anyList(),any(),any(),any())).thenReturn(old);
  String body="""
   {"source":"Synthetic","criteria":[{"id":"10000000-0000-0000-0000-000000000010","code":"C-1","name":"Criterio","description":"","position":1,"children":[]}],"scale":null,"formula":null}
   """;
  mvc.perform(post("/models/"+old.modelId()+"/versions").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
  verify(service).createDraft(eq(old.modelId()),eq("Synthetic"),argThat(nodes->nodes.size()==1&&nodes.getFirst().id().toString().equals("10000000-0000-0000-0000-000000000010")&&nodes.getFirst().nodeType()==null),isNull(),isNull(),any());
 }
 @Test void rejectsTypedNodeInputOnV1BeforeCallingTheService() throws Exception {
  String body="""
   {"source":"Synthetic","criteria":[{"code":"C-1","name":"Criterio","description":"","position":1,"children":[],"nodeType":"CRITERION"}],"scale":null,"formula":null}
   """;
  mvc.perform(post("/models/"+UUID.randomUUID()+"/versions").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
  verifyNoInteractions(service);
 }
}
