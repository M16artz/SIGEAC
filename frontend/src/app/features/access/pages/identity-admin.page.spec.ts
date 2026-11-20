import {TestBed} from '@angular/core/testing';
import {of, throwError} from 'rxjs';
import {describe, expect, it, vi} from 'vitest';
import {AccessApi} from '../data-access/access-api.service';
import {AssignmentView, UserView} from '../data-access/access.models';
import {IdentityAdminPageComponent} from './identity-admin.page';

const user: UserView = {id: 'user', username: 'synthetic', displayName: 'Persona sintetica', technical: false, active: true, revision: 0};
const assignment: AssignmentView = {id: 'assignment', roleCode: 'RESPONSIBLE', roleName: 'Responsable documental',
  careerId: 'career', processId: null, indicatorId: null, validFrom: '2026-01-01', validTo: '2030-01-01', revision: 7};

function setup(fails = false) {
  const api = {listUsers: vi.fn().mockReturnValue(of({items: [user], page: 0, size: 20, total: 1})),
    updateUserState: vi.fn().mockReturnValue(of({id: user.id, version: 1, status: 'UPDATED'})),
    listAssignments: vi.fn().mockReturnValueOnce(of([assignment])).mockReturnValue(of([])),
    createUser: vi.fn().mockReturnValue(of({id: 'new-user', version: 0, status: 'CREATED'})),
    assign: vi.fn().mockReturnValue(of({id: 'new-assignment', version: 0, status: 'CREATED'})),
    revokeAssignment: vi.fn().mockReturnValue(fails ? throwError(() => new Error('Forbidden')) : of({id: assignment.id, version: 8, status: 'REVOKED'}))};
  TestBed.configureTestingModule({imports: [IdentityAdminPageComponent], providers: [{provide: AccessApi, useValue: api}]});
  const fixture = TestBed.createComponent(IdentityAdminPageComponent);
  fixture.detectChanges(); fixture.componentInstance.select(user); fixture.detectChanges();
  return {fixture, api};
}

