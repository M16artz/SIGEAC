import {TestBed} from '@angular/core/testing';
import {of, Subject, throwError} from 'rxjs';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {AccessSessionService} from '../../access/data-access/access-session.service';
import {ProcessApi, ProcessView} from '../data-access/process-api.service';
import {ProcessStateControls} from './process-state-controls.component';

describe('Process state decisions', () => {
  const process: ProcessView = {id:'p-1',careerId:'c-1',code:'P',name:'Proceso',
    periodStart:'2026-01-01',periodEnd:'2026-12-31',modelVersionId:'v-1',
    modelVersionNumber:1,state:'ACTIVE',revision:4,indicators:[]};
  const command = {authorityAssignmentId:'a-1',legalBasis:'Designacion',
    documentReference:'fixture://decision',reason:'Decision justificada'};
  const api = {transition:vi.fn(),reopen:vi.fn()};

  beforeEach(() => {
    vi.resetAllMocks();
    api.transition.mockReturnValue(of({...process,state:'SUSPENDED',revision:5}));
    api.reopen.mockReturnValue(of({...process,state:'REOPENED',revision:5}));
    TestBed.configureTestingModule({imports:[ProcessStateControls],providers:[
      {provide:ProcessApi,useValue:api},
      {provide:AccessSessionService,useValue:{identity:() => ({assignments:[{id:'a-1',roleName:'Autoridad',careerId:'c-1'}]})}}
    ]});
  });

  function setup(state = process.state) {
    const fixture = TestBed.createComponent(ProcessStateControls);
    fixture.componentRef.setInput('process',{...process,state});
    fixture.detectChanges();
    return fixture;
  }

  it('requires provenance before allowing a transition or reopening', () => {
    const fixture = setup();
    const controls = fixture.componentInstance;
    controls.reason = command.reason;
    controls.move('SUSPENDED');
    controls.reopen();
    fixture.detectChanges();
    expect(api.transition).not.toHaveBeenCalled();
    expect(api.reopen).not.toHaveBeenCalled();
    expect(fixture.nativeElement.querySelector('button').disabled).toBe(true);
    expect(fixture.nativeElement.querySelector('[role="alert"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('#transition-authority option[value="a-1"]')).not.toBeNull();
  });

  it.each(['ACTIVE','SUSPENDED','REOPENED','CLOSED'] as const)('submits the decision for %s and emits the revised process', async state => {
    const fixture = setup(state);
    await fixture.whenStable();
    const controls = fixture.componentInstance;
    Object.assign(controls,command);
    const changed = vi.fn();
    controls.changed.subscribe(changed);
    fixture.changeDetectorRef.markForCheck();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.nativeElement.querySelector('button').click();
    if (state === 'CLOSED') expect(api.reopen).toHaveBeenCalledWith({...process,state},command);
    else expect(api.transition).toHaveBeenCalledWith({...process,state},state === 'ACTIVE' ? 'SUSPENDED' : 'ACTIVE',command);
    expect(changed).toHaveBeenCalledWith(expect.objectContaining({revision:5}));
    expect(controls.busy()).toBe(false);
  });

  it('retains the decision when the server rejects it', () => {
    const controls = setup().componentInstance;
    Object.assign(controls,command);
    api.transition.mockReturnValue(throwError(() => new Error('denied')));
    controls.move('SUSPENDED');
    expect(controls.error()).not.toBe('');
    expect(controls.busy()).toBe(false);
    expect(controls.documentReference).toBe(command.documentReference);
  });

  it('does not send another request while a decision is pending', () => {
    const controls = setup().componentInstance;
    Object.assign(controls,command);
    api.transition.mockReturnValue(new Subject<ProcessView>());
    controls.move('SUSPENDED');
    controls.move('SUSPENDED');
    expect(api.transition).toHaveBeenCalledTimes(1);
  });
});
