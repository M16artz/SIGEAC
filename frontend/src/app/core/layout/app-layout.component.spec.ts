import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';
import { AccessSessionService } from '../../features/access/data-access/access-session.service';
import { CurrentIdentity } from '../../features/access/data-access/access.models';
import { AppLayoutComponent } from './app-layout.component';

@Component({ standalone: true, template: '' })
class EmptyPage {}

const displayName = 'María Fernanda Apellidos Completos de la Universidad';

async function setup(logoutFails = false) {
  const identity = signal<CurrentIdentity | null>({
    id: 'user', displayName, technical: false,
    assignments: [{id: 'assignment', roleCode: 'FUNCTIONAL_ADMIN', roleName: '', careerId: 'career',
      processId: null, indicatorId: null, validFrom: '2026-01-01', validTo: '2030-01-01'}]
  });
  const session = { identity, clear: vi.fn(), logout: vi.fn().mockReturnValue(
    logoutFails ? throwError(() => new Error('Network error')) : of(undefined)
  ) };
  TestBed.configureTestingModule({imports: [AppLayoutComponent], providers: [
    provideRouter([{path: 'access', component: EmptyPage}, {path: 'access/login', component: EmptyPage}]),
    {provide: AccessSessionService, useValue: session}
  ]});
  const router = TestBed.inject(Router);
  await router.navigateByUrl('/access');
  const fixture = TestBed.createComponent(AppLayoutComponent);
  fixture.detectChanges();
  return {fixture, router, session};
}

describe('AppLayoutComponent profile', () => {
  it('renders one profile above the views, outside the sidebar, with real identity data', async () => {
    const {fixture} = await setup();
    const root = fixture.nativeElement as HTMLElement;
    expect(root.querySelectorAll('.app-profile')).toHaveLength(1);
    expect(root.querySelector('aside .app-profile')).toBeNull();
    expect(root.querySelector('.app-heading .profile-name')?.textContent).toContain(displayName);
    expect(root.querySelector('.profile-role')?.textContent).toContain('Administrador funcional');
    expect(root.querySelector('.profile-avatar')?.textContent?.trim()).toBe('MU');
  });

  it('toggles options and closes with Escape, outside clicks, or focus leaving the profile', async () => {
    const {fixture} = await setup();
    const root = fixture.nativeElement as HTMLElement;
    const trigger = root.querySelector('.profile-trigger') as HTMLButtonElement;
    trigger.click(); fixture.detectChanges();
    expect(trigger.getAttribute('aria-expanded')).toBe('true');
    expect(root.querySelector('#profile-options')?.textContent).toContain(displayName);
    expect(root.querySelector('#profile-options a')?.textContent).toContain('Mi acceso');
    document.dispatchEvent(new KeyboardEvent('keydown', {key: 'Escape', bubbles: true}));
    fixture.detectChanges();
    expect(trigger.getAttribute('aria-expanded')).toBe('false');
    expect(document.activeElement).toBe(trigger);
    trigger.click(); fixture.detectChanges();
    root.querySelector('.app-breadcrumb')?.dispatchEvent(new MouseEvent('click', {bubbles: true}));
    fixture.detectChanges();
    expect(root.querySelector('#profile-options')).toBeNull();
    trigger.click(); fixture.detectChanges();
    root.querySelector('.app-profile')?.dispatchEvent(new FocusEvent('focusout', {relatedTarget: root}));
    fixture.detectChanges();
    expect(root.querySelector('#profile-options')).toBeNull();
  });

  for (const fails of [false, true]) {
    it(`keeps logout and login navigation working (${fails ? 'error' : 'success'})`, async () => {
      const {fixture, router, session} = await setup(fails);
      const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);
      const root = fixture.nativeElement as HTMLElement;
      (root.querySelector('.profile-trigger') as HTMLButtonElement).click(); fixture.detectChanges();
      (root.querySelector('#profile-options button') as HTMLButtonElement).click(); fixture.detectChanges();
      expect(session.logout).toHaveBeenCalledTimes(1);
      expect(navigate).toHaveBeenCalledWith(['/access/login']);
      expect(session.clear).toHaveBeenCalledTimes(fails ? 1 : 0);
      expect(root.querySelector('#profile-options')).toBeNull();
    });
  }

  it('does not render the profile on the login page', async () => {
    const {fixture, router} = await setup();
    await router.navigateByUrl('/access/login'); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.app-heading')).toBeNull();
  });
});
