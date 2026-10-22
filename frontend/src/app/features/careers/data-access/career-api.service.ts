import {inject, Injectable} from '@angular/core';
import {ApiClient} from '../../../core/http/api-client';
export type CatalogStatus = 'ACTIVE' | 'DISABLED';
export interface UnitInput {code: string; name: string}
export interface CareerInput extends UnitInput {academicUnitId: string; validFrom: string; validUntil: string | null}
export interface CatalogRecord<T> {id: string; revision: number; createdBy: string; createdAt: string; status: CatalogStatus; data: T}
export interface Career extends CatalogRecord<CareerInput> {classificationPending: boolean}
export type AcademicUnit = CatalogRecord<UnitInput>;
export interface CatalogPage<T> {items: readonly T[]; page: number; size: number; totalElements: number}
@Injectable({providedIn: 'root'})
export class CareerApi {
  private readonly api = inject(ApiClient);
  access() {return this.api.get<{canManage: boolean}>('careers/access');}
  careers(page=0, q='', status?: CatalogStatus, size=20) {return this.api.get<CatalogPage<Career>>('careers', {page,size,q,...(status ? {status} : {})});}
  units(page=0,q='') {return this.api.get<CatalogPage<AcademicUnit>>('academic-units',{page,size:100,q});}
  getUnit(id: string) {return this.api.get<AcademicUnit>(`academic-units/${id}`);}
  history(id: string,page=0) {return this.api.get<CatalogPage<Career>>(`careers/${id}/history`,{page,size:20});}
  saveCareer(value: CareerInput, current: Career | null) {
    return current ? this.api.patch<Career>(`careers/${current.id}`,value,{'If-Match': `"${current.revision}"`}) : this.api.post<Career>('careers',value);
  }
  saveUnit(value: UnitInput, current: AcademicUnit | null) {
    return current ? this.api.patch<AcademicUnit>(`academic-units/${current.id}`,value,{'If-Match': `"${current.revision}"`}) : this.api.post<AcademicUnit>('academic-units',value);
  }
  careerState(value: Career, reason: string) {return this.api.post<Career>(`careers/${value.id}/state-decisions`,{status: value.status==='ACTIVE' ? 'DISABLED' : 'ACTIVE',reason},{'If-Match': `"${value.revision}"`});}
  unitState(value: AcademicUnit, reason: string) {return this.api.post<AcademicUnit>(`academic-units/${value.id}/state-decisions`,{status: value.status==='ACTIVE' ? 'DISABLED' : 'ACTIVE',reason},{'If-Match': `"${value.revision}"`});}
}
