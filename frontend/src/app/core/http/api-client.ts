import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, map, throwError } from 'rxjs';
import { ApiProblem, isApiProblem } from './problem';

export class ApiProblemError extends Error {
  constructor(readonly problem: ApiProblem) {
    super(problem.detail ?? problem.title);
  }
}

export interface ApiResource<T> {
  readonly body: T;
  readonly etag: string | null;
}

@Injectable({providedIn: 'root'})
export class ApiClient {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1';

  get<T>(path: string, params?: Readonly<Record<string, string | number | boolean>>): Observable<T> {
    let httpParams = new HttpParams();
    for (const [key, value] of Object.entries(params ?? {})) httpParams = httpParams.set(key, String(value));
    return this.http.get<T>(this.url(path), {params: httpParams}).pipe(catchError((error: unknown) => this.handle(error)));
  }

  getResource<T>(path: string): Observable<ApiResource<T>> {
    return this.http.get<T>(this.url(path), {observe: 'response'}).pipe(
      map(response => ({body: response.body as T, etag: response.headers.get('ETag')})),
      catchError((error: unknown) => this.handle(error))
    );
  }

  post<T>(path: string, body: unknown, headers?: Readonly<Record<string, string>>): Observable<T> {
    return this.http.post<T>(this.url(path), body, {headers}).pipe(catchError((error: unknown) => this.handle(error)));
  }

  put<T>(path: string, body: unknown, headers?: Readonly<Record<string, string>>): Observable<T> {
    return this.http.put<T>(this.url(path), body, {headers}).pipe(catchError((error: unknown) => this.handle(error)));
  }

  patch<T>(path: string, body: unknown, headers?: Readonly<Record<string, string>>): Observable<T> {
    return this.http.patch<T>(this.url(path), body, {headers}).pipe(catchError((error: unknown) => this.handle(error)));
  }

  postResource<T>(path: string, body: unknown): Observable<ApiResource<T>> {
    return this.http.post<T>(this.url(path), body, {observe: 'response'}).pipe(
      map(response => ({body: response.body as T, etag: response.headers.get('ETag')})),
      catchError((error: unknown) => this.handle(error))
    );
  }

  patchResource<T>(path: string, body: unknown, etag: string): Observable<ApiResource<T>> {
    return this.http.patch<T>(this.url(path), body, {headers: {'If-Match': etag}, observe: 'response'}).pipe(
      map(response => ({body: response.body as T, etag: response.headers.get('ETag')})),
      catchError((error: unknown) => this.handle(error))
    );
  }

  delete<T>(path: string, headers?: Readonly<Record<string, string>>): Observable<T> {
    return this.http.delete<T>(this.url(path), {headers}).pipe(catchError((error: unknown) => this.handle(error)));
  }

  private url(path: string): string {
    return `${this.baseUrl}/${path.replace(/^\/+/, '')}`;
  }

  private handle(error: unknown): Observable<never> {
    if (error instanceof HttpErrorResponse && isApiProblem(error.error)) return throwError(() => new ApiProblemError(error.error));
    return throwError(() => error);
  }
}
