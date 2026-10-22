import {ChangeDetectionStrategy, Component, input, output, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {Observable, finalize} from 'rxjs';
import {ProcessApi, ProcessState, ProcessView} from '../data-access/process-api.service';
import {AccessSessionService} from '../../access/data-access/access-session.service';
@Component({selector: 'sec-process-state-controls', standalone: true, imports: [FormsModule], changeDetection: ChangeDetectionStrategy.OnPush, template: `
  <section><h3>Estado del proceso</h3><p>{{process().state}}</p>
    @if(error()){<p role="alert">{{error()}}</p>}
    @if(process().state==='PREPARATION' || process().state==='DRAFT') {
      @if(!process().authorization) {
        <p>El inicio requiere una autorización institucional registrada por una asignación competente. No equivale a acreditación.</p>
        <form (ngSubmit)="authorize()">
          <label for="authority-assignment">Asignación autorizadora propia</label>
          <select id="authority-assignment" name="authorityAssignment" [(ngModel)]="authorityAssignmentId" required>
            <option value="">Seleccione una asignación</option>
            @for(a of session.identity()?.assignments ?? []; track a.id){<option [value]="a.id">{{a.roleName}} · {{a.careerId}}</option>}
          </select>
          <label for="legal-basis">Fundamento normativo de la competencia</label><textarea id="legal-basis" name="legalBasis" [(ngModel)]="legalBasis" required maxlength="4000"></textarea>
          <label for="authorization-document">Referencia al documento de autorización</label><input id="authorization-document" name="documentReference" [(ngModel)]="documentReference" required maxlength="2000">
          <label for="authorization-reason">Justificación</label><textarea id="authorization-reason" name="authorizationReason" [(ngModel)]="reason" required minlength="10" maxlength="2000"></textarea>
          <button [disabled]="busy() || !authorityAssignmentId || !legalBasis.trim() || !documentReference.trim() || reason.trim().length < 10">Registrar autorización</button>
        </form>
      } @else {
        <p>Autorización registrada: {{process().authorization?.documentReference}}</p><button [disabled]="busy()" (click)="activate()">Activar proceso autorizado</button>
      }
    } @else {
      <label for="transition-reason">Motivo de la transición</label><textarea id="transition-reason" [(ngModel)]="reason" minlength="10" maxlength="2000"></textarea>
      @if(process().state==='ACTIVE'){<button [disabled]="busy() || reason.trim().length < 10" (click)="move('SUSPENDED')">Suspender</button>}
      @if(process().state==='SUSPENDED' || process().state==='REOPENED'){<button [disabled]="busy() || reason.trim().length < 10" (click)="move('ACTIVE')">Retomar</button>}
      @if(process().state==='CLOSED'){<button [disabled]="busy() || reason.trim().length < 10" (click)="reopen()">Solicitar reapertura autorizada</button>}
      <p>El cierre documental e institucional requiere su flujo de validación; no está disponible como cambio genérico de estado.</p>
    }
  </section>`})
export class ProcessStateControls {
  readonly process = input.required<ProcessView>(); readonly changed = output<ProcessView>();
  readonly error = signal(''); readonly busy = signal(false);
  reason = ''; authorityAssignmentId = ''; legalBasis = ''; documentReference = '';
  constructor(private readonly api: ProcessApi, readonly session: AccessSessionService) {}
  authorize(): void { this.run(this.api.authorize(this.process(), {authorityAssignmentId: this.authorityAssignmentId, legalBasis: this.legalBasis, documentReference: this.documentReference, reason: this.reason})); }
  activate(): void { this.run(this.api.activate(this.process())); }
  move(target: ProcessState): void { this.run(this.api.transition(this.process(), target, this.reason)); }
  reopen(): void { this.run(this.api.reopen(this.process(), this.reason)); }
  private run(request: Observable<ProcessView>): void {
    if(this.busy()) return;
    this.busy.set(true); this.error.set('');
    request.pipe(finalize(() => this.busy.set(false))).subscribe({next: value => this.changed.emit(value),
      error: () => this.error.set('No se pudo realizar la operación. Verifique competencia, estado y revisión; recargue si otro usuario modificó el proceso.')});
  }
}
