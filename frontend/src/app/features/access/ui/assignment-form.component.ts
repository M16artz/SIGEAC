import { ChangeDetectionStrategy, Component, inject, input, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { AccessApi } from '../data-access/access-api.service';
import { accessErrorMessage } from '../data-access/access.errors';
import { AssignmentCommand, PILOT_CAREER_ID, PILOT_ROLES } from '../data-access/access.models';
import { DisplayLabelPipe } from '../../../shared/labels/display-label.pipe';

@Component({
  selector: 'sec-assignment-form',
  standalone: true,
  imports: [ReactiveFormsModule, DisplayLabelPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <form [formGroup]="form" (ngSubmit)="submit()" novalidate class="grid gap-4">
      <div class="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div class="flex items-start gap-3">
          <span
            class="grid h-9 w-9 shrink-0 place-items-center rounded-xl bg-white text-institutional-blue shadow-sm"
          >
            <svg
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="1.7"
              stroke-linecap="round"
              stroke-linejoin="round"
              aria-hidden="true"
              class="h-4 w-4"
            >
              <path d="M12 5v14 M5 12h14" />
            </svg>
          </span>
          <div>
            <h3 class="users-form-title">Nueva asignación</h3>
            <p class="users-section-copy">
              Defina la función, el ámbito y la vigencia del permiso para esta identidad.
            </p>
          </div>
        </div>
        <span class="sigeac-badge self-start sm:self-auto">Campos * obligatorios</span>
      </div>

      <div class="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
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
            Carrera (UUID sintético) <span class="sigeac-required" aria-hidden="true">*</span>
          </label>
          <input
            id="careerId"
            class="sigeac-input"
            type="text"
            formControlName="careerId"
            required
            [attr.aria-invalid]="form.controls.careerId.touched && form.controls.careerId.invalid ? 'true' : null"
          >
          @if (form.controls.careerId.touched && form.controls.careerId.invalid) {
            <p class="sigeac-field-error">Ingrese la carrera a la que aplica la asignación.</p>
          }
        </div>

        <div class="sigeac-field">
          <label class="sigeac-label" for="processId">Proceso <span class="sigeac-optional">Opcional</span></label>
          <input
            id="processId"
            class="sigeac-input"
            type="text"
            formControlName="processId"
            placeholder="UUID del proceso"
          >
          <p class="sigeac-help">Déjelo vacío para aplicar a todos los procesos.</p>
        </div>

        <div class="sigeac-field">
          <label class="sigeac-label" for="indicatorId">Indicador <span class="sigeac-optional">Opcional</span></label>
          <input
            id="indicatorId"
            class="sigeac-input"
            type="text"
            formControlName="indicatorId"
            placeholder="UUID del indicador"
          >
          <p class="sigeac-help">Requiere un proceso indicado.</p>
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
          >
          @if (form.controls.validFrom.touched && form.controls.validFrom.invalid) {
            <p class="sigeac-field-error">Seleccione el inicio de vigencia.</p>
          }
        </div>

        <div class="sigeac-field">
          <label class="sigeac-label" for="validUntil">Vigente hasta <span class="sigeac-optional">Opcional</span></label>
          <input
            id="validUntil"
            class="sigeac-input"
            type="datetime-local"
            formControlName="validUntil"
          >
          <p class="sigeac-help">Debe ser posterior al inicio de vigencia.</p>
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
      @if (success()) {
        <p class="sigeac-alert sigeac-alert--success" role="status">
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
            <path d="M12 3.5 20 7v5c0 4.5-3.2 8.2-8 9.5-4.8-1.3-8-5-8-9.5V7l8-3.5Z M9 12l2 2 4-4.5" />
          </svg>
          <span>Asignación registrada correctamente.</span>
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
  readonly userId = input.required<string>();
  readonly saved = output<void>();

  private readonly fb = inject(FormBuilder);
  private readonly api = inject(AccessApi);
  readonly roles = PILOT_ROLES;
  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);
  readonly success = signal(false);
  readonly form = this.fb.nonNullable.group({
    roleCode: [PILOT_ROLES[0].code, Validators.required],
    careerId: [PILOT_CAREER_ID, [Validators.required]],
    processId: [''],
    indicatorId: [''],
    validFrom: [localDateTime(new Date()), Validators.required],
    validUntil: [''],
    exceptionReason: ['']
  });

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
        this.form.patchValue({exceptionReason: '', validUntil: ''});
        this.saved.emit();
      },
      error: (error: unknown) => this.error.set(accessErrorMessage(error))
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
