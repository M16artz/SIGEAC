import {Injectable, inject} from '@angular/core';
import {Observable} from 'rxjs';
import {ApiClient} from '../../../core/http/api-client';

export interface VersionEventBase {
  id: string;
  versionId: string;
}

export interface FindingData {
  type: 'STRENGTH' | 'WEAKNESS' | 'OBSERVATION';
  title: string;
  description: string;
  indicatorIds: readonly string[];
  evaluationIds?: readonly string[];
  evidenceVersionIds?: readonly string[];
}

export interface Finding {
  id: string;
  revision: number;
  createdAt: string;
  createdBy: string | null;
  data: FindingData;
}

export interface FindingPage {
  items: readonly Finding[];
  page: number;
  size: number;
  totalElements: number;
}

export interface ConsolidationData {
  findingIds: readonly string[];
  aggregationRuleId: string | null;
  reason: string;
}

export interface Consolidation {
  id: string;
  revision: number;
  createdAt: string;
  createdBy: string | null;
  data: ConsolidationData;
}

export interface ReportVersionData {
  kind: 'PRELIMINARY' | 'FINAL';
  consolidationId: string;
  templateReference: string;
  templateValidated: boolean;
  content: string;
  predecessorVersionId: string | null;
}

export interface Socialization {
  id: string;
  versionId: string;
  occurredAt: string;
  participantAssignmentIds: readonly string[];
  recordReference: string;
  recordedBy: string | null;
  recordedAt: string;
}

export interface Observation {
  id: string;
  versionId: string;
  sectionReference: string;
  text: string;
  createdBy: string | null;
  createdAt: string;
}

export interface Reply {
  id: string;
  observationId: string;
  response: string;
  correctedVersionId: string | null;
  recordedBy: string | null;
  recordedAt: string;
}

export interface DecisionProvenance {
  decidedBy: string | null;
  authorityAssignmentId: string;
  legalBasis: string;
  documentReference: string;
  reason: string;
  decidedAt: string;
}

export interface ApprovalEvent {
  id: string;
  versionId: string;
  decision: 'APPROVED' | 'RETURNED';
  provenance: DecisionProvenance;
}

export interface ReportVersion {
  id: string;
  revision: number;
  createdAt: string | null;
  createdBy: string | null;
  data: ReportVersionData;
  state?: string | null;
  socializations?: readonly Socialization[];
  observations?: readonly Observation[];
  replies?: readonly Reply[];
  decisions?: readonly ApprovalEvent[];
}

export interface VersionPage {
  items: readonly ReportVersion[];
  page: number;
  size: number;
  totalElements: number;
}

export interface RegisterFindingCommand {
  type: FindingData['type'];
  title: string;
  description: string;
  indicatorIds: readonly string[];
  evaluationIds?: readonly string[];
  evidenceVersionIds?: readonly string[];
}

export interface ConsolidateCommand {
  findingIds: readonly string[];
  aggregationRuleId: string | null;
  reason: string;
}

export interface IssueCommand {
  kind: ReportVersionData['kind'];
  consolidationId: string;
  templateReference: string;
  templateValidated: boolean;
  content: string;
  predecessorVersionId: string | null;
}

export interface SocializeCommand {
  occurredAt: string;
  participantAssignmentIds: readonly string[];
  recordReference: string;
}

export interface ObserveCommand {
  sectionReference: string;
  text: string;
}

export interface ReplyCommand {
  response: string;
  correctedVersionId: string | null;
}

export interface DecideCommand {
  decision: ApprovalEvent['decision'];
  provenance: {
    authorityAssignmentId: string;
    legalBasis: string;
    documentReference: string;
    reason: string;
  };
}

export interface EventItem<D> {
  id: string;
  revision: number;
  createdAt: string | null;
  createdBy: string | null;
  data: D;
}

export interface SocializationData {
  occurredAt: string;
  participantAssignmentIds: readonly string[];
  recordReference: string;
}

export interface ObservationData {
  sectionReference: string;
  text: string;
}

export interface ReplyData {
  response: string;
  correctedVersionId: string | null;
}

export interface ApprovalData {
  decision: ApprovalEvent['decision'];
  provenance: DecideCommand['provenance'];
}

@Injectable({providedIn: 'root'})
export class ReportApi {
  private readonly http = inject(ApiClient);

  findings(processId: string, page = 0, size = 100): Observable<FindingPage> {
    return this.http.get<FindingPage>(`/processes/${processId}/findings?page=${page}&size=${size}`);
  }

  registerFinding(processId: string, command: RegisterFindingCommand): Observable<Finding> {
    return this.http.post<Finding>(`/processes/${processId}/findings`, command, {'If-Match': '"0"'});
  }

  consolidate(processId: string, expectedRevision: number, command: ConsolidateCommand): Observable<Consolidation> {
    return this.http.post<Consolidation>(`/processes/${processId}/consolidations`, command,
      {'If-Match': `"${expectedRevision}"`});
  }

  versions(processId: string, page = 0, size = 100): Observable<VersionPage> {
    return this.http.get<VersionPage>(`/processes/${processId}/report-versions?page=${page}&size=${size}`);
  }

  version(versionId: string): Observable<ReportVersion> {
    return this.http.get<ReportVersion>(`/report-versions/${versionId}`);
  }

  issueVersion(processId: string, expectedRevision: number, command: IssueCommand): Observable<ReportVersion> {
    return this.http.post<ReportVersion>(`/processes/${processId}/report-versions`, command,
      {'If-Match': `"${expectedRevision}"`});
  }

  socialize(versionId: string, expectedRevision: number, command: SocializeCommand): Observable<EventItem<SocializationData>> {
    return this.http.post<EventItem<SocializationData>>(`/report-versions/${versionId}/socializations`, command,
      {'If-Match': `"${expectedRevision}"`});
  }

  observe(versionId: string, expectedRevision: number, command: ObserveCommand): Observable<EventItem<ObservationData>> {
    return this.http.post<EventItem<ObservationData>>(`/report-versions/${versionId}/observations`, command,
      {'If-Match': `"${expectedRevision}"`});
  }

  reply(observationId: string, expectedRevision: number, command: ReplyCommand): Observable<EventItem<ReplyData>> {
    return this.http.post<EventItem<ReplyData>>(`/report-observations/${observationId}/responses`, command,
      {'If-Match': `"${expectedRevision}"`});
  }

  decide(versionId: string, expectedRevision: number, command: DecideCommand): Observable<EventItem<ApprovalData>> {
    return this.http.post<EventItem<ApprovalData>>(`/report-versions/${versionId}/approval-decisions`, command,
      {'If-Match': `"${expectedRevision}"`});
  }
}
