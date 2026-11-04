import {TestBed} from '@angular/core/testing';
import {provideRouter} from '@angular/router';
import {Subject, of, throwError} from 'rxjs';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {ApiProblemError} from '../../../core/http/api-client';
import {ProcessApi} from '../../processes/data-access/process-api.service';
import {DocumentaryClosureView, ReportingApi} from '../data-access/reporting-api.service';
import {DocumentaryClosurePageComponent} from './documentary-closure.page';

describe('DocumentaryClosurePage', () => {
  const api = {closures: vi.fn(), closure: vi.fn(), closeDocumentary: vi.fn()};
  const processes = {list: vi.fn(), get: vi.fn()};
  const closure: DocumentaryClosureView = {sha256: 'b'.repeat(64), data: {
    id: 'closure', processId: 'process', careerId: 'career', snapshotId: 'snapshot', snapshotSha256: 'a'.repeat(64),
    state: 'DOCUMENTARY_CLOSED', processRevision: 9, policyVersion: 'DOCUMENTARY_STRICT_V1', closedBy: 'actor',
    closedAt: '2026-10-24T12:00:00Z', reason: 'Cierre documental sintético con aceptación suficiente.', files: []
  }};
  beforeEach(() => {
    api.closures.mockReset().mockReturnValue(of({items: [], page: 0, size: 20, hasNext: false}));
    api.closure.mockReset().mockReturnValue(of(closure));
    api.closeDocumentary.mockReset().mockReturnValue(of(closure));
    processes.list.mockReset().mockReturnValue(of([]));
    processes.get.mockReset().mockImplementation((id: string) => of({id, state: 'ACTIVE', revision: 7}));
    TestBed.configureTestingModule({imports: [DocumentaryClosurePageComponent], providers: [provideRouter([]),
      {provide: ReportingApi, useValue: api}, {provide: ProcessApi, useValue: processes}]});
  });
  it('closes with the current process revision and links the preserved cut', () => {
    const fixture = TestBed.createComponent(DocumentaryClosurePageComponent);
    fixture.detectChanges();
    fixture.componentInstance.selectProcess('process');
    fixture.componentInstance.reason = closure.data.reason;
    fixture.componentInstance.close();
    fixture.detectChanges();
    expect(api.closeDocumentary).toHaveBeenCalledWith('process', closure.data.reason, '"7"');
    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('Fase documental cerrada');
    expect(element.textContent).toContain(closure.sha256);
    expect(element.querySelector('a[href*="snapshotId=snapshot"]')).not.toBeNull();
  });
  it('shows documentary blockers without retrying or retaining a prior closure', () => {
    const fixture = TestBed.createComponent(DocumentaryClosurePageComponent);
    fixture.detectChanges(); fixture.componentInstance.selectProcess('process'); fixture.componentInstance.open('closure');
    api.closeDocumentary.mockReturnValue(throwError(() => new ApiProblemError({type: 'urn:sec:closure', title: 'Blocked', status: 409,
      detail: 'Pending source', instance: '/', code: 'CLOSURE_BLOCKED', correlationId: 'synthetic-id', details: {sourceIds: 'source'}})));
    fixture.componentInstance.reason = closure.data.reason; fixture.componentInstance.close();
    expect(fixture.componentInstance.selected()).toBeNull();
    expect(fixture.componentInstance.blockers()).toContain('Fuentes pendientes: source');
    expect(api.closeDocumentary).toHaveBeenCalledTimes(1);
  });
  it('does not submit another closure when the process is no longer active', () => {
    processes.get.mockReturnValue(of({id: 'process', state: 'DOCUMENTARY_CLOSED', revision: 9}));
    const fixture = TestBed.createComponent(DocumentaryClosurePageComponent);
    fixture.detectChanges(); fixture.componentInstance.selectProcess('process');
    fixture.componentInstance.reason = closure.data.reason; fixture.componentInstance.close();
    expect(api.closeDocumentary).not.toHaveBeenCalled();
    expect(fixture.componentInstance.error()).toContain('estado activo');
  });
  it('cancels historical reads when switching process', () => {
    const pending = new Subject<DocumentaryClosureView>(); api.closure.mockReturnValue(pending);
    const fixture = TestBed.createComponent(DocumentaryClosurePageComponent);
    fixture.detectChanges(); fixture.componentInstance.selectProcess('process'); fixture.componentInstance.open('closure');
    fixture.componentInstance.selectProcess('other'); pending.next(closure);
    expect(fixture.componentInstance.selected()).toBeNull();
  });
});
