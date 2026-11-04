import {TestBed} from '@angular/core/testing';
import {of, Subject, throwError} from 'rxjs';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {ProcessApi} from '../../processes/data-access/process-api.service';
import {AccessSessionService} from '../../access/data-access/access-session.service';
import {ReconciliationApi, ReconciliationView, AcademicJudgement} from '../data-access/reconciliation-api.service';
import {AcademicDisagreementPage} from './academic-disagreement.page';

describe('AcademicDisagreementPage', () => {
  const first = {id: 'first', kind: 'MANUAL', authorId: 'one', ratingLabel: 'Cumple', result: null, justification: 'Primer fundamento', limitations: 'Limitación primera', method: 'Método publicado', supports: [], flags: []} as unknown as AcademicJudgement;
  const second = {...first, id: 'second', authorId: 'two', ratingLabel: 'Parcial', justification: 'Segundo fundamento'};
  const view = {processId: 'p', indicatorId: 'i', actorId: 'committee', processState: 'ACTIVE', canReconcile: true, status: 'PENDING_RECONCILIATION', result: null,
    current: [first, second], policy: {id: 'policy', revision: 1, procedureReference: 'Procedimiento sintético', legalBasis: 'Fundamento competente', documentReference: 'fixture://policy'},
    latestCase: {id: 'case', revision: 1, judgements: [first, second]}, resolution: null, history: [first, second], page: 0, hasNext: false,
    cases: [], casePage: 0, hasNextCase: false, size: 20} as unknown as ReconciliationView;
  const api = {view: vi.fn(), policy: vi.fn(), open: vi.fn(), resolve: vi.fn()};
  beforeEach(() => {
    for (const fn of Object.values(api)) fn.mockReset();api.view.mockReturnValue(of(view));api.resolve.mockReturnValue(of({}));api.policy.mockReturnValue(of({}));api.open.mockReturnValue(of({}));
    TestBed.configureTestingModule({imports: [AcademicDisagreementPage], providers: [{provide: ReconciliationApi, useValue: api},
      {provide: ProcessApi, useValue: {list: () => of([])}}, {provide: AccessSessionService, useValue: {identity: () => ({assignments: [{id: 'assignment', roleName: 'Comité'}]})}}]});
  });
  function page() {const fixture = TestBed.createComponent(AcademicDisagreementPage);fixture.detectChanges();const component = fixture.componentInstance;
    component.processId = 'p';component.selectIndicator('i');fixture.detectChanges();return {fixture, component};}
  it('shows both foundations and an explicit pending state without inventing a result', () => {
    const {fixture} = page();const text = (fixture.nativeElement as HTMLElement).textContent;
    expect(text).toContain('Pendiente de conciliación');expect(text).toContain('Primer fundamento');expect(text).toContain('Segundo fundamento');
    expect(text).toContain('No hay una resolución aplicable');expect(text).toContain('no constituye acreditación');
  });
  it('requires all participants and authority before selecting an existing judgement', () => {
    const {component} = page();component.assignmentId = 'assignment';component.selectedJudgement = 'first';component.resolutionReason = 'Resolución suficientemente fundamentada';
    component.resolve();expect(api.resolve).not.toHaveBeenCalled();
    for (const person of component.requiredParticipants(view)) component.toggle(person);component.resolve();
    expect(api.resolve).toHaveBeenCalledWith(view, 'first', ['one', 'two', 'committee'], 'Resolución suficientemente fundamentada', 'assignment');
  });
  it('does not allow resolution without current support, procedure or authority capability', () => {
    const {component} = page();expect(component.canResolve({...view, policy: null})).toBe(false);
    expect(component.canResolve({...view, canReconcile: false})).toBe(false);
    expect(component.canResolve({...view, current: [{...first, flags: ['SUPPORTS_INVALIDATED']}, second]})).toBe(false);
    expect(component.flagText('SUPPORTS_INVALIDATED')).toContain('aceptación vigente');
  });
  it('clears history on a stale error and never repeats a resolution automatically', () => {
    const {component} = page();component.assignmentId = 'assignment';component.selectedJudgement = 'first';component.resolutionReason = 'Resolución suficientemente fundamentada';
    component.participants = ['one', 'two', 'committee'];api.resolve.mockReturnValue(throwError(() => new Error('stale')));component.resolve();
    expect(component.view()).toBeNull();expect(api.resolve).toHaveBeenCalledTimes(1);
  });
  it('cancels previous scope requests and clears participants and grounds', () => {
    const pending = new Subject<ReconciliationView>();api.view.mockReturnValue(pending);const {component} = page();
    component.participants = ['one'];component.resolutionReason = 'Texto de otro ámbito';component.selectProcess('other');pending.next(view);
    expect(component.view()).toBeNull();expect(component.participants).toEqual([]);expect(component.resolutionReason).toBe('');
  });
});
