import {ApiResource} from '../../../core/http/api-client';

export interface EvidenceVersionSummary {
  readonly id: string;
  readonly revisionNumber: number;
  readonly predecessorId: string | null;
  readonly status: 'DRAFT' | 'SUBMITTED';
  readonly title: string;
  readonly createdAt: string;
  readonly submittedAt: string | null;
  readonly sourceCount: number;
}

export interface EvidenceView {
  readonly id: string;
  readonly processId: string;
  readonly ownerId: string;
  readonly createdAt: string;
  readonly latestRevisionNumber: number;
  readonly latestVersion: EvidenceVersionSummary;
}

export interface EvidenceVersionView {
  readonly id: string;
  readonly evidenceId: string;
  readonly revisionNumber: number;
  readonly predecessorId?: string | null;
  readonly status: 'DRAFT' | 'SUBMITTED';
  readonly title: string;
  readonly description: string;
  readonly provenance: string;
  readonly coveredFrom: string;
  readonly coveredTo: string;
  readonly indicatorIds: readonly string[];
  readonly createdBy: string;
  readonly createdAt: string;
  readonly submittedBy: string | null;
  readonly submittedAt: string | null;
  readonly revision: number;
  readonly files: readonly unknown[];
  readonly externalReferences: readonly unknown[];
}

export interface EvidencePage {
  readonly items: readonly EvidenceView[];
  readonly page: {readonly page: number; readonly size: number; readonly totalElements: number; readonly totalPages: number};
}

export interface EvidenceDraftCommand {
  readonly processId?: string;
  readonly title: string;
  readonly description: string;
  readonly provenance: string;
  readonly coveredFrom: string;
  readonly coveredTo: string;
  readonly indicatorIds: readonly string[];
}

export type EvidenceVersionResource = ApiResource<EvidenceVersionView>;
