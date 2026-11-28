import {DOCUMENT} from '@angular/common';
import {AfterViewInit, ChangeDetectionStrategy, Component, ElementRef, OnDestroy, ViewChild, effect, inject, input, signal, ViewEncapsulation} from '@angular/core';
import {NotificationService, ToastNotification} from './notification.service';

@Component({
  selector: 'sec-toast-container', standalone: true,
  templateUrl: './toast-container.component.html', styleUrl: './toast-container.component.scss',
  encapsulation: ViewEncapsulation.None, changeDetection: ChangeDetectionStrategy.OnPush
})
export class ToastContainerComponent implements AfterViewInit, OnDestroy {
  readonly service = inject(NotificationService);
  private readonly document = inject(DOCUMENT);
  readonly anchor = input.required<HTMLElement>();
  readonly suspended = input(false);
  readonly top = signal(100);
  readonly right = signal(24);
  readonly fallback = signal(false);
  readonly polite = signal('');
  readonly urgent = signal('');
  private readonly modalOpen = signal(false);
  private readonly ready = signal(false);
  private readonly announced = new Set<number>();
  private previousFocus: HTMLElement | null = null;
  private resizeObserver?: ResizeObserver;
  private mutationObserver?: MutationObserver;
  private announcementTimer?: ReturnType<typeof setTimeout>;
  private readonly pendingAnnouncements: ToastNotification[] = [];
  @ViewChild('stack', {static: true}) private stack!: ElementRef<HTMLElement>;
  private readonly onResize = () => this.position();
  private readonly onVisibility = () => this.service.suspend('document-hidden', this.document.hidden);

  constructor() {
    effect(() => {
      const paused = !this.ready() || this.suspended() || this.modalOpen();
      this.service.suspend('toast-container', paused);
      const items = this.service.notifications();
      if (!this.ready()) return;
      const element = this.stack.nativeElement;
      const visible = items.length > 0 && !paused;
      if (paused && this.pendingAnnouncements.length) {
        clearTimeout(this.announcementTimer);
        for (const item of this.pendingAnnouncements) this.announced.delete(item.id);
        this.pendingAnnouncements.length = 0;
      }
      if (typeof element.showPopover === 'function') {
        if (visible && !element.matches(':popover-open')) element.showPopover();
        if (!visible && element.matches(':popover-open')) element.hidePopover();
      } else { this.fallback.set(true); element.hidden = !visible; }
      if (visible) {
        this.position();
        const fresh = items.filter(item => !item.closing && !this.announced.has(item.id));
        if (fresh.length) {
          if (!this.previousFocus || !element.contains(this.document.activeElement)) this.previousFocus = this.document.activeElement as HTMLElement;
          for (const item of fresh) this.announced.add(item.id);
          this.announce(fresh);
        }
      }
      for (const id of this.announced) if (!items.some(item => item.id === id)) this.announced.delete(id);
    });
  }
  ngAfterViewInit(): void {
    const view = this.document.defaultView;
    if (view && typeof view.ResizeObserver === 'function') {
      this.resizeObserver = new view.ResizeObserver(() => this.position());
      this.resizeObserver.observe(this.anchor());
    }
    if (view && typeof view.MutationObserver === 'function') {
      this.mutationObserver = new view.MutationObserver(() => this.modalOpen.set(!!this.document.querySelector('dialog[open]')));
      this.mutationObserver.observe(this.document.body, {subtree: true, attributes: true, attributeFilter: ['open']});
    }
    this.modalOpen.set(!!this.document.querySelector('dialog[open]'));
    view?.addEventListener('resize', this.onResize);
    this.document.addEventListener('visibilitychange', this.onVisibility);
    this.onVisibility(); this.position(); this.ready.set(true);
  }
  private position(): void {
    const view = this.document.defaultView;
    if (!view) return;
    const rect = this.anchor().getBoundingClientRect();
    const belowHeading = Math.max(12, rect.bottom + 12);
    // At extreme text zoom the protected header can be taller than the viewport.
    // Keep the notification reachable rather than placing it outside the screen.
    this.top.set(belowHeading + 56 <= view.innerHeight ? belowHeading : 12);
    const padding = parseFloat(view.getComputedStyle(this.anchor()).paddingRight) || 0;
    this.right.set(Math.max(12, view.innerWidth - rect.right + padding));
  }
  close(item: ToastNotification): void {
    const element = this.stack.nativeElement.querySelector(`[data-toast-id="${item.id}"]`);
    if (element?.contains(this.document.activeElement)) {
      const next = this.service.notifications().find(value => value.id !== item.id && !value.closing);
      const target = next ? this.stack.nativeElement.querySelector<HTMLElement>(`[data-toast-id="${next.id}"] button`)
        : this.previousFocus?.isConnected ? this.previousFocus : this.document.querySelector<HTMLElement>('#main-content');
      target?.focus({preventScroll: true});
    }
    this.service.dismiss(item.id);
  }
  focusOut(id: number, event: FocusEvent): void {
    if (!(event.currentTarget as HTMLElement).contains(event.relatedTarget as Node | null)) this.service.resume(id, 'focus');
  }
  private announce(items: readonly ToastNotification[]): void {
    const document = this.document as Document & {ariaNotify?: (message: string, options: {priority: 'normal' | 'high'}) => void};
    if (typeof document.ariaNotify === 'function') {
      for (const item of items) document.ariaNotify(`${item.title}. ${item.description}`, {priority: item.kind === 'error' ? 'high' : 'normal'});
    } else {
      // Empty first so identical feedback on a later operation is announced again.
      this.polite.set(''); this.urgent.set(''); clearTimeout(this.announcementTimer);
      this.pendingAnnouncements.push(...items);
      this.announcementTimer = setTimeout(() => {
        const pending = this.pendingAnnouncements.splice(0).filter(item => this.service.notifications().some(active => active.id === item.id && !active.closing));
        this.polite.set(pending.filter(item => item.kind !== 'error').map(item => `${item.title}. ${item.description}`).join(' '));
        this.urgent.set(pending.filter(item => item.kind === 'error').map(item => `${item.title}. ${item.description}`).join(' '));
      }, 20);
    }
  }
  ngOnDestroy(): void {
    this.resizeObserver?.disconnect(); this.mutationObserver?.disconnect();
    clearTimeout(this.announcementTimer);
    this.document.defaultView?.removeEventListener('resize', this.onResize);
    this.document.removeEventListener('visibilitychange', this.onVisibility);
    this.service.clear(); this.service.suspend('toast-container', false); this.service.suspend('document-hidden', false);
  }
}
