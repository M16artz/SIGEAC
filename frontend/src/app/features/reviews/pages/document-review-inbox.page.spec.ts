import {TestBed} from '@angular/core/testing';
import {provideRouter} from '@angular/router';
import {of} from 'rxjs';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {REVIEW_ROUTES} from '../reviews.routes';
import {ReviewApi} from '../data-access/review-api.service';
import {ProcessApi} from '../../processes/data-access/process-api.service';
import {DocumentReviewInboxPage} from './document-review-inbox.page';

describe('DocumentReviewInboxPage', () => {
  const api = {history: vi.fn(), inbox: vi.fn(), decide: vi.fn()};
  const processes = {list: vi.fn()};

  const entry = {
    versionId: '00000000-0000-0000-0000-000000000501',
    evidenceId: '00000000-0000-0000-0000-000000000401',
    processId: '00000000-0000-0000-0000-000000000301',
    careerId: '00000000-0000-0000-0000-000000000201',
    revisionNumber: 1,
    title: 'Informe institucional',
    submitterId: '00000000-0000-0000-0000-000000000101',
    submittedAt: '2026-10-20T15:00:00Z',
    indicatorIds: ['00000000-0000-0000-0000-000000000402'],
    coveredRequiredSourceIds: ['00000000-0000-0000-0000-000000000601', '00000000-0000-0000-0000-000000000602']
  };

  beforeEach(() => {
    api.history.mockReset();
    api.history.mockReturnValue(of([]));
    api.inbox.mockReset();
    api.inbox.mockReturnValue(of([]));
    api.decide.mockReset();
    api.decide.mockReturnValue(of({
      id: '00000000-0000-0000-0000-000000000c01',
      versionId: entry.versionId,
      decision: 'OBSERVED',
      justification: 'Revisión documental observada con fundamento.',
      reviewerId: '00000000-0000-0000-0000-000000000c21',
      reviewedAt: '2026-10-23T12:00:00Z',
      selfReviewException: false,
      exceptionReason: null,
      predecessorId: null,
      successorVersionId: null,
      revision: 0,
      outcomes: []
    }));
    processes.list.mockReset();
    processes.list.mockReturnValue(of([]));
    TestBed.configureTestingModule({
      imports: [DocumentReviewInboxPage],
      providers: [
        provideRouter(REVIEW_ROUTES),
        {provide: ReviewApi, useValue: api},
        {provide: ProcessApi, useValue: processes}
      ]
    });
  });

  it('keeps a single inbox route for the reviewer surface', () => {
    expect(REVIEW_ROUTES.map(route => route.path)).toEqual(['']);
    expect(REVIEW_ROUTES.every(route => route.component !== undefined)).toBe(true);
  });

  it('renders the empty bandéja message when no submissions are pending', () => {
    const fixture = TestBed.createComponent(DocumentReviewInboxPage);
    fixture.componentRef.setInput('processId', '00000000-0000-0000-0000-000000000301');
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('h2')?.textContent).toContain('Revisión documental');
    expect(element.querySelector('[role="status"]')?.textContent).toContain('No hay versiones pendientes');
  });

  it('lists pending submissions and opens the per-coverage decision form', () => {
    processes.list.mockReturnValue(of([{id: '00000000-0000-0000-0000-000000000301', code: 'P-1', name: 'Proceso'}]));
    api.history.mockReturnValue(of([]));
    api.inbox.mockReturnValue(of([entry]));

    const fixture = TestBed.createComponent(DocumentReviewInboxPage);
    fixture.componentRef.setInput('processId', '00000000-0000-0000-0000-000000000301');
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    const rows = element.querySelectorAll('tbody tr');
    expect(rows.length).toBe(1);
    expect(rows[0].textContent).toContain('Informe institucional');
    (element.querySelector('button[type="button"]') as HTMLButtonElement)?.click();
    fixture.detectChanges();
    expect(element.querySelectorAll('select').length).toBe(3);
  });
});
