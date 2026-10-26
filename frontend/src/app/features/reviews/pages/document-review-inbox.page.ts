import {ChangeDetectionStrategy, Component, Input, OnChanges, OnInit, SimpleChanges, inject, signal} from '@angular/core';
import {DatePipe} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {ProcessApi, ProcessView} from '../../processes/data-access/process-api.service';
import {ReviewApi} from '../data-access/review-api.service';
import {
  ReviewDecisionView,
  ReviewInboxEntry,
  ReviewOutcomeCommand
} from '../data-access/review.models';

const OUTCOME_OPTIONS: ReviewOutcomeCommand['outcome'][] = ['ACCEPTED', 'OBSERVED', 'REJECTED'];

interface CoverageOutcomeDraft {
  readonly requiredSourceId: string;
  outcome: ReviewOutcomeCommand['outcome'];
  note: string;
}

@Component({
  selector: 'sec-document-review-inbox',
  standalone: true,
  imports: [FormsModule, DatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section>
      <h2>Revisión documental</h2>
      <label for="review-process">Proceso</label>
      <select id="review-process" [ngModel]="processId" (ngModelChange)="selectProcess($event)">
        <option value="">Seleccione un proceso</option>
        @for (process of processes(); track process.id) {
          <option [value]="process.id">{{process.code}} · {{process.name}}</option>
        }
      </select>

      @if (error()) { <p role="alert">{{error()}}</p> }
      @if (!processId) {
        <p role="status">Seleccione un proceso para revisar sus evidencias remitidas.</p>
      } @else if (!loading() && entries().length === 0) {
        <p role="status">No hay versiones pendientes de revisión documental.</p>
      } @else if (entries().length > 0) {
        <table>
          <caption>Bandeja del revisor: versiones más recientes remitidas por cada evidencia</caption>
          <thead>
            <tr><th scope="col">Título</th><th scope="col">Versión</th><th scope="col">Remisión</th>
                <th scope="col">Coberturas</th><th scope="col">Acción</th></tr>
          </thead>
          <tbody>
            @for (entry of entries(); track entry.versionId) {
              <tr>
                <td>{{entry.title}}</td>
                <td>{{entry.revisionNumber}}</td>
                <td>{{entry.submittedAt | date:'medium'}}</td>
                <td>{{entry.coveredRequiredSourceIds.length}}</td>
                <td><button type="button" (click)="evaluate(entry)">Evaluar</button></td>
              </tr>
            }
          </tbody>
        </table>
      }

      @if (selected(); as entry) {
        <section aria-labelledby="review-decision-form">
          <h3 id="review-decision-form">Decisión documental de "{{entry.title}}"</h3>
          @if (decisionError()) { <p role="alert">{{decisionError()}}</p> }
          <form (ngSubmit)="record()">
            <fieldset>
              <legend>Resultado por cobertura</legend>
              @for (draft of drafts(); track draft.requiredSourceId) {
                <div>
                  <label [for]="'outcome-' + draft.requiredSourceId">Cobertura {{draft.requiredSourceId}}</label>
                  <select [id]="'outcome-' + draft.requiredSourceId"
                          [ngModel]="draft.outcome"
                          [ngModelOptions]="{standalone: true}"
                          (ngModelChange)="setOutcome(draft, $event)">
                    @for (option of outcomeOptions; track option) {
                      <option [value]="option">{{option}}</option>
                    }
                  </select>
                  @if (draft.outcome !== 'ACCEPTED') {
                    <label [for]="'note-' + draft.requiredSourceId">Fundamento de la observación</label>
                    <textarea [id]="'note-' + draft.requiredSourceId"
                              [ngModel]="draft.note"
                              [ngModelOptions]="{standalone: true}"
                              (ngModelChange)="setNote(draft, $event)"
                              maxlength="4000"></textarea>
                  }
                </div>
              }
            </fieldset>
            <label for="review-justification">Justificación global</label>
            <textarea id="review-justification" [ngModel]="justification()"
                      [ngModelOptions]="{standalone: true}"
                      (ngModelChange)="justification.set($event)"
                      maxlength="4000" required></textarea>
            <label for="self-review-exception">Excepción de autorrevisión autorizada</label>
            <input id="self-review-exception" type="checkbox" [ngModel]="exception()"
                   [ngModelOptions]="{standalone: true}"
                   (ngModelChange)="exception.set($event)">
            @if (exception()) {
              <label for="self-review-reason">Motivo auditado de la excepción</label>
              <textarea id="self-review-reason" [ngModel]="exceptionReason()"
                        [ngModelOptions]="{standalone: true}"
                        (ngModelChange)="exceptionReason.set($event)"
                        maxlength="4000"></textarea>
            }
            <button type="submit">Registrar decisión</button>
            <button type="button" (click)="cancel()">Cancelar</button>
          </form>
        </section>
      }

      @if (decisions().length > 0) {
        <section aria-labelledby="review-decision-history">
          <h3 id="review-decision-history">Historial de decisiones del proceso</h3>
          <table>
            <caption>Decisiones documentales con resultados por cobertura y rectificaciones (FR-021/FR-024)</caption>
            <thead>
              <tr><th scope="col">Versión</th><th scope="col">Decisión</th><th scope="col">Fecha</th>
                  <th scope="col">Revisión</th><th scope="col">Resultados</th></tr>
            </thead>
            <tbody>
              @for (decision of decisions(); track decision.id) {
                <tr>
                  <td>{{decision.versionId}}</td>
                  <td>{{decision.decision}}</td>
                  <td>{{decision.reviewedAt | date:'medium'}}</td>
                  <td>{{decision.revision}}</td>
                  <td>
                    <ul>
                      @for (outcome of decision.outcomes; track outcome.requiredSourceId) {
                        <li>{{outcome.requiredSourceId}}: {{outcome.outcome}}</li>
                      }
                    </ul>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </section>
      }
    </section>
  `
})
export class DocumentReviewInboxPage implements OnInit, OnChanges {
  @Input() processId = '';
  private readonly api = inject(ReviewApi);
  private readonly processApi = inject(ProcessApi);
  readonly processes = signal<readonly ProcessView[]>([]);
  readonly entries = signal<readonly ReviewInboxEntry[]>([]);
  readonly decisions = signal<readonly ReviewDecisionView[]>([]);
  readonly selected = signal<ReviewInboxEntry | null>(null);
  readonly drafts = signal<readonly CoverageOutcomeDraft[]>([]);
  readonly justification = signal('');
  readonly exception = signal(false);
  readonly exceptionReason = signal('');
  readonly deciding = signal(false);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly decisionError = signal<string | null>(null);
  readonly outcomeOptions = OUTCOME_OPTIONS;

  ngOnInit(): void {
    this.processApi.list().subscribe({
      next: processes => {
        this.processes.set(processes);
        if (!this.processId && processes.length === 1) this.processId = processes[0].id;
        this.load();
      },
      error: () => this.error.set('No fue posible consultar los procesos.')
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (!changes['processId']?.firstChange) this.load();
  }

  selectProcess(processId: string): void {
    this.processId = processId;
    this.entries.set([]);
    this.decisions.set([]);
    this.load();
  }

  evaluate(entry: ReviewInboxEntry): void {
    this.selected.set(entry);
    this.justification.set('');
    this.exception.set(false);
    this.exceptionReason.set('');
    this.decisionError.set(null);
    this.drafts.set(entry.coveredRequiredSourceIds.map(requiredSourceId => ({
      requiredSourceId,
      outcome: 'ACCEPTED' as const,
      note: ''
    })));
  }

  cancel(): void {
    this.selected.set(null);
    this.drafts.set([]);
  }

  setOutcome(draft: CoverageOutcomeDraft, outcome: ReviewOutcomeCommand['outcome']): void {
    this.drafts.update(items => items.map(item => item === draft ? {...item, outcome} : item));
  }

  setNote(draft: CoverageOutcomeDraft, note: string): void {
    this.drafts.update(items => items.map(item => item === draft ? {...item, note} : item));
  }

  record(): void {
    const entry = this.selected();
    if (!entry) return;
    if (this.justification().trim().length < 10) {
      this.decisionError.set('La justificación global requiere al menos 10 caracteres con fundamento.');
      return;
    }
    const outcomes: ReviewOutcomeCommand[] = this.drafts().map(draft => ({
      requiredSourceId: draft.requiredSourceId,
      outcome: draft.outcome,
      note: draft.outcome === 'ACCEPTED' ? '' : draft.note.trim()
    }));
    if (outcomes.some(outcome => outcome.outcome !== 'ACCEPTED' && outcome.note.length < 10)) {
      this.decisionError.set('Cada observación o rechazo necesita un fundamento de al menos 10 caracteres.');
      return;
    }
    if (this.exception() && this.exceptionReason().trim().length < 10) {
      this.decisionError.set('La excepción de autorrevisión requiere un motivo auditado.');
      return;
    }
    this.deciding.set(true);
    this.decisionError.set(null);
    this.api.decide(entry.versionId, {
      selfReviewException: this.exception(),
      exceptionReason: this.exception() ? this.exceptionReason().trim() : null,
      justification: this.justification().trim(),
      outcomes
    }, '"0"').subscribe({
      next: () => {
        this.deciding.set(false);
        this.cancel();
        this.load();
      },
      error: () => {
        this.deciding.set(false);
        this.decisionError.set('No fue posible registrar la decisión; verifique la revisión vigente.');
      }
    });
  }

  private load(): void {
    if (!this.processId) return;
    this.loading.set(true);
    this.error.set(null);
    this.api.history(this.processId).subscribe({
      next: decisions => {
        this.decisions.set(decisions);
        this.api.inbox(this.processId).subscribe({
          next: entries => {
            this.entries.set(entries);
            this.loading.set(false);
          },
          error: () => {
            this.error.set('No fue posible consultar la bandeja del revisor.');
            this.loading.set(false);
          }
        });
      },
      error: () => {
        this.error.set('No fue posible consultar el historial de decisiones.');
        this.loading.set(false);
      }
    });
  }
}
