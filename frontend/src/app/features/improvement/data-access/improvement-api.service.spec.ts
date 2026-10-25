import {TestBed} from '@angular/core/testing';
import {provideHttpClient} from '@angular/common/http';
import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';
import {afterEach, beforeEach, describe, expect, it} from 'vitest';
import {ImprovementApi} from './improvement-api.service';

describe('ImprovementApi', () => {
  let api: ImprovementApi; let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({providers: [provideHttpClient(), provideHttpClientTesting()]});
    api = TestBed.inject(ImprovementApi); http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('creates a plan from an issued report with the plan chain revision', () => {
    api.createPlan('proc', 0, {title: 'Plan de mejora', reportVersionId: 'v1'}).subscribe();
    const request = http.expectOne('/api/v1/processes/proc/improvement-plans');
    expect(request.request.method).toBe('POST');
    expect(request.request.headers.get('If-Match')).toBe('"0"');
    expect(request.request.body.reportVersionId).toBe('v1');
    request.flush({id: 'p1', revision: 1, data: {title: 'Plan de mejora', reportVersionId: 'v1'}});
  });

  it('lists actions with pagination', () => {
    api.actions('proc', 0, 50).subscribe();
    const request = http.expectOne('/api/v1/processes/proc/improvement-actions?page=0&size=50');
    expect(request.request.method).toBe('GET');
    request.flush({items: [], page: 0, size: 50, totalElements: 0});
  });

  it('guards revisions for action changes and appends progress and decisions', () => {
    api.revise('a1', 2, {
      planId: 'p1', findingIds: ['f1'], title: 'Acción', description: 'Descripción suficiente',
      responsibleAssignmentId: 'asg', dueAt: '2027-01-31T00:00:00Z', target: 'Meta', verificationMethod: 'Revisión'
    }).subscribe();
    const revise = http.expectOne('/api/v1/improvement-actions/a1');
    expect(revise.request.method).toBe('PATCH');
    expect(revise.request.headers.get('If-Match')).toBe('"2"');
    revise.flush({id: 'a1', revision: 3, data: {}, state: 'PROPOSED', overdue: false, progress: [], decisions: []});

    api.progress('a1', 3, {
      description: 'Avance verificado', achievedValue: '75%', evidenceVersionIds: ['e1'], recordedAt: '2026-10-25T12:00:00Z'
    }).subscribe();
    const progress = http.expectOne('/api/v1/improvement-actions/a1/progress');
    expect(progress.request.headers.get('If-Match')).toBe('"3"');
    progress.flush({id: 'pr1', revision: 4, data: {}});

    api.decide('a1', 4, {
      action: 'VERIFY', provenance: {
        authorityAssignmentId: 'asg', legalBasis: 'Norma', documentReference: 'Oficio 1', reason: 'Verificación fundamentada'
      }
    }).subscribe();
    const decision = http.expectOne('/api/v1/improvement-actions/a1/decisions');
    expect(decision.request.headers.get('If-Match')).toBe('"4"');
    expect(decision.request.body.action).toBe('VERIFY');
    decision.flush({id: 'd1', revision: 5, action: 'VERIFY', provenance: {}});
  });
});
