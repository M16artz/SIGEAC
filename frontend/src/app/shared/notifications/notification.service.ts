import {Injectable, OnDestroy, signal} from '@angular/core';

export type NotificationKind = 'success' | 'error' | 'warning' | 'info';
export interface NotificationOptions { readonly key?: string; }
export interface ToastNotification {
  readonly id: number;
  readonly kind: NotificationKind;
  readonly title: string;
  readonly description: string;
  readonly key: string;
  readonly closing: boolean;
}
interface Lifetime { remaining: number; started: number; timer?: ReturnType<typeof setTimeout>; reasons: Set<string>; }

/** Operation feedback only. Field validation and persistent section state stay local. */
@Injectable({providedIn: 'root'})
export class NotificationService implements OnDestroy {
  private readonly state = signal<readonly ToastNotification[]>([]);
  readonly notifications = this.state.asReadonly();
  private readonly waiting: ToastNotification[] = [];
  private readonly lifetimes = new Map<number, Lifetime>();
  private readonly removalTimers = new Map<number, ReturnType<typeof setTimeout>>();
  private readonly globalPauses = new Set<string>();
  private sequence = 0;
  readonly limit = 3;

  success(title: string, description = 'Los cambios se guardaron correctamente.', options: NotificationOptions = {}): number {
    return this.show('success', title, description, options);
  }
  error(title: string, description = 'Intente nuevamente. Si el problema continúa, contacte al administrador.', options: NotificationOptions = {}): number {
    return this.show('error', title, description, options);
  }
  warning(title: string, description: string, options: NotificationOptions = {}): number {
    return this.show('warning', title, description, options);
  }
  info(title: string, description: string, options: NotificationOptions = {}): number {
    return this.show('info', title, description, options);
  }

  private show(kind: NotificationKind, title: string, description: string, options: NotificationOptions): number {
    title = this.safeText(title, {success: 'Operación completada', error: 'No se pudo completar la operación', warning: 'Revise la información', info: 'Información disponible'}[kind]);
    description = this.safeText(description, 'Revise la información e inténtelo nuevamente.');
    const key = `${kind}:${options.key ?? title + ':' + description}`;
    const duplicate = [...this.state(), ...this.waiting].find(item => item.key === key && !item.closing);
    if (duplicate) return duplicate.id;
    const item: ToastNotification = {id: ++this.sequence, kind, title, description, key, closing: false};
    if (this.state().length < this.limit) this.activate(item); else this.waiting.push(item);
    return item.id;
  }

  private activate(item: ToastNotification): void {
    this.state.update(items => [...items, item]);
    if (item.kind === 'success') {
      this.lifetimes.set(item.id, {remaining: 4000, started: Date.now(), reasons: new Set()});
      this.start(item.id);
    }
  }
  dismiss(id: number): void {
    const index = this.waiting.findIndex(item => item.id === id);
    if (index >= 0) { this.waiting.splice(index, 1); return; }
    if (!this.state().some(item => item.id === id && !item.closing)) return;
    this.stop(id);
    this.lifetimes.delete(id);
    this.state.update(items => items.map(item => item.id === id ? {...item, closing: true} : item));
    this.removalTimers.set(id, setTimeout(() => {
      this.removalTimers.delete(id);
      this.state.update(items => items.filter(item => item.id !== id));
      const next = this.waiting.shift();
      if (next) this.activate(next);
    }, 180));
  }
  pause(id: number, reason: string): void {
    this.stop(id);
    this.lifetimes.get(id)?.reasons.add(reason);
  }
  resume(id: number, reason: string): void {
    this.lifetimes.get(id)?.reasons.delete(reason);
    this.start(id);
  }
  suspend(reason: string, suspended: boolean): void {
    if (suspended) {
      if (this.globalPauses.has(reason)) return;
      for (const id of this.lifetimes.keys()) this.stop(id);
      this.globalPauses.add(reason);
    } else {
      this.globalPauses.delete(reason);
      for (const id of this.lifetimes.keys()) this.start(id);
    }
  }
  clear(): void {
    for (const id of this.lifetimes.keys()) this.stop(id);
    for (const timer of this.removalTimers.values()) clearTimeout(timer);
    this.lifetimes.clear(); this.removalTimers.clear(); this.waiting.length = 0;
    this.state.set([]);
  }
  ngOnDestroy(): void { this.clear(); }
  private stop(id: number): void {
    const life = this.lifetimes.get(id);
    if (!life?.timer) return;
    clearTimeout(life.timer); life.timer = undefined;
    life.remaining = Math.max(0, life.remaining - (Date.now() - life.started));
  }
  private start(id: number): void {
    const life = this.lifetimes.get(id);
    if (!life || life.timer || life.reasons.size || this.globalPauses.size) return;
    life.started = Date.now(); life.timer = setTimeout(() => this.dismiss(id), life.remaining);
  }
  private safeText(value: string, fallback: string): string {
    // No HTML interpretation and no accidental transport of backend diagnostics.
    if (!value?.trim() || /[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}|https?:\/\/|\b(?:Exception|stack trace|SQLSTATE|Traceback)\b|\bat \S+\([^)]*:\d+/i.test(value) || /\b[A-Z][A-Z0-9]+_[A-Z0-9_]+\b/.test(value)) return fallback;
    return value.replace(/\s+/g, ' ').trim().slice(0, 320);
  }
}
