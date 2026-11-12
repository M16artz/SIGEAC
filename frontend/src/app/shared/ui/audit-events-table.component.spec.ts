import { ComponentFixture, TestBed } from '@angular/core/testing';
import { describe, expect, it } from 'vitest';
import { AuditEventRow, AuditEventsTableComponent } from './audit-events-table.component';

const event: AuditEventRow = {
  id: 'event-1',
  occurredAt: '2026-01-15T10:30:00Z',
  correlationId: 'corr-123',
  action: 'ASSIGNMENT_CREATED',
  objectType: 'Assignment',
  objectId: 'assignment-9',
  outcome: 'SUCCESS',
  reasonCode: null
};

function setup(): ComponentFixture<AuditEventsTableComponent> {
  TestBed.configureTestingModule({ imports: [AuditEventsTableComponent] });
  const fixture = TestBed.createComponent(AuditEventsTableComponent);
  fixture.componentInstance.events = [event];
  fixture.detectChanges();
  return fixture;
}

describe('AuditEventsTableComponent', () => {
  it('shows a simplified main row and reveals technical details on demand', () => {
    const fixture = setup();
    const headers = Array.from(
      fixture.nativeElement.querySelectorAll('thead th') as NodeListOf<HTMLTableCellElement>
    ).map((th) => th.textContent?.trim());

    expect(headers).toEqual(['Fecha', 'Actividad', 'Resultado', 'Detalles']);
    expect(fixture.nativeElement.textContent).not.toContain('corr-123');

    const button = fixture.nativeElement.querySelector('.audit-events-toggle') as HTMLButtonElement;
    expect(button.getAttribute('aria-expanded')).toBe('false');

    button.click();
    fixture.detectChanges();

    expect(button.getAttribute('aria-expanded')).toBe('true');
    expect(fixture.nativeElement.textContent).toContain('Elemento afectado');
    expect(fixture.nativeElement.textContent).toContain('assignment-9');
    expect(fixture.nativeElement.textContent).toContain('corr-123');
  });
});
