import {DatePipe} from '@angular/common';
import {ChangeDetectionStrategy, Component, OnDestroy, OnInit, inject, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {Observable, Subscription, catchError, forkJoin, of, switchMap} from 'rxjs';
import {ApiProblemError} from '../../../core/http/api-client';
import {ProcessApi, ProcessView} from '../../processes/data-access/process-api.service';
import {Finding, ReportApi, ReportVersion} from '../../reports/data-access/report-api.service';
import {
  ActionCommand, ActionPage, ImprovementAction, ImprovementApi, ImprovementPlan, State, Transition
} from '../data-access/improvement-api.service';

const STATE_LABELS: Readonly<Record<State, string>> = {
  PROPOSED: 'Propuesta', APPROVED: 'Aprobada', IN_PROGRESS: 'En ejecución',
  PENDING_VERIFICATION: 'Pendiente de verificación', VERIFIED: 'Verificada'
};

const TRANSITIONS: readonly {value: Transition; label: string}[] = [
  {value: 'APPROVE', label: 'Aprobar'},
  {value: 'RETURN', label: 'Devolver a propuesta'},
  {value: 'START', label: 'Iniciar ejecución'},
  {value: 'SUBMIT_VERIFICATION', label: 'Enviar a verificación'},
  {value: 'VERIFY', label: 'Verificar cumplimiento'},
  {value: 'REQUEST_CORRECTION', label: 'Solicitar corrección'}
];

@Component({
  selector: 'sec-improvement-plan-page',
  standalone: true,
  imports: [FormsModule, DatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: [`
    section { max-width: 80rem; }
    select, textarea, input { display:block; margin-top:.2rem; padding:.3rem; }
    select { min-width: 16rem; } textarea { width:100%; min-height:4rem; box-sizing:border-box; }
    fieldset { margin:.8rem 0; } fieldset label { display:block; margin:.3rem 0; overflow-wrap:anywhere; }
    article { border:1px solid #ccc; border-radius:.4rem; padding:.6rem; margin:.8rem 0; overflow-wrap:anywhere; }
    article h4, article h5 { margin:.2rem 0; }
    .muted { color:#555; } .overdue { color:#a40000; font-weight:600; }
  `],
  template: `
    <section aria-labelledby="improvement-title">
      <h2 id="improvement-title">Planes de mejora y seguimiento</h2>
      <p class="muted">Cada acción conserva hallazgos de origen, responsable, plazo, meta y medio de verificación.
        Las modificaciones generan una nueva revisión y los avances son inmutables; el vencimiento se deriva del plazo.</p>
      <label for="improvement-process">Proceso</label>
      <select id="improvement-process" [ngModel]="processId" (ngModelChange)="selectProcess($event)" [disabled]="saving()">
        <option value="">Seleccione un proceso</option>
        @for (p of processes(); track p.id) { <option [value]="p.id">{{p.code}} · {{p.name}}</option> }
      </select>
      @if (loading()) { <p role="status">Consultando planes y acciones…</p> }
      @if (error()) { <p role="alert">{{error()}}</p> }
      @if (message()) { <p role="status">{{message()}}</p> }

      @if (processId) {
        <h3>Paso P15: plan y acciones medibles</h3>
        @if (plans().length > 0) {
          <p class="muted">Planes creados en esta sesión: {{plans().length}}.</p>
        }
        <form (ngSubmit)="createPlan()">
          <h4>Crear plan desde un informe emitido</h4>
          <p class="muted">El plan requiere una versión de informe final o aprobada. Indique la revisión de la
            cadena de planes (0 si es el primero).</p>
          <label for="plan-title">Título del plan</label>
          <input id="plan-title" type="text" name="planTitle" [(ngModel)]="planTitle" required maxlength="300" [disabled]="saving()" />
          <label for="plan-report">Versión de informe emitida</label>
          <select id="plan-report" name="planReport" [(ngModel)]="planReportVersionId" required [disabled]="saving()">
            <option value="">Seleccione la versión</option>
            @for (version of versions(); track version.id) {
              <option [value]="version.id">{{version.data.kind}} · ETag {{version.revision}} · {{version.state ?? ''}}</option>
            }
          </select>
          <label for="plan-revision">Revisión previa de la cadena de planes</label>
          <input id="plan-revision" type="text" name="planRevision" [(ngModel)]="planRevision" required inputmode="numeric" [disabled]="saving()" />
          <button type="submit" [disabled]="saving() || !planValid()">Crear plan</button>
        </form>

        <form (ngSubmit)="createAction()">
          <h4>Proponer acción de mejora</h4>
          <label for="action-plan">Plan</label>
          <select id="action-plan" name="actionPlan" [(ngModel)]="actionPlanId" required [disabled]="saving()">
            <option value="">Seleccione el plan</option>
            @for (plan of plans(); track plan.id) { <option [value]="plan.id">{{plan.data.title}}</option> }
          </select>
          <fieldset [disabled]="saving()">
            <legend>Hallazgos de origen</legend>
            @for (finding of findings(); track finding.id) {
              <label><input type="checkbox" [checked]="actionFindings().includes(finding.id)"
                (change)="toggleFinding(finding.id)" /> {{finding.data.title}}</label>
            } @empty { <p class="muted">No hay hallazgos visibles para este proceso.</p> }
          </fieldset>
          <label for="action-title">Título</label>
          <input id="action-title" type="text" name="actionTitle" [(ngModel)]="actionTitle" required maxlength="300" [disabled]="saving()" />
          <label for="action-description">Descripción</label>
          <textarea id="action-description" name="actionDescription" [(ngModel)]="actionDescription" required minlength="10" maxlength="4000" [disabled]="saving()"></textarea>
          <label for="action-responsible">Asignación del responsable</label>
          <input id="action-responsible" type="text" name="actionResponsible" [(ngModel)]="actionResponsible" required [disabled]="saving()" />
          <label for="action-due">Plazo</label>
          <input id="action-due" type="datetime-local" name="actionDue" [(ngModel)]="actionDue" required [disabled]="saving()" />
          <label for="action-target">Meta medible</label>
          <textarea id="action-target" name="actionTarget" [(ngModel)]="actionTarget" required maxlength="4000" [disabled]="saving()"></textarea>
          <label for="action-method">Medio de verificación</label>
          <textarea id="action-method" name="actionMethod" [(ngModel)]="actionMethod" required maxlength="4000" [disabled]="saving()"></textarea>
          <button type="submit" [disabled]="saving() || !actionValid()">Proponer acción</button>
        </form>

        <h3>Acciones registradas</h3>
        @for (action of actions(); track action.id) {
          <article>
            <strong>{{action.data.title}}</strong> · {{stateLabel(action.state)}}
            @if (action.overdue) { <span class="overdue"> · Vencida sin verificación</span> }
            <p class="muted">ETag {{action.revision}} · Responsable {{action.data.responsibleAssignmentId}} ·
              Plazo {{action.data.dueAt | date:'medium'}} · Hallazgos {{action.data.findingIds.length}} ·
              Avances {{action.progress.length}} · Decisiones {{action.decisions.length}}</p>
            <button type="button" (click)="selectAction(action.id)" [disabled]="saving()">Abrir seguimiento</button>
          </article>
        } @empty { <p>Aún no hay acciones de mejora para este proceso.</p> }

        @if (detail(); as d) {
          <article aria-label="Seguimiento de la acción seleccionada">
            <h4>{{d.data.title}} · ETag {{d.revision}} · {{stateLabel(d.state)}}</h4>
            @if (d.overdue) { <p class="overdue">Plazo vencido sin verificación satisfactoria; el historial se conserva.</p> }
            <p class="muted">Meta: {{d.data.target}} · Verificación: {{d.data.verificationMethod}}</p>
            <h5>Avances inmutables</h5>
            @for (entry of d.progress; track entry.id) {
              <p class="muted">{{entry.data.recordedAt | date:'medium'}} · {{entry.data.achievedValue}} ·
                Evidencias {{entry.data.evidenceVersionIds.length}} · {{entry.data.description}}</p>
            } @empty { <p class="muted">Sin avances registrados.</p> }
            @for (decision of d.decisions; track decision.id) {
              <p class="muted">{{decision.provenance.decidedAt | date:'medium'}} · {{decision.action}} ·
                {{decision.provenance.documentReference}}</p>
            }
            @if (d.state !== 'VERIFIED') {
              <form (ngSubmit)="recordProgress()">
                <h5>Registrar avance</h5>
                <label for="progress-description">Descripción</label>
                <textarea id="progress-description" name="progressDescription" [(ngModel)]="progressDescription" required minlength="10" maxlength="4000" [disabled]="saving()"></textarea>
                <label for="progress-value">Valor alcanzado</label>
                <input id="progress-value" type="text" name="progressValue" [(ngModel)]="progressValue" required maxlength="2000" [disabled]="saving()" />
                <label for="progress-evidence">Versiones de evidencia (separadas por coma)</label>
                <input id="progress-evidence" type="text" name="progressEvidence" [(ngModel)]="progressEvidence" required [disabled]="saving()" />
                <label for="progress-date">Fecha del avance</label>
                <input id="progress-date" type="datetime-local" name="progressDate" [(ngModel)]="progressDate" required [disabled]="saving()" />
                <button type="submit" [disabled]="saving() || !progressValid()">Registrar avance</button>
              </form>
            }
            <form (ngSubmit)="decide()">
              <h5>Decisión de la acción</h5>
              <p class="muted">Aprobar, devolver y verificar exigen capacidades distintas; toda decisión queda auditada.</p>
              <label for="decision-action">Transición</label>
              <select id="decision-action" name="decisionAction" [(ngModel)]="decisionTransition" required [disabled]="saving()">
                <option value="">Seleccione la transición</option>
                @for (option of transitions; track option.value) {
                  <option [value]="option.value">{{option.label}}</option>
                }
              </select>
              <label for="decision-assignment">Asignación con competencia</label>
              <input id="decision-assignment" type="text" name="decisionAssignment" [(ngModel)]="decisionAssignment" required [disabled]="saving()" />
              <label for="decision-legal">Fundamento normativo</label>
              <input id="decision-legal" type="text" name="decisionLegal" [(ngModel)]="decisionLegal" required maxlength="4000" [disabled]="saving()" />
              <label for="decision-document">Documento de respaldo</label>
              <input id="decision-document" type="text" name="decisionDocument" [(ngModel)]="decisionDocument" required maxlength="2000" [disabled]="saving()" />
              <label for="decision-reason">Motivo</label>
              <textarea id="decision-reason" name="decisionReason" [(ngModel)]="decisionReason" required minlength="10" maxlength="4000" [disabled]="saving()"></textarea>
              <button type="submit" [disabled]="saving() || !decisionValid()">Registrar decisión</button>
            </form>
            @if (d.state !== 'VERIFIED') {
              <form (ngSubmit)="revise()">
                <h5>Nueva revisión de la acción</h5>
                <p class="muted">Repita los datos vigentes con los cambios de responsable, plazo o meta; la revisión
                  anterior se conserva y la acción vuelve a propuesta.</p>
                <label for="revision-assignment">Asignación del responsable</label>
                <input id="revision-assignment" type="text" name="revisionAssignment" [(ngModel)]="revisionResponsible" required [disabled]="saving()" />
                <label for="revision-due">Plazo</label>
                <input id="revision-due" type="datetime-local" name="revisionDue" [(ngModel)]="revisionDue" required [disabled]="saving()" />
                <label for="revision-target">Meta medible</label>
                <textarea id="revision-target" name="revisionTarget" [(ngModel)]="revisionTarget" required maxlength="4000" [disabled]="saving()"></textarea>
                <button type="submit" [disabled]="saving() || revisionTarget.trim().length === 0">Guardar revisión</button>
              </form>
            }
          </article>
        }
      }
    </section>
  `
})
export class ImprovementPlanPage implements OnInit, OnDestroy {
  private readonly processesApi = inject(ProcessApi);
  private readonly reportsApi = inject(ReportApi);
  private readonly api = inject(ImprovementApi);
  private request?: Subscription;

  readonly processes = signal<readonly ProcessView[]>([]);
  readonly versions = signal<readonly ReportVersion[]>([]);
  readonly findings = signal<readonly Finding[]>([]);
  readonly plans = signal<readonly ImprovementPlan[]>([]);
  readonly actions = signal<readonly ImprovementAction[]>([]);
  readonly detail = signal<ImprovementAction | null>(null);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly message = signal('');
  readonly actionFindings = signal<readonly string[]>([]);
  readonly transitions = TRANSITIONS;

  processId = '';
  planTitle = '';
  planReportVersionId = '';
  planRevision = '0';
  actionPlanId = '';
  actionTitle = '';
  actionDescription = '';
  actionResponsible = '';
  actionDue = '';
  actionTarget = '';
  actionMethod = '';
  progressDescription = '';
  progressValue = '';
  progressEvidence = '';
  progressDate = '';
  decisionTransition: Transition | '' = '';
  decisionAssignment = '';
  decisionLegal = '';
  decisionDocument = '';
  decisionReason = '';
  revisionResponsible = '';
  revisionDue = '';
  revisionTarget = '';

  ngOnInit(): void {
    this.request = this.processesApi.list().pipe(catchError(() => of<readonly ProcessView[]>([])))
      .subscribe(rows => this.processes.set(rows));
  }

  ngOnDestroy(): void {
    this.request?.unsubscribe();
  }

  selectProcess(id: string): void {
    this.processId = id;
    this.plans.set([]);
    this.actions.set([]);
    this.detail.set(null);
    this.versions.set([]);
    this.findings.set([]);
    this.actionFindings.set([]);
    this.error.set('');
    this.message.set('');
    if (id) this.reload();
  }

  toggleFinding(id: string): void {
    const current = this.actionFindings();
    this.actionFindings.set(current.includes(id) ? current.filter(value => value !== id) : [...current, id]);
  }

  stateLabel(state: State): string {
    return STATE_LABELS[state];
  }

  planValid(): boolean {
    return this.planTitle.trim().length >= 1 && this.planReportVersionId !== ''
      && Number(this.planRevision) >= 0 && !Number.isNaN(Number(this.planRevision));
  }

  createPlan(): void {
    if (!this.planValid() || this.saving()) return;
    this.run(this.api.createPlan(this.processId, Number(this.planRevision), {
      title: this.planTitle.trim(), reportVersionId: this.planReportVersionId
    }).pipe(switchMap(plan => {
      this.plans.update(rows => [...rows, plan]);
      this.actionPlanId = plan.id;
      this.planRevision = String(plan.revision);
      this.planTitle = '';
      return this.api.actions(this.processId);
    })), 'Plan de mejora creado desde la versión emitida.');
  }

  actionValid(): boolean {
    return this.actionPlanId !== '' && this.actionFindings().length > 0
      && this.actionTitle.trim().length >= 1 && this.actionDescription.trim().length >= 10
      && this.actionResponsible.trim().length > 0 && this.actionDue !== ''
      && this.actionTarget.trim().length > 0 && this.actionMethod.trim().length > 0;
  }

  createAction(): void {
    if (!this.actionValid() || this.saving()) return;
    this.run(this.api.createAction(this.processId, this.command()).pipe(switchMap(() => this.api.actions(this.processId))),
      'Acción propuesta con meta, plazo y medio de verificación.');
    this.actionTitle = ''; this.actionDescription = ''; this.actionDue = ''; this.actionTarget = '';
    this.actionMethod = ''; this.actionFindings.set([]);
  }

  selectAction(id: string): void {
    this.request?.unsubscribe();
    this.error.set('');
    this.request = this.api.action(id).subscribe({next: detail => this.applyDetail(detail), error: e => this.fail(e)});
  }

  progressValid(): boolean {
    return this.progressDescription.trim().length >= 10 && this.progressValue.trim().length >= 1
      && this.evidenceIds().length > 0 && this.progressDate !== '';
  }

  recordProgress(): void {
    const detail = this.detail();
    if (!detail || !this.progressValid() || this.saving()) return;
    this.run(this.api.progress(detail.id, detail.revision, {
      description: this.progressDescription.trim(), achievedValue: this.progressValue.trim(),
      evidenceVersionIds: this.evidenceIds(), recordedAt: new Date(this.progressDate).toISOString()
    }).pipe(switchMap(() => this.refreshDetail(detail.id))), 'Avance registrado; el valor no puede reescribirse.');
    this.progressDescription = ''; this.progressValue = ''; this.progressEvidence = '';
  }

  decisionValid(): boolean {
    return this.decisionTransition !== '' && this.decisionAssignment.trim().length > 0
      && this.decisionLegal.trim().length > 0 && this.decisionDocument.trim().length > 0
      && this.decisionReason.trim().length >= 10;
  }

  decide(): void {
    const detail = this.detail();
    if (!detail || !this.decisionValid() || this.saving()) return;
    this.run(this.api.decide(detail.id, detail.revision, {
      action: this.decisionTransition as Transition,
      provenance: {
        authorityAssignmentId: this.decisionAssignment.trim(), legalBasis: this.decisionLegal.trim(),
        documentReference: this.decisionDocument.trim(), reason: this.decisionReason.trim()
      }
    }).pipe(switchMap(() => this.refreshDetail(detail.id))), 'Decisión registrada con trazabilidad y auditoría.');
    this.decisionReason = '';
  }

  revise(): void {
    const detail = this.detail();
    if (!detail || this.revisionTarget.trim().length === 0 || this.revisionResponsible.trim().length === 0
      || this.revisionDue === '' || this.saving()) return;
    const command: ActionCommand = {
      planId: detail.data.planId, findingIds: detail.data.findingIds, title: detail.data.title,
      description: detail.data.description, responsibleAssignmentId: this.revisionResponsible.trim(),
      dueAt: new Date(this.revisionDue).toISOString(), target: this.revisionTarget.trim(),
      verificationMethod: detail.data.verificationMethod
    };
    this.run(this.api.revise(detail.id, detail.revision, command).pipe(switchMap(() => this.refreshDetail(detail.id))),
      'Nueva revisión registrada; la anterior permanece íntegra.');
  }

  private command(): ActionCommand {
    return {
      planId: this.actionPlanId, findingIds: this.actionFindings(), title: this.actionTitle.trim(),
      description: this.actionDescription.trim(), responsibleAssignmentId: this.actionResponsible.trim(),
      dueAt: new Date(this.actionDue).toISOString(), target: this.actionTarget.trim(),
      verificationMethod: this.actionMethod.trim()
    };
  }

  private evidenceIds(): readonly string[] {
    return this.progressEvidence.split(',').map(value => value.trim()).filter(value => value.length > 0);
  }

  private applyDetail(detail: ImprovementAction): void {
    this.detail.set(detail);
    this.revisionResponsible = detail.data.responsibleAssignmentId;
    this.revisionTarget = detail.data.target;
    const due = new Date(detail.data.dueAt);
    this.revisionDue = Number.isNaN(due.getTime()) ? '' : this.localInput(due);
    this.progressEvidence = detail.progress.at(-1)?.data.evidenceVersionIds.join(',') ?? '';
    this.message.set(`Seguimiento de la acción · ETag ${detail.revision} · ${STATE_LABELS[detail.state]}`);
  }

  private localInput(date: Date): string {
    const pad = (value: number): string => String(value).padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
  }

  private refreshDetail(id: string): Observable<ImprovementAction> {
    return this.api.actions(this.processId).pipe(switchMap(page => {
      this.actions.set(page.items);
      return this.api.action(id);
    }));
  }

  private run(work: Observable<unknown>, message: string): void {
    this.saving.set(true); this.error.set(''); this.message.set('');
    this.request?.unsubscribe();
    this.request = work.pipe(switchMap(() => this.observeData())).subscribe({
      next: result => {
        this.versions.set(result.versions.items);
        this.findings.set(result.findings?.items ?? []);
        this.actions.set(result.actions.items);
        if (result.detail) this.detail.set(result.detail);
        this.saving.set(false);
        this.message.set(message);
      },
      error: e => this.fail(e)
    });
  }

  private reload(): void {
    this.request?.unsubscribe();
    this.error.set(''); this.loading.set(true);
    this.request = this.observeData().subscribe({
      next: result => {
        this.versions.set(result.versions.items);
        this.findings.set(result.findings?.items ?? []);
        this.actions.set(result.actions.items);
        this.loading.set(false);
      },
      error: e => this.fail(e)
    });
  }

  private observeData(): Observable<{
    versions: {items: readonly ReportVersion[]; totalElements: number};
    findings: {items: readonly Finding[]} | null;
    actions: ActionPage;
    detail: ImprovementAction | null;
  }> {
    const versions = this.reportsApi.versions(this.processId, 0, 100);
    const findings = this.reportsApi.findings(this.processId, 0, 100).pipe(catchError(() => of(null)));
    const actions = this.api.actions(this.processId);
    const current = this.detail();
    const detail = current ? this.api.action(current.id).pipe(catchError(() => of(null))) : of(null);
    return forkJoin({versions, findings, actions, detail});
  }

  private fail(error: unknown): void {
    this.saving.set(false); this.loading.set(false);
    this.error.set(error instanceof ApiProblemError ? error.message
      : 'No se pudo completar la operación de mejora. Recargue antes de continuar.');
  }
}

