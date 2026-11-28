import { ChangeDetectionStrategy, Component, inject, input, output, signal } from '@angular/core';
import {EntityNamesService} from '../../../shared/labels/entity-names.service';
import {NotificationService} from '../../../shared/notifications/notification.service';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { AccessApi } from '../data-access/access-api.service';
import { AssignmentCommand, PILOT_ROLES } from '../data-access/access.models';
import { DisplayLabelPipe } from '../../../shared/labels/display-label.pipe';

@Component({
  selector: 'sec-assignment-form',
  standalone: true,
  imports: [ReactiveFormsModule, DisplayLabelPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <form [formGroup]="form" (ngSubmit)="submit()" novalidate class="users-assignment-form" aria-labelledby="new-assignment-title" [attr.aria-busy]="submitting()">
      <div class="users-assignment-form-heading">
        <div>
          <h3 id="new-assignment-title" class="users-form-title">Nueva asignación</h3>
          <p class="users-section-copy">Elija la función y especifique dónde y durante cuánto tiempo se aplicará.</p>
        </div>
        <p class="users-required-note">Los campos con * son obligatorios.</p>
      </div>

      <div class="users-assignment-grid">
        <div class="sigeac-field">
          <label class="sigeac-label" for="roleCode">
            Función <span class="sigeac-required" aria-hidden="true">*</span>
          </label>
          <select id="roleCode" class="sigeac-input" formControlName="roleCode" required>
            @for (role of roles; track role.code) {
              <option [value]="role.code">{{ role.code | displayLabel }}</option>
            }
          </select>
        </div>

        <div class="sigeac-field">
          <label class="sigeac-label" for="careerId">
            Carrera <span class="sigeac-required" aria-hidden="true">*</span>
          </label>
          <select
            id="careerId"
            class="sigeac-input"
            formControlName="careerId"
            (change)="form.patchValue({processId: '', indicatorId: ''})"
            required
            [attr.aria-invalid]="form.controls.careerId.touched && form.controls.careerId.invalid ? 'true' : null"
            [attr.aria-describedby]="form.controls.careerId.touched && form.controls.careerId.invalid ? 'assignment-career-help assignment-career-error' : 'assignment-career-help'"
          ><option value="">Seleccione una carrera</option>
            @for (career of names.careers(); track career.id) {<option [value]="career.id">{{career.data.name}}</option>}
          </select>
          <p id="assignment-career-help" class="sigeac-help">{{ names.loading() ? 'Consultando carreras…' : names.careers().length ? '' : 'No hay carreras disponibles para su acceso.' }}</p>
          @if (form.controls.careerId.touched && form.controls.careerId.invalid) {
            <p id="assignment-career-error" class="sigeac-field-error">Ingrese la carrera a la que aplica la asignación.</p>
          }
        </div>

        <div class="sigeac-field">
          <label class="sigeac-label" for="processId">Proceso <span class="sigeac-optional">Opcional</span></label>
          <select
            id="processId"
            class="sigeac-input"
            formControlName="processId"
            (change)="form.patchValue({indicatorId: ''})"
            aria-describedby="assignment-process-help"
          ><option value="">Todos</option>
            @for (process of availableProcesses(); track process.id) {<option [value]="process.id">{{process.name}}</option>}
          </select>
          <p id="assignment-process-help" class="sigeac-help">Limite la asignación a un proceso si corresponde.</p>
        </div>

        <div class="sigeac-field">
          <label class="sigeac-label" for="indicatorId">Indicador <span class="sigeac-optional">Opcional</span></label>
          <select
            id="indicatorId"
            class="sigeac-input"
            formControlName="indicatorId"
            aria-describedby="assignment-indicator-help"
          ><option value="">Todos</option>
            @for (indicator of availableIndicators(); track indicator.id) {<option [value]="indicator.id">{{indicator.name}}</option>}
          </select>
          <p id="assignment-indicator-help" class="sigeac-help">Seleccione primero un proceso para limitar sus indicadores.</p>
        </div>

        <div class="sigeac-field">
          <label class="sigeac-label" for="validFrom">
            Vigente desde <span class="sigeac-required" aria-hidden="true">*</span>
          </label>
          <input
            id="validFrom"
            class="sigeac-input"
            type="datetime-local"
            formControlName="validFrom"
            required
            [attr.aria-invalid]="form.controls.validFrom.touched && form.controls.validFrom.invalid ? 'true' : null"
            [attr.aria-describedby]="form.controls.validFrom.touched && form.controls.validFrom.invalid ? 'assignment-from-error' : null"
          >
          @if (form.controls.validFrom.touched && form.controls.validFrom.invalid) {
            <p id="assignment-from-error" class="sigeac-field-error">Seleccione el inicio de vigencia.</p>
          }
        </div>

        <div class="sigeac-field">
          <label class="sigeac-label" for="validUntil">Vigente hasta <span class="sigeac-optional">Opcional</span></label>
          <input
            id="validUntil"
            class="sigeac-input"
            type="datetime-local"
            formControlName="validUntil"
            aria-describedby="assignment-until-help"
          >
          <p id="assignment-until-help" class="sigeac-help">Debe ser posterior al inicio de vigencia.</p>
        </div>
      </div>

      <div class="sigeac-field">
        <label class="sigeac-label" for="exceptionReason">Justificación de excepción <span class="sigeac-optional">Opcional</span></label>
        <textarea
          id="exceptionReason"
          class="sigeac-input"
          formControlName="exceptionReason"
          maxlength="1000"
          placeholder="Explique la excepción si la asignación no sigue el flujo habitual"
        ></textarea>
      </div>

      @if (error()) {
        <p class="sigeac-alert sigeac-alert--error" role="alert">
          <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="1.8"
            stroke-linecap="round"
            stroke-linejoin="round"
            aria-hidden="true"
            class="mt-0.5 h-4 w-4 shrink-0"
          >
            <path d="M12 3.5 21 20H3L12 3.5Z M12 9.5v4 M12 17h.01" />
          </svg>
          <span>{{ error() }}</span>
        </p>
      }

      <div class="flex justify-end">
        <button type="submit" class="sigeac-btn w-full sm:w-auto" [disabled]="submitting()">
          @if (submitting()) {
            <svg class="h-4 w-4 animate-spin" viewBox="0 0 24 24" fill="none" aria-hidden="true">
              <circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="2" opacity="0.25" />
              <path d="M21 12a9 9 0 0 0-9-9" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
            </svg>
          }
          {{ submitting() ? 'Guardando…' : 'Registrar asignación' }}
        </button>
      </div>
    </form>
  `
})
export class AssignmentFormComponent {
  readonly names = inject(EntityNamesService);
  readonly userId = input.required<string>();
  readonly saved = output<void>();

  private readonly fb = inject(FormBuilder);
  private readonly api = inject(AccessApi);
  private readonly notifications = inject(NotificationService);
  readonly roles = PILOT_ROLES;
  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);
  readonly success = signal(false);
  readonly form = this.fb.nonNullable.group({
    roleCode: [PILOT_ROLES[0].code, Validators.required],
    careerId: ['', [Validators.required]],
    processId: [''],
    indicatorId: [''],
    validFrom: [localDateTime(new Date()), Validators.required],
    validUntil: [''],
    exceptionReason: ['']
  });

  availableProcesses() { return this.names.processes().filter(item => item.careerId === this.form.controls.careerId.value); }
  availableIndicators() { return this.availableProcesses().find(item => item.id === this.form.controls.processId.value)?.indicators ?? []; }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    const processId = emptyToNull(raw.processId);
    const indicatorId = emptyToNull(raw.indicatorId);
    if (indicatorId && !processId) {
      this.error.set('El indicador requiere un proceso.');
      return;
    }
    const validFrom = new Date(raw.validFrom);
    const validUntil = raw.validUntil ? new Date(raw.validUntil) : null;
    if (validUntil && validUntil <= validFrom) {
      this.error.set('La vigencia final debe ser posterior a la inicial.');
      return;
    }
    const command: AssignmentCommand = {
      roleCode: raw.roleCode,
      careerId: raw.careerId,
      processId,
      indicatorId,
      validFrom: validFrom.toISOString(),
      validUntil: validUntil ? validUntil.toISOString() : null,
      exceptionReason: emptyToNull(raw.exceptionReason)
    };
    this.submitting.set(true);
    this.error.set(null);
    this.success.set(false);
    this.api.assign(this.userId(), command).pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: () => {
        this.success.set(true);
        this.notifications.success('Asignación registrada', 'La función y su periodo de acceso se guardaron correctamente.');
        this.form.patchValue({exceptionReason: '', validUntil: ''});
        this.saved.emit();
      },
      error: () => this.notifications.error('No se pudo registrar la asignación', 'Revise sus permisos, la vigencia y los posibles conflictos.')
    });
  }
}

function emptyToNull(value: string): string | null {
  const trimmed = value.trim();
  return trimmed.length === 0 ? null : trimmed;
}

function localDateTime(date: Date): string {
  const pad = (value: number) => String(value).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}
