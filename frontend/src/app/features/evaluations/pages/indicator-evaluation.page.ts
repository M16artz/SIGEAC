import {DatePipe} from '@angular/common';
import {ChangeDetectionStrategy, Component, OnDestroy, OnInit, inject, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {Subscription, switchMap} from 'rxjs';
import {ApiProblemError} from '../../../core/http/api-client';
import {ProcessApi, ProcessView} from '../../processes/data-access/process-api.service';
import {AcademicEvaluationApi, ManualEvaluationView} from '../data-access/academic-evaluation-api.service';

@Component({
  selector: 'sec-indicator-evaluation-page',
  standalone: true,
  imports: [FormsModule, DatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: [`
    section { max-width: 70rem; }
    select, textarea { display: block; margin: .4rem 0 1rem; max-width: 100%; }
    textarea { width: 100%; min-height: 6rem; box-sizing: border-box; }
    fieldset { margin: 1rem 0; } fieldset label { display: block; margin: .5rem 0; overflow-wrap: anywhere; }
    article { border-top: 1px solid #ccc; padding: .5rem 0; overflow-wrap: anywhere; }
    nav { background: transparent; padding: 1rem 0; color: inherit; } nav span { margin: 0 .5rem; }
  `],
  template: `
    <section aria-labelledby="evaluation-title">
      <h2 id="evaluation-title">Valoración académica manual</h2>
      <p>La aceptación documental habilita sustentos; el juicio académico requiere una valoración independiente.
        Este registro apoya la autoevaluación y no constituye acreditación.</p>
      <label for="evaluation-process">Proceso</label>
      <select id="evaluation-process" [ngModel]="processId" (ngModelChange)="selectProcess($event)" [disabled]="saving()">
        <option value="">Seleccione un proceso</option>
        @for (p of processes(); track p.id) { <option [value]="p.id">{{p.code}} · {{p.name}}</option> }
      </select>
      <label for="evaluation-indicator">Indicador</label>
      <select id="evaluation-indicator" [ngModel]="indicatorId" (ngModelChange)="selectIndicator($event)" [disabled]="saving()">
        <option value="">Seleccione un indicador</option>
        @for (i of indicators(); track i.id) { <option [value]="i.id">{{i.code}} · {{i.name}}</option> }
      </select>
      @if (loading()) { <p role="status">Consultando valoraciones…</p> }
      @if (error()) { <p role="alert">{{error()}}</p> }
      @if (message()) { <p role="status">{{message()}}</p> }
      @if (processId && indicatorId) { <button type="button" (click)="load()" [disabled]="loading() || saving()">Recargar valoración</button> }
      @if (view(); as v) {
        <p>Método de la versión publicada: {{v.method}}</p>
        <p>Modelo: {{v.modelVersionId}} · Estado del proceso: {{v.processState}}</p>
        @if (editable(v)) {
          <form (ngSubmit)="save()">
            <label for="evaluation-rating">Resultado según escala del indicador</label>
            <select id="evaluation-rating" name="rating" [(ngModel)]="ratingOptionId" required [disabled]="saving()">
              <option value="">Seleccione una opción</option>
              @for (option of v.ratingOptions; track option.id) { <option [value]="option.id">{{option.label}}</option> }
            </select>
            <fieldset [disabled]="saving()">
              <legend>Versiones aceptadas que sustentan este juicio</legend>
              @for (version of versions(); track version) {
                <label><input type="checkbox" [checked]="selected.includes(version)" (change)="toggle(version)" /> {{supportTitle(v, version)}} · {{version}}</label>
              } @empty { <p>No hay sustentos documentales aceptados y vigentes.</p> }
            </fieldset>
            <label for="evaluation-justification">Justificación</label>
            <textarea id="evaluation-justification" name="justification" [(ngModel)]="justification" required minlength="10" maxlength="4000" [disabled]="saving()"></textarea>
            <label for="evaluation-limitations">Limitaciones y método aplicado</label>
            <textarea id="evaluation-limitations" name="limitations" [(ngModel)]="limitations" required minlength="10" maxlength="4000" [disabled]="saving()"></textarea>
            <p>Revisión propia: {{v.ownRevision}}. Guardar conserva el juicio anterior y crea una nueva revisión propia.</p>
            <button type="submit" [disabled]="saving() || !valid()">Guardar valoración manual</button>
          </form>
        } @else { <p>El proceso permite únicamente consultar el historial académico.</p> }
        <h3>Historial de juicios conservados</h3>
        @for (item of v.items; track item.id) {
          <article>
            <h4>{{item.ratingLabel}} · revisión {{item.revision}}</h4>
            <p>{{item.evaluatedAt | date:'medium'}} · Evaluador: {{item.evaluatorId}}</p>
            <p>{{item.justification}}</p><p>Limitaciones: {{item.limitations}}</p>
            <p>Método conservado: {{item.method}}</p>
            @if (item.predecessorId) { <p>Juicio anterior: {{item.predecessorId}}</p> }
            <ul>
              @for (support of item.supports; track support.requiredSourceId + support.evidenceVersionId) {
                <li>{{support.title}} · Versión: {{support.evidenceVersionId}} · Fuente: {{support.requiredSourceId}} · Decisión: {{support.reviewDecisionId}}</li>
              }
            </ul>
          </article>
        } @empty { <p>Sin valoraciones registradas.</p> }
        <nav aria-label="Páginas de valoraciones">
          <button type="button" (click)="load(v.page - 1)" [disabled]="v.page === 0 || saving()">Anterior</button>
          <span>Página {{v.page + 1}}</span>
          <button type="button" (click)="load(v.page + 1)" [disabled]="!v.hasNext || saving()">Siguiente</button>
        </nav>
      }
    </section>
  `
})
export class IndicatorEvaluationPage implements OnInit, OnDestroy {
  private readonly api = inject(AcademicEvaluationApi);
  private readonly processApi = inject(ProcessApi);
  private request?: Subscription;
  private listing?: Subscription;
  readonly processes = signal<readonly ProcessView[]>([]);
  readonly indicators = signal<ProcessView['indicators']>([]);
  readonly view = signal<ManualEvaluationView | null>(null);
  readonly versions = signal<readonly string[]>([]);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly message = signal('');
  processId = ''; indicatorId = ''; ratingOptionId = ''; justification = ''; limitations = '';
  selected: string[] = [];
  ngOnInit(): void {
    this.listing = this.processApi.list().subscribe({next: rows => this.processes.set(rows), error: e => this.fail(e)});
  }
  ngOnDestroy(): void { this.request?.unsubscribe(); this.listing?.unsubscribe(); }
  selectProcess(id: string): void {
    this.request?.unsubscribe(); this.processId = id; this.indicatorId = '';
    this.indicators.set(this.processes().find(p => p.id === id)?.indicators ?? []); this.clear();
  }
  selectIndicator(id: string): void { this.indicatorId = id; this.clear(); if (id) this.load(); }
  load(page = 0): void {
    this.request?.unsubscribe(); this.view.set(null); this.versions.set([]); this.error.set(''); this.loading.set(true);
    this.request = this.api.view(this.processId, this.indicatorId, page).subscribe({
      next: v => { this.accept(v); this.loading.set(false); }, error: e => this.fail(e)
    });
  }
  toggle(version: string): void { this.selected = this.selected.includes(version) ? this.selected.filter(v => v !== version) : [...this.selected, version]; }
  supportTitle(view: ManualEvaluationView, version: string): string { return view.acceptedSupports.find(s => s.evidenceVersionId === version)?.title ?? version; }
  editable(v: ManualEvaluationView): boolean { return ['ACTIVE', 'DOCUMENTARY_CLOSED', 'REOPENED'].includes(v.processState); }
  valid(): boolean { return !!this.ratingOptionId && this.selected.length > 0 && this.justification.trim().length >= 10 && this.limitations.trim().length >= 10; }
  save(): void {
    const v = this.view();
    if (!v || !this.valid() || this.saving() || !this.editable(v)) return;
    this.saving.set(true); this.error.set(''); this.message.set('');
    this.request?.unsubscribe();
    this.request = this.api.save(v, {ratingOptionId: this.ratingOptionId, evidenceVersionIds: this.selected,
      justification: this.justification.trim(), limitations: this.limitations.trim(), predecessorId: v.ownLatestId}).pipe(
      switchMap(() => this.api.view(v.processId, v.indicatorId))
    ).subscribe({next: updated => { this.accept(updated); this.saving.set(false); this.message.set('Valoración conservada con sus sustentos y método.'); }, error: e => this.fail(e)});
  }
  private accept(v: ManualEvaluationView): void {
    this.view.set(v); this.versions.set([...new Set(v.acceptedSupports.map(s => s.evidenceVersionId))]);
    this.selected = this.selected.filter(id => this.versions().includes(id));
  }
  private clear(): void {
    this.request?.unsubscribe(); this.view.set(null); this.versions.set([]); this.selected = [];
    this.ratingOptionId = ''; this.justification = ''; this.limitations = '';
    this.error.set(''); this.message.set(''); this.loading.set(false);
  }
  private fail(error: unknown): void {
    this.view.set(null); this.versions.set([]); this.loading.set(false); this.saving.set(false);
    this.error.set(error instanceof ApiProblemError ? error.message : 'No se pudo consultar o guardar la valoración. Recargue antes de continuar.');
  }
}