describe('IdentityAdminPageComponent assignment revocation', () => {
  it('preserves state changes and assignment selection from the compact registry actions', () => {
    const {fixture, api} = setup();
    const root = fixture.nativeElement as HTMLElement;
    (root.querySelector('.users-table .sigeac-btn--danger-soft') as HTMLButtonElement).click();
    expect(api.updateUserState).toHaveBeenCalledWith(user.id, false, user.revision);
    fixture.componentInstance.users.set([{...user, active: false}]); fixture.detectChanges();
    (root.querySelector('.users-table .sigeac-btn--success-soft') as HTMLButtonElement).click();
    expect(api.updateUserState).toHaveBeenLastCalledWith(user.id, true, user.revision);
    const select = root.querySelector('.users-table button[aria-pressed]') as HTMLButtonElement;
    select.click(); fixture.detectChanges();
    expect(fixture.componentInstance.selected()).toBeNull();
    select.click(); fixture.detectChanges();
    expect(fixture.componentInstance.selected()?.id).toBe(user.id);
  });
  it('distinguishes deactivation and activation visually without changing the available action', () => {
    const {fixture} = setup();
    const root = fixture.nativeElement as HTMLElement;
    expect(root.querySelector('.users-table .sigeac-btn--danger-soft')?.textContent).toContain('Desactivar');
    fixture.componentInstance.users.set([{...user, active: false}]);
    fixture.detectChanges();
    expect(root.querySelector('.users-table .sigeac-btn--success-soft')?.textContent).toContain('Activar');
    expect(root.querySelector('.users-table .sigeac-btn--danger-soft')).toBeNull();
  });
  it('keeps the demonstration notice and uses small contextual icons selectively', () => {
    const {fixture} = setup();
    const root = fixture.nativeElement as HTMLElement;
    expect(root.querySelector('h1')?.textContent).toBe('Usuarios y asignaciones');
    expect(root.querySelector('.users-demo-note')?.textContent).toContain('datos sintéticos');
    expect(root.querySelector('.users-total')?.textContent).toContain('1');
    expect(root.querySelector('.users-total')?.textContent).toContain('usuario registrado');
    expect(root.querySelectorAll('.users-total .users-total-icon[aria-hidden="true"]').length).toBe(1);
    expect(root.querySelectorAll('#create-user-title .users-title-icon[aria-hidden="true"]').length).toBe(1);
    expect(root.querySelectorAll('.users-list-heading svg, .users-assignment-form-heading svg').length).toBe(0);
    expect(root.querySelector('#create-user-title')?.closest('.users-section-heading')?.querySelector('.users-section-copy')?.textContent).toContain('Datos de la cuenta');
    expect(root.querySelector('.users-table button svg')).not.toBeNull();
  });

  it('prioritizes the registry before registration without hiding either workflow', () => {
    const {fixture} = setup();
    const root = fixture.nativeElement as HTMLElement;
    const registry = root.querySelector('[aria-labelledby="users-list-title"]')!;
    const registration = root.querySelector('[aria-labelledby="create-user-title"]')!;
    expect(registry.compareDocumentPosition(registration) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    expect(root.querySelector('.users-create-form [formControlName="username"]')).not.toBeNull();
    expect(root.querySelector('.users-table .users-actions')).not.toBeNull();
  });

  it('keeps errors hidden initially and while an untouched username is being entered', () => {
    const {fixture} = setup();
    const root = fixture.nativeElement as HTMLElement;
    expect(root.querySelectorAll('.sigeac-field-error').length).toBe(0);
    expect(root.querySelectorAll('[aria-invalid="true"]').length).toBe(0);
    fixture.componentInstance.createForm.controls.username.setValue('a');
    fixture.detectChanges();
    expect(root.querySelector('#new-username-error')).toBeNull();
    fixture.componentInstance.createForm.controls.username.markAsTouched();
    fixture.detectChanges();
    expect(root.querySelector('#new-username-error')).not.toBeNull();
    expect(root.querySelector('#newUsername')?.getAttribute('aria-describedby')).toBe('new-username-error');
    fixture.componentInstance.createForm.controls.username.setValue('usuario.valido');
    fixture.detectChanges();
    expect(root.querySelector('#new-username-error')).toBeNull();
    expect(root.querySelector('#newUsername')?.getAttribute('aria-invalid')).toBeNull();
  });

  it('shows required-field errors only after an invalid submit without sending a request', () => {
    const {fixture, api} = setup();
    fixture.componentInstance.createUser();
    fixture.detectChanges();
    const root = fixture.nativeElement as HTMLElement;
    expect(root.querySelectorAll('.users-create-form .sigeac-field-error').length).toBe(3);
    expect(api.createUser).not.toHaveBeenCalled();
    expect(root.querySelector('#newPassword')?.getAttribute('aria-describedby')).toContain('new-password-error');
    expect(fixture.componentInstance.createForm.controls.initialPassword.hasError('required')).toBe(true);
  });

  it('retains password length limits and the creation payload', () => {
    const {fixture, api} = setup();
    fixture.componentInstance.createForm.setValue({username: 'demo.usuario', displayName: 'Persona de demostración', initialPassword: '12345678901', technical: false});
    fixture.componentInstance.createUser();
    expect(api.createUser).not.toHaveBeenCalled();
    expect(fixture.componentInstance.createForm.controls.initialPassword.hasError('minlength')).toBe(true);
    fixture.componentInstance.createForm.controls.initialPassword.setValue('x'.repeat(257));
    expect(fixture.componentInstance.createForm.controls.initialPassword.hasError('maxlength')).toBe(true);
    fixture.componentInstance.createForm.controls.initialPassword.setValue('ClaveDemostracion123');
    fixture.componentInstance.createUser();
    expect(api.createUser).toHaveBeenCalledWith({username: 'demo.usuario', displayName: 'Persona de demostración', initialPassword: 'ClaveDemostracion123', technical: false});
    expect(fixture.componentInstance.createForm.untouched).toBe(true);
  });

  it('omits the type column while preserving service-account identification and original values', () => {
    const {fixture} = setup();
    const service = {...user, id: 'service', username: 'integration', technical: true};
    fixture.componentInstance.users.set([user, service]); fixture.detectChanges();
    const root = fixture.nativeElement as HTMLElement;
    const headers = Array.from(root.querySelectorAll('.users-table thead th')).map(th => th.textContent?.trim());
    expect(headers).toEqual(['Cuenta', 'Estado', 'Acciones']);
    const account = root.querySelector('[data-user-id="user"] td')!;
    expect(account.querySelector('.users-account-name')?.textContent).toBe(user.displayName);
    expect(account.querySelector('.users-account-username')?.textContent).toBe(user.username);
    expect(root.querySelector('[data-user-id="user"] .users-actions')?.querySelectorAll('button')).toHaveLength(2);
    expect(root.querySelector('.users-table')?.textContent).not.toContain('Humana');
    expect(root.querySelector('[data-user-id="service"]')?.textContent).toContain('Cuenta de servicio');
    expect(root.querySelector('[data-user-id="user"]')?.textContent).not.toContain('Cuenta personal');
    expect(root.querySelector('[aria-labelledby="assignments-title"]')?.textContent).toContain('Cuenta personal');
    expect(fixture.componentInstance.users()[1].technical).toBe(true);
    const select = root.querySelector('[data-user-id="service"] button') as HTMLButtonElement;
    select.click(); fixture.detectChanges();
    expect(fixture.componentInstance.selected()?.technical).toBe(true);
    expect(root.querySelector('[aria-labelledby="assignments-title"]')?.textContent).toContain('Cuenta de servicio');
  });

  it('requires a reason, sends the selected revision and refreshes active assignments after success', () => {
    const {fixture, api} = setup();
    const button = fixture.nativeElement.querySelector('[data-assignment-id="assignment"]') as HTMLButtonElement;
    expect(button.disabled).toBe(true);
    fixture.componentInstance.revocationReason.setValue('Retiro sintetico de competencia vigente'); fixture.detectChanges();
    button.click(); fixture.detectChanges();
    expect(api.revokeAssignment).toHaveBeenCalledWith('user', 'assignment', 7, 'Retiro sintetico de competencia vigente');
    expect(fixture.componentInstance.assignments()).toEqual([]);
    expect(fixture.nativeElement.querySelector('[role="status"]').textContent).toContain('Asignación revocada');
    expect(fixture.nativeElement.querySelector('[data-assignment-id="assignment"]')).toBeNull();
  });

  it('keeps the assignment visible and reports an unsuccessful revocation', () => {
    const {fixture, api} = setup(true);
    fixture.componentInstance.revocationReason.setValue('Motivo sintetico documentado'); fixture.detectChanges();
    (fixture.nativeElement.querySelector('[data-assignment-id="assignment"]') as HTMLButtonElement).click(); fixture.detectChanges();
    expect(api.revokeAssignment).toHaveBeenCalledTimes(1);
    expect(fixture.componentInstance.assignments()).toEqual([assignment]);
    expect(fixture.componentInstance.assignmentMessage()).toBeNull();
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('No fue posible');
  });
});
