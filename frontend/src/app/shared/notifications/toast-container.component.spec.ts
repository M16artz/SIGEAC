import {TestBed} from '@angular/core/testing';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {ToastContainerComponent} from './toast-container.component';
import {NotificationService} from './notification.service';

function setup() {
  TestBed.configureTestingModule({imports: [ToastContainerComponent]});
  const anchor = document.createElement('div'); document.body.append(anchor);
  const fixture = TestBed.createComponent(ToastContainerComponent);
  fixture.componentRef.setInput('anchor', anchor); fixture.detectChanges(); fixture.detectChanges();
  const service = TestBed.inject(NotificationService);
  const root = fixture.nativeElement as HTMLElement;
  return {fixture, root, service, anchor};
}
describe('ToastContainerComponent', () => {
  beforeEach(() => vi.useFakeTimers());
  afterEach(() => { TestBed.resetTestingModule(); document.querySelectorAll('body > div:empty').forEach(el => el.remove()); vi.useRealTimers(); });
  it('renders semantic icons, titles, descriptions and explicit keyboard-operable close controls', () => {
    const {fixture, service, root} = setup();
    service.success('Usuario registrado', 'La cuenta se creó.'); service.error('No se pudo guardar', 'Revise los datos.'); service.warning('Información incompleta', 'Complete los campos.'); fixture.detectChanges();
    expect(root.querySelectorAll('.toast-card')).toHaveLength(3);
    expect(root.querySelector('[popover="manual"][role="region"]')?.getAttribute('aria-label')).toBe('Notificaciones');
    expect(root.querySelectorAll('.toast-icon[aria-hidden="true"]')).toHaveLength(3);
    expect(root.querySelector('.toast-close')?.getAttribute('aria-label')).toContain('Usuario registrado');
    (root.querySelector('.toast-close') as HTMLButtonElement).click(); fixture.detectChanges(); vi.advanceTimersByTime(180); fixture.detectChanges();
    expect(root.querySelectorAll('.toast-card')).toHaveLength(2);
  });
  it('renders information notifications without changing focus on appearance', () => {
    const {fixture, service, root, anchor} = setup(); const trigger = document.createElement('button'); anchor.append(trigger); trigger.focus();
    service.info('Información actualizada', 'Los datos están disponibles.'); fixture.detectChanges();
    expect(root.querySelector('.toast-info .toast-title')?.textContent).toBe('Información actualizada');
    expect(document.activeElement).toBe(trigger);
  });
  it('uses existing polite and assertive fallback regions without announcing the close label', () => {
    const {fixture, service, root} = setup();
    service.info('Datos disponibles', 'Puede consultarlos.'); service.error('No se pudo guardar', 'Inténtelo nuevamente.'); fixture.detectChanges(); vi.advanceTimersByTime(20); fixture.detectChanges();
    expect(root.querySelector('[role="status"]')?.textContent).toContain('Datos disponibles');
    expect(root.querySelector('[role="alert"]')?.textContent).toContain('No se pudo guardar');
    expect(root.querySelector('[role="status"]')?.textContent).not.toContain('Cerrar');
  });
  it('hides the stack and pauses time while the profile menu is open', () => {
    const {fixture, service, root} = setup(); service.success('Guardado'); fixture.detectChanges();
    fixture.componentRef.setInput('suspended', true); fixture.detectChanges(); vi.advanceTimersByTime(10000); fixture.detectChanges();
    expect(service.notifications()[0].closing).toBe(false);
    expect((root.querySelector('.sigeac-toasts') as HTMLElement).hidden).toBe(true);
    fixture.componentRef.setInput('suspended', false); fixture.detectChanges(); vi.advanceTimersByTime(4180); fixture.detectChanges(); expect(service.notifications()).toHaveLength(0);
  });
  it('pauses expiry while a toast has keyboard focus', () => {
    const {fixture, service, root} = setup(); service.success('Guardado'); fixture.detectChanges();
    const card = root.querySelector('.toast-card')!; card.dispatchEvent(new FocusEvent('focusin', {bubbles: true}));
    vi.advanceTimersByTime(10000); expect(service.notifications()[0].closing).toBe(false);
    card.dispatchEvent(new FocusEvent('focusout', {bubbles: true})); vi.advanceTimersByTime(4180); fixture.detectChanges(); expect(service.notifications()).toHaveLength(0);
  });
  it('moves keyboard focus to the next toast, then back to the prior trigger on manual close', () => {
    const {fixture, service, root, anchor} = setup(); const trigger = document.createElement('button'); anchor.append(trigger); trigger.focus();
    service.error('Primer error'); service.info('Datos disponibles', 'Puede consultarlos.'); fixture.detectChanges();
    const buttons = root.querySelectorAll<HTMLButtonElement>('.toast-close'); buttons[0].focus(); buttons[0].click(); fixture.detectChanges();
    expect(document.activeElement).toBe(buttons[1]);
    vi.advanceTimersByTime(180); fixture.detectChanges(); buttons[1].click(); fixture.detectChanges();
    expect(document.activeElement).toBe(trigger);
  });
  it('uses ariaNotify when supported instead of duplicating live region announcements', () => {
    const announce = vi.fn(); Object.defineProperty(document, 'ariaNotify', {value: announce, configurable: true});
    try {
      const {fixture, service, root} = setup(); service.error('No se pudo guardar', 'Revise los datos.'); fixture.detectChanges();
      expect(announce).toHaveBeenCalledWith('No se pudo guardar. Revise los datos.', {priority: 'high'});
      expect(root.querySelector('[role="alert"]')?.textContent).toBe('');
    } finally { delete (document as Document & {ariaNotify?: unknown}).ariaNotify; }
  });
  it('supports native manual popovers without light dismissal and hides them when suspended', () => {
    const {fixture, service, root} = setup(); const stack = root.querySelector('.sigeac-toasts') as HTMLElement;
    let open = false; const show = vi.fn(() => { open = true; }); const hide = vi.fn(() => { open = false; });
    Object.defineProperty(stack, 'showPopover', {value: show}); Object.defineProperty(stack, 'hidePopover', {value: hide});
    const original = stack.matches.bind(stack); vi.spyOn(stack, 'matches').mockImplementation(s => s === ':popover-open' ? open : original(s));
    service.info('Consulta disponible', 'Datos actualizados.'); fixture.detectChanges(); expect(show).toHaveBeenCalledOnce();
    fixture.componentRef.setInput('suspended', true); fixture.detectChanges(); expect(hide).toHaveBeenCalledOnce();
  });
  it('clears notifications when the authenticated host is destroyed', () => {
    const {fixture, service} = setup(); service.error('No se pudo guardar'); fixture.detectChanges(); fixture.destroy(); expect(service.notifications()).toHaveLength(0);
  });
});
