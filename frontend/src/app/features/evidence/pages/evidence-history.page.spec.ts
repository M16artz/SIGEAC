import {TestBed} from '@angular/core/testing';
import {provideRouter} from '@angular/router';
import {describe, expect, it} from 'vitest';
import {EvidenceHistoryPage} from './evidence-history.page';

describe('EvidenceHistoryPage', () => {
  it('shows successor action and ordered versions', () => {
    TestBed.configureTestingModule({
      imports: [EvidenceHistoryPage],
      providers: [provideRouter([])]
    });
    const fixture = TestBed.createComponent(EvidenceHistoryPage);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-create-successor]')).not.toBeNull();
    expect(fixture.nativeElement.querySelectorAll('.timeline .version').length).toBeGreaterThan(0);
  });
});
