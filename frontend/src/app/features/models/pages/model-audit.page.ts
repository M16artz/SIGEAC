import {ChangeDetectionStrategy,Component,signal} from '@angular/core';import {FormsModule} from '@angular/forms';import {DatePipe} from '@angular/common';import {ModelApi} from '../data-access/model-api.service';
import {AuditEventView} from '../../access/data-access/access.models';
import {DisplayLabelPipe} from '../../../shared/labels/display-label.pipe';
@Component({selector:'sec-model-audit',standalone:true,imports:[FormsModule,DatePipe,DisplayLabelPipe],changeDetection:ChangeDetectionStrategy.OnPush,template:`
<div class="sigeac-page">
 <header class="sigeac-page-header">
  <span class="sigeac-badge">Modelos</span>
  <h2>Auditoría de modelos</h2>
  <p>Consulte los eventos registrados de los modelos y sus versiones.</p>
 </header>
 <section class="sigeac-page-body">
  <div class="sigeac-form-search"><div class="sigeac-form-field"><label for="audit-version">Versión opcional</label><input id="audit-version" [(ngModel)]="versionId"></div><button (click)="load()">Consultar</button></div><ol>@for(event of events();track event.id){<li>{{event.occurredAt | date:'dd/MM/yyyy HH:mm'}} · {{event.action | displayLabel}} · {{event.objectType | displayLabel}} · {{event.objectId}} · {{event.outcome | displayLabel}} · Motivo: {{event.reasonCode | displayLabel}} · Correlación: {{event.correlationId}}</li>}</ol>
 </section>
</div>`})export class ModelAuditPage{versionId='';readonly events=signal<readonly AuditEventView[]>([]);constructor(private readonly api:ModelApi){}load(){this.api.audit(this.versionId||undefined).subscribe(v=>this.events.set(v.items))}}
