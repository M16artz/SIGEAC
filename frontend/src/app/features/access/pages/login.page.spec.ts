import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, provideRouter } from '@angular/router';
import { Subject, of } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiProblemError } from '../../../core/http/api-client';
import { AccessSessionService } from '../data-access/access-session.service';
import { LoginPageComponent } from './login.page';

describe('LoginPageComponent', () => {
  const session = { login: vi.fn() };

  beforeEach(() => {
    session.login.mockReset().mockReturnValue(of({}));
    TestBed.configureTestingModule({
      imports: [LoginPageComponent],
      providers: [provideRouter([]), { provide: AccessSessionService, useValue: session }]
    });
  });

  function page() {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    const fixture = TestBed.createComponent(LoginPageComponent);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    function enter(id: string, value: string): HTMLInputElement {
      const input = element.querySelector<HTMLInputElement>(`#${id}`)!;
      input.value = value;
      input.dispatchEvent(new Event('input', { bubbles: true }));
      fixture.detectChanges();
      return input;
    }
    function submit(): void {
      element.querySelector('form')!.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
      fixture.detectChanges();
    }
    return { fixture, element, enter, submit, navigate };
  }

  it('uses the original logo and does not invent a remember-session option', () => {
    const { element } = page();
    expect(element.querySelectorAll('img')).toHaveLength(1);
    expect(element.querySelector('img')?.getAttribute('src')).toBe('/images/logo-evaluar.png');
    expect(element.querySelector('#username')?.getAttribute('placeholder')).toBe('Usuario');
    expect(element.querySelector('#password')?.getAttribute('placeholder')).toBe('Contraseña');
    expect(element.querySelector('input[type=checkbox]')).toBeNull();
    expect(element.querySelector('.login-back')?.getAttribute('href')).toBe('/access');
  });

  it('keeps the existing validation and does not send an invalid form', () => {
    const { element, submit } = page();
    submit();
    expect(session.login).not.toHaveBeenCalled();
    expect(element.querySelector('#username-error')?.textContent).toContain('al menos 3 caracteres');
    expect(element.querySelector('#password-error')?.textContent).toContain('al menos 12 caracteres');
    expect(element.querySelector('#username')?.getAttribute('aria-invalid')).toBe('true');
  });

  it('toggles only password visibility without changing its value or submitting', () => {
    const { element, enter, fixture } = page();
    const input = enter('password', 'synthetic-password');
    const toggle = element.querySelector<HTMLButtonElement>('.login-password-toggle')!;
    expect(input.type).toBe('password');
    toggle.click();
    fixture.detectChanges();
    expect(input.type).toBe('text');
    expect(input.value).toBe('synthetic-password');
    expect(toggle.getAttribute('aria-label')).toBe('Ocultar contraseña');
    expect(toggle.getAttribute('aria-pressed')).toBe('true');
    toggle.click();
    fixture.detectChanges();
    expect(input.type).toBe('password');
    expect(session.login).not.toHaveBeenCalled();
  });

  it('keeps loading, prevents another button submission and displays the API error', () => {
    const pending = new Subject<unknown>();
    session.login.mockReturnValue(pending);
    const { element, enter, submit, fixture } = page();
    enter('username', 'synthetic-user');
    enter('password', 'synthetic-password');
    submit();
    const button = element.querySelector<HTMLButtonElement>('.login-submit')!;
    expect(session.login).toHaveBeenCalledExactlyOnceWith('synthetic-user', 'synthetic-password');
    expect(button.disabled).toBe(true);
    expect(button.textContent).toContain('Ingresando…');
    expect(element.querySelector('form')?.getAttribute('aria-busy')).toBe('true');
    pending.error(new ApiProblemError({
      type: 'urn:sec-unl:problem:authentication-failed', title: 'Unauthorized', status: 401,
      detail: 'Las credenciales no fueron aceptadas.', instance: '/api/v1/sessions',
      code: 'AUTHENTICATION_FAILED', correlationId: 'synthetic-correlation', details: {}
    }));
    fixture.detectChanges();
    expect(button.disabled).toBe(false);
    expect(element.querySelector('.login-error')?.textContent).toContain('Las credenciales no fueron aceptadas.');
    expect(session.login).toHaveBeenCalledTimes(1);
  });

  it('redirects to the access page after a successful response', () => {
    const { enter, submit, navigate } = page();
    enter('username', 'synthetic-user');
    enter('password', 'synthetic-password');
    submit();
    expect(navigate).toHaveBeenCalledWith('/access');
  });

  it.each([
    ['/processes', '/processes'],
    ['//external.invalid', '/access']
  ])('preserves the existing returnUrl handling for %s', (returnUrl, destination) => {
    const { enter, submit, navigate } = page();
    vi.spyOn(TestBed.inject(ActivatedRoute).snapshot.queryParamMap, 'get').mockReturnValue(returnUrl);
    enter('username', 'synthetic-user');
    enter('password', 'synthetic-password');
    submit();
    expect(navigate).toHaveBeenCalledWith(destination);
  });
});
