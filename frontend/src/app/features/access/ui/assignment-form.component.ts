import { ChangeDetectionStrategy, Component, inject, input, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { AccessApi } from '../data-access/access-api.service';
import { accessErrorMessage } from '../data-access/access.errors';
import { AssignmentCommand, PILOT_CAREER_ID, PILOT_ROLES } from '../data-access/access.models';

@Component({
  selector: 'sec-assignment-form',
  standalone: true,
  imports: [ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
      <h4>Nueva asignación</h4>
      <div>
        <label for="roleCode">Función</label>
        <select id="roleCode" formControlName="roleCode" required>
          @for (role of roles; track role.code) {
            <option [value]="role.code">{{ role.name }}</option>
          }
        </select>
      </div>
      <div>
        <label for="careerId">Carrera (UUID sintético)</label>
        <input id="careerId" type="text" formControlName="careerId" required>
      </div>
      <div>
        <label for="processId">Proceso (opcional)</label>
        <input id="processId" type="text" formControlName="processId">
      </div>
      <div>
        <label for="indicatorId">Indicador (opcional, requiere proceso)</label>
        <input id="indicatorId" type="text" formControlName="indicatorId">
      </div>
      <div>
        <label for="validFrom">Vigente desde</label>
        <input id="validFrom" type="datetime-local" formControlName="validFrom" required>
      </div>
      <div>
        <label for="validUntil">Vigente hasta (opcional)</label>
        <input id="validUntil" type="datetime-local" formControlName="validUntil">
      </div>
      <div>
        <label for="exceptionReason">Justificación de excepción (opcional)</label>
        <textarea id="exceptionReason" formControlName="exceptionReason" maxlength="1000"></textarea>
      </div>
      @if (error()) {
        <p role="alert">{{ error() }}</p>
      }
      @if (success()) {
        <p role="status">Asignación registrada correctamente.</p>
      }
      <button type="submit" [disabled]="submitting()">{{ submitting() ? 'Guardando…' : 'Registrar asignación' }}</button>
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
