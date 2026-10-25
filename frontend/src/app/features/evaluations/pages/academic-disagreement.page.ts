import {DatePipe} from '@angular/common';
import {Component, ChangeDetectionStrategy, OnInit, OnDestroy, inject, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {Observable, Subscription, switchMap} from 'rxjs';
import {ApiProblemError} from '../../../core/http/api-client';
import {AccessSessionService} from '../../access/data-access/access-session.service';
import {ProcessApi, ProcessView} from '../../processes/data-access/process-api.service';
import {ReconciliationApi, ReconciliationView} from '../data-access/reconciliation-api.service';

@Component({
  selector: 'sec-academic-disagreement-page',
  standalone: true,
  imports: [FormsModule, DatePipe], changeDetection: ChangeDetectionStrategy.OnPush,
  styles: [`select,textarea {display:block;max-width:100%;margin:.4rem 0 1rem} textarea {width:100%;min-height:5rem}
    article {border-top:1px solid #ccc;padding:.8rem 0;overflow-wrap:anywhere} fieldset label {display:block;margin:.5rem 0}
    nav {background:transparent;color:inherit;padding:1rem 0} nav span {margin:0 .5rem}`],
  template: `
    <section aria-labelledby="reconciliation-title">
      <h2 id="reconciliation-title">Discrepancias y conciliación académica</h2>
      <p>Los juicios previos permanecen consultables. Una resolución requiere procedimiento y asignación competente; no constituye acreditación.</p>
      <label for="reconciliation-process">Proceso</label>
      <select id="reconciliation-process" [ngModel]="processId" (ngModelChange)="selectProcess($event)" [disabled]="busy()">
        <option value="">Seleccione un proceso</option>
        @for (p of processes(); track p.id) {<option [value]="p.id">{{p.code}} · {{p.name}}</option>}
      </select>
      <label for="reconciliation-indicator">Indicador</label>
      <select id="reconciliation-indicator" [ngModel]="indicatorId" (ngModelChange)="selectIndicator($event)" [disabled]="busy()">
        <option value="">Seleccione un indicador</option>
        @for (i of indicators(); track i.id) {<option [value]="i.id">{{i.code}} · {{i.name}}</option>}
      </select>
      @if (error()) {<p role="alert">{{error()}}</p>}
      @if (message()) {<p role="status">{{message()}}</p>}
      @if (processId && indicatorId) {<button type="button" (click)="load()" [disabled]="busy()">Recargar conciliación</button>}
      @if (view(); as v) {
        <h3>Estado: {{statusText(v.status)}}</h3>
        @if (v.result; as result) {<p>Resultado seleccionado aplicable: {{result.ratingLabel}} · Juicio {{result.id}}</p>}
        @else {<p>No hay una resolución aplicable que consolide estos juicios.</p>}
        <h3>Juicios actuales por evaluador</h3>
        @for (j of v.current; track j.id) {
          <article>
            <h4>{{j.ratingLabel}} @if (j.result !== null) {· {{j.result}} %}</h4>
            <p>Evaluador: {{j.authorId}} · {{j.recordedAt | date:'medium'}} · {{j.id}}</p>
            <p>{{j.justification}}</p><p>Limitaciones: {{j.limitations}}</p><p>Método: {{j.method}}</p>
            @for (flag of j.flags; track flag) {<p role="note">{{flagText(flag)}}</p>}
            <ul>@for (support of j.supports; track support.evidenceVersionId + support.requiredSourceId) {
              <li>{{support.title}} · Versión {{support.evidenceVersionId}} · Decisión {{support.reviewDecisionId}}</li>
            }</ul>
          </article>
        } @empty {<p>No hay valoraciones registradas.</p>}
        @if (editable(v) && v.current.length >= 2 && !(caseFresh(v) && !v.resolution) && v.status !== 'RESOLVED') {
          <form (ngSubmit)="open()">
            <label for="disagreement-reason">Fundamento de la discrepancia</label>
            <textarea id="disagreement-reason" name="disagreementReason" [(ngModel)]="disagreementReason" required minlength="10" maxlength="4000" [disabled]="busy()"></textarea>
            <button type="submit" [disabled]="busy() || disagreementReason.trim().length < 10">Registrar discrepancia conservando ambos juicios</button>
          </form>
        }
        @if (v.canReconcile && editable(v)) {
          <label for="reconciliation-authority">Asignación autorizadora</label>
          <select id="reconciliation-authority" [(ngModel)]="assignmentId" [disabled]="busy()">
            <option value="">Seleccione una asignación competente</option>
            @for (a of session.identity()?.assignments ?? []; track a.id) {<option [value]="a.id">{{a.roleName}} · {{a.id}}</option>}
          </select>
          <details>
            <summary>Registrar o versionar procedimiento de conciliación</summary>
            <p>Método soportado: selección fundamentada de un juicio existente. No se promedian resultados.</p>
            <form (ngSubmit)="configure()">
              <label for="reconciliation-procedure">Referencia y versión del procedimiento</label>
              <input id="reconciliation-procedure" name="procedure" [(ngModel)]="procedure" required minlength="3" maxlength="300" [disabled]="busy()" />
              <label for="reconciliation-legal-basis">Fundamento de competencia</label>
              <textarea id="reconciliation-legal-basis" name="legalBasis" [(ngModel)]="legalBasis" required minlength="10" maxlength="4000" [disabled]="busy()"></textarea>
              <label for="reconciliation-document">Constancia documental</label>
              <input id="reconciliation-document" name="documentReference" [(ngModel)]="documentReference" required minlength="3" maxlength="1000" [disabled]="busy()" />
              <button type="submit" [disabled]="busy() || !assignmentId || procedure.trim().length < 3 || legalBasis.trim().length < 10 || documentReference.trim().length < 3">Conservar procedimiento autorizado</button>
            </form>
          </details>
        }
        @if (v.policy; as policy) {<p>Procedimiento vigente: {{policy.procedureReference}} · revisión {{policy.revision}} · {{policy.legalBasis}} · {{policy.documentReference}}</p>}
        @else {<p>Sin procedimiento autorizado registrado; la resolución está bloqueada.</p>}
        @if (canResolve(v)) {
          <form (ngSubmit)="resolve()">
            <label for="reconciliation-selected">Juicio existente seleccionado</label>
            <select id="reconciliation-selected" name="selected" [(ngModel)]="selectedJudgement" required [disabled]="busy()">
              <option value="">Seleccione un juicio vigente</option>
              @for (j of v.current; track j.id) {<option [value]="j.id">{{j.ratingLabel}} · {{j.authorId}} · {{j.id}}</option>}
            </select>
            <fieldset [disabled]="busy()"><legend>Participantes cuya intervención se registra</legend>
              @for (participant of requiredParticipants(v); track participant) {
                <label><input type="checkbox" [checked]="participants.includes(participant)" (change)="toggle(participant)" /> {{participant}}</label>
              }
            </fieldset>
            <label for="reconciliation-resolution-reason">Fundamento de la resolución</label>
            <textarea id="reconciliation-resolution-reason" name="resolutionReason" [(ngModel)]="resolutionReason" required minlength="10" maxlength="4000" [disabled]="busy()"></textarea>
            <button type="submit" [disabled]="busy() || !assignmentId || !selectedJudgement || resolutionReason.trim().length < 10 || participants.length !== requiredParticipants(v).length">Conservar resolución autorizada</button>
          </form>
        }
        <h3>Historial de juicios y sustentos</h3>
        @for (j of v.history; track j.id) {
          <article><h4>{{j.ratingLabel}} · {{j.id}}</h4><p>Evaluador: {{j.authorId}} · {{j.recordedAt | date:'medium'}}</p>
            <p>{{j.justification}}</p><p>Limitaciones: {{j.limitations}}</p><p>Método conservado: {{j.method}}</p>
            @for (flag of j.flags; track flag) {<p>{{flagText(flag)}}</p>}
            <details><summary>Sustentos conservados de este juicio</summary><ul>
              @for (support of j.supports; track support.evidenceVersionId + support.requiredSourceId) {<li>{{support.title}} · Versión {{support.evidenceVersionId}} · Decisión {{support.reviewDecisionId}}</li>}
            </ul></details>
          </article>
        }
        <nav aria-label="Páginas de juicios">
          <button type="button" (click)="load(v.page - 1, v.casePage)" [disabled]="busy() || v.page === 0">Juicios anteriores</button>
          <span>Página {{v.page + 1}}</span><button type="button" (click)="load(v.page + 1, v.casePage)" [disabled]="busy() || !v.hasNext">Más juicios</button>
        </nav>
        <h3>Discrepancias y resoluciones conservadas</h3>
        @for (item of v.cases; track item.disagreement.id) {
          <article>
            <p>Discrepancia {{item.disagreement.revision}} · {{item.disagreement.reason}}</p>
            <details><summary>Fundamentos originales de los evaluadores</summary>
              @for (j of item.disagreement.judgements; track j.id) {<p>{{j.authorId}} · {{j.ratingLabel}} · {{j.justification}}</p>}
            </details>
            @if (item.resolution; as resolution) {
              <p>Resolución conservada: {{resolution.reason}}</p>
              <p>Juicio seleccionado: {{resolution.selectedJudgement.id}} · {{resolution.selectedJudgement.ratingLabel}}</p>
              <p>Procedimiento conservado: {{resolution.policy.procedureReference}} · {{resolution.policy.documentReference}}</p>
              <p>Autoridad: {{resolution.resolvedBy}} · Asignación: {{resolution.authorityAssignmentId}}</p>
              <p>Participantes: {{resolution.participants.join(', ')}}</p>
              <p>{{item.applicable ? 'Aplicable a los juicios actuales.' : 'Histórica; no consolida los juicios actuales.'}}</p>
            } @else {<p>Sin resolución conservada.</p>}
          </article>
        }
        <nav aria-label="Páginas de discrepancias">
          <button type="button" (click)="load(v.page, v.casePage - 1)" [disabled]="busy() || v.casePage === 0">Discrepancias anteriores</button>
          <span>Página {{v.casePage + 1}}</span><button type="button" (click)="load(v.page, v.casePage + 1)" [disabled]="busy() || !v.hasNextCase">Más discrepancias</button>
        </nav>
      }
    </section>
  `
})
export class AcademicDisagreementPage implements OnInit, OnDestroy {
  private readonly api = inject(ReconciliationApi); private readonly processApi = inject(ProcessApi); readonly session = inject(AccessSessionService);
  private request?: Subscription; private listing?: Subscription;
  readonly processes = signal<readonly ProcessView[]>([]); readonly indicators = signal<ProcessView['indicators']>([]);
  readonly view = signal<ReconciliationView | null>(null); readonly busy = signal(false); readonly error = signal(''); readonly message = signal('');
  processId = ''; indicatorId = ''; disagreementReason = ''; assignmentId = ''; procedure = ''; legalBasis = ''; documentReference = ''; selectedJudgement = ''; resolutionReason = ''; participants: string[] = [];
  ngOnInit(): void {this.listing = this.processApi.list().subscribe({next: p => this.processes.set(p), error: e => this.fail(e)});}
  ngOnDestroy(): void {this.request?.unsubscribe(); this.listing?.unsubscribe();}
  selectProcess(id: string): void {this.reset(); this.processId = id; this.indicatorId = ''; this.indicators.set(this.processes().find(p => p.id === id)?.indicators ?? []);}
  selectIndicator(id: string): void {this.reset(); this.indicatorId = id; if (id) this.load();}
  load(page = 0, casePage = 0): void {this.request?.unsubscribe(); this.view.set(null); this.busy.set(true); this.error.set(''); this.message.set('');
    this.request = this.api.view(this.processId, this.indicatorId, page, casePage).subscribe({next: v => this.accept(v), error: e => this.fail(e)});}
  editable(v: ReconciliationView): boolean {return ['ACTIVE', 'DOCUMENTARY_CLOSED', 'REOPENED'].includes(v.processState);}
  caseFresh(v: ReconciliationView): boolean {return !!v.latestCase && v.latestCase.judgements.length === v.current.length && v.current.every(j => v.latestCase?.judgements.some(prior => prior.id === j.id && prior.kind === j.kind));}
  canResolve(v: ReconciliationView): boolean {return v.canReconcile && this.editable(v) && !!v.policy && !v.resolution && this.caseFresh(v) && v.current.every(j => j.flags.length === 0);}
  requiredParticipants(v: ReconciliationView): readonly string[] {return [...new Set([...v.current.map(j => j.authorId), v.actorId])];}
  toggle(id: string): void {this.participants = this.participants.includes(id) ? this.participants.filter(p => p !== id) : [...this.participants, id];}
  configure(): void {const v = this.view(); if (!v || !v.canReconcile || !this.editable(v) || this.busy() || !this.assignmentId || this.procedure.trim().length < 3 || this.legalBasis.trim().length < 10 || this.documentReference.trim().length < 3) return;
    this.perform(this.api.policy(v, {procedureReference: this.procedure.trim(), method: 'SELECT_EXISTING_JUDGEMENT_V1', legalBasis: this.legalBasis.trim(), documentReference: this.documentReference.trim(), authorityAssignmentId: this.assignmentId}), 'Procedimiento autorizado conservado.');}
  open(): void {const v = this.view(); if (v && this.editable(v) && !this.busy() && this.disagreementReason.trim().length >= 10) this.perform(this.api.open(v, this.disagreementReason.trim()), 'Discrepancia y fundamentos originales conservados.');}
  resolve(): void {const v = this.view(); if (v && this.canResolve(v) && !this.busy() && this.assignmentId && this.selectedJudgement && this.resolutionReason.trim().length >= 10 && this.requiredParticipants(v).every(id => this.participants.includes(id)))
    this.perform(this.api.resolve(v, this.selectedJudgement, this.participants, this.resolutionReason.trim(), this.assignmentId), 'Resolución autorizada conservada; los juicios previos permanecen intactos.');}
  statusText(status: string): string {return ({NO_EVALUATION: 'Sin valoraciones', UNCONSOLIDATED: 'Sin consolidar', PENDING_RECONCILIATION: 'Pendiente de conciliación', REQUIRES_REVIEW: 'Requiere revisión de juicios o sustentos', RESOLVED: 'Resolución aplicable'} as Record<string, string>)[status] ?? status;}
  flagText(flag: string): string {return ({SUPERSEDED: 'Juicio sustituido por una revisión posterior del mismo evaluador.', SUPPORTS_INVALIDATED: 'Los sustentos documentales ya no coinciden con la aceptación vigente.', INPUTS_SUPERSEDED: 'Las variables tienen una revisión posterior; este cálculo es histórico.', RULE_MISMATCH: 'La regla o escala no coincide con el modelo fijado.'} as Record<string, string>)[flag] ?? flag;}
  private perform(operation: Observable<unknown>, message: string): void {this.request?.unsubscribe(); this.busy.set(true); this.error.set(''); this.message.set('');
    this.request = operation.pipe(switchMap(() => this.api.view(this.processId, this.indicatorId))).subscribe({next: v => {this.accept(v); this.message.set(message);}, error: e => this.fail(e)});}
  private accept(v: ReconciliationView): void {this.view.set(v); this.busy.set(false); this.participants = this.participants.filter(id => this.requiredParticipants(v).includes(id)); if (!v.current.some(j => j.id === this.selectedJudgement)) this.selectedJudgement = '';}
  private reset(): void {this.request?.unsubscribe(); this.view.set(null); this.busy.set(false); this.error.set(''); this.message.set(''); this.disagreementReason = ''; this.assignmentId = ''; this.procedure = ''; this.legalBasis = ''; this.documentReference = ''; this.selectedJudgement = ''; this.resolutionReason = ''; this.participants = [];}
  private fail(e: unknown): void {this.view.set(null); this.busy.set(false); this.error.set(e instanceof ApiProblemError ? e.message : 'No se completó la operación. Recargue el historial antes de repetirla.');}
}
