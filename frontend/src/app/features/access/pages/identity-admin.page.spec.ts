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
    listAssignments: vi.fn().mockReturnValueOnce(of([assignment])).mockReturnValue(of([])),
    revokeAssignment: vi.fn().mockReturnValue(fails ? throwError(() => new Error('Forbidden')) : of({id: assignment.id, version: 8, status: 'REVOKED'}))};
  TestBed.configureTestingModule({imports: [IdentityAdminPageComponent], providers: [{provide: AccessApi, useValue: api}]});
  const fixture = TestBed.createComponent(IdentityAdminPageComponent);
  fixture.detectChanges(); fixture.componentInstance.select(user); fixture.detectChanges();
  return {fixture, api};
}

describe('IdentityAdminPageComponent assignment revocation', () => {
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
