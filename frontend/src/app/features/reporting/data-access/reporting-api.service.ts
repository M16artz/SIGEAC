import {inject, Injectable} from '@angular/core';
import {Observable} from 'rxjs';
import {ApiClient} from '../../../core/http/api-client';

export interface DocumentarySourceView {
  readonly id: string;
  readonly indicatorId: string;
  readonly code: string;
  readonly name: string;
  readonly required: boolean;
  readonly applicable: boolean;
  readonly cardinalityMin: number;
  readonly coverages: readonly {
    evidenceId: string; versionId: string; decisionId: string | null;
    outcome: 'ACCEPTED' | 'OBSERVED' | 'REJECTED' | 'PENDING_REVIEW';
  }[];
}

export interface DocumentaryProgressView {
  readonly processId: string;
  readonly modelVersionId: string;
  readonly calculatedAt: string;
  readonly formulaVersion: string;
  readonly state: 'CONFIGURED' | 'NOT_CONFIGURED' | 'NOT_APPLICABLE';
  readonly requiredSourceTotal: number;
  readonly registeredSourceTotal: number;
  readonly acceptedSourceTotal: number;
  readonly collectionPercent: number | null;
  readonly documentReviewPercent: number | null;
  readonly scopeDescription: string;
  readonly indicatorIds: readonly string[];
  readonly sources: readonly DocumentarySourceView[];
  readonly page: number;
  readonly size: number;
  readonly totalSources: number;
}

export interface DocumentarySnapshotHeader {
  readonly id: string; readonly processId: string; readonly careerId: string; readonly modelVersionId: string;
  readonly processRevision: number; readonly createdBy: string; readonly createdAt: string; readonly reason: string;
  readonly sha256: string;
  readonly progress: {
    status: DocumentaryProgressView['state']; formulaVersion: string;
    requiredSourceTotal: number; registeredSourceTotal: number; acceptedSourceTotal: number;
  };
}

export interface DocumentarySnapshotView {
  readonly header: DocumentarySnapshotHeader;
  readonly data: Omit<DocumentarySnapshotHeader, 'sha256'> & {
    readonly processState: 'ACTIVE';
    readonly indicatorIds: readonly string[];
    readonly sources: readonly DocumentarySourceView[];
    readonly applicability: readonly {id: string; subjectType: string; subjectId: string; decision: string; revision: number}[];
  };
}

export interface DocumentarySnapshotPage {
  readonly items: readonly DocumentarySnapshotHeader[];
  readonly page: number; readonly size: number; readonly hasNext: boolean;
}

export interface DocumentaryClosureHeader {
  readonly id: string; readonly processId: string; readonly careerId: string; readonly snapshotId: string;
  readonly processRevision: number; readonly policyVersion: string; readonly closedBy: string;
  readonly closedAt: string; readonly reason: string; readonly sha256: string;
}
export interface DocumentaryClosureView {
  readonly sha256: string;
  readonly data: Omit<DocumentaryClosureHeader, 'sha256'> & {
    readonly state: 'DOCUMENTARY_CLOSED'; readonly snapshotSha256: string;
    readonly files: readonly {versionId: string; fileId: string; sizeBytes: number; sha256: string}[];
  };
}
export interface DocumentaryClosurePage {
  readonly items: readonly DocumentaryClosureHeader[]; readonly page: number; readonly size: number; readonly hasNext: boolean;
}

@Injectable({providedIn: 'root'})
export class ReportingApi {
  private readonly api = inject(ApiClient);

  progress(processId: string, page = 0, requiredSourceId = ''): Observable<DocumentaryProgressView> {
    return this.api.get(`processes/${processId}/documentary-progress`, {
      page, size: 20, ...(requiredSourceId ? {requiredSourceId} : {})
    });
  }

  snapshots(processId: string, page = 0): Observable<DocumentarySnapshotPage> {
    return this.api.get(`processes/${processId}/documentary-snapshots`, {page, size: 20});
  }

  snapshot(snapshotId: string): Observable<DocumentarySnapshotView> {
    return this.api.get(`documentary-snapshots/${snapshotId}`);
  }

  createSnapshot(processId: string, reason: string, etag: string): Observable<DocumentarySnapshotView> {
    return this.api.post(`processes/${processId}/documentary-snapshots`, {reason}, {'If-Match': etag});
  }
  closeDocumentary(processId: string, reason: string, etag: string): Observable<DocumentaryClosureView> {
    return this.api.post(`processes/${processId}/documentary-close`,
      {closurePolicyVersion: 'DOCUMENTARY_STRICT_V1', reason}, {'If-Match': etag});
  }
  closures(processId: string, page = 0): Observable<DocumentaryClosurePage> {
    return this.api.get(`processes/${processId}/documentary-closures`, {page, size: 20});
  }
  closure(id: string): Observable<DocumentaryClosureView> {
    return this.api.get(`documentary-closures/${id}`);
  }
}
