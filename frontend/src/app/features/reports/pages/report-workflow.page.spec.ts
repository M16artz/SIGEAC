import {provideZonelessChangeDetection} from '@angular/core';
import {TestBed} from '@angular/core/testing';
import {of} from 'rxjs';
import {describe, beforeEach, expect, it, vi} from 'vitest';
import {ProcessApi} from '../../processes/data-access/process-api.service';
import {ReportApi} from '../data-access/report-api.service';
import {ReportWorkflowPage} from './report-workflow.page';

describe('ReportWorkflowPage', () => {
  const finding = {
    id: 'finding', revision: 1, createdAt: '2026-10-01T00:00:00Z', createdBy: 'coord',
    data: {type: 'WEAKNESS' as const, title: 'Matrícula tardía', description: 'Descripción sustentada del hallazgo', indicatorIds: ['ind']}
  };
  const version = {
    id: 'version', revision: 0, createdAt: '2026-10-05T00:00:00Z', createdBy: 'coord',
    data: {kind: 'PRELIMINARY' as const, consolidationId: 'consolidation', templateReference: 'Plantilla', templateValidated: true, content: 'Contenido', predecessorVersionId: null},
    state: 'PRELIMINARY', socializations: [], observations: [], replies: [], decisions: []
  };
  const api = {
    findings: vi.fn(), versions: vi.fn(), registerFinding: vi.fn(), consolidate: vi.fn(),
    issueVersion: vi.fn(), version: vi.fn(), socialize: vi.fn(), observe: vi.fn(), reply: vi.fn(), decide: vi.fn()
  };
  beforeEach(() => {
    api.findings.mockReset().mockReturnValue(of({items: [finding], page: 0, size: 100, totalElements: 1}));
    api.versions.mockReset().mockReturnValue(of({items: [version], page: 0, size: 100, totalElements: 1}));
    api.registerFinding.mockReset().mockReturnValue(of(finding));
    api.consolidate.mockReset().mockReturnValue(of({id: 'c1', revision: 1, createdAt: '', createdBy: null, data: {findingIds: ['finding'], aggregationRuleId: null, reason: 'razón'}}));
    api.issueVersion.mockReset().mockReturnValue(of(version));
    api.version.mockReset().mockReturnValue(of(version));
    api.socialize.mockReset().mockReturnValue(of({id: 's1', revision: 1, createdAt: '', createdBy: null, data: {occurredAt: '', participantAssignmentIds: ['a1'], recordReference: 'ref'}}));
    api.observe.mockReset().mockReturnValue(of({id: 'o1', revision: 1, createdAt: '', createdBy: null, data: {sectionReference: 'Cap', text: 'Texto'}}));
    api.reply.mockReset().mockReturnValue(of({id: 'r1', revision: 2, createdAt: '', createdBy: null, data: {response: 'Respuesta', correctedVersionId: null}}));
    api.decide.mockReset().mockReturnValue(of({id: 'd1', revision: 3, createdAt: '', createdBy: null, data: {decision: 'APPROVED', provenance: {decidedBy: null, authorityAssignmentId: 'asg', legalBasis: 'REA', documentReference: 'Doc 1', reason: 'Justificación sustentada', decidedAt: ''}}}));
    vi.clearAllMocks();
    TestBed.configureTestingModule({imports: [ReportWorkflowPage], providers: [
      provideZonelessChangeDetection(),
      {provide: ReportApi, useValue: api},
      {provide: ProcessApi, useValue: {list: () => of([{id: 'proc-1', careerId: 'c', code: 'SI', name: 'Ingeniería', indicators: []}])}}
    ]});
  });
  function page() {
    const fixture = TestBed.createComponent(ReportWorkflowPage);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component.selectProcess('proc-1'); fixture.detectChanges();
    return {fixture, component};
  }
  it('shows the immutability notice, the finding register and the empty consolidation hint', () => {
    const {fixture} = page();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('no constituye acreditación');
    expect(text).toContain('Matrícula tardía');
    expect(text).toContain('Registre y consolide hallazgos antes de emitir versiones del informe.');
  });
  it('registers a finding with indicator supports and reloads findings', () => {
    const {component} = page();
    component.findingType = 'STRENGTH'; component.findingTitle = 'Plan de estudios actualizado';
    component.findingDescription = 'Descripción sustentada del hallazgo'; component.toggleSupport('ind');
    component.registerFinding();
    expect(api.registerFinding).toHaveBeenCalledWith('proc-1', {
      type: 'STRENGTH', title: 'Plan de estudios actualizado',
      description: 'Descripción sustentada del hallazgo', indicatorIds: ['ind']
    });
    expect(component.message()).toContain('Hallazgo registrado');
    expect(component.findingSupports()).toEqual([]);
  });
  it('does not register a finding without supports', () => {
    const {component} = page();
    component.findingType = 'STRENGTH'; component.findingTitle = 'Plan de estudios actualizado';
    component.findingDescription = 'Descripción sustentada del hallazgo';
    component.registerFinding();
    expect(api.registerFinding).not.toHaveBeenCalled();
  });
  it('consolidates selected findings and updates the chain revision', () => {
    const {component} = page();
    component.toggleFinding('finding'); component.expectedConsolidation = '0';
    component.consolidationReason = 'Se agrupan por soporte común de indicador';
    component.consolidate();
    expect(api.consolidate).toHaveBeenCalledWith('proc-1', 0, {
      findingIds: ['finding'], aggregationRuleId: null, reason: 'Se agrupan por soporte común de indicador'
    });
    expect(component.consolidationId()).toBe('c1');
    expect(component.expectedConsolidation).toBe('1');
  });
  it('issues a preliminary version chained to the latest consolidation', () => {
    const {component} = page();
    component.toggleFinding('finding'); component.consolidationReason = 'Se agrupan por soporte común de indicador';
    component.consolidate();
    component.templateReference = 'Plantilla oficial'; component.issueContent = 'Contenido sustentado del informe preliminar';
    component.issue('PRELIMINARY');
    expect(api.issueVersion).toHaveBeenCalledWith('proc-1', 0, {
      kind: 'PRELIMINARY', consolidationId: 'c1', templateReference: 'Plantilla oficial',
      templateValidated: true, content: 'Contenido sustentado del informe preliminar', predecessorVersionId: null
    });
  });
  it('loads the version detail with its events and processing revision', () => {
    const {component} = page();
    component.viewDetail(version);
    expect(api.version).toHaveBeenCalledWith('version');
    expect(component.selectedRevision()).toBe(0);
    expect(component.socializations()).toEqual([]);
    expect(component.message()).toContain('PRELIMINARY');
  });
  it('filters replies for their observation', () => {
    const {component} = page();
    component.replies.set([{id: 'r1', observationId: 'o1', response: 'Respuesta', correctedVersionId: 'v2', recordedBy: null, recordedAt: ''}]);
    expect(component.repliesFor('o1').length).toBe(1);
    expect(component.repliesFor('o2').length).toBe(0);
  });
  it('requires an authority assignment, legal basis and reasoned motivation to decide', () => {
    const {component} = page();
    component.viewDetail(version); component.decisionValue = 'APPROVED';
    component.decide();
    expect(api.decide).not.toHaveBeenCalled();
    component.decisionAssignmentId = 'asg'; component.decisionLegalBasis = 'REA';
    component.decisionDocumentReference = 'Doc 1'; component.decisionReason = 'Justificación sustentada de la decisión';
    component.decide();
    expect(api.decide).toHaveBeenCalledWith('version', 0, {
      decision: 'APPROVED', provenance: {
        authorityAssignmentId: 'asg', legalBasis: 'REA', documentReference: 'Doc 1', reason: 'Justificación sustentada de la decisión'
      }
    });
  });
});
