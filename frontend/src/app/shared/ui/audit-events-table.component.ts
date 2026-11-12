import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input, OnChanges, SimpleChanges, signal } from '@angular/core';
import { DisplayLabelPipe } from '../labels/display-label.pipe';

/** Estructura mínima de un evento de auditoría para su presentación. */
export interface AuditEventRow {
  readonly id: string;
  readonly occurredAt: string;
  readonly correlationId: string;
  readonly action: string;
  readonly objectType: string;
  readonly objectId: string;
  readonly outcome: string;
  readonly reasonCode: string | null;
}

/**
 * Tabla compartida de auditoría. La fila principal prioriza fecha, actividad y
 * resultado; el resto de datos técnicos se consulta en una fila de detalles
 * accesible por teclado. No altera ni descarta la información del evento.
 */
@Component({
  selector: 'sec-audit-events-table',
  standalone: true,
  imports: [DatePipe, DisplayLabelPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './audit-events-table.component.html',
  styleUrl: './audit-events-table.component.scss'
})
export class AuditEventsTableComponent implements OnChanges {
  @Input({ required: true }) events: readonly AuditEventRow[] = [];

  readonly expandedEventId = signal<string | null>(null);

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['events']) {
      this.expandedEventId.set(null);
    }
  }

  isExpanded(eventId: string): boolean {
    return this.expandedEventId() === eventId;
  }

  toggleDetails(eventId: string): void {
    this.expandedEventId.update((current) => (current === eventId ? null : eventId));
  }
}
