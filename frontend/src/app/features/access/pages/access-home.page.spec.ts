import {TestBed} from '@angular/core/testing';
import {provideHttpClient} from '@angular/common/http';
import {provideHttpClientTesting} from '@angular/common/http/testing';
import {signal} from '@angular/core';
import {provideRouter} from '@angular/router';
import {afterEach, describe, expect, it, vi} from 'vitest';
import {of, Subject, throwError} from 'rxjs';
import {AccessApi} from '../data-access/access-api.service';
import {CareerApi} from '../../careers/data-access/career-api.service';
import {AccessSessionService} from '../data-access/access-session.service';
import {AssignmentView, CurrentIdentity} from '../data-access/access.models';
import {AccessHomePageComponent} from './access-home.page';

const assignment: AssignmentView = {id: 'assignment-id', roleCode: 'FUNCTIONAL_ADMIN', roleName: 'Administrador funcional',
  careerId: 'career-id', processId: 'process-id', indicatorId: 'indicator-id', validFrom: '2026-01-01', validTo: '2030-01-01'};

function setup(assignments: readonly AssignmentView[] = [assignment], options: {usersFail?: boolean; careersFail?: boolean; accessFail?: boolean; pending?: Subject<unknown>} = {}) {
  const identity = signal<CurrentIdentity | null>({id: 'user', displayName: 'Persona sintética', technical: false, assignments});
  const api = {listUsers: vi.fn().mockImplementation((_page: number, size: number) => size === 1 && options.pending ? options.pending : (options.usersFail ? throwError(() => new Error('private')) : of({items: [], page: 0, size: 100, total: 27})))};
  const careers = {
    access: vi.fn().mockReturnValue(options.accessFail ? throwError(() => new Error('denied')) : of({canManage: false})),
    careers: vi.fn().mockReturnValue(options.careersFail ? throwError(() => new Error('private')) : of({items: [], totalElements: 6}))
  };
  TestBed.configureTestingModule({imports: [AccessHomePageComponent], providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting(),
    {provide: AccessApi, useValue: api}, {provide: CareerApi, useValue: careers},
    {provide: AccessSessionService, useValue: {identity}}]});
  const fixture = TestBed.createComponent(AccessHomePageComponent);
  fixture.detectChanges();
  return {fixture, identity, api, careers, page: fixture.componentInstance, root: fixture.nativeElement as HTMLElement};
}

