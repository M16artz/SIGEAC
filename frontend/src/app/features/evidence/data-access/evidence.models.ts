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

export interface RequiredSourceState {
  readonly id: string;
  readonly indicatorId: string;
  readonly indicatorCode: string;
  readonly code: string;
  readonly name: string;
  readonly sourceType: string;
  readonly required: boolean;
  readonly covered: boolean;
}

export interface SourceCoverageView {
  readonly id: string;
  readonly versionId: string;
  readonly requiredSourceId: string;
  readonly coverageNote: string;
  readonly declaredBy: string;
  readonly declaredAt: string;
}

export interface SourceCoverageCommand {
  readonly requiredSourceId: string;
  readonly coverageNote: string;
}

export interface DecisionProvenanceCommand {
  readonly authorityAssignmentId: string;
  readonly legalBasis: string;
  readonly documentReference: string;
  readonly reason: string;
}

export interface ApplicabilityDecisionCommand {
  readonly subjectType: 'INDICATOR' | 'REQUIRED_SOURCE';
  readonly subjectId: string;
  readonly decision: 'APPLICABLE' | 'NOT_APPLICABLE';
  readonly ruleReference: string;
  readonly provenance: DecisionProvenanceCommand;
}

export interface ApplicabilityDecisionView {
  readonly id: string;
  readonly processId: string;
  readonly subjectType: 'INDICATOR' | 'REQUIRED_SOURCE';
  readonly subjectId: string;
  readonly decision: 'APPLICABLE' | 'NOT_APPLICABLE';
  readonly ruleReference: string;
  readonly justification: string;
  readonly legalBasis: string;
  readonly documentReference: string;
  readonly authorizedBy: string;
  readonly decidedAt: string;
  readonly predecessorId: string | null;
  readonly revision: number;
}

export interface StructuredSourceView {
  readonly id: string;
  readonly status: string;
  readonly createdAt: string;
  readonly requiredSourceId: string;
  readonly kind: 'STRUCTURED_DATA' | 'INSTITUTIONAL_REFERENCE';
  readonly value: string | number | boolean;
  readonly valueType: 'NUMBER' | 'STRING' | 'BOOLEAN';
  readonly unit: string | null;
  readonly periodStart: string;
  readonly periodEnd: string;
  readonly reference: string | null;
}

export interface StructuredSourceCommand {
  readonly requiredSourceId: string;
  readonly kind: 'STRUCTURED_DATA' | 'INSTITUTIONAL_REFERENCE';
  readonly value: string | number | boolean;
  readonly unit: string | null;
  readonly periodStart: string;
  readonly periodEnd: string;
  readonly reference: string | null;
}
