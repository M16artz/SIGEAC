package ec.edu.unl.sec.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.unl.sec.evaluationmodel.domain.*;
import ec.edu.unl.sec.evaluationmodel.infrastructure.JdbcEvaluationModelRepository;
import ec.edu.unl.sec.identityaccess.infrastructure.SyntheticAccessData;
import ec.edu.unl.sec.selfassessment.domain.ProcessState;
import ec.edu.unl.sec.selfassessment.domain.SelfAssessmentProcess;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Test-classpath-only four-source fixture; never rewrites an existing published model. */
@Component
@Profile("d7-manual-test")
@Order(3)
public class D7ManualEvaluationData implements ApplicationRunner {
    public static final UUID PROCESS=UUID.fromString("d7000000-0000-0000-0000-000000000003");
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    public D7ManualEvaluationData(JdbcTemplate jdbc,ObjectMapper json) { this.jdbc=jdbc;this.json=json; }
    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (jdbc.queryForObject("select count(*) from self_assessment_process where id=?",Long.class,PROCESS)>0) return;
        var original=new JdbcEvaluationModelRepository(jdbc,json).findVersion(UUID.fromString("60000000-0000-0000-0000-000000000002")).orElseThrow();
        var model=UUID.fromString("d7000000-0000-0000-0000-000000000001");
        var versionId=UUID.fromString("d7000000-0000-0000-0000-000000000002");
        var admin=user("admin");var coordinator=user("coordinador");var responsible=user("responsable");
        var definitions=new ArrayList<IndicatorDefinition>();
        var scale=new RatingScale(UUID.fromString("d7000000-0000-0000-0000-000000000010"),"D7-SCALE","Escala cualitativa sintética",
                List.of(new RatingScale.Option(UUID.fromString("d7000000-0000-0000-0000-000000000011"),"SATISFIES","Satisface el estándar sintético",1,null),
                        new RatingScale.Option(UUID.fromString("d7000000-0000-0000-0000-000000000012"),"PARTIAL","Satisfacción parcial sintética",2,null)));
        var rules=new ArrayList<ValuationRule>();
        int ordinal=0;
        for (var indicator:original.configuration().indicators()) {
            var sources=new ArrayList<RequiredSource>();
            for (int position=1;position<=2;position++) {
                ordinal++;
                var id=UUID.fromString("d7000000-0000-0000-0000-00000000010"+ordinal);
                sources.add(new RequiredSource(id,indicator.nodeId(),"D7-SRC-"+ordinal,RequiredSource.SourceType.EXTERNAL_LINK,
                        "Fuente documental sintética "+ordinal,"Referencia de prueba de software D7",true,1,null,"Período sintético 2026",position));
            }
            var rule=new ValuationRule(UUID.nameUUIDFromBytes(("d7-rule:"+indicator.nodeId()).getBytes(StandardCharsets.UTF_8)),indicator.nodeId(),
                    ValuationRule.Type.MANUAL_QUALITATIVE,"Juicio cualitativo sintético sustentado en evidencia aceptada",null,null,null,scale.id(),
                    List.of(),List.of(),"Declarar limitaciones explícitas",ValuationRule.Status.VALIDATED);
            boolean quantitative=indicator.nodeId().equals(ec.edu.unl.sec.selfassessment.infrastructure.SyntheticPilotData.INDICATOR_B_ID);
            if(quantitative) rule=new ValuationRule(rule.id(),indicator.nodeId(),ValuationRule.Type.QUANTITATIVE,
                    "Razón porcentual sintética con cifras verificadas","A / B * 100","ratio-percent","1",scale.id(),
                    List.of(new ValuationVariable("A","Cantidad A","personas sintéticas",ValuationVariable.DataType.INTEGER,true,"NON_NEGATIVE"),
                            new ValuationVariable("B","Cantidad B","personas sintéticas",ValuationVariable.DataType.INTEGER,true,"POSITIVE")),
                    List.of(new ValuationThreshold(scale.options().get(1).id(),java.math.BigDecimal.ZERO,new java.math.BigDecimal("50"),true,false),
                            new ValuationThreshold(scale.options().getFirst().id(),new java.math.BigDecimal("50"),null,true,false)),"REJECT",ValuationRule.Status.VALIDATED);
            rules.add(rule);
            definitions.add(new IndicatorDefinition(indicator.nodeId(),quantitative?IndicatorDefinition.Nature.QUANTITATIVE:IndicatorDefinition.Nature.QUALITATIVE,indicator.standardText(),indicator.fundamentalElements(),
                    indicator.evaluationPeriodDefinition(),scale.id(),rule.id(),sources,indicator.normativeReferences()));
        }
        if (ordinal!=4) throw new IllegalStateException("D7 acceptance requires exactly four synthetic sources");
        var now=Instant.now();
        var version=new ModelVersion(versionId,model,1,ModelVersionStatus.PUBLISHED,"fixture://d7-manual-evaluation",admin,coordinator,null,0,
                now,now,original.criteria(),scale,original.formula(),new ModelConfiguration(definitions,List.of(scale),rules));
        jdbc.update("insert into evaluation_model(id,name,description,created_by,created_at) values(?,?,?,?,?)",model,
                "Modelo sintético D7","Configuración de cuatro fuentes para aceptación de software.",admin,Timestamp.from(now));
        jdbc.update("insert into model_version(id,model_id,version_number,status,source,author_id,approver_id,revision,created_at,published_at,snapshot_json)"
                        +" values(?,?,1,'PUBLISHED',?,?,?,0,?,?,?)",versionId,model,version.source(),admin,coordinator,Timestamp.from(now),Timestamp.from(now),json.writeValueAsString(version));
        var indicators=definitions.stream().map(indicator -> new SelfAssessmentProcess.IndicatorSnapshot(indicator.nodeId(),
                indicator.nodeId().equals(ec.edu.unl.sec.selfassessment.infrastructure.SyntheticPilotData.INDICATOR_A_ID)?"IND-SQL-A":"IND-SQL-B","Indicador sintético D7")).toList();
        var process=new SelfAssessmentProcess(PROCESS,SyntheticAccessData.PILOT_CAREER,"D7-MVP","Proceso sintético D7 documental",
                LocalDate.of(2026,1,1),LocalDate.of(2026,12,31),versionId,1,ProcessState.ACTIVE,1,admin,now,indicators);
        jdbc.update("insert into self_assessment_process(id,career_id,code,name,period_start,period_end,model_version_id,model_version_number,state,revision,created_by,created_at,snapshot_json)"
                        +" values(?,?,?,?,?,?,?,1,'ACTIVE',1,?,?,?)",PROCESS,process.careerId(),process.code(),process.name(),process.periodStart(),process.periodEnd(),versionId,admin,Timestamp.from(now),json.writeValueAsString(process));
        for (var indicator:indicators) {
            jdbc.update("insert into process_indicator(process_id,indicator_id,code,name) values(?,?,?,?)",PROCESS,indicator.id(),indicator.code(),indicator.name());
            jdbc.update("insert into process_assignment(id,process_id,indicator_id,identity_id,function_code,valid_from,valid_to,created_by,reason,revision) values(?,?,?,?,'RESPONSIBLE',?,?,?,'Asignación sintética D7',0)",
                    UUID.randomUUID(),PROCESS,indicator.id(),responsible,Timestamp.from(Instant.parse("2020-01-01T00:00:00Z")),Timestamp.from(Instant.parse("2030-01-01T00:00:00Z")),admin);
        }
    }
    private static UUID user(String login) { return UUID.nameUUIDFromBytes(("sec-unl:"+login).getBytes(StandardCharsets.UTF_8)); }
}
