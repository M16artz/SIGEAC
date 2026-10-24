import {TestBed} from '@angular/core/testing';
import {provideRouter} from '@angular/router';
import {Subject, of, throwError} from 'rxjs';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {ApiProblemError} from '../../../core/http/api-client';
import {ProcessApi} from '../../processes/data-access/process-api.service';
import {DocumentarySnapshotHeader, DocumentarySnapshotView, ReportingApi} from '../data-access/reporting-api.service';
import {DocumentaryHistoryPage} from './documentary-history.page';

describe('DocumentaryHistoryPage', () => {
  const api = {snapshots: vi.fn(), snapshot: vi.fn(), createSnapshot: vi.fn()};
  const processes = {list: vi.fn(), get: vi.fn()};
  const header: DocumentarySnapshotHeader = {
    id: 'snapshot', processId: 'process', careerId: 'career', modelVersionId: 'model', processRevision: 8,
    createdBy: 'actor', createdAt: '2026-10-24T12:00:00Z', reason: 'Corte sintético de seguimiento.', sha256: 'a'.repeat(64),
    progress: {status: 'CONFIGURED', formulaVersion: 'SOURCE_COVERAGE_V1', requiredSourceTotal: 4, registeredSourceTotal: 3, acceptedSourceTotal: 2}
  };
  const snapshot: DocumentarySnapshotView = {header, data: {...header, processState: 'ACTIVE', indicatorIds: [], sources: [], applicability: []}};

  beforeEach(() => {
    api.snapshots.mockReset().mockReturnValue(of({items: [header], page: 0, size: 20, hasNext: false}));
    api.snapshot.mockReset().mockReturnValue(of(snapshot));
    api.createSnapshot.mockReset().mockReturnValue(of(snapshot));
    processes.list.mockReset().mockReturnValue(of([]));
    processes.get.mockReset().mockReturnValue(of({id: 'process', state: 'ACTIVE', revision: 7}));
    TestBed.configureTestingModule({imports: [DocumentaryHistoryPage], providers: [provideRouter([]),
      {provide: ReportingApi, useValue: api}, {provide: ProcessApi, useValue: processes}]});
  });

  it('shows saved counts and digest without requesting current progress', () => {
    const fixture = TestBed.createComponent(DocumentaryHistoryPage);
    fixture.detectChanges();
    fixture.componentInstance.selectProcess('process');
    fixture.componentInstance.open('snapshot');
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent;
    expect(text).toContain('75.00 %');
    expect(text).toContain('50.00 %');
    expect(text).toContain(header.sha256);
    expect(text).toContain('no determina cumplimiento académico');
  });

  it('reads the latest process revision and sends only the reason with If-Match', () => {
    const fixture = TestBed.createComponent(DocumentaryHistoryPage);
    fixture.detectChanges();
    fixture.componentInstance.selectProcess('process');
    fixture.componentInstance.reason = 'Corte actual con pendientes explícitos.';
    fixture.componentInstance.create();
    expect(processes.get).toHaveBeenCalledWith('process');
    expect(api.createSnapshot).toHaveBeenCalledWith('process', 'Corte actual con pendientes explícitos.', '"7"');
    expect(fixture.componentInstance.message()).toContain('El proceso continúa activo');
  });

  it('does not submit snapshots for inactive processes', () => {
    processes.get.mockReturnValue(of({id: 'process', state: 'DOCUMENTARY_CLOSED', revision: 7}));
    const fixture = TestBed.createComponent(DocumentaryHistoryPage);
    fixture.detectChanges();
    fixture.componentInstance.selectProcess('process');
    fixture.componentInstance.reason = 'Corte actual de un proceso cerrado.';
    fixture.componentInstance.create();
    expect(api.createSnapshot).not.toHaveBeenCalled();
    expect(fixture.componentInstance.error()).toContain('proceso esté activo');
  });

  it('clears stale historical content when integrity or authorization checks fail', () => {
    const fixture = TestBed.createComponent(DocumentaryHistoryPage);
    fixture.detectChanges();
    fixture.componentInstance.selectProcess('process');
    fixture.componentInstance.open('snapshot');
    api.snapshot.mockReturnValue(throwError(() => new Error('SNAPSHOT_INTEGRITY_FAILED')));
    fixture.componentInstance.open('snapshot');
    expect(fixture.componentInstance.selected()).toBeNull();
    expect(fixture.componentInstance.history()).toBeNull();
    expect(fixture.componentInstance.error()).toContain('verificar');
  });

  it('cancels detail loading when switching process and handles stale writes without retrying', () => {
    const pending = new Subject<DocumentarySnapshotView>();
    api.snapshot.mockReturnValue(pending);
    const fixture = TestBed.createComponent(DocumentaryHistoryPage);
    fixture.detectChanges();
    fixture.componentInstance.selectProcess('process');
    fixture.componentInstance.open('snapshot');
    fixture.componentInstance.selectProcess('another');
    pending.next(snapshot);
    expect(fixture.componentInstance.selected()).toBeNull();
    api.createSnapshot.mockReturnValue(throwError(() => new ApiProblemError({type: 'urn:sec:test', title: 'Stale',
      status: 412, detail: 'Stale process', instance: '/', code: 'STALE_PROCESS', correlationId: 'synthetic-id', details: {}})));
    fixture.componentInstance.reason = 'Corte rechazado por revisión obsoleta.';
    fixture.componentInstance.create();
    expect(api.createSnapshot).toHaveBeenCalledTimes(1);
    expect(fixture.componentInstance.error()).toContain('El proceso cambió');
  });
});
