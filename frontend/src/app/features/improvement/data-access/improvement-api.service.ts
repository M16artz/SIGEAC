import {Injectable, inject} from '@angular/core';
import {Observable} from 'rxjs';
import {ApiClient} from '../../../core/http/api-client';

export interface ImprovementPlan {
  id: string;
  revision: number;
  createdAt: string | null;
  createdBy: string | null;
  data: {title: string; reportVersionId: string};
}

export interface ImprovementActionData {
  planId: string;
  findingIds: readonly string[];
  title: string;
  description: string;
  responsibleAssignmentId: string;
  dueAt: string;
  target: string;
  verificationMethod: string;
}

export interface ActionProgress {
  id: string;
  actionId: string;
  data: {
    description: string;
    achievedValue: string;
    evidenceVersionIds: readonly string[];
    recordedAt: string;
  };
  recordedBy: string | null;
  createdAt: string;
}

export interface ActionDecision {
  id: string;
  actionId: string;
  action: Transition;
  provenance: DecisionProvenance;
}

export interface DecisionProvenance {
  decidedBy: string | null;
  authorityAssignmentId: string;
  legalBasis: string;
  documentReference: string;
  reason: string;
  decidedAt: string;
}

export type State = 'PROPOSED' | 'APPROVED' | 'IN_PROGRESS' | 'PENDING_VERIFICATION' | 'VERIFIED';
export type Transition = 'APPROVE' | 'RETURN' | 'START' | 'SUBMIT_VERIFICATION' | 'VERIFY' | 'REQUEST_CORRECTION';

export interface ImprovementAction {
  id: string;
  revision: number;
  createdAt: string | null;
  createdBy: string | null;
  data: ImprovementActionData;
  state: State;
  overdue: boolean;
  progress: readonly ActionProgress[];
  decisions: readonly ActionDecision[];
}

export interface ActionPage {
  items: readonly ImprovementAction[];
  page: number;
  size: number;
  totalElements: number;
}

export interface PlanCommand {
  title: string;
  reportVersionId: string;
}

export interface ActionCommand {
  planId: string;
  findingIds: readonly string[];
  title: string;
  description: string;
  responsibleAssignmentId: string;
  dueAt: string;
  target: string;
  verificationMethod: string;
}

export interface ProgressCommand {
  description: string;
  achievedValue: string;
  evidenceVersionIds: readonly string[];
  recordedAt: string;
}

export interface DecisionCommand {
  action: Transition;
  provenance: {
    authorityAssignmentId: string;
    legalBasis: string;
    documentReference: string;
    reason: string;
  };
}

export interface ProgressView {
  id: string;
  actionId: string;
  revision: number;
  createdAt: string | null;
  createdBy: string | null;
  data: ActionProgress['data'];
}

export interface DecisionView {
  id: string;
  actionId: string;
  revision: number;
  createdAt: string | null;
  createdBy: string | null;
  action: Transition;
  provenance: DecisionProvenance;
}

@Injectable({providedIn: 'root'})
export class ImprovementApi {
  private readonly http = inject(ApiClient);

  createPlan(processId: string, expectedRevision: number, command: PlanCommand): Observable<ImprovementPlan> {
    return this.http.post<ImprovementPlan>(`/processes/${processId}/improvement-plans`, command,
      {'If-Match': `"${expectedRevision}"`});
  }

  actions(processId: string, page = 0, size = 100): Observable<ActionPage> {
    return this.http.get<ActionPage>(`/processes/${processId}/improvement-actions?page=${page}&size=${size}`);
  }

  createAction(processId: string, command: ActionCommand): Observable<ImprovementAction> {
    return this.http.post<ImprovementAction>(`/processes/${processId}/improvement-actions`, command,
      {'If-Match': '"0"'});
  }

  action(actionId: string): Observable<ImprovementAction> {
    return this.http.get<ImprovementAction>(`/improvement-actions/${actionId}`);
  }

  revise(actionId: string, expectedRevision: number, command: ActionCommand): Observable<ImprovementAction> {
    return this.http.patch<ImprovementAction>(`/improvement-actions/${actionId}`, command,
      {'If-Match': `"${expectedRevision}"`});
  }

  progress(actionId: string, expectedRevision: number, command: ProgressCommand): Observable<ProgressView> {
    return this.http.post<ProgressView>(`/improvement-actions/${actionId}/progress`, command,
      {'If-Match': `"${expectedRevision}"`});
  }

  decide(actionId: string, expectedRevision: number, command: DecisionCommand): Observable<DecisionView> {
    return this.http.post<DecisionView>(`/improvement-actions/${actionId}/decisions`, command,
      {'If-Match': `"${expectedRevision}"`});
  }
}
