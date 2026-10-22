import {Injectable, inject} from '@angular/core';
import {Observable, switchMap} from 'rxjs';
import {ApiClient} from '../../../core/http/api-client';
export type ProcessState = 'DRAFT' | 'PREPARATION' | 'ACTIVE' | 'SUSPENDED' | 'DOCUMENTARY_CLOSED' | 'CLOSING' | 'CLOSED' | 'REOPENED';
export interface ProcessAuthorization {id: string; authorityAssignmentId: string; legalBasis: string; documentReference: string; reason: string; authorizedBy: string; authorizedAt: string}
export interface ProcessView {
  id: string; careerId: string; code: string; name: string; periodStart: string; periodEnd: string;
  modelVersionId: string; modelVersionNumber: number; state: ProcessState; revision: number;
  authorization?: ProcessAuthorization | null; indicators: readonly {id: string; code: string; name: string}[];
}
export interface AuthorizationCommand {authorityAssignmentId: string; legalBasis: string; documentReference: string; reason: string}
@Injectable({providedIn: 'root'})
export class ProcessApi {
  private readonly api = inject(ApiClient);
  list(): Observable<readonly ProcessView[]> { return this.api.get('processes'); }
  create(command: unknown): Observable<ProcessView> { return this.api.post('processes', command); }
  get(id: string): Observable<ProcessView> { return this.api.get(`processes/${id}`); }
  authorize(p: ProcessView, command: AuthorizationCommand): Observable<ProcessView> {
    return this.api.post(`processes/${p.id}/authorizations`, command, this.match(p)).pipe(switchMap(() => this.get(p.id)));
  }
  activate(p: ProcessView): Observable<ProcessView> { return this.api.post(`processes/${p.id}/activate`, {}, this.match(p)); }
  transition(p: ProcessView, target: ProcessState, reason: string): Observable<ProcessView> { return this.api.post(`processes/${p.id}/transitions`, {target, reason}, this.match(p)); }
  reopen(p: ProcessView, reason: string): Observable<ProcessView> { return this.api.post(`processes/${p.id}/reopen`, {reason}, this.match(p)); }
  assign(p: ProcessView, command: unknown): Observable<ProcessView> {
    return this.api.post(`processes/${p.id}/assignments`, command, this.match(p)).pipe(switchMap(() => this.get(p.id)));
  }
  private match(p: ProcessView): Record<string, string> { return {'If-Match': `"${p.revision}"`}; }
}
