import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AccessSessionService } from '../data-access/access-session.service';
import { accessErrorMessage } from '../data-access/access.errors';

@Component({
  selector: 'sec-access-login',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section aria-labelledby="login-title">
      <h2 id="login-title">Iniciar sesión</h2>
      <p>Use las credenciales sintéticas asignadas para el piloto.</p>
      <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
        <div>
          <label for="username">Usuario</label>
          <input id="username" type="text" formControlName="username" autocomplete="username" required>
          @if (form.controls.username.touched && form.controls.username.invalid) {
            <p role="alert">Ingrese un usuario de al menos 3 caracteres.</p>
          }
        </div>
        <div>
          <label for="password">Contraseña</label>
          <input id="password" type="password" formControlName="password" autocomplete="current-password" required>
          @if (form.controls.password.touched && form.controls.password.invalid) {
            <p role="alert">Ingrese una contraseña de al menos 12 caracteres.</p>
          }
        </div>
        @if (error()) {
          <p role="alert">{{ error() }}</p>
        }
        <button type="submit" [disabled]="submitting()">{{ submitting() ? 'Ingresando…' : 'Ingresar' }}</button>
      </form>
      <p><a routerLink="/access">Volver al panel de acceso</a></p>
    </section>
  `
})
export class LoginPageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly session = inject(AccessSessionService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);
  readonly form = this.fb.nonNullable.group({
    username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(100)]],
    password: ['', [Validators.required, Validators.minLength(12), Validators.maxLength(256)]]
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.error.set(null);
    const {username, password} = this.form.getRawValue();
    this.session.login(username, password).pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: () => {
        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
        void this.router.navigateByUrl(returnUrl?.startsWith('/') && !returnUrl.startsWith('//') ? returnUrl : '/access');
      },
      error: (error: unknown) => this.error.set(accessErrorMessage(error))
    });
  }
}
