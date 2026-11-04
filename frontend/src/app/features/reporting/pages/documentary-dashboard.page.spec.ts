import {TestBed} from '@angular/core/testing';
import {provideRouter} from '@angular/router';
import {Subject, of, throwError} from 'rxjs';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {ProcessApi} from '../../processes/data-access/process-api.service';
import {DocumentaryProgressView, ReportingApi} from '../data-access/reporting-api.service';
import {DocumentaryDashboardPage} from './documentary-dashboard.page';

describe('DocumentaryDashboardPage', () => {
  const api = {progress: vi.fn()};
  const processes = {list: vi.fn()};
  const view: DocumentaryProgressView = {
    processId: 'process', modelVersionId: 'model', calculatedAt: '2026-10-24T12:00:00Z',
    formulaVersion: 'SOURCE_COVERAGE_V1', state: 'CONFIGURED', requiredSourceTotal: 4,
    registeredSourceTotal: 3, acceptedSourceTotal: 2, collectionPercent: 75, documentReviewPercent: 50,
    scopeDescription: 'Solo indicadores autorizados', indicatorIds: ['indicator'],
    page: 0, size: 20, totalSources: 21,
    sources: [{id: 'source', indicatorId: 'indicator', code: 'F1', name: 'Informe sintético',
      required: true, applicable: true, cardinalityMin: 1,
      coverages: [{evidenceId: 'evidence', versionId: 'version', decisionId: 'decision', outcome: 'ACCEPTED'}]}]
  };

  beforeEach(() => {
    api.progress.mockReset().mockReturnValue(of(view));
    processes.list.mockReset().mockReturnValue(of([]));
    TestBed.configureTestingModule({imports: [DocumentaryDashboardPage], providers: [
      provideRouter([]), {provide: ReportingApi, useValue: api}, {provide: ProcessApi, useValue: processes}
    ]});
  });

  it('shows source metrics, scope, formula and academic separation', () => {
    const fixture = TestBed.createComponent(DocumentaryDashboardPage);
    fixture.detectChanges();
    fixture.componentInstance.selectProcess('process');
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent;
    expect(text).toContain('75.00 %');
    expect(text).toContain('50.00 %');
    expect(text).toContain('Solo indicadores autorizados');
    expect(text).toContain('fuentes obligatorias aplicables × 100');
    expect(text).toContain('no determina cumplimiento académico');
    expect(text).toContain('Informe sintético');
    expect(text).toContain('Aceptada');
  });

  it('shows distinct zero-denominator reasons without inventing percentages', () => {
    const fixture = TestBed.createComponent(DocumentaryDashboardPage);
    fixture.detectChanges();
    for (const state of ['NOT_CONFIGURED', 'NOT_APPLICABLE'] as const) {
      api.progress.mockReturnValue(of({...view, state, collectionPercent: null, documentReviewPercent: null,
        requiredSourceTotal: 0, registeredSourceTotal: 0, acceptedSourceTotal: 0, sources: []}));
      fixture.componentInstance.selectProcess('process');
      fixture.detectChanges();
      const element = fixture.nativeElement as HTMLElement;
      expect(element.textContent).toContain('No calculable');
      expect(element.querySelector('[role="status"]')?.textContent)
        .toContain(state === 'NOT_CONFIGURED' ? 'No hay fuentes obligatorias' : 'Todas las fuentes obligatorias están excluidas');
      expect(element.textContent).not.toContain('100.00 %');
    }
  });

  it('filters a source and paginates through the API', () => {
    const fixture = TestBed.createComponent(DocumentaryDashboardPage);
    fixture.detectChanges();
    fixture.componentInstance.selectProcess('process');
    fixture.componentInstance.focusSource('source');
    expect(api.progress).toHaveBeenLastCalledWith('process', 0, 'source');
    fixture.componentInstance.load(1);
    expect(api.progress).toHaveBeenLastCalledWith('process', 1, 'source');
    fixture.componentInstance.focusSource('');
    expect(api.progress).toHaveBeenLastCalledWith('process', 0, '');
  });

  it('cancels earlier requests when switching process and clears stale data', () => {
    const stale = new Subject<DocumentaryProgressView>();
    api.progress.mockReturnValueOnce(stale).mockReturnValueOnce(of({...view, processId: 'second'}));
    const fixture = TestBed.createComponent(DocumentaryDashboardPage);
    fixture.detectChanges();
    fixture.componentInstance.selectProcess('first');
    fixture.componentInstance.selectProcess('second');
    stale.next(view);
    expect(fixture.componentInstance.view()?.processId).toBe('second');
    fixture.componentInstance.selectProcess('');
    expect(fixture.componentInstance.view()).toBeNull();
  });

  it('removes prior data after a denied or failed request', () => {
    const fixture = TestBed.createComponent(DocumentaryDashboardPage);
    fixture.detectChanges();
    fixture.componentInstance.selectProcess('process');
    api.progress.mockReturnValue(throwError(() => new Error('Forbidden')));
    fixture.componentInstance.load();
    fixture.detectChanges();
    expect(fixture.componentInstance.view()).toBeNull();
    expect((fixture.nativeElement as HTMLElement).querySelector('[role="alert"]')?.textContent)
      .toContain('ámbito autorizado');
  });
});
