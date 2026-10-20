import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiClient } from '../../../core/http/api-client';
import { storeCsrfToken } from '../../../core/http/csrf-token';
import {
  AssignmentCommand,
  AssignmentView,
  AuditEventView,
  AuditSearch,
  ConflictExceptionCommand,
  CurrentIdentity,
  PageView,
  ResourceView,
  SessionView,
  UserCommand,
  UserView
} from './access.models';

@Injectable({providedIn: 'root'})
export class AccessApi {
  private readonly api = inject(ApiClient);

  login(username: string, password: string): Observable<SessionView> {
    return this.api.post<SessionView>('sessions', {username, password});
  }

  logout(): Observable<void> {
    return this.api.delete<void>('sessions');
  }

  me(): Observable<CurrentIdentity> {
    return this.api.get<CurrentIdentity>('me');
  }

  listUsers(page = 0, size = 20): Observable<PageView<UserView>> {
    return this.api.get<PageView<UserView>>('users', {page, size});
  }

  createUser(command: UserCommand): Observable<ResourceView> {
    return this.api.post<ResourceView>('users', command);
  }

  updateUserState(userId: string, active: boolean, revision: number): Observable<ResourceView> {
    return this.api.patch<ResourceView>(`users/${userId}`, {active}, {'If-Match': String(revision)});
  }

  listAssignments(userId: string): Observable<readonly AssignmentView[]> {
    return this.api.get<readonly AssignmentView[]>(`users/${userId}/assignments`);
  }

  assign(userId: string, command: AssignmentCommand): Observable<ResourceView> {
    return this.api.post<ResourceView>(`users/${userId}/assignments`, command);
  }

  createConflictException(command: ConflictExceptionCommand): Observable<ResourceView> {
    return this.api.post<ResourceView>('conflict-exceptions', command);
  }

  searchAudit(search: AuditSearch): Observable<PageView<AuditEventView>> {
    const params: Record<string, string | number> = {
      page: search.page ?? 0,
      size: search.size ?? 20
    };
    if (search.objectType) params['objectType'] = search.objectType;
    if (search.objectId) params['objectId'] = search.objectId;
    return this.api.get<PageView<AuditEventView>>('audit-events', params);
  }

  rememberCsrf(session: SessionView): void {
    storeCsrfToken(session.csrfToken);
  }
}
