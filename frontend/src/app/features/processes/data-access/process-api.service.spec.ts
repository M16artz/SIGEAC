import {TestBed} from '@angular/core/testing';
import {of} from 'rxjs';
import {describe, it, expect, vi, beforeEach} from 'vitest';
import {ApiClient} from '../../../core/http/api-client';
import {ProcessApi, ProcessView} from './process-api.service';

describe('ProcessApi concurrency', () => {
  const process: ProcessView = {id:'p-1',careerId:'c-1',code:'P',name:'Proceso',periodStart:'2026-01-01',periodEnd:'2026-12-31',modelVersionId:'v-1',modelVersionNumber:1,state:'ACTIVE',revision:4,indicators:[]};
  let api: ProcessApi;
  const client = {post:vi.fn(),get:vi.fn()};
  beforeEach(() => {
    client.post.mockReset().mockReturnValue(of({})); client.get.mockReset().mockReturnValue(of({...process,revision:5}));
    TestBed.configureTestingModule({providers:[ProcessApi,{provide:ApiClient,useValue:client}]});
    api=TestBed.inject(ProcessApi);
  });
  it('sends authority provenance and a strong If-Match for suspension and resumption', () => {
    const command = {authorityAssignmentId:'a-1',legalBasis:'Designacion',documentReference:'fixture://decision',reason:'Decision justificada'};
    for (const target of ['SUSPENDED', 'ACTIVE'] as const) {
      api.transition(process,target,command).subscribe();
      expect(client.post).toHaveBeenLastCalledWith('processes/p-1/transitions',{target,...command},{'If-Match':'"4"'});
    }
  });
  it('sends the complete reopening decision with the current revision', () => {
    const command = {authorityAssignmentId:'a-1',legalBasis:'Designacion',documentReference:'fixture://reopening',reason:'Reapertura justificada'};
    api.reopen({...process,state:'CLOSED'},command).subscribe();
    expect(client.post).toHaveBeenCalledWith('processes/p-1/reopen',command,{'If-Match':'"4"'});
  });
  it('reloads the process after assigning so the next command uses the new revision', () => {
    let revision=0;
    api.assign(process,{identityId:'u-1'}).subscribe(value => revision=value.revision);
    expect(client.post).toHaveBeenCalledWith('processes/p-1/assignments',{identityId:'u-1'},{'If-Match':'"4"'});
    expect(client.get).toHaveBeenCalledWith('processes/p-1'); expect(revision).toBe(5);
  });
  it('records authority provenance and reloads before activation', () => {
    const command={authorityAssignmentId:'a-1',legalBasis:'Designación',documentReference:'fixture://authorization',reason:'Inicio autorizado sintético'};
    api.authorize(process,command).subscribe();
    expect(client.post).toHaveBeenCalledWith('processes/p-1/authorizations',command,{'If-Match':'"4"'});
    expect(client.get).toHaveBeenCalledWith('processes/p-1');
  });
});
