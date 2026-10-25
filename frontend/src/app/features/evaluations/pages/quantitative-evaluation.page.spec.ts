import {TestBed} from '@angular/core/testing';
import {provideRouter} from '@angular/router';
import {of, Subject, throwError} from 'rxjs';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {ProcessApi} from '../../processes/data-access/process-api.service';
import {QuantitativeApi, QuantitativeView, QuantitativeInputs} from '../data-access/quantitative-api.service';
import {QuantitativeEvaluationPage} from './quantitative-evaluation.page';

describe('QuantitativeEvaluationPage', () => {
  const inputs = {id: 'set', revision: 1, periodStart: '2026-01-01', periodEnd: '2026-06-30', variables: []} as unknown as QuantitativeInputs;
  const view: QuantitativeView = {processId: 'p', indicatorId: 'i', processState: 'ACTIVE', periodStart: '2026-01-01', periodEnd: '2026-12-31',
    rule: {id: 'rule', methodDefinition: 'Razón sintética', formulaDefinition: 'A / B * 100', strategyKey: 'ratio-percent', strategyVersion: '1',
      variables: [{code: 'A', name: 'Numerador', unit: 'personas', dataType: 'INTEGER', validationDefinition: 'NON_NEGATIVE'}, {code: 'B', name: 'Denominador', unit: 'personas', dataType: 'INTEGER', validationDefinition: 'POSITIVE'}]},
    acceptedSupports: [{evidenceVersionId: 'evidence', requiredSourceId: 'source', reviewDecisionId: 'decision', title: 'Sustento aceptado'}],
    ownInputs: null, verification: null, items: [], page: 0, size: 20, hasNext: false};
  const api = {view: vi.fn(), capture: vi.fn(), verify: vi.fn(), calculate: vi.fn()};
  beforeEach(() => {
    for (const fn of Object.values(api)) fn.mockReset(); api.view.mockReturnValue(of(view)); api.capture.mockReturnValue(of(inputs));
    api.verify.mockReturnValue(of({})); api.calculate.mockReturnValue(of({}));
    TestBed.configureTestingModule({imports: [QuantitativeEvaluationPage], providers: [provideRouter([]),
      {provide: ProcessApi, useValue: {list: () => of([])}}, {provide: QuantitativeApi, useValue: api}]});
  });
  function page() {const fixture = TestBed.createComponent(QuantitativeEvaluationPage); fixture.detectChanges(); const component = fixture.componentInstance;
    component.processId = 'p'; component.selectIndicator('i'); fixture.detectChanges(); return {fixture, component};}
  it('shows exact published formula and variable units with accepted support titles', () => {
    const {fixture} = page(); const text = (fixture.nativeElement as HTMLElement).textContent;
    expect(text).toContain('A / B * 100'); expect(text).toContain('personas'); expect(text).toContain('Sustento aceptado');
    expect(text).toContain('no constituye acreditación');
  });
  it('captures distinct per-variable support choices and the declared period', () => {
    const {component} = page(); component.values = {A: '3', B: '4'}; component.toggle('A', 'evidence'); component.toggle('B', 'evidence'); component.capture();
    expect(api.capture).toHaveBeenCalledWith(view, {periodStart: '2026-01-01', periodEnd: '2026-12-31', predecessorId: null,
      variables: {A: {value: '3', unit: 'personas', evidenceVersionIds: ['evidence']}, B: {value: '4', unit: 'personas', evidenceVersionIds: ['evidence']}}});
  });
  it('requires verification before calculation and does not repeat failed operations', () => {
    api.view.mockReturnValue(of({...view, ownInputs: inputs})); const {component} = page();
    component.justification = 'Justificación suficiente'; component.limitations = 'Limitaciones suficientes'; component.calculate();
    expect(api.calculate).not.toHaveBeenCalled(); component.reason = 'Cifras y fuentes verificadas';
    api.verify.mockReturnValue(throwError(() => new Error('stale'))); component.verify();
    expect(api.verify).toHaveBeenCalledTimes(1); expect(component.view()).toBeNull(); expect(component.versions()).toEqual([]);
  });
  it('keeps the current set id for verified calculation', () => {
    const verified = {...view, ownInputs: inputs, verification: {inputSetId: 'set', verifiedBy: 'actor', verifiedAt: '', reason: 'Verificación suficiente'}};
    api.view.mockReturnValue(of(verified)); const {component} = page(); component.justification = 'Justificación suficiente'; component.limitations = 'Limitaciones suficientes'; component.calculate();
    expect(api.calculate).toHaveBeenCalledWith(verified, 'Justificación suficiente', 'Limitaciones suficientes');
  });
  it('clears scope and cancels an old response when selecting another process', () => {
    const pending = new Subject<QuantitativeView>(); api.view.mockReturnValue(pending); const {component} = page();
    component.values = {A: '3'}; component.selectProcess('other'); pending.next(view);
    expect(component.view()).toBeNull(); expect(component.values).toEqual({}); expect(component.selected).toEqual({});
  });
});
