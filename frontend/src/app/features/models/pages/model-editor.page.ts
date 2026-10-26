import {ChangeDetectionStrategy,Component,signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {ModelApi} from '../data-access/model-api.service';
import {ApplicabilityInput,ModelCareerApplicability,ModelConfiguration,ModelNode,ModelVersion} from '../data-access/model.models';
import {PublicationPanel} from '../ui/publication-panel.component';
@Component({
 selector:'sec-model-editor',standalone:true,imports:[FormsModule,PublicationPanel],changeDetection:ChangeDetectionStrategy.OnPush,
 template:`<section>
 <h2>Modelo de evaluación versionado</h2>
 <p>Use únicamente datos sintéticos; jerarquías, fuentes, reglas y referencias no constituyen decisiones institucionales.</p>
 <form (ngSubmit)="createModel()">
  <label for="model-name">Nombre</label><input id="model-name" [(ngModel)]="name" name="name" required maxlength="200">
  <label for="model-description">Descripción</label><textarea id="model-description" [(ngModel)]="description" name="description"></textarea>
  <button>Crear modelo</button>
 </form>
 @if(modelId()){
 <form (ngSubmit)="createConfigured()">
  <h3>Versión en borrador completa</h3>
  <label for="source">Fuente</label><textarea id="source" [(ngModel)]="source" name="source" required></textarea>
  <label for="nodes-json">Jerarquía de nodos y tipos (JSON)</label><textarea id="nodes-json" rows="10" [(ngModel)]="nodesJson" name="nodesJson" required></textarea>
  <label for="configuration-json">Indicadores, fuentes y reglas de valoración (JSON)</label><textarea id="configuration-json" rows="16" [(ngModel)]="configurationJson" name="configurationJson" required></textarea>
  <button>Crear borrador configurable</button>
 </form>}
 @if(version();as value){
 <section aria-labelledby="version-title">
  <h3 id="version-title">Versión cargada</h3>
  <p role="status">v{{value.versionNumber}} · {{value.status}} · revisión {{value.revision}} · {{referenceCount(value)}} referencia(s), {{pendingCount(value)}} pendiente(s)</p>
  @if(pendingCount(value)>0){<p role="alert">La activación institucional permanece bloqueada mientras existan referencias PENDING_VALIDATION.</p>}
  @if(value.status==='DRAFT'){<sec-publication-panel [version]="value" [configured]="true" (published)="published($event)"/>}
 </section>}
 @if(version();as value){@if(value.status!=='DRAFT'){
 <section aria-labelledby="applicability-title">
  <h3 id="applicability-title">Aplicabilidad institucional por carrera</h3>
  <p>La publicación no infiere vigencia. Registre una decisión explícita con procedencia.</p>
  <form (ngSubmit)="decide(value)">
   <label for="career-id">Carrera (UUID)</label><input id="career-id" [(ngModel)]="careerId" name="careerId" required>
   <label for="decision">Decisión</label><select id="decision" [(ngModel)]="decision" name="decision"><option value="APPLICABLE">Aplicable</option><option value="NOT_APPLICABLE">No aplicable</option><option value="EXPIRED">Vencida</option></select>
   <label for="valid-from">Vigente desde</label><input id="valid-from" type="date" [(ngModel)]="validFrom" name="validFrom" required>
   <label for="valid-until">Vigente hasta (opcional)</label><input id="valid-until" type="date" [(ngModel)]="validUntil" name="validUntil">
   <label for="authority">Autoridad competente</label><input id="authority" [(ngModel)]="authorityFunction" name="authorityFunction" required>
   <label for="legal-basis">Base legal o decisión</label><input id="legal-basis" [(ngModel)]="legalBasis" name="legalBasis" required>
   <label for="decision-document">Documento de respaldo</label><input id="decision-document" [(ngModel)]="decisionDocument" name="decisionDocument" required>
   <label for="decision-reason">Motivo</label><textarea id="decision-reason" [(ngModel)]="decisionReason" name="decisionReason" required></textarea>
   <button [disabled]="applicabilityBusy()">Registrar decisión</button>
  </form>
  @if(latestApplicability();as latest){<p role="status">Última decisión: {{latest.status}} · revisión {{latest.revision}} · desde {{latest.validFrom}}</p>}
 </section>}}
 @if(message()){<p role="alert">{{message()}}</p>}
 </section>`})
export class ModelEditorPage{
 name='Modelo Piloto Sintético';
 description='Configuración demostrativa sin valor institucional';
 source='Fuente sintética versionada con referencias pendientes';
 nodesJson=JSON.stringify(defaultNodes(),null,2);
 configurationJson=JSON.stringify(defaultConfiguration(),null,2);
 readonly modelId=signal('');
 readonly version=signal<ModelVersion|null>(null);
 readonly message=signal('');
 readonly applicability=signal<readonly ModelCareerApplicability[]>([]);
 readonly applicabilityBusy=signal(false);
 careerId='';
 decision:'APPLICABLE'|'NOT_APPLICABLE'|'EXPIRED'='APPLICABLE';
 validFrom=new Date().toISOString().slice(0,10);
 validUntil='';
 authorityFunction='Dirección de Carrera';
 legalBasis='CL-011 (decisión sintética)';
 decisionDocument='fixture://decisions/cl-011';
 decisionReason='Solo prueba de software';
 constructor(private readonly api:ModelApi){}
 createModel(){this.api.createModel(this.name,this.description).subscribe({next:m=>{this.modelId.set(m.id);this.message.set('Modelo creado.');},error:()=>this.message.set('No fue posible crear el modelo.')});}
 createConfigured(){const nodes=this.parse<readonly ModelNode[]>(this.nodesJson,'jerarquía');const configuration=this.parse<ModelConfiguration>(this.configurationJson,'configuración');if(!nodes||!configuration)return;this.api.createConfiguredVersion(this.modelId(),{source:this.source,nodes,configuration}).subscribe({next:v=>{this.version.set(v);this.message.set('Borrador configurable creado.');},error:()=>this.message.set('No fue posible crear el borrador configurable.')});}
 published(version:ModelVersion){this.version.set(version);this.api.applicability(version.id).subscribe({next:values=>this.applicability.set(values),error:()=>this.applicability.set([])});}
 decide(version:ModelVersion){if(!this.careerId){this.message.set('Indique la carrera.');return;}this.applicabilityBusy.set(true);const revision=this.latestApplicability()?.revision??0;const input:ApplicabilityInput={careerId:this.careerId,decision:this.decision,validFrom:this.validFrom,validUntil:this.validUntil||null,provenance:{authorityFunction:this.authorityFunction,legalBasis:this.legalBasis,documentReference:this.decisionDocument,reason:this.decisionReason}};this.api.decideApplicability(version.id,revision,input).subscribe({next:value=>{this.applicability.set([...this.applicability(),value]);this.applicabilityBusy.set(false);this.message.set('Decisión de aplicabilidad registrada.');},error:error=>{this.applicabilityBusy.set(false);this.message.set(error?.problem?.detail??'No fue posible registrar la decisión.');}});}
 latestApplicability():ModelCareerApplicability|null{const values=this.applicability().filter(value=>value.careerId===this.careerId);return values.length?values[values.length-1]:null;}
 referenceCount(version:ModelVersion){return version.configuration?.indicators.reduce((total,indicator)=>total+(indicator.normativeReferences?.length??0),0)??0;}
 pendingCount(version:ModelVersion){return version.configuration?.indicators.reduce((total,indicator)=>total+(indicator.normativeReferences?.filter(reference=>reference.verificationStatus==='PENDING_VALIDATION').length??0),0)??0;}
 private parse<T>(value:string,label:string):T|null{try{return JSON.parse(value) as T;}catch{this.message.set('El JSON de '+label+' no es válido.');return null;}}
}
function defaultNodes():readonly ModelNode[]{return[{id:'62000000-0000-0000-0000-000000001020',code:'C-1',name:'Criterio sintético',description:'',position:1,nodeType:'CRITERION',children:[{id:'62000000-0000-0000-0000-000000001010',code:'SC-1',name:'Subcriterio sintético',description:'',position:1,nodeType:'SUBCRITERION',children:[{id:'62000000-0000-0000-0000-000000001001',code:'I-CUAL',name:'Indicador cualitativo sintético',description:'',position:1,nodeType:'INDICATOR',children:[]},{id:'62000000-0000-0000-0000-000000001002',code:'I-CUAN',name:'Indicador cuantitativo sintético',description:'',position:2,nodeType:'INDICATOR',children:[]}]}]}];}
function defaultConfiguration():ModelConfiguration{return{scales:[{id:'62000000-0000-0000-0000-000000001030',code:'ESC-CUAL',name:'Escala sintética',options:[{id:'62000000-0000-0000-0000-000000001031',code:'DECLARADO',label:'Declarado',position:1,declaredValue:1}]}],indicators:[{nodeId:'62000000-0000-0000-0000-000000001001',nature:'QUALITATIVE',standardText:'Estándar cualitativo sintético',fundamentalElements:['Elemento sintético'],evaluationPeriodDefinition:'Ciclo sintético 2026',ratingScaleId:'62000000-0000-0000-0000-000000001030',valuationRuleId:'62000000-0000-0000-0000-000000001050',sources:[{id:'62000000-0000-0000-0000-000000001060',indicatorNodeId:'62000000-0000-0000-0000-000000001001',code:'F-1',sourceType:'FILE',name:'Fuente sintética F-1',description:'No representa requisitos normativos oficiales',required:true,cardinalityMin:1,cardinalityMax:null,periodRule:'Período sintético 2026',ordinal:1}],normativeReferences:[{id:'62000000-0000-0000-0000-000000002001',title:'Referencia sintética pendiente',issuer:null,edition:null,documentReference:'fixture://normative/pending',documentSha256:null,pages:null,disposition:null,verificationStatus:'PENDING_VALIDATION',verifiedBy:null,verifiedAt:null}]},{nodeId:'62000000-0000-0000-0000-000000001002',nature:'QUANTITATIVE',standardText:'Estándar cuantitativo sintético',fundamentalElements:[],evaluationPeriodDefinition:'Año sintético 2026',ratingScaleId:null,valuationRuleId:'62000000-0000-0000-0000-000000001051',sources:[{id:'62000000-0000-0000-0000-000000001062',indicatorNodeId:'62000000-0000-0000-0000-000000001002',code:'F-1',sourceType:'STRUCTURED_DATA',name:'Fuente sintética F-1',description:'No representa requisitos normativos oficiales',required:true,cardinalityMin:1,cardinalityMax:null,periodRule:'Período sintético 2026',ordinal:1}],normativeReferences:[{id:'62000000-0000-0000-0000-000000002002',title:'Referencia sintética pendiente',issuer:null,edition:null,documentReference:'fixture://normative/pending-2',documentSha256:null,pages:null,disposition:null,verificationStatus:'PENDING_VALIDATION',verifiedBy:null,verifiedAt:null}]}],rules:[{id:'62000000-0000-0000-0000-000000001050',indicatorNodeId:'62000000-0000-0000-0000-000000001001',ruleType:'MANUAL_QUALITATIVE',methodDefinition:'Valoración cualitativa sintética',formulaDefinition:null,strategyKey:null,strategyVersion:null,scaleId:'62000000-0000-0000-0000-000000001030',variables:[],thresholds:[],missingDataPolicy:'EXCLUDE',status:'DRAFT'},{id:'62000000-0000-0000-0000-000000001051',indicatorNodeId:'62000000-0000-0000-0000-000000001002',ruleType:'QUANTITATIVE',methodDefinition:'Valoración cuantitativa sintética',formulaDefinition:'metadata -> data.valor',strategyKey:'synthetic',strategyVersion:'1',scaleId:null,variables:[{code:'VALOR',name:'Valor sintético',unit:'unidad',dataType:'DECIMAL',required:true,validationDefinition:'0..1'}],thresholds:[],missingDataPolicy:'EXCLUDE',status:'DRAFT'}]};}
