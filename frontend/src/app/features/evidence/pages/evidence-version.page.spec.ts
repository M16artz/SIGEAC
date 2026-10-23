import {TestBed} from '@angular/core/testing';
import {ActivatedRoute, convertToParamMap, provideRouter} from '@angular/router';
import {of, throwError} from 'rxjs';
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest';
import {ApiProblemError} from '../../../core/http/api-client';
import {EvidenceApi} from '../data-access/evidence-api.service';
import {EvidenceVersionPage} from './evidence-version.page';

describe('EvidenceVersionPage', () => {
  const versionId = '00000000-0000-0000-0000-000000000501';
  const fileId = '00000000-0000-0000-0000-000000000502';
  const sha256 = 'a'.repeat(64);
  const file = {
    id: fileId,
    originalName: 'synthetic.txt',
    declaredMediaType: 'text/plain',
    detectedMediaType: 'text/plain',
    sizeBytes: 18,
    sha256,
    status: 'AVAILABLE' as const
  };
  const version = {
    id: versionId,
    evidenceId: '00000000-0000-0000-0000-000000000601',
    revisionNumber: 1,
    status: 'SUBMITTED' as const,
    title: 'Informe sintético',
    description: 'Descripción sintética',
    provenance: 'Procedencia sintética',
    coveredFrom: '2026-01-01',
    coveredTo: '2026-06-30',
    indicatorIds: ['00000000-0000-0000-0000-000000000401'],
    createdBy: '00000000-0000-0000-0000-000000000101',
    createdAt: '2026-10-20T15:00:00Z',
    submittedBy: '00000000-0000-0000-0000-000000000101',
    submittedAt: '2026-10-20T16:00:00Z',
    revision: 1,
    files: [file],
    externalReferences: []
  };
  const api = {
    getVersion: vi.fn(),
    submit: vi.fn(),
    downloadFile: vi.fn(),
    requiredSources: vi.fn(),
    coverages: vi.fn(),
    structuredSnapshots: vi.fn(),
    declareCoverage: vi.fn(),
    removeCoverage: vi.fn(),
    registerStructuredSource: vi.fn()
  };
  let anchorClick: ReturnType<typeof vi.spyOn>;
  let createObjectUrl: ReturnType<typeof vi.spyOn>;
  let revokeObjectUrl: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    vi.clearAllMocks();
    api.getVersion.mockReturnValue(of({body: version, etag: '"1"'}));
    api.requiredSources.mockReturnValue(of([]));
    api.coverages.mockReturnValue(of([]));
    api.structuredSnapshots.mockReturnValue(of([]));
    api.downloadFile.mockReturnValue(of(new Blob(['synthetic evidence'], {type: 'text/plain'})));
    anchorClick = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined);
    createObjectUrl = vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:synthetic');
    revokeObjectUrl = vi.spyOn(URL, 'revokeObjectURL').mockImplementation(() => undefined);
    TestBed.configureTestingModule({
      imports: [EvidenceVersionPage],
      providers: [
        provideRouter([]),
        {provide: EvidenceApi, useValue: api},
        {provide: ActivatedRoute, useValue: {snapshot: {paramMap: convertToParamMap({versionId})}}}
      ]
    });
  });

  afterEach(() => {
    anchorClick.mockRestore();
    createObjectUrl.mockRestore();
    revokeObjectUrl.mockRestore();
  });

  it('shows a submitted version as read-only with its files', () => {
    const fixture = TestBed.createComponent(EvidenceVersionPage);
    fixture.detectChanges();

    expect(fixture.componentInstance.version()?.status).toBe('SUBMITTED');
    expect(fixture.nativeElement.querySelector('[data-submit-version]')).toBeNull();
    expect(fixture.nativeElement.querySelector('[data-download-file]')).not.toBeNull();
  });

  it('downloads a safe Blob and announces the verified filename', () => {
    const fixture = TestBed.createComponent(EvidenceVersionPage);
    fixture.detectChanges();

    fixture.componentInstance.download(file);
    fixture.detectChanges();

    expect(api.downloadFile).toHaveBeenCalledWith(versionId, fileId);
    expect(createObjectUrl).toHaveBeenCalled();
    expect(anchorClick).toHaveBeenCalled();
    expect(revokeObjectUrl).toHaveBeenCalledWith('blob:synthetic');
    expect(fixture.componentInstance.message()).toContain('synthetic.txt');
  });

  it('does not deliver bytes when integrity verification fails', () => {
    api.downloadFile.mockReturnValue(throwError(() => new ApiProblemError({
      type: 'urn:sec-unl:problem:file-integrity',
      title: 'Conflict',
      status: 409,
      detail: 'El archivo no supera la verificación.',
      instance: `/evidence-versions/${versionId}/files/${fileId}/content`,
      code: 'FILE_INTEGRITY_FAILED',
      correlationId: 'integrity-test-1234',
      details: {}
    })));
    const fixture = TestBed.createComponent(EvidenceVersionPage);
    fixture.detectChanges();

    fixture.componentInstance.download(file);
    fixture.detectChanges();

    expect(anchorClick).not.toHaveBeenCalled();
    expect(fixture.componentInstance.error()).toContain('integridad');
  });
});
