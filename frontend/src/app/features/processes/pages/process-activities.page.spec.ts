import {TestBed} from '@angular/core/testing';
import {ActivatedRoute, convertToParamMap, provideRouter} from '@angular/router';
import {of, Subject, throwError} from 'rxjs';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {ProcessActivity, ProcessApi, ProcessView} from '../data-access/process-api.service';
import {ProcessActivitiesPage} from './process-activities.page';

describe('Participant activities', () => {
  const process: ProcessView = {id:'p-1',careerId:'c-1',code:'P',name:'Proceso',
    periodStart:'2026-01-01',periodEnd:'2026-12-31',modelVersionId:'v-1',modelVersionNumber:1,
    state:'ACTIVE',revision:4,indicators:[{id:'i-1',code:'IND-1',name:'Indicador visible'}]};
  const activity: ProcessActivity = {id:'activity-1',processId:process.id,indicatorId:'i-1',
    requiredSourceId:null,assignmentId:'a-1',title:'Actividad asignada',dueAt:'2026-11-01T12:00:00Z',
    status:'PLANNED',dependencyIds:[],deadlineExceptionReason:null,deadlineExceptionAuthorityAssignmentId:null,
    deadlineExceptionLegalBasis:null,createdBy:'u-1',createdAt:'2026-10-01T00:00:00Z',revision:0};
  const api = {get:vi.fn(),activities:vi.fn()};

  beforeEach(() => {
    vi.resetAllMocks();
    api.get.mockReturnValue(of(process)); api.activities.mockReturnValue(of([activity]));
    TestBed.configureTestingModule({imports:[ProcessActivitiesPage],providers:[provideRouter([]),
      {provide:ActivatedRoute,useValue:{snapshot:{paramMap:convertToParamMap({processId:process.id})}}},
      {provide:ProcessApi,useValue:api}
    ]});
  });

  function setup() {
    const fixture = TestBed.createComponent(ProcessActivitiesPage);
    fixture.detectChanges();
    return fixture;
  }

  it('shows the activities returned by the scoped API without administrative controls', () => {
    const fixture = setup();
    expect(api.activities).toHaveBeenCalledWith(process.id);
    expect(fixture.nativeElement.textContent).toContain(activity.title);
    expect(fixture.nativeElement.textContent).toContain('IND-1 · Indicador visible');
    expect(fixture.nativeElement.querySelector('caption').textContent).toContain('ámbito');
    expect(fixture.nativeElement.querySelector('form')).toBeNull();
    expect(fixture.componentInstance.loading()).toBe(false);
  });

  it('reports no current activities when the server returns an empty list', () => {
    api.activities.mockReturnValue(of([]));
    const fixture = setup();
    expect(fixture.nativeElement.querySelector('table')).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('No hay actividades');
  });

  it('shows suspension while retaining the authorized read view', () => {
    api.get.mockReturnValue(of({...process,state:'SUSPENDED'}));
    const fixture = setup();
    expect(fixture.nativeElement.querySelector('[role="status"]').textContent).toContain('suspendido');
    expect(fixture.nativeElement.textContent).toContain(activity.title);
  });

  it('handles denied or failed requests without showing partial results', () => {
    api.activities.mockReturnValue(throwError(() => new Error('denied')));
    const fixture = setup();
    expect(fixture.nativeElement.querySelector('[role="alert"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('table')).toBeNull();
    expect(fixture.componentInstance.process()).toBeNull();
    expect(fixture.componentInstance.loading()).toBe(false);
  });

  it('shows a loading state until the requests complete', () => {
    const pending = new Subject<readonly ProcessActivity[]>();
    api.activities.mockReturnValue(pending);
    const fixture = setup();
    expect(fixture.nativeElement.querySelector('[role="status"]').textContent).toContain('Consultando');
    pending.next([activity]); pending.complete(); fixture.detectChanges();
    expect(fixture.componentInstance.loading()).toBe(false);
    expect(fixture.nativeElement.textContent).toContain(activity.title);
  });
});
