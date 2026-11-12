import {ChangeDetectionStrategy,Component,signal} from '@angular/core';import {FormsModule} from '@angular/forms';import {ModelApi} from '../data-access/model-api.service';import {ModelComparison} from '../data-access/model.models';
import {DisplayLabelPipe} from '../../../shared/labels/display-label.pipe';
@Component({selector:'sec-model-compare',standalone:true,imports:[FormsModule,DisplayLabelPipe],changeDetection:ChangeDetectionStrategy.OnPush,template:`
<div class="sigeac-page">
 <header class="sigeac-page-header">
  <span class="sigeac-badge">Modelos</span>
  <h2>Comparar versiones</h2>
  <p>Identifique los cambios entre dos versiones del modelo de evaluación.</p>
 </header>
 <section class="sigeac-page-body">
  <div class="sigeac-form-layout"><div class="sigeac-form-grid">
   <div class="sigeac-form-field"><label for="from">Desde</label><input id="from" [(ngModel)]="from"></div>
   <div class="sigeac-form-field"><label for="to">Hasta</label><input id="to" [(ngModel)]="to"></div>
   <div class="sigeac-form-actions"><button (click)="compare()">Comparar</button></div>
  </div></div>
  <ul>@for(c of result()?.changes??[];track c.key){<li>{{c.kind | displayLabel}} · {{c.key}}</li>}</ul>
 </section>
</div>`})export class ModelComparePage{from='';to='';readonly result=signal<ModelComparison|null>(null);constructor(private readonly api:ModelApi){}compare(){this.api.compare(this.from,this.to).subscribe(v=>this.result.set(v))}}
