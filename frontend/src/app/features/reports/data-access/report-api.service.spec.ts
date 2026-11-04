import {TestBed} from '@angular/core/testing';
import {provideHttpClient} from '@angular/common/http';
import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';
import {afterEach, beforeEach, describe, expect, it} from 'vitest';
import {ReportApi} from './report-api.service';

describe('ReportApi', () => {
  let api: ReportApi; let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({providers: [provideHttpClient(), provideHttpClientTesting()]});
    api = TestBed.inject(ReportApi); http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('lists findings of the process with page and size params', () => {
    api.findings('proc', 1, 50).subscribe();
    const request = http.expectOne('/api/v1/processes/proc/findings?page=1&size=50');
    expect(request.request.method).toBe('GET');
    request.flush({items: [], page: 1, size: 50, totalElements: 0});
  });

  it('registers the first finding with the empty chain ETag', () => {
    api.registerFinding('proc', {
      type: 'WEAKNESS', title: 'Título', description: 'Descripción sustentada',
      indicatorIds: ['ind-1']
    }).subscribe();
    const request = http.expectOne('/api/v1/processes/proc/findings');
    expect(request.request.headers.get('If-Match')).toBe('"0"');
    expect(request.request.body.type).toBe('WEAKNESS');
    request.flush({id: 'f1', revision: 1, data: {}});
  });

  it('consolidates findings with the expected chain revision', () => {
    api.consolidate('proc', 2, {
      findingIds: ['f1'], aggregationRuleId: null, reason: 'Se agrupan por soporte común'
    }).subscribe();
    const request = http.expectOne('/api/v1/processes/proc/consolidations');
    expect(request.request.headers.get('If-Match')).toBe('"2"');
    expect(request.request.body.aggregationRuleId).toBeNull();
    request.flush({id: 'c1', revision: 3, data: {}});
  });

  it('issues versions and records events with the processing revision guard', () => {
    api.issueVersion('proc', 0, {
      kind: 'PRELIMINARY', consolidationId: 'c1', templateReference: 'Plantilla validada',
      templateValidated: true, content: 'Contenido sustentado', predecessorVersionId: null
    }).subscribe();
    const issue = http.expectOne('/api/v1/processes/proc/report-versions');
    expect(issue.request.headers.get('If-Match')).toBe('"0"');
    issue.flush({id: 'v1', revision: 0, data: {kind: 'PRELIMINARY'}});

    api.observe('v1', 0, {sectionReference: 'Capítulo 3', text: 'Observación sustentada'}).subscribe();
    const observation = http.expectOne('/api/v1/report-versions/v1/observations');
    expect(observation.request.headers.get('If-Match')).toBe('"0"');
    observation.flush({id: 'o1', revision: 1, data: {}});
  });

  it('loads the single version detail and its events by id', () => {
    api.version('v2').subscribe();
    const request = http.expectOne('/api/v1/report-versions/v2');
    expect(request.request.method).toBe('GET');
    request.flush({id: 'v2', revision: 4, state: 'UNDER_OBSERVATION', data: {}, observations: []});
  });
});
