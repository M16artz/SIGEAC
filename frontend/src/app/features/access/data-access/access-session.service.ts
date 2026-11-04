import { Injectable, inject, signal } from '@angular/core';
import { Observable, finalize, of, switchMap, tap } from 'rxjs';
import { clearCsrfToken } from '../../../core/http/csrf-token';
import { AccessApi } from './access-api.service';
import { CurrentIdentity } from './access.models';

@Injectable({providedIn: 'root'})
export class AccessSessionService {
  private readonly api = inject(AccessApi);
  private readonly current = signal<CurrentIdentity | null>(null);

  readonly identity = this.current.asReadonly();

  login(username: string, password: string): Observable<CurrentIdentity> {
    return this.api.login(username, password).pipe(
      tap((session) => this.api.rememberCsrf(session)),
      switchMap(() => this.api.me()),
      tap((identity) => this.current.set(identity))
    );
  }

  load(): Observable<CurrentIdentity> {
    return this.api.me().pipe(tap((identity) => this.current.set(identity)));
  }

  ensure(): Observable<CurrentIdentity> {
    const existing = this.current();
    return existing ? of(existing) : this.load();
  }

  logout(): Observable<void> {
    return this.api.logout().pipe(finalize(() => {
      clearCsrfToken();
      this.current.set(null);
    }));
  }

  clear(): void {
    clearCsrfToken();
    this.current.set(null);
  }
}