describe('AccessHomePageComponent', () => {
  afterEach(() => { TestBed.resetTestingModule(); vi.useRealTimers(); });
  it('shows readable roles and no technical identifiers or disclosures', () => {
    const {root} = setup();
    expect(root.querySelector('tbody tr td')?.textContent).toContain(assignment.roleName);
    expect(root.querySelectorAll('tbody details').length).toBe(0);
    expect(root.querySelectorAll('details[open]').length).toBe(0);
    for (const id of [assignment.id, assignment.roleCode, assignment.careerId, assignment.processId, assignment.indicatorId]) {
      expect(root.querySelector('tbody')?.textContent).not.toContain(id);
    }
    expect(root.querySelector('header .access-welcome-name')?.textContent).toContain('Persona sintética');
    expect(root.querySelector('header .access-session-summary')).toBeNull();
    expect(root.querySelectorAll('.access-session-summary .access-session-stat')).toHaveLength(3);
    expect(root.querySelector('.sigeac-table-scroll')?.getAttribute('tabindex')).toBe('0');
  });

  it('uses a compact indefinite end only for the documented access sentinel', () => {
    const {fixture, identity, root, page} = setup([{...assignment, validTo: '9999-12-31T00:00:00Z'}]);
    fixture.detectChanges();
    expect(root.querySelector('.access-assignments tbody')?.textContent).toContain('Indefinida');
    expect(page.formatDate('2030-01-01')).toBe('01/01/2030');
    identity.update(value => ({...value!, assignments: [{...assignment, validTo: '2030-01-01'}]}));
    fixture.detectChanges();
    expect(root.querySelector('.access-assignments tbody')?.textContent).not.toContain('Indefinida');
  });

  it('retains all statistics and role-dependent quick links', () => {
    const {fixture, identity, root} = setup([assignment, {...assignment, id: 'other', roleCode: 'REVIEWER'}]);
    expect(Array.from(root.querySelectorAll('.access-card-value'), el => el.textContent?.trim())).toEqual(['27', '2', '6']);
    expect(root.querySelector('.access-scope-summary')?.textContent).toContain('1 ámbito en 1 carrera asignada');
    expect(root.querySelectorAll('.access-link-card').length).toBe(3);
    identity.update(value => ({...value!, assignments: [{...assignment, roleCode: 'COORDINATOR'}]}));
    fixture.detectChanges();
    expect(root.querySelectorAll('.access-link-card').length).toBe(1);
    expect(root.querySelector('.access-link-card')?.getAttribute('href')).toBe('/access/audit');
    identity.update(value => ({...value!, assignments: [{...assignment, roleCode: 'REVIEWER'}]}));
    fixture.detectChanges();
    expect(root.querySelectorAll('.access-link-card').length).toBe(0);
  });

  it('uses server totals, never the size of a page, and labels the session-only function count', () => {
    const {api, careers, root} = setup();
    expect(api.listUsers).toHaveBeenCalledWith(0, 1);
    expect(careers.careers).toHaveBeenCalledWith(0, '', undefined, 1);
    expect(root.querySelectorAll('.access-card-value')[0].textContent).toContain('27');
    expect(root.querySelectorAll('.access-card-copy')[1].textContent).toContain('En su sesión');
    expect(root.querySelector('#assignments-title')?.textContent).toBe('Tus asignaciones');
    expect(root.textContent).not.toContain('Actividad reciente');
  });

  it('does not request the users register for a non-administrator or expose admin shortcuts', () => {
    const {api, root} = setup([{...assignment, roleCode: 'REVIEWER'}]);
    expect(api.listUsers).not.toHaveBeenCalled();
    expect(root.querySelector('.access-card-value')?.textContent).toContain('N/D');
    expect(root.querySelectorAll('.access-link-card')).toHaveLength(0);
    expect(root.textContent).toContain('No hay herramientas de administración');
  });

  it('does not turn failed queries into zeroes and supports retry without hiding assignments', () => {
    const {fixture, page, api, root} = setup([assignment], {usersFail: true, careersFail: true});
    expect(page.usersCount()).toEqual({value: null, state: 'error'});
    expect(page.configuredCareers()).toEqual({value: null, state: 'error'});
    expect(root.querySelector('tbody tr')).not.toBeNull();
    expect(root.textContent).not.toContain('private');
    api.listUsers.mockReturnValue(of({items: [], total: 0}));
    page.retryCounts(); fixture.detectChanges();
    expect(page.usersCount()).toEqual({value: 0, state: 'ready'});
  });

  it('queries careers only after authorized catalogue access and cancels pending requests on destruction', () => {
    const pending = new Subject<unknown>();
    const {fixture, careers, page} = setup([assignment], {accessFail: true, pending});
    expect(careers.careers).not.toHaveBeenCalled();
    expect(page.configuredCareers().state).toBe('unavailable');
    expect(page.usersCount().state).toBe('loading');
    expect(pending.observed).toBe(true);
    fixture.destroy();
    expect(pending.observed).toBe(false);
  });

  it('shows a Monday-first informational calendar with the actual current month and no selectable dates', () => {
    vi.useFakeTimers(); vi.setSystemTime(new Date(2026, 10, 27, 12));
    const {page, root} = setup();
    expect(page.monthLabel()).toBe('noviembre de 2026');
    expect(root.querySelector('.access-calendar-grid th abbr')?.getAttribute('title')).toBe('Lunes');
    expect(page.calendarWeeks()[0].slice(0, 6)).toEqual(Array(6).fill(null));
    expect(page.calendarWeeks()[0][6]?.day).toBe(1);
    expect(root.querySelector('[aria-current="date"]')?.getAttribute('datetime')).toBe('2026-11-27');
    expect(root.querySelectorAll('.access-calendar-grid button')).toHaveLength(0);
    expect(root.querySelectorAll('.access-calendar button')).toHaveLength(2);
  });

  it('handles leap years and year transitions without timezone-dependent ISO parsing', () => {
    const {page} = setup();
    for (const [year, count] of [[2024, 29], [2025, 28], [2000, 29], [2100, 28]]) {
      page.shownMonth.set(year * 12 + 1);
      expect(page.calendarWeeks().flat().filter(Boolean)).toHaveLength(count);
    }
    page.shownMonth.set(2026 * 12 + 11); page.changeMonth(1);
    expect(page.monthLabel()).toBe('enero de 2027');
    page.changeMonth(-1); expect(page.monthLabel()).toBe('diciembre de 2026');
  });

  it('updates today across midnight and preserves a month manually consulted by the user', () => {
    vi.useFakeTimers(); vi.setSystemTime(new Date(2026, 10, 30, 23, 59, 30));
    const {page} = setup();
    vi.advanceTimersByTime(60000);
    expect(page.monthLabel()).toBe('diciembre de 2026');
    expect(page.calendarWeeks().flat().find(day => day?.current)?.iso).toBe('2026-12-01');
    page.changeMonth(-2);
    vi.advanceTimersByTime(86400000);
    expect(page.monthLabel()).toBe('octubre de 2026');
    expect(page.calendarWeeks().flat().some(day => day?.current)).toBe(false);
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
