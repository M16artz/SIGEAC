import {TestBed} from '@angular/core/testing';
import {provideHttpClient} from '@angular/common/http';
import {provideHttpClientTesting} from '@angular/common/http/testing';
import {NotificationService} from '../../../shared/notifications/notification.service';
import {of, Subject, throwError} from 'rxjs';
import {describe, expect, it, vi} from 'vitest';
import {AccessApi} from '../data-access/access-api.service';
import {ConflictExceptionsPageComponent} from './conflict-exceptions.page';
import {CONFLICT_POLICY_OPTIONS, ConflictPolicyOption} from '../../../core/config/conflict-policy-options';

function setup(policies: readonly ConflictPolicyOption[] = [{id: 'policy-id', label: 'Política autorizada de prueba'}]) {
  const api = {createConflictException: vi.fn().mockReturnValue(of({id: 'synthetic-exception'}))};
  TestBed.configureTestingModule({imports: [ConflictExceptionsPageComponent], providers: [provideHttpClient(), provideHttpClientTesting(), {provide: CONFLICT_POLICY_OPTIONS, useValue: policies}, {provide: AccessApi, useValue: api}]});
  const fixture = TestBed.createComponent(ConflictExceptionsPageComponent);
  fixture.detectChanges();
  const page = fixture.componentInstance;
  page.form.controls.careerId.setValue('career-id');
  page.form.controls.policyId.setValue('policy-id');
  return {fixture, page, api, root: fixture.nativeElement as HTMLElement};
}

describe('ConflictExceptionsPageComponent', () => {
  it('omits the security conditions panel while retaining the exception form', () => {
    const {root} = setup();
    expect(root.querySelector('.access-policy')).toBeNull();
    expect(root.textContent).not.toContain('Alcance y condiciones de seguridad');
    expect(root.querySelectorAll('[formControlName]').length).toBe(8);
    expect(root.querySelector('#justification')?.getAttribute('maxlength')).toBe('1000');
    expect(root.querySelector('header')?.textContent).not.toContain('Temporal');
  });

  it('preserves required, minimum and maximum justification validation', () => {
    const {fixture, page, api, root} = setup();
    page.submit(); fixture.detectChanges();
    expect(api.createConflictException).not.toHaveBeenCalled();
    expect(root.querySelector('#targetUserId')?.getAttribute('aria-invalid')).toBe('true');
    page.form.patchValue({targetUserId: 'target', justification: 'short'});
    page.submit(); expect(api.createConflictException).not.toHaveBeenCalled();
    page.form.controls.justification.setValue('x'.repeat(1001));
    page.submit(); expect(api.createConflictException).not.toHaveBeenCalled();
  });

  it('keeps the form in the workspace and associates persistent field guidance', () => {
    const {root} = setup();
    expect(root.querySelector('.conflicts-workbench form')).not.toBeNull();
    for (const id of ['targetUserId', 'indicatorId', 'justification']) {
      const control = root.querySelector(`#${id}`)!;
      const help = control.getAttribute('aria-describedby');
      expect(help).toBeTruthy();
      expect(root.querySelector(`#${help}`)?.textContent?.trim()).toBeTruthy();
    }
    expect(root.querySelector('a[href="#conflict-form-title"]')?.textContent).toBe('Registrar excepción');
  });

  it('uses a named policy selector with unchanged identifiers and no technical disclosure', () => {
    const {root, page} = setup([{id: 'policy-id', label: 'Responsable y revisor documental'}]);
    expect(root.querySelector('details, summary')).toBeNull();
    expect(root.querySelector('#policyId')?.tagName).toBe('SELECT');
    expect(root.querySelector('#policyId')?.textContent).toContain('Responsable y revisor documental');
    expect(root.textContent).not.toContain('policy-id');
    expect(page.form.controls.policyId.value).toBe('policy-id');
  });

  it('shows a non-editable unavailable state rather than inventing a policy or exposing an ID', () => {
    const {root, page, api} = setup([]);
    page.form.controls.policyId.setValue('');
    const policy = root.querySelector<HTMLInputElement>('#policyId')!;
    expect(policy.readOnly).toBe(true);
    expect(policy.value).toBe('Sin política disponible');
    page.submit();
    expect(api.createConflictException).not.toHaveBeenCalled();
    expect(root.querySelector('details, summary')).toBeNull();
  });

  it('groups fields semantically and preserves their DOM and keyboard order', () => {
    const {root} = setup();
    expect(Array.from(root.querySelectorAll('fieldset > legend'), el => el.textContent)).toEqual([
      'Destino y política', 'Ámbito de la excepción', 'Vigencia', 'Justificación y acciones'
    ]);
    expect(root.querySelectorAll('.access-form-group').length).toBe(4);
    expect(Array.from(root.querySelectorAll('[formControlName]'), el => el.getAttribute('formControlName'))).toEqual([
      'policyId', 'targetUserId', 'careerId', 'processId', 'indicatorId', 'validFrom', 'validTo', 'justification'
    ]);
    expect(root.querySelector('.access-form-group:last-child .access-form-actions button')?.getAttribute('type')).toBe('submit');
  });

  it('preserves process dependency and date ordering restrictions', () => {
    const {page, api} = setup();
    page.form.patchValue({targetUserId: 'target', justification: 'Motivo sintético válido', indicatorId: 'indicator', processId: ''});
    page.submit(); expect(page.error()).toBe('El indicador requiere un proceso.');
    page.form.patchValue({processId: 'process', validFrom: '2026-11-20T10:00', validTo: '2026-11-20T09:00'});
    page.submit(); expect(page.error()).toBe('La vigencia final debe ser posterior a la inicial.');
    expect(api.createConflictException).not.toHaveBeenCalled();
  });

  it('submits identical internal values and retains the resulting identifier', () => {
    const {fixture, page, api, root} = setup();
    page.form.patchValue({targetUserId: 'target-uuid', justification: 'Motivo sintético válido', processId: ' process-uuid ', indicatorId: ' ', validFrom: '2026-11-20T10:00', validTo: '2026-11-21T10:00'});
    const raw = page.form.getRawValue();
    page.submit(); fixture.detectChanges();
    expect(api.createConflictException).toHaveBeenCalledWith({...raw, processId: 'process-uuid', indicatorId: null,
      validFrom: new Date(raw.validFrom).toISOString(), validTo: new Date(raw.validTo).toISOString()});
    expect(page.createdId()).toBe('synthetic-exception');
    expect(TestBed.inject(NotificationService).notifications()).toEqual([expect.objectContaining({kind: 'success', title: 'Excepción registrada'})]);
    expect(root.textContent).not.toContain('synthetic-exception');
  });

  it('preserves loading and error states', () => {
    const {fixture, page, api, root} = setup();
    page.form.patchValue({targetUserId: 'target', justification: 'Motivo sintético válido'});
    const pending = new Subject(); api.createConflictException.mockReturnValueOnce(pending);
    page.submit(); fixture.detectChanges();
    expect(root.querySelector('button')?.disabled).toBe(true);
    pending.complete();
    api.createConflictException.mockReturnValueOnce(throwError(() => new Error('Failed')));
    page.submit(); fixture.detectChanges();
    expect(TestBed.inject(NotificationService).notifications()).toEqual([expect.objectContaining({kind: 'error'})]);
    expect(root.querySelector('[role="alert"]')).toBeNull();
    expect(page.submitting()).toBe(false);
  });
});
