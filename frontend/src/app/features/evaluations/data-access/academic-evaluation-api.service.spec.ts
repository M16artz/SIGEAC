import {TestBed} from '@angular/core/testing';
import {provideHttpClient} from '@angular/common/http';
import {HttpTestingController, provideHttpClientTesting} from '@angular/common/http/testing';
import {afterEach, beforeEach, describe, it, expect} from 'vitest';
import {AcademicEvaluationApi, ManualEvaluationView} from './academic-evaluation-api.service';

describe('AcademicEvaluationApi', () => {
  let api: AcademicEvaluationApi; let http: HttpTestingController;
  beforeEach(() => { TestBed.configureTestingModule({providers: [provideHttpClient(), provideHttpClientTesting()]}); api = TestBed.inject(AcademicEvaluationApi); http = TestBed.inject(HttpTestingController); });
  afterEach(() => http.verify());
  it('uses a process-scoped URL and the evaluator revision for If-Match', () => {
    const view = {processId: 'p', indicatorId: 'i', ownRevision: 3} as ManualEvaluationView;
    api.save(view, {ratingOptionId: 'rating', evidenceVersionIds: ['v'], justification: 'Justificación', limitations: 'Limitaciones', predecessorId: 'prior'}).subscribe();
    const request = http.expectOne('/api/v1/processes/p/indicators/i/manual-academic-evaluations');
    expect(request.request.headers.get('If-Match')).toBe('"3"'); expect(request.request.body.predecessorId).toBe('prior'); request.flush({});
  });
});
