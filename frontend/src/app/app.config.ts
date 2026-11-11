import { ApplicationConfig } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import { csrfInterceptor } from './core/http/csrf.interceptor';
import { responseEnvelopeInterceptor } from './core/http/response-envelope.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [provideRouter(routes), provideHttpClient(withInterceptors([responseEnvelopeInterceptor, csrfInterceptor]))]
};
