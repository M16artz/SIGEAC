import {Injectable,inject} from '@angular/core';import {Observable} from 'rxjs';import {ApiClient} from '../../../core/http/api-client';import {ApplicabilityInput,ConfiguredVersionDraft,EvaluationModel,ModelCareerApplicability,ModelComparison,ModelVersion,NormativeReference,VersionDraft} from './model.models';
@Injectable({providedIn:'root'}) export class ModelApi {
 private readonly api=inject(ApiClient);
 models():Observable<readonly EvaluationModel[]>{return this.api.get('models')}
 createModel(name:string,description:string):Observable<EvaluationModel>{return this.api.post('models',{name,description})}
 versions(modelId:string):Observable<readonly ModelVersion[]>{return this.api.get(`models/${modelId}/versions`)}
 configuredVersions(modelId:string):Observable<readonly ModelVersion[]>{return this.api.get(`models/${modelId}/versions/configured`)}
 createVersion(modelId:string,draft:VersionDraft):Observable<ModelVersion>{return this.api.post(`models/${modelId}/versions`,draft)}
 createConfiguredVersion(modelId:string,draft:ConfiguredVersionDraft):Observable<ModelVersion>{return this.api.post(`models/${modelId}/versions/configured`,draft)}
 version(id:string):Observable<ModelVersion>{return this.api.get(`model-versions/${id}`)}
 replace(id:string,revision:number,draft:VersionDraft):Observable<ModelVersion>{return this.api.put(`model-versions/${id}`,draft,{'If-Match':`"${revision}"`})}
 replaceConfigured(id:string,revision:number,draft:ConfiguredVersionDraft):Observable<ModelVersion>{return this.api.put(`model-versions/${id}/configured`,draft,{'If-Match':`"${revision}"`})}
 publish(id:string,revision:number,approverId:string):Observable<ModelVersion>{return this.api.post(`model-versions/${id}/publish`,{approverId},{'If-Match':`"${revision}"`})}
 publishConfigured(id:string,revision:number,approverId:string):Observable<ModelVersion>{return this.api.post(`model-versions/${id}/publish-configured`,{approverId},{'If-Match':`"${revision}"`})}
 successor(id:string):Observable<ModelVersion>{return this.api.post(`model-versions/${id}/successors`,{})}
 configuredSuccessor(id:string):Observable<ModelVersion>{return this.api.post(`model-versions/${id}/successors/configured`,{})}
 compare(from:string,to:string):Observable<ModelComparison>{return this.api.get('model-versions/compare',{from,to})}
 registerReference(reference:NormativeReference):Observable<NormativeReference>{return this.api.post('normative-references',reference)}
 references(versionId:string):Observable<readonly NormativeReference[]>{return this.api.get(`model-versions/${versionId}/normative-references`)}
 applicability(versionId:string):Observable<readonly ModelCareerApplicability[]>{return this.api.get(`model-versions/${versionId}/applicability-decisions`)}
 decideApplicability(versionId:string,revision:number,input:ApplicabilityInput):Observable<ModelCareerApplicability>{return this.api.post(`model-versions/${versionId}/applicability-decisions`,input,{'If-Match':`"${revision}"`})}
 audit(versionId?:string):Observable<{items:readonly unknown[];total:number}>{return this.api.get('model-audit-events',versionId?{versionId}:{})}
}
