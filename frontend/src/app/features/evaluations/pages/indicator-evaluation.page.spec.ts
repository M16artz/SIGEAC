import {TestBed} from '@angular/core/testing';
import {Subject, of, throwError} from 'rxjs';
import {describe, beforeEach, it, expect, vi} from 'vitest';
import {AcademicEvaluationApi, ManualEvaluationView} from '../data-access/academic-evaluation-api.service';
import {ProcessApi} from '../../processes/data-access/process-api.service';
import {IndicatorEvaluationPage} from './indicator-evaluation.page';

describe('IndicatorEvaluationPage', () => {
  const view: ManualEvaluationView = {processId: 'p', indicatorId: 'i', modelVersionId: 'm', processState: 'DOCUMENTARY_CLOSED',
    method: 'Juicio cualitativo publicado', ratingOptions: [{id: 'option', code: 'OK', label: 'Cumple', position: 1}],
    acceptedSupports: [{evidenceVersionId: 'version', requiredSourceId: 'source', reviewDecisionId: 'decision', title: 'Sustento sintético'}],
    ownRevision: 2, ownLatestId: 'previous', items: [], page: 0, size: 20, hasNext: false};
  const api = {view: vi.fn(), save: vi.fn()};
  beforeEach(() => {
    api.view.mockReset().mockReturnValue(of(view)); api.save.mockReset().mockReturnValue(of({}));
    TestBed.configureTestingModule({imports: [IndicatorEvaluationPage], providers: [
      {provide: AcademicEvaluationApi, useValue: api}, {provide: ProcessApi, useValue: {list: () => of([])}}
    ]});
  });
  function page() {
    const fixture = TestBed.createComponent(IndicatorEvaluationPage); fixture.detectChanges();
    const component = fixture.componentInstance; component.processId = 'p'; component.selectIndicator('i'); fixture.detectChanges();
    return {fixture, component};
  }
  it('shows the published method, accepted supports and independent academic meaning', () => {
    const {fixture} = page(); const text = (fixture.nativeElement as HTMLElement).textContent;
    expect(text).toContain('Juicio cualitativo publicado'); expect(text).toContain('version');
    expect(text).toContain('Sustento sintético');
    expect(text).toContain('no constituye acreditación'); expect(text).toContain('Revisión propia: 2');
  });
  it('saves selected support and its own predecessor then reloads the history', () => {
    const {component} = page(); component.ratingOptionId = 'option'; component.toggle('version');
    component.justification = 'Justificación suficiente'; component.limitations = 'Limitaciones declaradas'; component.save();
    expect(api.save).toHaveBeenCalledWith(view, {ratingOptionId: 'option', evidenceVersionIds: ['version'],
      justification: 'Justificación suficiente', limitations: 'Limitaciones declaradas', predecessorId: 'previous'});
    expect(api.view).toHaveBeenCalledTimes(2); expect(component.message()).toContain('Valoración conservada');
  });
  it('clears protected history on error and does not retry a failed save', () => {
    const {component} = page(); component.ratingOptionId = 'option'; component.toggle('version');
    component.justification = 'Justificación suficiente'; component.limitations = 'Limitaciones declaradas';
    api.save.mockReturnValue(throwError(() => new Error('stale'))); component.save();
    expect(component.view()).toBeNull(); expect(component.versions()).toEqual([]); expect(api.save).toHaveBeenCalledTimes(1);
  });
  it('does not save a closed process or a form without accepted evidence', () => {
    const {component} = page(); component.ratingOptionId = 'option'; component.justification = 'Justificación suficiente';
    component.limitations = 'Limitaciones declaradas'; component.save(); expect(api.save).not.toHaveBeenCalled();
    api.view.mockReturnValue(of({...view, processState: 'CLOSED'})); component.load(); component.toggle('version'); component.save();
    expect(api.save).not.toHaveBeenCalled();
  });
  it('cancels old indicator requests and clears previous form on scope change', () => {
    const pending = new Subject<ManualEvaluationView>(); api.view.mockReturnValue(pending);
    const {component} = page(); component.justification = 'Texto privado'; component.selectIndicator('other');
    component.selectProcess('next'); pending.next(view);
    expect(component.view()).toBeNull(); expect(component.justification).toBe(''); expect(component.selected).toEqual([]);
  });
});
