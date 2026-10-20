import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { AccessApi } from '../data-access/access-api.service';
import { accessErrorMessage } from '../data-access/access.errors';
import { ConflictExceptionCommand, PILOT_CAREER_ID, PILOT_CONFLICT_POLICY_ID } from '../data-access/access.models';

@Component({
  selector: 'sec-conflict-exceptions',
  standalone: true,
  imports: [ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section aria-labelledby="conflicts-title">
      <h2 id="conflicts-title">Conflictos y excepciones temporales</h2>
      <p>
        Una excepción solo habilita la combinación declarada durante su vigencia y ámbito. No modifica la política base
        y requiere una justificación de al menos 10 caracteres.
      </p>
      <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
        <div>
          <label for="policyId">Política de conflicto (UUID sintético)</label>
          <input id="policyId" type="text" formControlName="policyId" required>
        </div>
        <div>
          <label for="targetUserId">Usuario destino</label>
          <input id="targetUserId" type="text" formControlName="targetUserId" required>
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
          <label for="justification">Justificación</label>
          <textarea id="justification" formControlName="justification" minlength="10" maxlength="1000" required></textarea>
        </div>
        <div>
          <label for="validFrom">Vigente desde</label>
          <input id="validFrom" type="datetime-local" formControlName="validFrom" required>
        </div>
        <div>
          <label for="validTo">Vigente hasta</label>
          <input id="validTo" type="datetime-local" formControlName="validTo" required>
        </div>
        @if (error()) {
          <p role="alert">{{ error() }}</p>
        }
        @if (createdId()) {
          <p role="status">Excepción registrada con identificador {{ createdId() }}.</p>
        }
        <button type="submit" [disabled]="submitting()">{{ submitting() ? 'Registrando…' : 'Registrar excepción' }}</button>
      </form>
    </section>
  `
})
export class ConflictExceptionsPageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(AccessApi);

  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);
  readonly createdId = signal<string | null>(null);
  readonly form = this.fb.nonNullable.group({
    policyId: [PILOT_CONFLICT_POLICY_ID, Validators.required],
    targetUserId: ['', Validators.required],
    careerId: [PILOT_CAREER_ID, Validators.required],
    processId: [''],
    indicatorId: [''],
    justification: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(1000)]],
    validFrom: [localDateTime(new Date()), Validators.required],
    validTo: [localDateTime(new Date(Date.now() + 86_400_000)), Validators.required]
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
    const validTo = new Date(raw.validTo);
    if (validTo <= validFrom) {
      this.error.set('La vigencia final debe ser posterior a la inicial.');
      return;
    }
    const command: ConflictExceptionCommand = {
      policyId: raw.policyId,
      targetUserId: raw.targetUserId,
      careerId: raw.careerId,
      processId,
      indicatorId,
      justification: raw.justification,
      validFrom: validFrom.toISOString(),
      validTo: validTo.toISOString()
    };
    this.submitting.set(true);
    this.error.set(null);
    this.createdId.set(null);
    this.api.createConflictException(command).pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: (resource) => {
        this.createdId.set(resource.id);
        this.form.patchValue({justification: ''});
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
