import {TestBed} from '@angular/core/testing';
import {of, Subject, throwError} from 'rxjs';
import {describe, expect, it, vi} from 'vitest';
import {AccessApi} from '../data-access/access-api.service';
import {AuditEventView} from '../data-access/access.models';
import {AccessAuditPageComponent} from './access-audit.page';

const event: AuditEventView = {id: 'event-id', occurredAt: '2026-11-20T10:00:00Z', correlationId: 'correlation-uuid',
  action: 'IDENTITY_CREATED', objectType: 'ACCESS_IDENTITY', objectId: 'object-uuid', outcome: 'SUCCESS', reasonCode: null, scopeCareerId: 'career', scopeProcessId: null};

function setup() {
  const api = {searchAudit: vi.fn().mockReturnValue(of({items: [event], page: 0, size: 20, total: 21}))};
  TestBed.configureTestingModule({imports: [AccessAuditPageComponent], providers: [{provide: AccessApi, useValue: api}]});
  const fixture = TestBed.createComponent(AccessAuditPageComponent);
  fixture.detectChanges();
  return {fixture, page: fixture.componentInstance, api, root: fixture.nativeElement as HTMLElement};
}

describe('AccessAuditPageComponent', () => {
  it('retains filters and pagination request values', () => {
    const {page, api, root} = setup();
    expect(root.querySelector('.access-total')?.textContent).toContain('21');
    expect(api.searchAudit).toHaveBeenCalledWith({objectType: undefined, objectId: undefined, page: 0, size: 20});
    page.filter.patchValue({objectType: ' ACCESS_IDENTITY ', objectId: ' object-uuid '}); page.search(1);
    expect(api.searchAudit).toHaveBeenLastCalledWith({objectType: 'ACCESS_IDENTITY', objectId: 'object-uuid', page: 1, size: 20});
    expect(root.querySelector('.access-pagination')?.textContent).toContain('Página 1 de 2');
  });

  it('keeps technical traceability in expandable details, not the main row', () => {
    const {fixture, root} = setup();
    expect(root.querySelector('tbody')?.textContent).not.toContain(event.objectId);
    const toggle = root.querySelector<HTMLButtonElement>('.audit-events-toggle')!;
    toggle.click(); fixture.detectChanges();
    expect(toggle.getAttribute('aria-expanded')).toBe('true');
    expect(root.querySelector('.audit-events-details')?.textContent).toContain(event.objectId);
    expect(root.querySelector('.audit-events-details')?.textContent).toContain(event.correlationId);
  });

  it('retains loading, empty and error feedback', () => {
    const {fixture, page, api, root} = setup();
    const pending = new Subject(); api.searchAudit.mockReturnValueOnce(pending);
    page.search(0); fixture.detectChanges();
    expect(root.textContent).toContain('Consultando eventos');
    pending.complete();
    api.searchAudit.mockReturnValueOnce(of({items: [], page: 0, size: 20, total: 0}));
    page.search(0); fixture.detectChanges();
    expect(root.textContent).toContain('No hay eventos visibles');
    api.searchAudit.mockReturnValueOnce(throwError(() => new Error('Failed')));
    page.search(0); fixture.detectChanges();
    expect(root.querySelector('[role="alert"]')).not.toBeNull();
  });
});
