import {TestBed} from '@angular/core/testing';
import {provideRouter} from '@angular/router';
import {of} from 'rxjs';
import {beforeEach, describe, expect, it, vi} from 'vitest';
import {EvidenceApi} from '../data-access/evidence-api.service';
import {ProcessApi} from '../../processes/data-access/process-api.service';
import {EVIDENCE_ROUTES} from '../evidence.routes';
import {EvidenceListPage} from './evidence-list.page';

describe('EvidenceListPage', () => {
  const api = {list: vi.fn()};
  const processes = {list: vi.fn()};

  beforeEach(() => {
    api.list.mockReset();
    api.list.mockReturnValue(of({items: [], page: {page: 0, size: 20, totalElements: 0, totalPages: 0}}));
    processes.list.mockReset();
    processes.list.mockReturnValue(of([]));
    TestBed.configureTestingModule({
      imports: [EvidenceListPage],
      providers: [
        provideRouter(EVIDENCE_ROUTES),
        {provide: EvidenceApi, useValue: api},
        {provide: ProcessApi, useValue: processes}
      ]
    });
  });

  it('declares list, new, version detail and edit routes without reusing the placeholder page', () => {
    expect(EVIDENCE_ROUTES.map(route => route.path)).toEqual(
      ['', 'new', 'versions/:versionId', 'versions/:versionId/edit']);
    expect(EVIDENCE_ROUTES.every(route => route.component !== undefined)).toBe(true);
  });

  it('renders an accessible empty result and a route to create a draft', () => {
    const fixture = TestBed.createComponent(EvidenceListPage);
    fixture.componentRef.setInput('processId', '00000000-0000-0000-0000-000000000301');
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('h2')?.textContent).toContain('Evidencias');
    expect(element.querySelector('[role="status"]')?.textContent).toContain('No hay evidencias');
    expect(element.querySelector<HTMLAnchorElement>('a[href*="/new"]')?.textContent).toContain('Nueva evidencia');
  });
});
