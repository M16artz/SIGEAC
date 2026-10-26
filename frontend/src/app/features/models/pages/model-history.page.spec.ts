import {TestBed} from '@angular/core/testing';
import {of, throwError} from 'rxjs';
import {describe, expect, it, vi} from 'vitest';
import {ModelApi} from '../data-access/model-api.service';
import {ModelVersion} from '../data-access/model.models';
import {ModelHistoryPage} from './model-history.page';

describe('ModelHistoryPage', () => {
  it('loads configured history and creates the corresponding successor from its button', () => {
    const version = {id: 'v1', versionNumber: 1, status: 'PUBLISHED', source: 'fixture://model',
      configuration: {indicators: [], scales: [], rules: []}} as unknown as ModelVersion;
    const api = {configuredVersions: vi.fn().mockReturnValue(of([version])),
      configuredSuccessor: vi.fn().mockReturnValue(of({...version, id: 'v2', status: 'DRAFT'})), successor: vi.fn()};
    TestBed.configureTestingModule({imports: [ModelHistoryPage], providers: [{provide: ModelApi, useValue: api}]});
    const fixture = TestBed.createComponent(ModelHistoryPage);
    fixture.componentInstance.modelId = 'model'; fixture.componentInstance.load(); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('PUBLISHED');
    const buttons = fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>;
    buttons[1].click();
    expect(api.configuredSuccessor).toHaveBeenCalledWith('v1');
    expect(api.successor).not.toHaveBeenCalled();
    expect(api.configuredVersions).toHaveBeenCalledTimes(2);
  });
  it('clears the previous model when the new history cannot be read', () => {
    const api = {configuredVersions: vi.fn().mockReturnValue(throwError(() => new Error('Forbidden')))};
    TestBed.configureTestingModule({imports: [ModelHistoryPage], providers: [{provide: ModelApi, useValue: api}]});
    const fixture = TestBed.createComponent(ModelHistoryPage);
    fixture.componentInstance.versions.set([{id: 'old'} as ModelVersion]);
    fixture.componentInstance.load(); fixture.detectChanges();
    expect(fixture.componentInstance.versions()).toEqual([]);
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('historial autorizado');
  });
});
