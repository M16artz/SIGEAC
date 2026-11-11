import { ChangeDetectionStrategy, Component, ViewEncapsulation, inject, signal } from '@angular/core';
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
  encapsulation: ViewEncapsulation.None,
  styleUrl: './login.page.scss',
  template: `
    <div id="sigeac-login" class="login-screen">
      <div class="login-decoration" aria-hidden="true">
        <span class="login-halo"></span>
        <span class="login-circle login-circle--left"></span>
        <span class="login-circle login-circle--right"></span>
        <span class="login-dots login-dots--left"></span>
        <span class="login-dots login-dots--right"></span>
        <span class="login-arcs"></span>
      </div>

      <section class="login-card" aria-labelledby="login-title">
        <header class="login-header">
          <img class="login-logo" src="/images/logo-evaluar.png" alt="evaluar" width="1247" height="273">
          <div class="login-identifier"><span>SIGEAC</span></div>
          <h1 id="login-title" class="login-title">Iniciar sesión</h1>
          <p class="login-description">Ingresa tus credenciales para acceder al sistema.</p>
        </header>

        <form class="login-form" [formGroup]="form" (ngSubmit)="submit()" novalidate [attr.aria-busy]="submitting()">
          <div class="login-field">
            <label class="login-label" for="username">Usuario</label>
            <div class="login-control">
              <svg class="login-field-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <circle cx="12" cy="8" r="3.5" />
                <path d="M5 20v-1a7 7 0 0 1 14 0v1" />
              </svg>
              <input id="username" class="login-input" type="text" formControlName="username" autocomplete="username" required placeholder="Usuario"
                [attr.aria-invalid]="form.controls.username.touched && form.controls.username.invalid ? 'true' : null"
                [attr.aria-describedby]="form.controls.username.touched && form.controls.username.invalid ? 'username-error' : null">
            </div>
            @if (form.controls.username.touched && form.controls.username.invalid) {
              <p id="username-error" class="login-field-error" role="alert">Ingrese un usuario de al menos 3 caracteres.</p>
            }
          </div>
          <div class="login-field">
            <label class="login-label" for="password">Contraseña</label>
            <div class="login-control">
              <svg class="login-field-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <rect x="5" y="10" width="14" height="11" rx="3" />
                <path d="M8 10V7a4 4 0 0 1 8 0v3 M12 14v3" />
              </svg>
              <input id="password" class="login-input login-input--password" [type]="passwordVisible() ? 'text' : 'password'" formControlName="password" autocomplete="current-password" required placeholder="Contraseña"
                [attr.aria-invalid]="form.controls.password.touched && form.controls.password.invalid ? 'true' : null"
                [attr.aria-describedby]="form.controls.password.touched && form.controls.password.invalid ? 'password-error' : null">
              <button class="login-password-toggle" type="button" (click)="passwordVisible.set(!passwordVisible())"
                [attr.aria-label]="passwordVisible() ? 'Ocultar contraseña' : 'Mostrar contraseña'" [attr.aria-pressed]="passwordVisible()" aria-controls="password">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                  @if (passwordVisible()) {
                    <path d="m3 3 18 18 M10.6 10.6a2 2 0 0 0 2.8 2.8 M9.9 5.2A10.8 10.8 0 0 1 12 5c5.5 0 9 7 9 7a17 17 0 0 1-3.1 3.9 M6.3 6.3A18.7 18.7 0 0 0 3 12s3.5 7 9 7a9.9 9.9 0 0 0 4-.9" />
                  } @else {
                    <path d="M3 12s3.5-7 9-7 9 7 9 7-3.5 7-9 7-9-7-9-7Z" />
                    <circle cx="12" cy="12" r="3" />
                  }
                </svg>
              </button>
            </div>
            @if (form.controls.password.touched && form.controls.password.invalid) {
              <p id="password-error" class="login-field-error" role="alert">Ingrese una contraseña de al menos 12 caracteres.</p>
            }
          </div>
          @if (error()) {
            <p class="login-error" role="alert">{{ error() }}</p>
          }
          <button class="login-submit" type="submit" [disabled]="submitting()">
            {{ submitting() ? 'Ingresando…' : 'Ingresar' }}
            @if (submitting()) {
              <span class="login-spinner" aria-hidden="true"></span>
            } @else {
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M4 12h16 M14 6l6 6-6 6" /></svg>
            }
          </button>
        </form>

        <footer class="login-footer">
          <p class="login-security">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="m12 3 8 3v6c0 4-3 7-8 9-5-2-8-5-8-9V6l8-3Z M8.5 12l2.5 2.5 4.5-5" /></svg>
            <span>Acceso exclusivo para usuarios autorizados.</span>
          </p>
          <a class="login-back" routerLink="/access">Volver al panel de acceso</a>
        </footer>
      </section>
    </div>
  `
})
export class LoginPageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly session = inject(AccessSessionService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly submitting = signal(false);
  readonly passwordVisible = signal(false);
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
