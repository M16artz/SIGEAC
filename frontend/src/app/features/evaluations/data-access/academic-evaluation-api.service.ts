import {Injectable, inject} from '@angular/core';
import {Observable} from 'rxjs';
import {ApiClient} from '../../../core/http/api-client';

export interface EvaluationSupport {evidenceVersionId: string; requiredSourceId: string; reviewDecisionId: string; title: string}
export interface ManualEvaluation {
  id: string; processId: string; indicatorId: string; modelVersionId: string; ruleId: string; scaleId: string;
  ratingOptionId: string; ratingLabel: string; method: string; limitations: string; justification: string;
  supports: readonly EvaluationSupport[]; evaluatorId: string; evaluatedAt: string; revision: number; predecessorId: string | null;
}
export interface ManualEvaluationView {
  processId: string; indicatorId: string; modelVersionId: string; processState: string; method: string;
  ratingOptions: readonly {id: string; code: string; label: string; position: number}[];
  acceptedSupports: readonly EvaluationSupport[]; ownRevision: number; ownLatestId: string | null;
  items: readonly ManualEvaluation[]; page: number; size: number; hasNext: boolean;
}
export interface ManualEvaluationCommand {
  ratingOptionId: string; evidenceVersionIds: readonly string[]; justification: string;
  limitations: string; predecessorId: string | null;
}
@Injectable({providedIn: 'root'})
export class AcademicEvaluationApi {
  private readonly api = inject(ApiClient);
  view(process: string, indicator: string, page = 0): Observable<ManualEvaluationView> {
    return this.api.get(this.path(process, indicator), {page, size: 20});
  }
  save(view: ManualEvaluationView, command: ManualEvaluationCommand): Observable<ManualEvaluation> {
    return this.api.post(this.path(view.processId, view.indicatorId), command, {'If-Match': `"${view.ownRevision}"`});
  }
  private path(process: string, indicator: string): string {
    return `processes/${process}/indicators/${indicator}/manual-academic-evaluations`;
  }
}
