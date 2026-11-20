import {TestBed} from '@angular/core/testing';
import {signal} from '@angular/core';
import {provideRouter} from '@angular/router';
import {describe, expect, it} from 'vitest';
import {AccessSessionService} from '../data-access/access-session.service';
import {AssignmentView, CurrentIdentity} from '../data-access/access.models';
import {AccessHomePageComponent} from './access-home.page';

const assignment: AssignmentView = {id: 'assignment-id', roleCode: 'FUNCTIONAL_ADMIN', roleName: 'Administrador funcional',
  careerId: 'career-id', processId: 'process-id', indicatorId: 'indicator-id', validFrom: '2026-01-01', validTo: '2030-01-01'};

function setup(assignments: readonly AssignmentView[] = [assignment]) {
  const identity = signal<CurrentIdentity | null>({id: 'user', displayName: 'Persona sintética', technical: false, assignments});
  TestBed.configureTestingModule({imports: [AccessHomePageComponent], providers: [provideRouter([]),
    {provide: AccessSessionService, useValue: {identity}}]});
  const fixture = TestBed.createComponent(AccessHomePageComponent);
  fixture.detectChanges();
  return {fixture, identity, root: fixture.nativeElement as HTMLElement};
}

describe('AccessHomePageComponent', () => {
  it('uses available role names once and retains technical identifiers in native disclosures', () => {
    const {root} = setup();
    expect(root.querySelector('tbody tr td')?.textContent).toContain(assignment.roleName);
    expect(root.querySelectorAll('tbody details').length).toBe(4);
    expect(root.querySelectorAll('details[open]').length).toBe(0);
    for (const id of [assignment.id, assignment.roleCode, assignment.careerId, assignment.processId, assignment.indicatorId]) {
      expect(root.querySelector('tbody')?.textContent).toContain(id);
    }
    expect(root.querySelector('header .access-eyebrow')?.textContent).toContain('Acceso');
    expect(root.querySelector('header .access-session-summary')).toBeNull();
    expect(root.querySelectorAll('.access-session-summary .access-session-stat')).toHaveLength(3);
    expect(root.querySelector('.sigeac-table-scroll')?.getAttribute('tabindex')).toBe('0');
  });

  it('retains all statistics and role-dependent quick links', () => {
    const {fixture, identity, root} = setup([assignment, {...assignment, id: 'other', roleCode: 'REVIEWER'}]);
    expect(Array.from(root.querySelectorAll('.access-card-value'), el => el.textContent?.trim())).toEqual(['2', '1', '1']);
    expect(root.querySelectorAll('.access-link-card').length).toBe(3);
    identity.update(value => ({...value!, assignments: [{...assignment, roleCode: 'COORDINATOR'}]}));
    fixture.detectChanges();
    expect(root.querySelectorAll('.access-link-card').length).toBe(1);
    expect(root.querySelector('.access-link-card')?.getAttribute('href')).toBe('/access/audit');
    identity.update(value => ({...value!, assignments: [{...assignment, roleCode: 'REVIEWER'}]}));
    fixture.detectChanges();
    expect(root.querySelectorAll('.access-link-card').length).toBe(0);
  });

  it('retains loading, empty and unrestricted-scope presentations', () => {
    const {fixture, identity, root} = setup([]);
    expect(root.textContent).toContain('No tiene asignaciones funcionales vigentes.');
    identity.update(value => ({...value!, assignments: [{...assignment, processId: null, indicatorId: null}]}));
    fixture.detectChanges();
    expect(root.querySelector('tbody')?.textContent?.match(/Todos/g)?.length).toBe(2);
    identity.set(null); fixture.detectChanges();
    expect(root.textContent).toContain('Cargando identidad');
  });
});
