import {TestBed} from '@angular/core/testing';
import {describe, expect, it} from 'vitest';
import {EvidenceAuditPage} from './evidence-audit.page';

describe('EvidenceAuditPage', () => {
  it('renders filters and safe empty state', () => {
    TestBed.configureTestingModule({imports: [EvidenceAuditPage]});
    const fixture = TestBed.createComponent(EvidenceAuditPage);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.filters')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('.events')).not.toBeNull();
  });
});
