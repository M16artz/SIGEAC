import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ModelApi } from '../data-access/model-api.service';
import { AuditEventView } from '../../access/data-access/access.models';
import { AuditEventsTableComponent } from '../../../shared/ui/audit-events-table.component';

@Component({
  selector: 'sec-model-audit',
  standalone: true,
  imports: [FormsModule, AuditEventsTableComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="sigeac-page">
      <header class="sigeac-page-header">
        <span class="sigeac-badge">Modelos</span>
        <h2>Auditoría de modelos</h2>
        <p>Consulte los eventos registrados de los modelos y sus versiones.</p>
      </header>
      <section class="sigeac-page-body">
        <div class="sigeac-form-search">
          <div class="sigeac-form-field">
            <label for="audit-version">Versión del modelo</label>
            <input id="audit-version" name="auditVersion" [(ngModel)]="versionId" placeholder="Todas las versiones">
          </div>
          <button type="button" (click)="load()">Consultar</button>
        </div>
        <p class="sigeac-help">El filtro por versión es opcional.</p>

        @if (events().length > 0) {
          <sec-audit-events-table [events]="events()" />
        } @else {
          <p role="status">No hay eventos de auditoría para el filtro seleccionado.</p>
        }
      </section>
    </div>
  `
})
export class ModelAuditPage {
  versionId = '';
  readonly events = signal<readonly AuditEventView[]>([]);

  constructor(private readonly api: ModelApi) {}

  load(): void {
    this.api.audit(this.versionId || undefined).subscribe((value) => this.events.set(value.items));
  }
}
