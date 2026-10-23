import {TestBed} from '@angular/core/testing';
import {ActivatedRoute, convertToParamMap, provideRouter} from '@angular/router';
import {of} from 'rxjs';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {AccessSessionService} from '../../access/data-access/access-session.service';
import {ProcessActivity, ProcessApi, ProcessAssignment, ProcessView} from '../data-access/process-api.service';
import {ProcessPlanningPage} from './process-planning.page';

describe('Process planning integrity', () => {
  const process: ProcessView = {id:'p-1',careerId:'c-1',code:'P',name:'Proceso',
    periodStart:'2026-01-01',periodEnd:'2026-12-31',modelVersionId:'v-1',modelVersionNumber:1,
    state:'ACTIVE',revision:4,indicators:[{id:'i-1',code:'I1',name:'Uno'},{id:'i-2',code:'I2',name:'Dos'}]};
  const assignment: ProcessAssignment = {id:'a-1',processId:process.id,indicatorId:'i-1',identityId:'u-1',
    teamId:null,function:'RESPONSIBLE',validFrom:'2026-01-01T00:00:00Z',validTo:'2027-01-01T00:00:00Z',
    reason:'Designacion',revokedAt:null,revision:0};
  const activity: ProcessActivity = {id:'activity-1',processId:process.id,indicatorId:'i-1',
    requiredSourceId:'source-1',assignmentId:assignment.id,title:'Recoleccion',dueAt:'2026-11-01T12:00:00Z',
    status:'PLANNED',dependencyIds:[],deadlineExceptionReason:null,deadlineExceptionAuthorityAssignmentId:null,
    deadlineExceptionLegalBasis:null,createdBy:'u-1',createdAt:'2026-10-01T00:00:00Z',revision:0};
  const api = {get:vi.fn(),teams:vi.fn(),assignments:vi.fn(),activities:vi.fn(),
    createActivity:vi.fn(),updateActivity:vi.fn(),addMember:vi.fn()};

  beforeEach(() => {
    vi.resetAllMocks();
    api.get.mockReturnValue(of(process)); api.teams.mockReturnValue(of([]));
    api.assignments.mockReturnValue(of([assignment])); api.activities.mockReturnValue(of([activity]));
    api.createActivity.mockReturnValue(of({...process,revision:5}));
    api.updateActivity.mockReturnValue(of({...process,revision:5}));
    api.addMember.mockReturnValue(of({...process,revision:5}));
    TestBed.configureTestingModule({imports:[ProcessPlanningPage],providers:[provideRouter([]),
      {provide:ActivatedRoute,useValue:{snapshot:{paramMap:convertToParamMap({processId:process.id})}}},
      {provide:ProcessApi,useValue:api}, {provide:AccessSessionService,useValue:{identity:() => null}}
    ]});
  });

  function setup() {
    const fixture = TestBed.createComponent(ProcessPlanningPage);
    fixture.detectChanges();
    return fixture;
  }

  it('offers only unrevoked assignments for the selected indicator', () => {
    const page = setup().componentInstance;
    page.assignments.set([assignment,{...assignment,id:'other',indicatorId:'i-2'},
      {...assignment,id:'revoked',revokedAt:'2026-10-01T00:00:00Z'}]);
    page.activityIndicator = 'i-1';
    expect(page.activityAssignments()).toEqual([assignment]);
    page.activityIndicator = 'i-2';
    expect(page.activityAssignments().map(a => a.id)).toEqual(['other']);
  });

  it('rejects assignments from another indicator and invalid dates before mutation', () => {
    const page = setup().componentInstance;
    page.activityTitle = 'Actividad'; page.activityIndicator = 'i-2';
    page.activityAssignment = assignment.id; page.activityDue = '2026-11-01T12:00';
    page.saveActivity(); expect(api.createActivity).not.toHaveBeenCalled();
    page.activityIndicator = 'i-1'; page.activityDue = 'invalid';
    expect(() => page.saveActivity()).not.toThrow();
    expect(api.createActivity).not.toHaveBeenCalled(); expect(page.error()).not.toBe('');
  });

  it('preserves the required source when updating an activity and adopts the returned revision', () => {
    const page = setup().componentInstance;
    page.editActivity(activity); page.activityTitle = 'Titulo corregido'; page.saveActivity();
    expect(api.updateActivity).toHaveBeenCalledWith(process,activity.id,expect.objectContaining({
      title:'Titulo corregido',requiredSourceId:activity.requiredSourceId,assignmentId:assignment.id,
      dueAt:new Date(activity.dueAt).toISOString()
    }));
    expect(page.process()?.revision).toBe(5);
    expect(page.editingActivity).toBeNull(); expect(page.activitySource).toBe('');
  });

  it('creates activities with an explicit source and parsed dependencies', () => {
    const page = setup().componentInstance;
    page.activityTitle = 'Actividad'; page.activityIndicator = 'i-1'; page.activityAssignment = assignment.id;
    page.activityDue = '2026-11-01T12:00'; page.activitySource = ' source-1 ';
    page.activityDependencies = ' dep-1, dep-2, ';
    page.saveActivity();
    expect(api.createActivity).toHaveBeenCalledWith(process,expect.objectContaining({
      requiredSourceId:'source-1',dependencyIds:['dep-1','dep-2']
    }));
  });

  it('uses the role assignment supplied for the member', () => {
    const page = setup().componentInstance;
    page.memberTeam = 'team-1'; page.memberUser = 'member-1'; page.memberRole = 'member-role-1';
    page.memberFrom = '2026-10-01T12:00'; page.memberUntil = '2026-11-01T12:00'; page.addMember();
    expect(api.addMember).toHaveBeenCalledWith(process,'team-1',expect.objectContaining({
      userId:'member-1',roleAssignmentId:'member-role-1'
    }));
  });

  it('hides activity editing while the process is suspended', () => {
    api.get.mockReturnValue(of({...process,state:'SUSPENDED'}));
    const fixture = setup();
    expect(fixture.nativeElement.querySelector('#activity-title')).toBeNull();
    expect([...fixture.nativeElement.querySelectorAll('button')].some((button) =>
      (button as HTMLButtonElement).textContent?.trim() === 'Editar')).toBe(false);
  });
});
