import {TestBed} from '@angular/core/testing';
import {ActivatedRoute, convertToParamMap, provideRouter} from '@angular/router';
import {of, throwError} from 'rxjs';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {ApiProblemError} from '../../../core/http/api-client';
import {ProcessApi} from '../../processes/data-access/process-api.service';
import {EvidenceApi} from '../data-access/evidence-api.service';
import {EvidenceEditorPage} from './evidence-editor.page';

describe('EvidenceEditorPage', () => {
  const processId = '00000000-0000-0000-0000-000000000301';
  const indicatorId = '00000000-0000-0000-0000-000000000401';
  const versionId = '00000000-0000-0000-0000-000000000501';
  const evidence = {
    id: versionId,
    evidenceId: '00000000-0000-0000-0000-000000000601',
    revisionNumber: 1,
    status: 'DRAFT' as const,
    title: 'Informe sintético',
    description: 'Descripción sintética',
    provenance: 'Procedencia sintética',
    coveredFrom: '2026-01-01',
    coveredTo: '2026-06-30',
    indicatorIds: [indicatorId],
    createdBy: '00000000-0000-0000-0000-000000000101',
    createdAt: '2026-10-20T15:00:00Z',
    submittedBy: null,
    submittedAt: null,
    revision: 0,
    files: [],
    externalReferences: []
  };
  const api = {create: vi.fn(), getVersion: vi.fn(), update: vi.fn()};
  const processes = {get: vi.fn()};

  beforeEach(() => {
    vi.clearAllMocks();
    api.getVersion.mockReturnValue(of({body: evidence, etag: '"0"'}));
    api.update.mockReturnValue(of({body: {...evidence, revision: 1}, etag: '"1"'}));
    processes.get.mockReturnValue(of({
      id: processId,
      indicators: [{id: indicatorId, code: 'IND-1', name: 'Indicador sintético'}]
    }));
  });

  it('uses reactive validation and requires at least one indicator', () => {
    configure(null);
    const fixture = TestBed.createComponent(EvidenceEditorPage);
    fixture.detectChanges();

    fixture.componentInstance.save();

    expect(fixture.componentInstance.form.invalid).toBe(true);
    expect(fixture.componentInstance.form.controls.indicatorIds.hasError('required')).toBe(true);
    expect(api.create).not.toHaveBeenCalled();
  });

  it('loads indicator choices from the process and preserves selection', () => {
    configure(null);
    const fixture = TestBed.createComponent(EvidenceEditorPage);
    fixture.componentInstance.selectProcess(processId);

    expect(processes.get).toHaveBeenCalledWith(processId);
    expect(fixture.componentInstance.indicators()).toEqual([
      {id: indicatorId, code: 'IND-1', name: 'Indicador sintético'}
    ]);
    fixture.componentInstance.toggleIndicator(indicatorId, true);
    expect(fixture.componentInstance.form.controls.indicatorIds.value).toEqual([indicatorId]);
  });

  it('propagates the exact strong ETag when editing', () => {
    configure(versionId);
    const fixture = TestBed.createComponent(EvidenceEditorPage);
    fixture.detectChanges();

    fixture.componentInstance.save();

    expect(api.update).toHaveBeenCalledWith(versionId, expect.any(Object), '"0"');
    expect(fixture.componentInstance.etag()).toBe('"1"');
  });

  it('does not retry stale writes and moves keyboard focus to the recovery action', async () => {
    api.update.mockReturnValue(throwError(() => new ApiProblemError({
      type: 'urn:sec-unl:problem:stale-draft',
      title: 'Precondition Failed',
      status: 412,
      detail: 'El borrador cambió; recargue antes de continuar.',
      instance: `/evidence-versions/${versionId}`,
      code: 'STALE_DRAFT',
      correlationId: 'stale-test-1234',
      details: {}
    })));
    configure(versionId);
    const fixture = TestBed.createComponent(EvidenceEditorPage);
    fixture.detectChanges();

    fixture.componentInstance.save();
    fixture.detectChanges();
    await fixture.whenStable();

    const recovery = fixture.nativeElement.querySelector('[data-stale-reload]') as HTMLButtonElement;
    expect(api.update).toHaveBeenCalledTimes(1);
    expect(fixture.componentInstance.stale()).toBe(true);
    expect(document.activeElement).toBe(recovery);
  });

  function configure(currentVersionId: string | null): void {
    TestBed.configureTestingModule({
      imports: [EvidenceEditorPage],
      providers: [
        provideRouter([]),
        {provide: EvidenceApi, useValue: api},
        {provide: ProcessApi, useValue: processes},
        {provide: ActivatedRoute, useValue: {snapshot: {
          paramMap: convertToParamMap({versionId: currentVersionId}),
          queryParamMap: convertToParamMap({})
        }}}
      ]
    });
  }
});
