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
export interface ProcessTeam {id: string; processId: string; name: string; processRole: string; validFrom: string; validUntil: string; createdBy: string; createdAt: string; revision: number}
export interface TeamMembership {id: string; teamId: string; userId: string; roleAssignmentId: string; validFrom: string; validUntil: string; createdBy: string; createdAt: string}
export interface ProcessAssignment {id: string; processId: string; indicatorId: string; identityId: string | null; teamId: string | null; function: string; validFrom: string; validTo: string; reason: string; revokedAt: string | null; revision: number}
export interface ProcessActivity {id: string; processId: string; indicatorId: string; requiredSourceId: string | null; assignmentId: string; title: string; dueAt: string; status: string; dependencyIds: readonly string[]; deadlineExceptionReason: string | null; deadlineExceptionAuthorityAssignmentId: string | null; deadlineExceptionLegalBasis: string | null; createdBy: string; createdAt: string; revision: number}
export interface AuthorizationCommand {authorityAssignmentId: string; legalBasis: string; documentReference: string; reason: string}
export interface TeamCommand {name: string; processRole: string; validFrom: string; validUntil: string}
export interface MemberCommand {userId: string; roleAssignmentId: string; validFrom: string; validUntil: string}
export interface ActivityCommand {title: string; indicatorId: string; requiredSourceId: string | null; assignmentId: string; dueAt: string; dependencyIds: readonly string[]; exceptionReason: string | null; exceptionAuthorityAssignmentId: string | null; exceptionLegalBasis: string | null}
@Injectable({providedIn: 'root'})
export class ProcessApi {
  private readonly api = inject(ApiClient);
  list(): Observable<readonly ProcessView[]> { return this.api.get('processes'); }
  create(command: unknown): Observable<ProcessView> { return this.api.post('processes', command); }
  get(id: string): Observable<ProcessView> { return this.api.get(`processes/${id}`); }
  teams(id: string): Observable<readonly ProcessTeam[]> { return this.api.get(`processes/${id}/teams`); }
  memberships(teamId: string): Observable<readonly TeamMembership[]> { return this.api.get(`teams/${teamId}/memberships`); }
  assignments(id: string): Observable<readonly ProcessAssignment[]> { return this.api.get(`processes/${id}/assignments`); }
  activities(id: string): Observable<readonly ProcessActivity[]> { return this.api.get(`processes/${id}/activities`); }
  authorize(p: ProcessView, command: AuthorizationCommand): Observable<ProcessView> {
    return this.api.post(`processes/${p.id}/authorizations`, command, this.match(p)).pipe(switchMap(() => this.get(p.id)));
  }
  activate(p: ProcessView): Observable<ProcessView> { return this.api.post(`processes/${p.id}/activate`, {}, this.match(p)); }
  transition(p: ProcessView, target: ProcessState, reason: string): Observable<ProcessView> { return this.api.post(`processes/${p.id}/transitions`, {target, reason}, this.match(p)); }
  reopen(p: ProcessView, reason: string): Observable<ProcessView> { return this.api.post(`processes/${p.id}/reopen`, {reason}, this.match(p)); }
  assign(p: ProcessView, command: unknown): Observable<ProcessView> {
    return this.api.post(`processes/${p.id}/assignments`, command, this.match(p)).pipe(switchMap(() => this.get(p.id)));
  }
  createTeam(p: ProcessView, command: TeamCommand): Observable<ProcessView> {
    return this.api.post(`processes/${p.id}/teams`, command, this.match(p)).pipe(switchMap(() => this.get(p.id)));
  }
  addMember(p: ProcessView, teamId: string, command: MemberCommand): Observable<ProcessView> {
    return this.api.post(`teams/${teamId}/memberships`, command, this.match(p)).pipe(switchMap(() => this.get(p.id)));
  }
  createActivity(p: ProcessView, command: ActivityCommand): Observable<ProcessView> {
    return this.api.post(`processes/${p.id}/activities`, command, this.match(p)).pipe(switchMap(() => this.get(p.id)));
  }
  updateActivity(p: ProcessView, activityId: string, command: ActivityCommand): Observable<ProcessView> {
    return this.api.patch(`activities/${activityId}`, command, this.match(p)).pipe(switchMap(() => this.get(p.id)));
  }
  private match(p: ProcessView): Record<string, string> { return {'If-Match': `"${p.revision}"`}; }
}
