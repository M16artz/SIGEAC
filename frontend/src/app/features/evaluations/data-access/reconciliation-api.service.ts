import {Injectable, inject} from '@angular/core';
import {Observable} from 'rxjs';
import {ApiClient} from '../../../core/http/api-client';
import {EvaluationSupport} from './academic-evaluation-api.service';

export interface AcademicJudgement {kind: string; id: string; authorId: string; modelVersionId: string; ruleId: string; scaleId: string;
  ratingOptionId: string; ratingLabel: string; result: string | null; method: string; justification: string; limitations: string;
  recordedAt: string; supports: readonly EvaluationSupport[]; flags: readonly string[]}
export interface ReconciliationPolicy {id: string; processId: string; indicatorId: string; revision: number; predecessorId: string | null;
  procedureReference: string; method: string; legalBasis: string; documentReference: string; authorityAssignmentId: string; configuredBy: string; configuredAt: string}
export interface AcademicDisagreement {id: string; processId: string; indicatorId: string; revision: number; predecessorId: string | null;
  fingerprint: string; judgements: readonly AcademicJudgement[]; reason: string; openedBy: string; openedAt: string}
export interface ReconciliationResolution {id: string; disagreementId: string; policy: ReconciliationPolicy; selectedJudgement: AcademicJudgement;
  participants: readonly string[]; reason: string; authorityAssignmentId: string; resolvedBy: string; resolvedAt: string; fingerprint: string}
export interface ReconciliationView {processId: string; indicatorId: string; actorId: string; processState: string; canReconcile: boolean; status: string;
  result: AcademicJudgement | null; current: readonly AcademicJudgement[]; policy: ReconciliationPolicy | null; latestCase: AcademicDisagreement | null;
  resolution: ReconciliationResolution | null; history: readonly AcademicJudgement[]; page: number; hasNext: boolean;
  cases: readonly {disagreement: AcademicDisagreement; resolution: ReconciliationResolution | null; applicable: boolean}[]; casePage: number; hasNextCase: boolean; size: number}
export interface ReconciliationPolicyCommand {procedureReference: string; method: string; legalBasis: string; documentReference: string; authorityAssignmentId: string}
@Injectable({providedIn: 'root'})
export class ReconciliationApi {
  private readonly api = inject(ApiClient);
  view(p: string, i: string, page = 0, casePage = 0): Observable<ReconciliationView> {
    return this.api.get(`${this.path(p, i)}/academic-reconciliation`, {page, casePage, size: 20});
  }
  policy(v: ReconciliationView, command: ReconciliationPolicyCommand): Observable<ReconciliationPolicy> {
    return this.api.post(`${this.path(v.processId, v.indicatorId)}/academic-reconciliation-policies`, command, this.match(v.policy?.revision ?? 0));
  }
  open(v: ReconciliationView, reason: string): Observable<AcademicDisagreement> {
    return this.api.post(`${this.path(v.processId, v.indicatorId)}/academic-disagreements`, {reason}, this.match(v.latestCase?.revision ?? 0));
  }
  resolve(v: ReconciliationView, selectedJudgementId: string, participants: readonly string[], reason: string, authorityAssignmentId: string): Observable<ReconciliationResolution> {
    if (!v.latestCase || !v.policy) throw new Error('Se requiere discrepancia y procedimiento registrados.');
    return this.api.post(`${this.path(v.processId, v.indicatorId)}/academic-disagreements/${v.latestCase.id}/resolution`,
      {policyId: v.policy.id, selectedJudgementId, participants, reason, authorityAssignmentId}, this.match(v.latestCase.revision));
  }
  private path(p: string, i: string): string {return `processes/${p}/indicators/${i}`;}
  private match(revision: number): Record<string, string> {return {'If-Match': `"${revision}"`};}
}
