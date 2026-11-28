import {TestBed} from '@angular/core/testing';
import {signal} from '@angular/core';
import {provideHttpClient} from '@angular/common/http';
import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';
import {afterEach, describe, expect, it} from 'vitest';
import {AccessSessionService} from '../../features/access/data-access/access-session.service';
import {CurrentIdentity} from '../../features/access/data-access/access.models';
import {EntityNamesService} from './entity-names.service';

function setup(role = 'FUNCTIONAL_ADMIN') {
  const identity = signal<CurrentIdentity | null>({id: 'self', displayName: 'Persona de prueba', technical: false,
    assignments: [{id: 'assignment', careerId: 'career', roleCode: role, roleName: '', processId: null, indicatorId: null, validFrom: '', validTo: ''}]});
  TestBed.configureTestingModule({providers: [provideHttpClient(), provideHttpClientTesting(), {provide: AccessSessionService, useValue: {identity}}]});
  const names = TestBed.inject(EntityNamesService), http = TestBed.inject(HttpTestingController);
  TestBed.tick();
  return {names, http, identity};
}
describe('EntityNamesService', () => {
  afterEach(() => TestBed.resetTestingModule());
  it('resolves real catalogue labels and follows pagination, not the first page only', () => {
    const {names, http} = setup();
    http.expectOne(r => r.url.endsWith('/careers/access')).flush({canManage: true});
    http.expectOne(r => r.url.endsWith('/careers') && r.params.get('page') === '0').flush({items: [], page: 0, size: 100, totalElements: 101});
    http.expectOne(r => r.url.endsWith('/careers') && r.params.get('page') === '1').flush({items: [{id: 'career', data: {name: 'Carrera oficial de prueba'}}], page: 1, size: 100, totalElements: 101});
    http.expectOne(r => r.url.endsWith('/processes')).flush([{id: 'process', name: 'Proceso oficial', indicators: [{id: 'indicator', name: 'Indicador oficial'}]}]);
    http.expectOne(r => r.url.endsWith('/users')).flush({items: [{id: 'person', displayName: 'Nombre real del servicio'}], page: 0, size: 100, total: 1});
    expect(names.career('career')).toBe('Carrera oficial de prueba');
    expect(names.process('process')).toBe('Proceso oficial');
    expect(names.indicator('indicator')).toBe('Indicador oficial');
    expect(names.user('person')).toBe('Nombre real del servicio');
    expect(names.user('self')).toBe('Persona de prueba');
    expect(names.assignment('assignment')).toContain('Administrador funcional');
    expect(names.career('private-uuid')).toBe('Carrera no disponible');
    http.verify();
  });
  it('never requests the users register for a non-admin or reveals IDs on denied catalogues', () => {
    const {names, http} = setup('REVIEWER');
    http.expectNone(r => r.url.endsWith('/users'));
    http.expectOne(r => r.url.endsWith('/careers/access')).flush({}, {status: 403, statusText: 'Forbidden'});
    http.expectNone(r => r.url.endsWith('/careers'));
    http.expectOne(r => r.url.endsWith('/processes')).flush({}, {status: 403, statusText: 'Forbidden'});
    expect(names.career('secret')).toBe('Carrera no disponible');
    expect(names.process('secret')).toBe('Proceso no disponible');
    expect(names.user('secret')).toBe('Usuario no disponible');
    http.verify();
  });
  it('cancels pending reads and clears the catalogue on logout', () => {
    const {names, http, identity} = setup();
    const access = http.expectOne(r => r.url.endsWith('/careers/access'));
    const processes = http.expectOne(r => r.url.endsWith('/processes'));
    const users = http.expectOne(r => r.url.endsWith('/users'));
    names.users.set([{id: 'old', displayName: 'Anterior'} as never]);
    identity.set(null); TestBed.tick();
    expect(access.cancelled && processes.cancelled && users.cancelled).toBe(true);
    expect(names.users()).toEqual([]);
    expect(names.user('old')).toBe('Usuario no disponible');
    http.verify();
  });
});
