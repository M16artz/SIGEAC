export interface ReviewInboxEntry {
  readonly versionId: string;
  readonly evidenceId: string;
  readonly processId: string;
  readonly careerId: string | null;
  readonly revisionNumber: number;
  readonly title: string;
  readonly submitterId: string;
  readonly submittedAt: string;
  readonly indicatorIds: readonly string[];
  readonly coveredRequiredSourceIds: readonly string[];
}

export interface ReviewOutcomeCommand {
  readonly requiredSourceId: string;
  readonly outcome: 'ACCEPTED' | 'OBSERVED' | 'REJECTED';
  readonly note: string;
}

export interface ReviewDecisionCommand {
  readonly selfReviewException: boolean;
  readonly exceptionReason: string | null;
  readonly justification: string;
  readonly outcomes: readonly ReviewOutcomeCommand[];
}

export interface ReviewOutcomeView {
  readonly requiredSourceId: string;
  readonly outcome: string;
  readonly note: string | null;
}

export interface ReviewDecisionView {
  readonly id: string;
  readonly versionId: string;
  readonly decision: 'ACCEPTED' | 'OBSERVED' | 'REJECTED';
  readonly justification: string;
  readonly reviewerId: string;
  readonly reviewedAt: string;
  readonly selfReviewException: boolean;
  readonly exceptionReason: string | null;
  readonly predecessorId: string | null;
  readonly successorVersionId: string | null;
  readonly revision: number;
  readonly outcomes: readonly ReviewOutcomeView[];
}

export interface VersionReviewView {
  readonly decisionId: string | null;
  readonly decision: string;
  readonly revision: number;
  readonly successorVersionId: string | null;
  readonly outcomes: readonly ReviewOutcomeView[];
}

export interface ReviewCorrectionView {
  readonly correctedVersionId: string;
  readonly successorVersionId: string;
  readonly successorRevision: number;
}
