import {TestBed} from '@angular/core/testing';
import {signal} from '@angular/core';
import {of, throwError} from 'rxjs';
import {afterEach, describe, expect, it, vi} from 'vitest';
import {EvidenceApi} from '../../features/evidence/data-access/evidence-api.service';
import {AccessSessionService} from '../../features/access/data-access/access-session.service';
import {EvidenceReferenceComponent} from './evidence-reference.component';

describe('EvidenceReferenceComponent', () => {
  afterEach(() => TestBed.resetTestingModule());
  function setup(failed = false) {
    const api = {getVersion: vi.fn().mockReturnValue(failed ? throwError(() => new Error('secret')) : of({body: {title: 'Informe desde el servicio', revisionNumber: 3}})),
      requiredSources: vi.fn().mockReturnValue(of([{id: 'source', name: 'Fuente oficial'}]))};
    TestBed.configureTestingModule({imports: [EvidenceReferenceComponent], providers: [
      {provide: EvidenceApi, useValue: api}, {provide: AccessSessionService, useValue: {identity: signal({id: 'test-user'})}}]});
    const fixture = TestBed.createComponent(EvidenceReferenceComponent);
    fixture.componentRef.setInput('versionId', 'private-version-id'); fixture.detectChanges(); fixture.detectChanges();
    return {fixture, api};
  }
  it('shows the actual title and version without rendering the HTTP identifier', () => {
    const {fixture, api} = setup();
    expect(fixture.nativeElement.textContent).toBe('Informe desde el servicio (versión 3)');
    expect(api.getVersion).toHaveBeenCalledWith('private-version-id');
  });
  it('resolves a source name and does not repeat a successful read', () => {
    const {fixture, api} = setup(); fixture.componentRef.setInput('sourceId', 'source'); fixture.detectChanges(); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toBe('Fuente oficial');
    fixture.detectChanges(); expect(api.requiredSources).toHaveBeenCalledOnce();
  });
  it('does not expose diagnostics or identifiers after a failed read', () => {
    const {fixture} = setup(true);
    expect(fixture.nativeElement.textContent).toBe('Evidencia no disponible');
  });
});
