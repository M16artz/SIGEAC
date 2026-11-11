import { TestBed } from '@angular/core/testing';
import { HttpClient, HttpErrorResponse, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { responseEnvelopeInterceptor, unwrapApiResponse } from './response-envelope.interceptor';

describe('responseEnvelopeInterceptor', () => {
  let http: HttpClient;
  let requests: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(withInterceptors([responseEnvelopeInterceptor])), provideHttpClientTesting()] });
    http = TestBed.inject(HttpClient); requests = TestBed.inject(HttpTestingController);
  });
  afterEach(() => requests.verify());

  it('keeps domain data distinct from the transport data and preserves ETag', () => {
    http.get('/api/v1/careers/one', { observe: 'response' }).subscribe(response => {
      expect(response.body).toEqual({ id: 'one', data: { name: 'Carrera' } });
      expect(response.headers.get('ETag')).toBe('"1"');
    });
    requests.expectOne('/api/v1/careers/one').flush({ success: true, message: 'OK', data: { id: 'one', data: { name: 'Carrera' } }, errors: null, meta: {} }, { headers: { ETag: '"1"' } });
  });
  it('restores the existing paged view model from meta', () => {
    expect(unwrapApiResponse({ success: true, message: 'OK', data: { items: [1] }, errors: null, meta: { page: 0, size: 1, totalElements: 2 } }))
      .toEqual({ items: [1], page: 0, size: 1, totalElements: 2 });
  });
  it('preserves field errors and the HTTP status', () => {
    http.post('/api/v1/careers', {}).subscribe({ error: (error: HttpErrorResponse) => {
      expect(error.status).toBe(400); expect(error.error).toEqual({ code: 'VALIDATION_FAILED', details: { name: 'required' } });
    } });
    requests.expectOne('/api/v1/careers').flush({ success: false, message: 'Invalid', data: null, errors: { code: 'VALIDATION_FAILED', details: { name: 'required' } }, meta: {} }, { status: 400, statusText: 'Bad Request' });
  });
  it('leaves 204 bodies and protocol documents untouched', () => {
    http.delete('/api/v1/sessions').subscribe(body => expect(body).toBeNull());
    requests.expectOne('/api/v1/sessions').flush(null, { status: 204, statusText: 'No Content' });
    http.get('/external/document').subscribe(body => expect(body).toEqual({ openapi: '3.1.0' }));
    requests.expectOne('/external/document').flush({ openapi: '3.1.0' });
  });
});
