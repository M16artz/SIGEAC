import { HttpInterceptorFn } from '@angular/common/http';
import { readCsrfToken } from './csrf-token';

const SAFE_METHODS = new Set(['GET', 'HEAD', 'OPTIONS']);

export const csrfInterceptor: HttpInterceptorFn = (request, next) => {
  if (SAFE_METHODS.has(request.method.toUpperCase()) || request.headers.has('X-CSRF-TOKEN')) return next(request);
  const token = readCsrfToken();
  return next(token ? request.clone({setHeaders: {'X-CSRF-TOKEN': token}}) : request);
};
