import { HttpErrorResponse, HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { catchError, map, throwError } from 'rxjs';

interface Envelope {
  success: boolean;
  message: string;
  data: unknown;
  errors: unknown;
  meta: Record<string, unknown>;
}

function envelope(value: unknown): value is Envelope {
  return typeof value === 'object' && value !== null && 'success' in value &&
    typeof value.success === 'boolean' && 'message' in value && typeof value.message === 'string' &&
    'data' in value && 'errors' in value && 'meta' in value && typeof value.meta === 'object' && value.meta !== null;
}

export function unwrapApiResponse(value: unknown): unknown {
  if (!envelope(value)) return value;
  if (!value.success) return value.errors;
  // Keep view models stable while pagination travels in meta on the wire.
  if (typeof value.data === 'object' && value.data !== null && !Array.isArray(value.data) && 'page' in value.meta) {
    return { ...value.data, ...value.meta };
  }
  return value.data;
}

export const responseEnvelopeInterceptor: HttpInterceptorFn = (request, next) => {
  if (!request.url.startsWith('/api/v1/')) return next(request);
  return next(request).pipe(
    map(event => event instanceof HttpResponse && request.responseType === 'json'
      ? event.clone({ body: unwrapApiResponse(event.body) }) : event),
    catchError((error: unknown) => throwError(() => error instanceof HttpErrorResponse
      ? new HttpErrorResponse({ error: unwrapApiResponse(error.error), headers: error.headers,
          status: error.status, statusText: error.statusText, url: error.url ?? undefined }) : error))
  );
};
