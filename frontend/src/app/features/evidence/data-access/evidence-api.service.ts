import {Injectable, inject} from '@angular/core';
import {Observable} from 'rxjs';
import {ApiClient} from '../../../core/http/api-client';
import {
  ApplicabilityDecisionCommand,
  ApplicabilityDecisionView,
  EvidenceDraftCommand,
  EvidencePage,
  EvidenceVersionResource,
  EvidenceVersionView,
  EvidenceView,
  RequiredSourceState,
  SourceCoverageCommand,
  SourceCoverageView,
  StructuredSourceCommand,
  StructuredSourceView
} from './evidence.models';

@Injectable({providedIn: 'root'})
export class EvidenceApi {
  private readonly api = inject(ApiClient);

  list(processId: string, indicatorId?: string): Observable<EvidencePage> {
    return this.api.get('evidences', indicatorId ? {processId, indicatorId} : {processId});
  }

  getEvidence(id: string): Observable<EvidenceView> {
    return this.api.get(`evidences/${id}`);
  }

  getVersion(id: string): Observable<EvidenceVersionResource> {
    return this.api.getResource(`evidence-versions/${id}`);
  }

  create(command: EvidenceDraftCommand): Observable<EvidenceVersionResource> {
    return this.api.postResource('evidences', command);
  }

  update(id: string, command: EvidenceDraftCommand, etag: string): Observable<EvidenceVersionResource> {
    const content = {
      title: command.title,
      description: command.description,
      provenance: command.provenance,
      coveredFrom: command.coveredFrom,
      coveredTo: command.coveredTo,
      indicatorIds: command.indicatorIds
    };
    return this.api.patchResource(`evidence-versions/${id}`, content, etag);
  }

  requiredSources(versionId: string): Observable<readonly RequiredSourceState[]> {
    return this.api.get(`evidence-versions/${versionId}/required-sources`);
  }

  coverages(versionId: string): Observable<readonly SourceCoverageView[]> {
    return this.api.get(`evidence-versions/${versionId}/source-coverages`);
  }

  declareCoverage(versionId: string, command: SourceCoverageCommand, etag: string): Observable<SourceCoverageView> {
    return this.api.post(`evidence-versions/${versionId}/source-coverages`, command, {'If-Match': etag});
  }

  removeCoverage(versionId: string, coverageId: string, etag: string): Observable<void> {
    return this.api.delete(`evidence-versions/${versionId}/source-coverages/${coverageId}`, {'If-Match': etag});
  }

  submit(versionId: string, etag: string): Observable<EvidenceVersionView> {
    return this.api.post(`evidence-versions/${versionId}/submit`, {}, {'If-Match': etag});
  }

  structuredSnapshots(versionId: string): Observable<readonly StructuredSourceView[]> {
    return this.api.get(`evidence-versions/${versionId}/structured-sources`);
  }

  registerStructuredSource(versionId: string, command: StructuredSourceCommand,
                           etag: string): Observable<StructuredSourceView> {
    return this.api.post(`evidence-versions/${versionId}/structured-sources`, command, {'If-Match': etag});
  }

  applicabilityHistory(processId: string): Observable<readonly ApplicabilityDecisionView[]> {
    return this.api.get(`processes/${processId}/applicability-decisions`);
  }

  decideApplicability(processId: string, command: ApplicabilityDecisionCommand,
                      etag: string): Observable<ApplicabilityDecisionView> {
    return this.api.post(`processes/${processId}/applicability-decisions`, command, {'If-Match': etag});
  }
}
