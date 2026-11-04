package ec.edu.unl.sec.evaluationmodel.support;

import ec.edu.unl.sec.evaluationmodel.domain.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ModelFixtures {
    public static final UUID MODEL=UUID.fromString("10000000-0000-0000-0000-000000000001"),AUTHOR=UUID.fromString("10000000-0000-0000-0000-000000000002"),APPROVER=UUID.fromString("10000000-0000-0000-0000-000000000003");
    private ModelFixtures(){}
    public static List<ModelNode> criteria(){ModelNode indicator=new ModelNode(null," IND-1 ","Indicador sintético","",1,List.of());ModelNode standard=new ModelNode(null,"EST-1","Estándar sintético","",1,List.of(indicator));return List.of(new ModelNode(null,"CRI-1","Criterio sintético","",1,List.of(standard)));}
    public static RatingScale scale(){return new RatingScale(null,"ESC-1","Escala sintética",List.of(new RatingScale.Option(null,"OK","Declarado",1,BigDecimal.ONE)));}
    public static ProgressFormula formula(){return new ProgressFormula("FORM-1","sec.progress/v1",List.of("progress"),List.of("IND-1"),"ZERO","EXCLUDE",List.of(new ProgressFormula.Example(Map.of("IND-1",1),Map.of("progress",1))),Map.of());}
    public static ModelVersion draft(){return new ModelVersion(null,MODEL,1,ModelVersionStatus.DRAFT,"Fuente sintética",AUTHOR,null,null,0,Instant.parse("2026-01-01T00:00:00Z"),null,criteria(),scale(),formula());}
}
