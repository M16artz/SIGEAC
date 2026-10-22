import {TestBed} from '@angular/core/testing';
import {of} from 'rxjs';
import {describe,it,expect,vi} from 'vitest';
import {ApiClient} from '../../../core/http/api-client';
import {CareerApi,Career} from './career-api.service';
describe('CareerApi',()=>{
  it('uses the strong revision for updates and explicit state decisions',()=>{
    const client={patch:vi.fn().mockReturnValue(of({})),post:vi.fn().mockReturnValue(of({}))};
    TestBed.configureTestingModule({providers:[CareerApi,{provide:ApiClient,useValue:client}]});
    const api=TestBed.inject(CareerApi);const career={id:'c',revision:7,status:'ACTIVE',data:{code:'C',name:'Carrera',academicUnitId:'u',validFrom:'2020-01-01',validUntil:null}} as Career;
    api.saveCareer(career.data,career).subscribe();api.careerState(career,'Cambio autorizado sintético').subscribe();
    expect(client.patch).toHaveBeenCalledWith('careers/c',career.data,{'If-Match':'"7"'});
    expect(client.post).toHaveBeenCalledWith('careers/c/state-decisions',{status:'DISABLED',reason:'Cambio autorizado sintético'},{'If-Match':'"7"'});
  });
});
