import {ChangeDetectionStrategy, Component, input, output, signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {finalize} from 'rxjs';
import {ProcessApi, ProcessView} from '../data-access/process-api.service';
@Component({selector: 'sec-process-assignment-form', standalone: true, imports: [FormsModule], changeDetection: ChangeDetectionStrategy.OnPush, template: `
  @if(process().state==='ACTIVE'){
    <form (ngSubmit)="save()"><h3>Asignar participante</h3>@if(error()){<p role="alert">{{error()}}</p>}
      <label for="indicator">Indicador</label><select id="indicator" [(ngModel)]="indicatorId" name="indicator" required><option value="">Seleccione un indicador</option>@for(i of process().indicators; track i.id){<option [value]="i.id">{{i.code}} · {{i.name}}</option>}</select>
      <label for="identity">Identidad (UUID)</label><input id="identity" [(ngModel)]="identityId" name="identity" required>
      <label for="function">Función</label><select id="function" [(ngModel)]="function" name="function"><option>RESPONSIBLE</option><option>REVIEWER</option><option>EVALUATOR</option></select>
      <label for="validFrom">Desde</label><input id="validFrom" type="datetime-local" [(ngModel)]="validFrom" name="validFrom" required>
      <label for="validTo">Hasta</label><input id="validTo" type="datetime-local" [(ngModel)]="validTo" name="validTo" required>
      <label for="reason-assignment">Motivo</label><textarea id="reason-assignment" [(ngModel)]="reason" name="reason" required maxlength="2000"></textarea><button [disabled]="busy()">Asignar</button>
    </form>
  } @else {<p>Las asignaciones requieren un proceso activo.</p>}`})
export class ProcessAssignmentForm {
  readonly process = input.required<ProcessView>(); readonly changed = output<ProcessView>();
  readonly error = signal(''); readonly busy = signal(false);
  indicatorId = ''; identityId = ''; function = 'RESPONSIBLE'; validFrom = ''; validTo = ''; reason = '';
  constructor(private readonly api: ProcessApi) {}
  save(): void {
    if(this.busy()) return;
    const from = new Date(this.validFrom), to = new Date(this.validTo);
    if(!this.indicatorId || !this.identityId.trim() || !this.reason.trim() || !Number.isFinite(from.getTime()) || !Number.isFinite(to.getTime()) || from >= to){this.error.set('Complete indicador, identidad, motivo y un intervalo de fechas válido.'); return;}
    this.error.set(''); this.busy.set(true);
    this.api.assign(this.process(), {indicatorId: this.indicatorId, identityId: this.identityId, function: this.function, validFrom: from.toISOString(), validTo: to.toISOString(), reason: this.reason})
      .pipe(finalize(() => this.busy.set(false))).subscribe({next: value => this.changed.emit(value), error: () => this.error.set('No fue posible asignar. Verifique permisos, fechas y conflictos; recargue el proceso si cambió.')});
  }
}
