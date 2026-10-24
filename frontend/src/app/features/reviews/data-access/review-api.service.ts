import {Injectable, inject} from '@angular/core';
import {Observable} from 'rxjs';
import {ApiClient} from '../../../core/http/api-client';
import {
  ReviewCorrectionView,
  ReviewDecisionCommand,
  ReviewDecisionView,
  ReviewInboxEntry,
  VersionReviewView
} from './review.models';

@Injectable({providedIn: 'root'})
export class ReviewApi {
  private readonly api = inject(ApiClient);

  inbox(processId: string): Observable<readonly ReviewInboxEntry[]> {
    return this.api.get(`processes/${processId}/document-review/inbox`);
  }

  history(processId: string): Observable<readonly ReviewDecisionView[]> {
    return this.api.get(`processes/${processId}/document-review-decisions`);
  }

  versionReview(versionId: string): Observable<VersionReviewView> {
    return this.api.get(`evidence-versions/${versionId}/document-review`);
  }

  decide(versionId: string, command: ReviewDecisionCommand, etag: string): Observable<ReviewDecisionView> {
    return this.api.post(`evidence-versions/${versionId}/document-review`, command, {'If-Match': etag});
  }

  correctDecision(decisionId: string, etag: string): Observable<ReviewCorrectionView> {
    return this.api.post(`document-review-decisions/${decisionId}/correction`, {}, {'If-Match': etag});
  }
}
