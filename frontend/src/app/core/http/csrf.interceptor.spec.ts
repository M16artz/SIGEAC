import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { csrfInterceptor } from './csrf.interceptor';
import { clearCsrfToken, storeCsrfToken } from './csrf-token';

describe('csrfInterceptor origin boundary', () => {
  let http: HttpClient;
  let requests: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({providers: [provideHttpClient(withInterceptors([csrfInterceptor])), provideHttpClientTesting()]});
    http = TestBed.inject(HttpClient); requests = TestBed.inject(HttpTestingController);
    storeCsrfToken('synthetic-csrf-token');
  });
  afterEach(() => { requests.verify(); clearCsrfToken(); });
  it('adds the token only to mutations of the local API', () => {
    http.post('/api/v1/careers', {}).subscribe();
    const request = requests.expectOne('/api/v1/careers');
    expect(request.request.headers.get('X-CSRF-TOKEN')).toBe('synthetic-csrf-token'); request.flush({});
  });
  it('does not send the token to external URLs', () => {
    http.post('https://external.example/action', {}).subscribe();
    const request = requests.expectOne('https://external.example/action');
    expect(request.request.headers.has('X-CSRF-TOKEN')).toBe(false); request.flush({});
  });
  it('does not attach CSRF to reads', () => {
    http.get('/api/v1/careers').subscribe();
    const request = requests.expectOne('/api/v1/careers');
    expect(request.request.headers.has('X-CSRF-TOKEN')).toBe(false); request.flush({});
  });
});
