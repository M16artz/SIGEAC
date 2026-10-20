import {Injectable,inject} from '@angular/core';import {Observable} from 'rxjs';import {ApiClient} from '../../../core/http/api-client';import {EvaluationModel,ModelComparison,ModelVersion,VersionDraft} from './model.models';
@Injectable({providedIn:'root'}) export class ModelApi {
 private readonly api=inject(ApiClient);
 models():Observable<readonly EvaluationModel[]>{return this.api.get('models')}
 createModel(name:string,description:string):Observable<EvaluationModel>{return this.api.post('models',{name,description})}
 versions(modelId:string):Observable<readonly ModelVersion[]>{return this.api.get(`models/${modelId}/versions`)}
 createVersion(modelId:string,draft:VersionDraft):Observable<ModelVersion>{return this.api.post(`models/${modelId}/versions`,draft)}
 version(id:string):Observable<ModelVersion>{return this.api.get(`model-versions/${id}`)}
 replace(id:string,revision:number,draft:VersionDraft):Observable<ModelVersion>{return this.api.put(`model-versions/${id}`,draft,{'If-Match':String(revision)})}
 publish(id:string,revision:number,approverId:string):Observable<ModelVersion>{return this.api.post(`model-versions/${id}/publish`,{approverId},{'If-Match':String(revision)})}
 successor(id:string):Observable<ModelVersion>{return this.api.post(`model-versions/${id}/successors`,{})}
 compare(from:string,to:string):Observable<ModelComparison>{return this.api.get('model-versions/compare',{from,to})}
 audit(versionId?:string):Observable<{items:readonly unknown[];total:number}>{return this.api.get('model-audit-events',versionId?{versionId}:{})}
}
