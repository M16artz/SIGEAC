package ec.edu.unl.sec.evaluationmodel.api;

import static org.mockito.ArgumentMatchers.*;import static org.mockito.Mockito.*;import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import ec.edu.unl.sec.evaluationmodel.application.*;import ec.edu.unl.sec.evaluationmodel.support.ModelFixtures;import java.util.UUID;import org.junit.jupiter.api.BeforeEach;import org.junit.jupiter.api.Test;import org.springframework.http.MediaType;import org.springframework.test.web.servlet.MockMvc;import org.springframework.test.web.servlet.setup.MockMvcBuilders;
class EvaluationModelControllerTest {
 private EvaluationModelService service;private MockMvc mvc;
 @BeforeEach void setup(){service=mock(EvaluationModelService.class);mvc=MockMvcBuilders.standaloneSetup(new EvaluationModelController(service,mock(ModelSuccessionService.class),mock(ModelComparisonService.class))).setControllerAdvice(new EvaluationModelExceptionHandler()).build();}
 @Test void getsVersionWithRevisionEtag() throws Exception{var draft=ModelFixtures.draft();when(service.get(eq(draft.id()),any())).thenReturn(draft);mvc.perform(get("/model-versions/"+draft.id())).andExpect(status().isOk()).andExpect(header().string("ETag","\"0\""));}
 @Test void replacesDraftUsingIfMatch() throws Exception{var draft=ModelFixtures.draft();when(service.replace(eq(draft.id()),eq(0L),anyString(),anyList(),any(),any(),any())).thenReturn(draft);String body="{\"source\":\"Synthetic\",\"criteria\":[],\"scale\":null,\"formula\":null}";mvc.perform(put("/model-versions/"+draft.id()).header("If-Match","\"0\"").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk());verify(service).replace(eq(draft.id()),eq(0L),eq("Synthetic"),anyList(),isNull(),isNull(),any());}
}
