import {TestBed} from '@angular/core/testing';
import {provideHttpClient} from '@angular/common/http';
import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';
import {afterEach, beforeEach, describe, expect, it} from 'vitest';
import {ReportingApi} from './reporting-api.service';

describe('ReportingApi', () => {
  let http: HttpTestingController;
  let api: ReportingApi;

  beforeEach(() => {
    TestBed.configureTestingModule({providers: [provideHttpClient(), provideHttpClientTesting()]});
    http = TestBed.inject(HttpTestingController);
    api = TestBed.inject(ReportingApi);
  });

  afterEach(() => http.verify());
  it('sends strict closure policy and process revision, then retrieves the sealed closure', () => {
    api.closeDocumentary('process', 'Cierre documental sintético.', '"7"').subscribe();
    const request = http.expectOne('/api/v1/processes/process/documentary-close');
    expect(request.request.body).toEqual({closurePolicyVersion: 'DOCUMENTARY_STRICT_V1', reason: 'Cierre documental sintético.'});
    expect(request.request.headers.get('If-Match')).toBe('"7"'); request.flush({});
    api.closure('closure').subscribe(); http.expectOne('/api/v1/documentary-closures/closure').flush({});
  });

  it('creates a current snapshot with process revision and reads the saved history', () => {
    api.createSnapshot('process', 'Corte documental sintético.', '"7"').subscribe();
    const create = http.expectOne('/api/v1/processes/process/documentary-snapshots');
    expect(create.request.headers.get('If-Match')).toBe('"7"');
    expect(create.request.body).toEqual({reason: 'Corte documental sintético.'});
    create.flush({});
    api.snapshots('process', 1).subscribe();
    const history = http.expectOne(req => req.url === '/api/v1/processes/process/documentary-snapshots');
    expect(history.request.params.get('page')).toBe('1');
    history.flush({items: [], page: 1, size: 20, hasNext: false});
    api.snapshot('snapshot').subscribe();
    http.expectOne('/api/v1/documentary-snapshots/snapshot').flush({});
  });

  it('sends the source filter and page without requesting a historical cutoff', () => {
    api.progress('process', 2, 'source').subscribe();
    const request = http.expectOne(req => req.url === '/api/v1/processes/process/documentary-progress');
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('requiredSourceId')).toBe('source');
    expect(request.request.params.get('page')).toBe('2');
    expect(request.request.params.get('size')).toBe('20');
    expect(request.request.params.has('cutoffAt')).toBe(false);
    request.flush({});
  });
});
