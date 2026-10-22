import {Injectable, inject} from '@angular/core';
import {Observable} from 'rxjs';
import {ApiClient} from '../../../core/http/api-client';
import {EvidenceDraftCommand, EvidencePage, EvidenceVersionResource, EvidenceView} from './evidence.models';

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
}
