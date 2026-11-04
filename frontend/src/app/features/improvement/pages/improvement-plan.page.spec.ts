import {provideZonelessChangeDetection} from '@angular/core';
import {TestBed} from '@angular/core/testing';
import {of} from 'rxjs';
import {describe, beforeEach, expect, it, vi} from 'vitest';
import {ProcessApi} from '../../processes/data-access/process-api.service';
import {ReportApi} from '../../reports/data-access/report-api.service';
import {ImprovementApi} from '../data-access/improvement-api.service';
import {ImprovementPlanPage} from './improvement-plan.page';

describe('ImprovementPlanPage', () => {
  const finding = {
    id: 'finding', revision: 1, createdAt: '2026-10-01T00:00:00Z', createdBy: 'coord',
    data: {type: 'WEAKNESS' as const, title: 'Matrícula tardía', description: 'Hallazgo sustentado', indicatorIds: ['ind']}
  };
  const version = {
    id: 'version', revision: 2, createdAt: '2026-10-05T00:00:00Z', createdBy: 'coord',
    data: {kind: 'FINAL' as const, consolidationId: 'c', templateReference: 'Plantilla', templateValidated: true,
      content: 'Contenido', predecessorVersionId: null},
    state: 'FINAL', socializations: [], observations: [], replies: [], decisions: []
  };
  const plan = {
    id: 'plan', revision: 1, createdAt: '2026-10-06T00:00:00Z', createdBy: 'coord',
    data: {title: 'Plan de mejora', reportVersionId: 'version'}
  };
  const action = {
    id: 'action', revision: 2, createdAt: '2026-10-07T00:00:00Z', createdBy: 'coord',
    data: {
      planId: 'plan', findingIds: ['finding'], title: 'Acción de mejora', description: 'Descripción suficiente',
      responsibleAssignmentId: 'asg', dueAt: '2026-10-01T00:00:00Z', target: 'Reducir la brecha',
      verificationMethod: 'Revisión documental'
    },
    state: 'PROPOSED' as const, overdue: true, progress: [], decisions: []
  };
  const api = {
    createPlan: vi.fn(), actions: vi.fn(), createAction: vi.fn(), action: vi.fn(), revise: vi.fn(),
    progress: vi.fn(), decide: vi.fn()
  };
  const reports = {versions: vi.fn(), findings: vi.fn()};
  beforeEach(() => {
    api.createPlan.mockReset().mockReturnValue(of(plan));
    api.actions.mockReset().mockReturnValue(of({items: [action], page: 0, size: 100, totalElements: 1}));
    api.createAction.mockReset().mockReturnValue(of(action));
    api.action.mockReset().mockReturnValue(of(action));
    api.revise.mockReset().mockReturnValue(of({...action, revision: 3, state: 'PROPOSED'}));
    api.progress.mockReset().mockReturnValue(of({id: 'progress', actionId: 'action', revision: 3, data: {}}));
    api.decide.mockReset().mockReturnValue(of({id: 'decision', actionId: 'action', revision: 4, action: 'START', provenance: {}}));
    reports.versions.mockReset().mockReturnValue(of({items: [version], page: 0, size: 100, totalElements: 1}));
    reports.findings.mockReset().mockReturnValue(of({items: [finding], page: 0, size: 100, totalElements: 1}));
    vi.clearAllMocks();
    TestBed.configureTestingModule({imports: [ImprovementPlanPage], providers: [
      provideZonelessChangeDetection(),
      {provide: ImprovementApi, useValue: api},
      {provide: ReportApi, useValue: reports},
      {provide: ProcessApi, useValue: {list: () => of([{id: 'proc-1', careerId: 'c', code: 'SI', name: 'Ingeniería', indicators: []}])}}
    ]});
  });
  function page() {
    const fixture = TestBed.createComponent(ImprovementPlanPage);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component.selectProcess('proc-1'); fixture.detectChanges();
    return {fixture, component};
  }
  it('shows traceability, derived overdue and the measurable action list', () => {
    const {fixture} = page();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('meta y medio de verificación');
    expect(text).toContain('Acción de mejora');
    expect(text).toContain('Vencida sin verificación');
  });
  it('creates a plan from a FINAL report and keeps the chain revision', () => {
    const {component} = page();
    component.planTitle = 'Plan de mejora'; component.planReportVersionId = 'version'; component.planRevision = '0';
    component.createPlan();
    expect(api.createPlan).toHaveBeenCalledWith('proc-1', 0, {title: 'Plan de mejora', reportVersionId: 'version'});
    expect(component.plans().length).toBe(1);
    expect(component.actionPlanId).toBe('plan');
    expect(component.message()).toContain('Plan de mejora creado');
  });
  it('proposes an action only with findings, target and verification method', () => {
    const {component} = page();
    component.createAction();
    expect(api.createAction).not.toHaveBeenCalled();
    component.actionPlanId = 'plan'; component.toggleFinding('finding'); component.actionTitle = 'Acción de mejora';
    component.actionDescription = 'Descripción suficiente'; component.actionResponsible = 'asg';
    component.actionDue = '2026-12-31T00:00'; component.actionTarget = 'Reducir la brecha';
    component.actionMethod = 'Revisión documental';
    component.createAction();
    expect(api.createAction).toHaveBeenCalledWith('proc-1', {
      planId: 'plan', findingIds: ['finding'], title: 'Acción de mejora', description: 'Descripción suficiente',
      responsibleAssignmentId: 'asg', dueAt: new Date('2026-12-31T00:00').toISOString(),
      target: 'Reducir la brecha', verificationMethod: 'Revisión documental'
    });
  });
  it('records immutable progress with evidence at the selected revision', () => {
    const {component} = page();
    component.selectAction('action');
    component.progressDescription = 'Avance verificado'; component.progressValue = '75%';
    component.progressEvidence = 'ev-1, ev-2'; component.progressDate = '2026-10-20T10:00';
    component.recordProgress();
    expect(api.progress).toHaveBeenCalledWith('action', 2, {
      description: 'Avance verificado', achievedValue: '75%', evidenceVersionIds: ['ev-1', 'ev-2'],
      recordedAt: new Date('2026-10-20T10:00').toISOString()
    });
  });
  it('requires a reasoned provenance before a decision and reopens a revision', () => {
    const {component} = page();
    component.selectAction('action');
    component.decisionTransition = 'APPROVE'; component.decisionAssignment = 'asg';
    component.decisionLegal = 'Norma'; component.decisionDocument = 'Oficio 1';
    component.decide();
    expect(api.decide).not.toHaveBeenCalled();
    component.decisionReason = 'Aprobación fundamentada de la acción';
    component.decide();
    expect(api.decide).toHaveBeenCalledWith('action', 2, {
      action: 'APPROVE', provenance: {
        authorityAssignmentId: 'asg', legalBasis: 'Norma', documentReference: 'Oficio 1',
        reason: 'Aprobación fundamentada de la acción'
      }
    });
    component.revisionResponsible = 'asg2'; component.revisionDue = '2027-01-31T00:00';
    component.revisionTarget = 'Reducir la brecha al 10%';
    component.revise();
    expect(api.revise).toHaveBeenCalledWith('action', 2, expect.objectContaining({
      responsibleAssignmentId: 'asg2', target: 'Reducir la brecha al 10%'
    }));
  });
});
