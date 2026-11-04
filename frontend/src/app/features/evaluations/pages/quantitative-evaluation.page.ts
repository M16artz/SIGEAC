import {Component, ChangeDetectionStrategy, OnInit, OnDestroy, inject, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {DatePipe} from '@angular/common';
import {RouterLink} from '@angular/router';
import {Observable, Subscription, switchMap} from 'rxjs';
import {ApiProblemError} from '../../../core/http/api-client';
import {ProcessApi, ProcessView} from '../../processes/data-access/process-api.service';
import {CaptureVariables, QuantitativeApi, QuantitativeView} from '../data-access/quantitative-api.service';

@Component({selector: 'sec-quantitative-evaluation', standalone: true, imports: [FormsModule, DatePipe, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: [`select, textarea, input[type=date] {display:block; margin:.4rem 0 1rem; max-width:100%} textarea {width:100%;min-height:5rem}
    article {border-top:1px solid #ccc; padding:1rem 0; overflow-wrap:anywhere} fieldset {margin:1rem 0} fieldset label {display:block;margin:.5rem 0}
    nav {background:transparent;color:inherit;padding:1rem 0} nav span {margin:0 .5rem}`],
  template: `
    <section aria-labelledby="quantitative-title">
      <h2 id="quantitative-title">Variables y cálculo cuantitativo</h2>
      <p><a routerLink="/evaluations">Consultar valoración cualitativa</a></p>
      <p>Capture cifras con sus sustentos, verifíquelas y ejecute la regla publicada. El resultado no constituye acreditación.</p>
      <label for="quant-process">Proceso</label>
      <select id="quant-process" [ngModel]="processId" (ngModelChange)="selectProcess($event)" [disabled]="busy()">
        <option value="">Seleccione un proceso</option>
        @for (p of processes(); track p.id) {<option [value]="p.id">{{p.code}} · {{p.name}}</option>}
      </select>
      <label for="quant-indicator">Indicador</label>
      <select id="quant-indicator" [ngModel]="indicatorId" (ngModelChange)="selectIndicator($event)" [disabled]="busy()">
        <option value="">Seleccione un indicador</option>
        @for (i of indicators(); track i.id) {<option [value]="i.id">{{i.code}} · {{i.name}}</option>}
      </select>
      @if (error()) {<p role="alert">{{error()}}</p>}
      @if (message()) {<p role="status">{{message()}}</p>}
      @if (processId && indicatorId) {<button type="button" (click)="load()" [disabled]="busy()">Recargar variables</button>}
      @if (view(); as v) {
        <p>Método: {{v.rule.methodDefinition}}</p>
        <p>Fórmula publicada: {{v.rule.formulaDefinition}} · Estrategia: {{v.rule.strategyKey}} / {{v.rule.strategyVersion}}</p>
        @if (editable(v)) {
          <form (ngSubmit)="capture()">
            <label for="quant-period-start">Inicio del período de variables</label>
            <input id="quant-period-start" name="start" type="date" [(ngModel)]="periodStart" [min]="v.periodStart" [max]="v.periodEnd" required [disabled]="busy()" />
            <label for="quant-period-end">Fin del período de variables</label>
            <input id="quant-period-end" name="end" type="date" [(ngModel)]="periodEnd" [min]="v.periodStart" [max]="v.periodEnd" required [disabled]="busy()" />
            @for (variable of v.rule.variables; track variable.code) {
              <fieldset [disabled]="busy()">
                <legend>{{variable.code}} · {{variable.name}} · {{variable.unit}}</legend>
                <label [for]="'quant-value-' + variable.code">Valor {{variable.code}}</label>
                <input [id]="'quant-value-' + variable.code" [name]="variable.code" [(ngModel)]="values[variable.code]" inputmode="decimal" maxlength="19" required />
                <p>Tipo: {{variable.dataType}} · Restricción publicada: {{variable.validationDefinition}}</p>
                <p>Sustentos aceptados de esta variable:</p>
                @for (version of versions(); track version.id) {
                  <label><input type="checkbox" [checked]="selected[variable.code]?.includes(version.id)" (change)="toggle(variable.code, version.id)" /> {{version.title}} · {{version.id}}</label>
                } @empty {<p>Sin sustentos documentales aceptados.</p>}
              </fieldset>
            }
            <button type="submit" [disabled]="busy() || !canCapture(v)">Conservar nueva revisión de variables</button>
          </form>
        } @else {<p>El estado del proceso permite únicamente consultar el historial.</p>}
        @if (v.ownInputs; as inputs) {
          <article>
            <h3>Variables conservadas · revisión {{inputs.revision}}</h3>
            <p>{{inputs.periodStart}} a {{inputs.periodEnd}} · {{inputs.capturedAt | date:'medium'}}</p>
            @for (variable of inputs.variables; track variable.code) {<p>{{variable.code}} = {{variable.value}} {{variable.unit}}</p>}
            @if (v.verification; as verification) {<p>Verificadas por {{verification.verifiedBy}} · {{verification.reason}}</p>}
            @else if (editable(v)) {
              <form (ngSubmit)="verify()">
                <label for="quant-verification-reason">Fundamento de la verificación</label>
                <textarea id="quant-verification-reason" name="verification" [(ngModel)]="reason" required minlength="10" maxlength="4000" [disabled]="busy()"></textarea>
                <button type="submit" [disabled]="busy() || reason.trim().length < 10">Verificar cifras, fuentes y período</button>
              </form>
            }
            @if (editable(v) && v.verification && !alreadyCalculated(v)) {
              <form (ngSubmit)="calculate()">
                <label for="quant-justification">Justificación del resultado</label>
                <textarea id="quant-justification" name="justification" [(ngModel)]="justification" required minlength="10" maxlength="4000" [disabled]="busy()"></textarea>
                <label for="quant-limitations">Limitaciones del cálculo</label>
                <textarea id="quant-limitations" name="limitations" [(ngModel)]="limitations" required minlength="10" maxlength="4000" [disabled]="busy()"></textarea>
                <button type="submit" [disabled]="busy() || justification.trim().length < 10 || limitations.trim().length < 10">Calcular y conservar resultado</button>
              </form>
            }
          </article>
        }
        <h3>Cálculos conservados</h3>
        @for (run of v.items; track run.id) {
          <article>
            <h4>Resultado: {{run.result}} % · {{run.ratingLabel}}</h4>
            <p>{{run.executedAt | date:'medium'}} · {{run.executedBy}}</p>
            <p>Fórmula: {{run.rule.formulaDefinition}} · {{run.rule.strategyKey}} / {{run.rule.strategyVersion}} · {{run.arithmetic}}</p>
            <p>Umbral conservado: {{run.threshold.lowerBound ?? 'sin límite inferior'}} a {{run.threshold.upperBound ?? 'sin límite superior'}}.
              Inclusión inferior: {{run.threshold.lowerInclusive}} · superior: {{run.threshold.upperInclusive}}</p>
            <p>{{run.justification}}</p><p>Limitaciones: {{run.limitations}}</p>
            @for (variable of run.inputs.variables; track variable.code) {
              <p>{{variable.code}} = {{variable.value}} {{variable.unit}} · {{run.inputs.periodStart}} a {{run.inputs.periodEnd}}</p>
              <ul>@for (support of variable.supports; track support.requiredSourceId + support.evidenceVersionId) {
                <li>{{support.title}} · Versión: {{support.evidenceVersionId}} · Decisión: {{support.reviewDecisionId}}</li>
              }</ul>
            }
            <p>Verificación: {{run.verification.verifiedBy}} · {{run.verification.reason}}</p>
          </article>
        } @empty {<p>No hay cálculos registrados.</p>}
        <nav aria-label="Páginas de cálculos">
          <button type="button" (click)="load(v.page - 1)" [disabled]="busy() || v.page === 0">Anterior</button>
          <span>Página {{v.page + 1}}</span><button type="button" (click)="load(v.page + 1)" [disabled]="busy() || !v.hasNext">Siguiente</button>
        </nav>
      }
    </section>`})
export class QuantitativeEvaluationPage implements OnInit, OnDestroy {
  private readonly api = inject(QuantitativeApi); private readonly processApi = inject(ProcessApi);
  private request?: Subscription; private listing?: Subscription;
  readonly processes = signal<readonly ProcessView[]>([]); readonly indicators = signal<ProcessView['indicators']>([]);
  readonly view = signal<QuantitativeView | null>(null); readonly versions = signal<readonly {id: string; title: string}[]>([]);
  readonly busy = signal(false); readonly error = signal(''); readonly message = signal('');
  processId = ''; indicatorId = ''; periodStart = ''; periodEnd = ''; reason = ''; justification = ''; limitations = '';
  values: Record<string, string> = {}; selected: Record<string, string[]> = {};
  ngOnInit(): void {this.listing = this.processApi.list().subscribe({next: p => this.processes.set(p), error: e => this.fail(e)});}
  ngOnDestroy(): void {this.listing?.unsubscribe(); this.request?.unsubscribe();}
  selectProcess(id: string): void {this.reset(); this.processId = id; this.indicatorId = ''; this.indicators.set(this.processes().find(p => p.id === id)?.indicators ?? []);}
  selectIndicator(id: string): void {this.reset(); this.indicatorId = id; if (id) this.load();}
  load(page = 0): void {this.request?.unsubscribe(); this.view.set(null); this.busy.set(true); this.error.set(''); this.request = this.api.view(this.processId, this.indicatorId, page).subscribe({next: v => this.accept(v), error: e => this.fail(e)});}
  toggle(code: string, id: string): void {const prior = this.selected[code] ?? []; this.selected[code] = prior.includes(id) ? prior.filter(v => v !== id) : [...prior, id];}
  editable(v: QuantitativeView): boolean {return ['ACTIVE', 'DOCUMENTARY_CLOSED', 'REOPENED'].includes(v.processState);}
  canCapture(v: QuantitativeView): boolean {return !!this.periodStart && !!this.periodEnd && v.rule.variables.every(x => !!this.values[x.code] && (this.selected[x.code]?.length ?? 0) > 0);}
  alreadyCalculated(v: QuantitativeView): boolean {return v.items.some(r => r.inputs.id === v.ownInputs?.id);}
  capture(): void {
    const v = this.view(); if (!v || this.busy() || !this.editable(v) || !this.canCapture(v)) return;
    const variables: CaptureVariables['variables'] = {};
    for (const definition of v.rule.variables) variables[definition.code] = {value: this.values[definition.code].trim(), unit: definition.unit, evidenceVersionIds: this.selected[definition.code]};
    this.perform(this.api.capture(v, {periodStart: this.periodStart, periodEnd: this.periodEnd, variables, predecessorId: v.ownInputs?.id ?? null}), 'Nueva revisión de variables conservada; requiere verificación.');
  }
  verify(): void {const v = this.view(); if (v?.ownInputs && !v.verification && this.editable(v) && !this.busy() && this.reason.trim().length >= 10) this.perform(this.api.verify(v, this.reason.trim()), 'Verificación de variables conservada.');}
  calculate(): void {const v = this.view(); if (v?.verification && this.editable(v) && !this.busy() && !this.alreadyCalculated(v) && this.justification.trim().length >= 10 && this.limitations.trim().length >= 10) this.perform(this.api.calculate(v, this.justification.trim(), this.limitations.trim()), 'Cálculo y fundamentos conservados.');}
  private perform(operation: Observable<unknown>, message: string): void {
    this.busy.set(true); this.error.set(''); this.message.set(''); this.request?.unsubscribe();
    this.request = operation.pipe(switchMap(() => this.api.view(this.processId, this.indicatorId))).subscribe({next: v => {this.accept(v); this.message.set(message);}, error: e => this.fail(e)});
  }
  private accept(v: QuantitativeView): void {
    this.view.set(v); this.busy.set(false);
    this.periodStart ||= v.periodStart; this.periodEnd ||= v.periodEnd;
    this.versions.set([...new Map(v.acceptedSupports.map(s => [s.evidenceVersionId, {id: s.evidenceVersionId, title: s.title}])).values()]);
    for (const variable of v.rule.variables) this.selected[variable.code] = (this.selected[variable.code] ?? []).filter(id => this.versions().some(v => v.id === id));
  }
  private reset(): void {this.request?.unsubscribe(); this.view.set(null); this.versions.set([]); this.busy.set(false); this.error.set(''); this.message.set(''); this.values = {}; this.selected = {}; this.periodStart = ''; this.periodEnd = ''; this.reason = ''; this.justification = ''; this.limitations = '';}
  private fail(e: unknown): void {this.view.set(null); this.versions.set([]); this.busy.set(false); this.error.set(e instanceof ApiProblemError ? e.message : 'No se completó la operación. Recargue y consulte el historial antes de repetirla.');}
}
