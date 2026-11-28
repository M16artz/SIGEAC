import {TestBed} from '@angular/core/testing';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {NotificationService} from './notification.service';

describe('NotificationService', () => {
  let service: NotificationService;
  beforeEach(() => { vi.useFakeTimers(); TestBed.configureTestingModule({}); service = TestBed.inject(NotificationService); });
  afterEach(() => { service.clear(); vi.useRealTimers(); });
  it('provides four kinds with Spanish feedback and read-only state', () => {
    service.success('Usuario registrado', 'La cuenta se creó.');
    service.error('No se pudo guardar', 'Inténtelo nuevamente.');
    service.warning('Información incompleta', 'Revise los campos.');
    service.info('Datos disponibles', 'Puede consultarlos.');
    expect(service.notifications().map(item => item.kind)).toEqual(['success', 'error', 'warning']);
    expect(service.notifications()[0].description).toBe('La cuenta se creó.');
    expect('set' in service.notifications).toBe(false);
  });
  it('expires success after four seconds and a short exit, never expires important errors', () => {
    service.success('Guardado'); service.error('No se pudo guardar');
    vi.advanceTimersByTime(3999); expect(service.notifications()[0].closing).toBe(false);
    vi.advanceTimersByTime(1); expect(service.notifications()[0].closing).toBe(true);
    vi.advanceTimersByTime(180); expect(service.notifications().map(item => item.kind)).toEqual(['error']);
    vi.advanceTimersByTime(60000); expect(service.notifications()).toHaveLength(1);
  });
  it('preserves info and warning until dismissed', () => {
    service.info('Actualizado', 'Datos disponibles.'); service.warning('Revise los datos', 'Hay información pendiente.');
    vi.advanceTimersByTime(60000); expect(service.notifications()).toHaveLength(2);
  });
  it('deduplicates active and queued notifications by operation key', () => {
    const a = service.success('Guardado', 'Datos guardados.', {key: 'save-user'});
    expect(service.success('Guardado', 'Datos guardados.', {key: 'save-user'})).toBe(a);
    service.error('Error A'); service.error('Error B');
    const pending = service.info('Consulta disponible', 'Consulte los datos.');
    expect(service.info('Consulta disponible', 'Consulte los datos.')).toBe(pending);
    service.dismiss(a); vi.advanceTimersByTime(180);
    expect(service.notifications()).toHaveLength(3);
    expect(service.notifications().at(-1)?.id).toBe(pending);
  });
  it('starts a queued success lifetime only after a visible slot is available', () => {
    const first = service.error('Uno'); service.error('Dos'); service.error('Tres');
    const queued = service.success('Cuatro');
    vi.advanceTimersByTime(10000); expect(service.notifications().some(item => item.id === queued)).toBe(false);
    service.dismiss(first); vi.advanceTimersByTime(180);
    vi.advanceTimersByTime(3999); expect(service.notifications().at(-1)?.closing).toBe(false);
    vi.advanceTimersByTime(181); expect(service.notifications()).toHaveLength(2);
  });
  it('pauses independently for pointer and keyboard focus without resetting the remaining time', () => {
    const id = service.success('Guardado'); vi.advanceTimersByTime(1000);
    service.pause(id, 'pointer'); service.pause(id, 'focus'); vi.advanceTimersByTime(10000);
    service.resume(id, 'pointer'); vi.advanceTimersByTime(10000); expect(service.notifications()[0].closing).toBe(false);
    service.resume(id, 'focus'); vi.advanceTimersByTime(2999); expect(service.notifications()[0].closing).toBe(false);
    vi.advanceTimersByTime(181); expect(service.notifications()).toHaveLength(0);
  });
  it('suspends visible and newly queued lifetimes while navigation chrome covers the region', () => {
    service.suspend('profile', true); const id = service.success('Guardado');
    vi.advanceTimersByTime(10000); expect(service.notifications()[0].closing).toBe(false);
    service.suspend('hidden', true); service.suspend('profile', false); vi.advanceTimersByTime(10000);
    expect(service.notifications()[0].id).toBe(id);
    service.suspend('hidden', false); vi.advanceTimersByTime(4180); expect(service.notifications()).toHaveLength(0);
  });
  it('dismisses once and promotes only one waiting notification', () => {
    const first = service.error('Uno'); service.error('Dos'); service.error('Tres'); service.error('Cuatro'); service.error('Cinco');
    service.dismiss(first); service.dismiss(first); vi.advanceTimersByTime(180);
    expect(service.notifications().map(item => item.title)).toEqual(['Dos', 'Tres', 'Cuatro']);
  });
  it('clears queued messages and timers on logout or destruction', () => {
    service.success('Uno'); service.error('Dos'); service.error('Tres'); service.error('Cuatro');
    service.clear(); vi.runAllTimers(); expect(service.notifications()).toHaveLength(0);
  });
  it('does not expose diagnostic codes, UUIDs, stack traces or URLs', () => {
    service.error('STALE_DRAFT', '11111111-1111-1111-1111-111111111111');
    service.error('No se pudo guardar', 'Exception at https://internal.example/trace');
    expect(JSON.stringify(service.notifications())).not.toMatch(/STALE_DRAFT|11111111|internal\.example/);
  });
});
