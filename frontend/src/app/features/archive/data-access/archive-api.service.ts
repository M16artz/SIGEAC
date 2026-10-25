import {Injectable, inject} from '@angular/core';
import {Observable} from 'rxjs';
import {ApiClient} from '../../../core/http/api-client';

export interface ClosurePreview {
  processId: string;
  revision: number;
  policyVersion: string;
  closureType: 'DOCUMENTARY_CLOSURE' | 'INTEGRAL_CLOSURE';
  blockers: readonly string[];
  warnings: readonly string[];
  eligible: boolean;
}

export interface ClosureRecord {
  id: string;
  processId: string;
  type: 'DOCUMENTARY_CLOSURE' | 'INTEGRAL_CLOSURE';
  policyVersion: string;
  sha256: string;
  pendingIds: readonly string[];
  exceptionDecisionIds: readonly string[];
  createdBy: string | null;
  createdAt: string;
}

export interface ClosurePage {
  items: readonly ClosureRecord[];
  page: number;
  size: number;
  totalElements: number;
}

export interface ArchiveRecord {
  id: string;
  processId: string;
  closureSnapshotId: string;
  archivedAt: string;
  archivedBy: string | null;
  retentionPolicyVersion: string;
  integrityStatus: string;
}

export interface ProvenanceCommand {
  authorityAssignmentId: string;
  legalBasis: string;
  documentReference: string;
  reason: string;
}

export interface CloseCommand {
  closurePolicyVersion: string;
  provenance: ProvenanceCommand;
  exceptionDecisionIds: readonly string[];
}

export interface ArchiveCommand {
  closureSnapshotId: string;
  retentionPolicyVersion: string;
  provenance: ProvenanceCommand;
}

@Injectable({providedIn: 'root'})
export class ArchiveApi {
  private readonly http = inject(ApiClient);

  preview(processId: string, closureType: 'DOCUMENTARY_CLOSURE' | 'INTEGRAL_CLOSURE' = 'INTEGRAL_CLOSURE'): Observable<ClosurePreview> {
    return this.http.get<ClosurePreview>(`/processes/${processId}/closure-preview`, {closureType});
  }

  close(processId: string, revision: number, command: CloseCommand): Observable<ClosureRecord> {
    return this.http.post<ClosureRecord>(`/processes/${processId}/close`, command, {'If-Match': `"${revision}"`});
  }

  archive(processId: string, command: ArchiveCommand): Observable<ArchiveRecord> {
    return this.http.post<ArchiveRecord>(`/processes/${processId}/archive`, command, {'If-Match': '"1"'});
  }

  history(processId: string, page = 0, size = 100): Observable<ClosurePage> {
    return this.http.get<ClosurePage>(`/processes/${processId}/closures`, {page, size});
  }
}
