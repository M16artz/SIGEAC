import {TestBed} from '@angular/core/testing';
import {provideHttpClient} from '@angular/common/http';
import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';
import {afterEach, beforeEach, describe, expect, it} from 'vitest';
import {ReconciliationApi, ReconciliationView} from './reconciliation-api.service';
describe('ReconciliationApi', () => {
  let api: ReconciliationApi;let http: HttpTestingController;
  beforeEach(() => {TestBed.configureTestingModule({providers: [provideHttpClient(), provideHttpClientTesting()]});api = TestBed.inject(ReconciliationApi);http = TestBed.inject(HttpTestingController);});
  afterEach(() => http.verify());
  it('uses distinct policy and disagreement revisions and never sends an actor chosen by the client', () => {
    const view = {processId: 'p', indicatorId: 'i', policy: {id: 'policy', revision: 3}, latestCase: {id: 'case', revision: 2}} as ReconciliationView;
    api.policy(view, {procedureReference: 'procedure', method: 'SELECT_EXISTING_JUDGEMENT_V1', legalBasis: 'Fundamento competente', documentReference: 'fixture://policy', authorityAssignmentId: 'assignment'}).subscribe();
    const policy = http.expectOne('/api/v1/processes/p/indicators/i/academic-reconciliation-policies');expect(policy.request.headers.get('If-Match')).toBe('"3"');policy.flush({});
    api.resolve(view, 'judgement', ['one', 'two'], 'Fundamento suficiente', 'assignment').subscribe();
    const resolution = http.expectOne('/api/v1/processes/p/indicators/i/academic-disagreements/case/resolution');
    expect(resolution.request.headers.get('If-Match')).toBe('"2"');expect(resolution.request.body.policyId).toBe('policy');expect(resolution.request.body.resolvedBy).toBeUndefined();resolution.flush({});
  });
});
